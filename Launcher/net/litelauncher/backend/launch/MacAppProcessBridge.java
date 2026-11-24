package net.litelauncher.backend.launch;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.CancellationToken;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.LauncherPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CancellationException;

/** Launch Services owns the app; its entry point execs Java without changing the game PID. */
final class MacAppProcessBridge {
    private static final String MARKER = "LiteLauncher Minecraft application v1\n";

    private MacAppProcessBridge() { }

    static ProcessHandle start(ProcessBuilder original, String versionId, CancellationToken cancellation) throws Exception {
        Path java = Path.of(original.command().getFirst()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(java) || !Files.isExecutable(java)) throw new IOException("Minecraft Java is not executable.");
        String version = versionId.replaceAll("[^\\p{L}\\p{N} ._+\\-]", "_");
        if (version.isBlank()) version = "unknown";
        if (version.length() > 80) version = version.substring(0, 80);
        String name = "Minecraft " + version;
        String id = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((versionId + "\0" + java).getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        Path app = Path.of(System.getProperty("user.home"), "Applications", "LiteLauncher Games", id, name + ".app");
        ensureApp(app, java, name, id);

        Path work = original.directory().toPath().toAbsolutePath().normalize();
        Path logs = work.resolve("logs");
        Files.createDirectories(logs);
        Path log = logs.resolve("litelauncher-game.log");
        Path data = LauncherPaths.launcherDataDirectory();
        Files.createDirectories(data);
        Path handshake = Files.createTempFile(data, "mac-game-", ".pid");
        List<String> command = new ArrayList<>(List.of("/usr/bin/open", "-n", "-W", "-a", app.toString()));
        original.environment().forEach((key, value) -> { command.add("--env"); command.add(key + "=" + value); });
        command.addAll(List.of("--args", work.toString(), handshake.toString(), log.toString()));
        command.addAll(original.command().subList(1, original.command().size()));
        Process opener = null;
        ProcessHandle game = null;
        boolean started = false;
        Instant earliest = Instant.now().minusSeconds(2);
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        try {
            opener = new ProcessBuilder(command).directory(original.directory()).inheritIO().start();
            while (System.nanoTime() < deadline) {
                checkCancelled(cancellation);
                String value = Files.readString(handshake).trim();
                if (value.matches("[0-9]+")) {
                    game = ProcessHandle.of(Long.parseLong(value)).orElseThrow(() -> new IOException("Minecraft exited during startup. See " + log));
                    if (game.pid() == opener.pid() || game.pid() == ProcessHandle.current().pid()
                            || game.info().startInstant().filter(time -> !time.isBefore(earliest)).isEmpty())
                        throw new IOException("Invalid Minecraft process identity.");
                    Files.writeString(handshake, "run\n");
                    break;
                }
                if (!opener.isAlive()) throw new IOException("macOS could not open Minecraft. See " + log);
                Thread.sleep(20);
            }
            if (game == null) throw new IOException("macOS did not report the Minecraft process.");
            while (Files.exists(handshake) && System.nanoTime() < deadline) {
                checkCancelled(cancellation);
                if (!game.isAlive()) throw new IOException("Minecraft exited during startup. See " + log);
                Thread.sleep(20);
            }
            checkCancelled(cancellation);
            if (Files.exists(handshake)) throw new IOException("Minecraft startup acknowledgement timed out.");
            Process wait = opener;
            game.onExit().thenRun(() -> {
                wait.destroy();
                LauncherLog.info("Minecraft process exited for " + versionId);
            });
            LauncherLog.info("Minecraft macOS application started; pid=" + game.pid() + "; output: " + log);
            started = true;
            return game;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Minecraft startup cancelled.");
        } finally {
            try {
                Files.deleteIfExists(handshake);
            } finally {
                if (!started) {
                    if (game != null && game.isAlive()) {
                        game.destroyForcibly();
                        game.onExit().join();
                    }
                    if (opener != null) opener.destroyForcibly();
                }
            }
        }
    }

    private static void ensureApp(Path app, Path java, String name, String id) throws IOException {
        Path contents = app.resolve("Contents");
        Path resources = contents.resolve("Resources");
        Path macos = contents.resolve("MacOS");
        Path marker = resources.resolve("litelauncher.marker");
        if (Files.exists(app, LinkOption.NOFOLLOW_LINKS)
                && (!Files.isDirectory(app, LinkOption.NOFOLLOW_LINKS)
                || !Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS) || !Files.readString(marker).equals(MARKER)))
            throw new IOException("Refusing to replace an application not created by LiteLauncher: " + app);
        for (Path directory : List.of(contents, resources, macos)) {
            if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Invalid Minecraft application directory: " + directory);
            Files.createDirectories(directory);
        }
        write(marker, MARKER);
        String plist = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0"><dict>
                <key>CFBundleExecutable</key><string>Minecraft</string>
                <key>CFBundleIdentifier</key><string>local.litelauncher.minecraft.%s</string>
                <key>CFBundleName</key><string>Minecraft</string>
                <key>CFBundleDisplayName</key><string>%s</string>
                <key>CFBundlePackageType</key><string>APPL</string>
                <key>CFBundleVersion</key><string>1</string>
                <key>NSHighResolutionCapable</key><true/>
                </dict></plist>
                """.formatted(id, xml(name));
        write(contents.resolve("Info.plist"), plist);
        // The PID-only handshake prevents a cancelled/delayed Launch Services request from starting Java.
        String entry = """
                #!/bin/sh
                work=$1
                handshake=$2
                log=$3
                shift 3
                [ -f "$handshake" ] || exit 1
                cd "$work" || exit 1
                exec >>"$log" 2>&1
                trap 'rm -f "$handshake"' EXIT
                printf '%s\\n' "$$" >"$handshake" || exit 1
                count=0
                while [ "$count" -lt 750 ] && [ -f "$handshake" ]; do
                    IFS= read -r state <"$handshake"
                    if [ "$state" = run ]; then
                        rm -f "$handshake"
                        exec JAVA_BINARY "$@"
                        exit 1
                    fi
                    count=$((count + 1))
                    sleep 0.02
                done
                exit 1
                """.replace("JAVA_BINARY", "'" + java.toString().replace("'", "'\"'\"'") + "'");
        Path executable = macos.resolve("Minecraft");
        write(executable, entry);
        if (!executable.toFile().setExecutable(true, false)) throw new IOException("Unable to make Minecraft application executable.");
    }

    private static void write(Path path, String text) throws IOException {
        if (Files.isSymbolicLink(path)) throw new IOException("Invalid Minecraft application file: " + path);
        if (!Files.exists(path) || !Files.readString(path).equals(text)) BackendUtils.writeAtomic(path, text);
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void checkCancelled(CancellationToken cancellation) {
        if (cancellation != null) cancellation.throwIfCancelled();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Minecraft startup cancelled.");
    }
}
