package net.litelauncher.backend.discord;

import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.backend.version.Version;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscordRpcService {

    static final String LAUNCHER_ACTIVITY = "LiteLauncher";

    private final Path stateFile = LauncherPaths.launcherDataDirectory().resolve("discord-rpc.json");
    private final Map<Long, String> games = new LinkedHashMap<>();
    private final long launcherPid = ProcessHandle.current().pid();
    private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final Thread worker = Thread.ofVirtual().name("LiteLauncher Discord RPC").start(this::workLoop);
    private final AtomicBoolean shutdownStarted = new AtomicBoolean(false);

    private Process companionProcess;
    private boolean closed;

    public void showLauncher() {
        submit(this::showLauncherNow);
    }

    public void showGame(Process process, Version version) {
        if (process == null) return;
        submit(() -> showGameNow(process, version));
    }

    public void hideGame(Process process) {
        if (process == null) return;
        submit(() -> hideGameNow(process));
    }

    public void shutdown() {
        if (!shutdownStarted.compareAndSet(false, true)) return;

        CountDownLatch completed = new CountDownLatch(1);
        tasks.offer(() -> {
            try {
                shutdownNow();
            } finally {
                completed.countDown();
            }
        });
        try {
            if (!completed.await(5L, TimeUnit.SECONDS)) LauncherLog.error("Timed out while shutting down Discord RPC service.", null);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } finally {
            worker.interrupt();
        }
    }

    private void submit(Runnable task) {
        if (!shutdownStarted.get() && task != null) tasks.offer(task);
    }

    private void workLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                tasks.take().run();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } catch (Throwable exception) {
                LauncherLog.error("Discord RPC task failed.", exception);
            }
        }
    }

    private void showLauncherNow() {
        if (!closed) publish(launcherPid, 0L, LAUNCHER_ACTIVITY);
    }

    private void showGameNow(Process process, Version version) {
        if (closed) return;
        games.put(process.pid(), activity(version));
        publishCurrent();
    }

    private void hideGameNow(Process process) {
        games.remove(process.pid());
        if (!closed) publishCurrent();
    }

    private void shutdownNow() {
        if (closed) return;
        closed = true;

        Long gamePid = currentGamePid();
        if (gamePid == null) {
            write(0L, 0L, LAUNCHER_ACTIVITY, true);
            return;
        }

        if (write(0L, gamePid, games.get(gamePid), false)) ensureCompanion();
    }

    private void publishCurrent() {
        Long gamePid = currentGamePid();
        if (gamePid == null) publish(launcherPid, 0L, LAUNCHER_ACTIVITY);
        else publish(launcherPid, gamePid, games.get(gamePid));
    }

    private Long currentGamePid() {
        games.keySet().removeIf(pid -> ProcessHandle.of(pid).filter(ProcessHandle::isAlive).isEmpty());

        Long last = null;
        for (Long pid : games.keySet()) last = pid;
        return last;
    }

    private void publish(long launcherPid, long gamePid, String details) {
        if (write(launcherPid, gamePid, details, false)) ensureCompanion();
    }

    private void ensureCompanion() {
        if (companionProcess != null && companionProcess.isAlive()) return;
        try {
            ProcessBuilder builder = new ProcessBuilder(companionCommand());
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            companionProcess = builder.start();
            LauncherLog.info("Discord RPC companion spawned.");
        } catch (Exception exception) {
            LauncherLog.error("Unable to spawn Discord RPC companion.", exception);
        }
    }

    private List<String> companionCommand() {
        List<String> command = new ArrayList<>();
        command.add(javaCommand());
        command.add("-cp");
        command.add(System.getProperty("java.class.path", "."));
        command.add(DiscordRpcCompanion.class.getName());
        command.add(stateFile.toString());
        return command;
    }

    private String javaCommand() {
        String executable = OSUtils.os().windows() ? "javaw.exe" : "java";
        Path java = Path.of(System.getProperty("java.home", "."), "bin", executable);
        if (Files.isRegularFile(java)) return java.toString();

        Path fallback = Path.of(System.getProperty("java.home", "."), "bin", "java");
        return Files.isRegularFile(fallback) ? fallback.toString() : "java";
    }

    private boolean write(long launcherPid, long gamePid, String details, boolean shutdown) {
        try {
            Files.createDirectories(stateFile.getParent());

            JsonObject json = new JsonObject();
            json.put("launcherPid", Math.max(0L, launcherPid));
            json.put("gamePid", Math.max(0L, gamePid));
            json.put("details", safe(details));
            json.put("shutdown", shutdown);

            BackendUtils.writeAtomic(stateFile, JsonWriter.string().value(json).done());
            return true;
        } catch (Exception exception) {
            LauncherLog.error("Unable to write Discord RPC state.", exception);
            return false;
        }
    }

    private String activity(Version version) {
        return version == null ? "Minecraft" : safe(version.title());
    }

    static String safe(String details) {
        String value = details == null || details.isBlank() ? LAUNCHER_ACTIVITY : details.trim();
        return value.length() <= 128 ? value : value.substring(0, 128);
    }
}
