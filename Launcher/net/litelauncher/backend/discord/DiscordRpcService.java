package net.litelauncher.backend.discord;

import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;
import net.litelauncher.backend.version.Version;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** RPC belongs to this launcher process and ends with it. */
public final class DiscordRpcService {
    static final String LAUNCHER_ACTIVITY = "LiteLauncher";
    private final Map<ProcessHandle, String> games = new LinkedHashMap<>();
    private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final DiscordRpcClient client = new DiscordRpcClient();
    private final long pid = ProcessHandle.current().pid();
    private final Thread worker = Thread.ofVirtual().name("LiteLauncher Discord RPC").start(this::workLoop);

    public DiscordRpcService() {
        // Retire a companion left running by a previous launcher version.
        var legacy = LauncherPaths.launcherDataDirectory().resolve("discord-rpc.json");
        try {
            if (Files.isRegularFile(legacy)) BackendUtils.writeAtomic(legacy, "{\"shutdown\":true}");
        } catch (Exception exception) {
            LauncherLog.error("Unable to retire legacy Discord RPC companion.", exception);
        }
    }

    public void showLauncher() { submit(this::publish); }

    public void showGame(ProcessHandle process, Version version) {
        if (process == null) return;
        submit(() -> {
            games.put(process, version == null ? "Minecraft" : safe(version.title()));
            publish();
        });
    }

    public void hideGame(ProcessHandle process) {
        submit(() -> {
            games.remove(process);
            publish();
        });
    }

    private void submit(Runnable task) {
        if (!closed.get()) tasks.offer(task);
    }

    private void workLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Runnable task = tasks.poll(15, TimeUnit.SECONDS);
                if (task != null) task.run();
                else if (!closed.get()) publish();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            LauncherLog.error("Discord RPC failed.", exception);
        } finally {
            client.close();
        }
    }

    private void publish() {
        games.keySet().removeIf(process -> !process.isAlive());
        String details = LAUNCHER_ACTIVITY;
        for (String activity : games.values()) details = activity;
        client.setActivity(pid, details);
    }

    public void shutdown() {
        if (!closed.compareAndSet(false, true)) return;
        CountDownLatch done = new CountDownLatch(1);
        tasks.offer(() -> {
            try { client.clearActivity(pid); }
            finally { done.countDown(); }
        });
        try { done.await(2, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        finally { worker.interrupt(); }
    }

    static String safe(String details) {
        String value = details == null || details.isBlank() ? LAUNCHER_ACTIVITY : details.trim();
        return value.length() <= 128 ? value : value.substring(0, 128);
    }
}
