package net.litelauncher.backend.platform;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class OSUtils {

    public static Path minecraftDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) return Path.of(appData, ".minecraft");
            return Path.of(home, "AppData", "Roaming", ".minecraft");
        }

        if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "minecraft");
        return Path.of(home, ".minecraft");
    }

    public static Path logsDirectory() {
        return minecraftDirectory().resolve("logs");
    }

    public static Path liteLauncherDirectory() {
        return minecraftDirectory().resolve("litelauncher");
    }

    public static Path javaDirectory() {
        return liteLauncherDirectory().resolve("java");
    }

    public static Path javaRuntimeDirectory(String runtimeId) {
        return javaDirectory().resolve(runtimeId == null || runtimeId.isBlank() ? "jre-8" : runtimeId);
    }

    public static Path bootstrapDirectory() {
        return liteLauncherDirectory().resolve("bootstrap");
    }

    public static Path launcherDirectory() {
        return bootstrapDirectory().resolve("launcher");
    }

    public static Path desktopDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win") || os.contains("mac")) return Path.of(System.getProperty("user.home", "."), "Desktop");

        String desktop = readXdgDesktopDirectory();
        if (desktop != null && !desktop.isBlank()) return Path.of(desktop);
        return Path.of(System.getProperty("user.home", "."), "Desktop");
    }

    public static boolean isWindows() {
        return os().contains("win");
    }

    public static boolean isMac() {
        return os().contains("mac");
    }

    public static boolean isLinux() {
        return os().contains("linux");
    }

    public static String os() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    public static String arch() {
        return System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    }

    public static void openFile(Path file) throws IOException {
        if (file == null) throw new IOException("File path is empty.");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file.toFile());
            return;
        }

        openWithSystemCommand(file.toString());
    }

    private static void openWithSystemCommand(String target) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder builder;

        if (os.contains("mac")) builder = new ProcessBuilder("open", target);
        else if (os.contains("win")) builder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", target);
        else builder = new ProcessBuilder("xdg-open", target);

        builder.start();
    }

    public static void makeExecutable(Path file) {
        try {
            if (file == null || isWindows() || !Files.exists(file)) return;
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
            );
            Files.setPosixFilePermissions(file, permissions);
        } catch (Exception ignored) {
        }
    }

    public static void deleteDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) return;
        try (var stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    public static void deleteQuietly(Path path) {
        try {
            if (Files.isDirectory(path)) deleteDirectory(path);
            else Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    public static String bash(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readXdgDesktopDirectory() {
        Path config = Path.of(System.getProperty("user.home", "."), ".config", "user-dirs.dirs");
        if (!Files.isRegularFile(config)) return null;

        try {
            String home = System.getProperty("user.home", ".");
            for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (!line.startsWith("XDG_DESKTOP_DIR=")) continue;
                int first = line.indexOf('"');
                int second = first < 0 ? -1 : line.indexOf('"', first + 1);
                if (first < 0 || second <= first) continue;
                return line.substring(first + 1, second).replace("$HOME", home);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

}
