package net.litelauncher.backend.discord;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.backend.LauncherLog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiscordRpcCompanion {

    private static final long POLL_DELAY_MS = 1_000L;
    private static final long REFRESH_DELAY_MS = 15_000L;
    private static final long MISSING_STATE_TIMEOUT_MS = 60_000L;

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            LauncherLog.info("Discord RPC companion stopped: state file argument is missing.");
            return;
        }
        run(Path.of(args[0]));
    }

    private static void run(Path stateFile) {
        LauncherLog.info("Discord RPC companion started.");

        try (DiscordRpcClient client = new DiscordRpcClient()) {
            long rpcPid = ProcessHandle.current().pid();
            String activeDetails = null;
            long lastRefresh = 0L;
            long missingStateSince = 0L;
            while (!Thread.currentThread().isInterrupted()) {
                State state = State.read(stateFile);
                if (state == null) {
                    long now = System.currentTimeMillis();
                    if (missingStateSince == 0L) missingStateSince = now;
                    if (now - missingStateSince >= MISSING_STATE_TIMEOUT_MS) {
                        LauncherLog.info("Discord RPC companion stopped: state file was not available.");
                        return;
                    }
                    sleep();
                    continue;
                }
                missingStateSince = 0L;

                if (state.shutdown()) {
                    client.clearActivity(rpcPid);
                    return;
                }

                String details = state.activeDetails();
                if (details == null) {
                    client.clearActivity(rpcPid);
                    return;
                }

                long now = System.currentTimeMillis();
                if (!details.equals(activeDetails) || now - lastRefresh >= REFRESH_DELAY_MS) {
                    client.setActivity(rpcPid, details);
                    activeDetails = details;
                    lastRefresh = now;
                }

                sleep();
            }
        } finally {
            LauncherLog.info("Discord RPC companion stopped.");
        }
    }

    private static boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private static void sleep() {
        try {
            Thread.sleep(DiscordRpcCompanion.POLL_DELAY_MS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private record State(long launcherPid, long gamePid, String details, boolean shutdown) {
        static State read(Path file) {
            try {
                if (file == null || !Files.isRegularFile(file)) return null;
                JsonObject json = JsonParser.object().from(Files.readString(file, StandardCharsets.UTF_8));
                return new State(
                        longValue(json, "launcherPid"),
                        longValue(json, "gamePid"),
                        DiscordRpcService.safe(json.getString("details", DiscordRpcService.LAUNCHER_ACTIVITY)),
                        json.getBoolean("shutdown", false)
                );
            } catch (Exception _) {
                return null;
            }
        }

        String activeDetails() {
            if (gamePid > 0L && isAlive(gamePid)) return details;
            if (launcherPid > 0L && isAlive(launcherPid)) return DiscordRpcService.LAUNCHER_ACTIVITY;
            return null;
        }

        private static long longValue(JsonObject json, String key) {
            Object value = json.get(key);
            if (value instanceof Number number) return number.longValue();
            if (value instanceof String string) {
                try {
                    return Long.parseLong(string);
                } catch (NumberFormatException _) {
                }
            }
            return 0L;
        }
    }
}
