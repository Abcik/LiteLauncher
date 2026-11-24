package net.litelauncher.backend.auth;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;

import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/** Serializes account operations across threads and launcher processes. */
final class MicrosoftAccountLock implements AutoCloseable {

    private static final ConcurrentHashMap<Path, Semaphore> LOCKS = new ConcurrentHashMap<>();

    private final Semaphore local;
    private final FileChannel channel;
    private final Path gamesFile;
    private boolean closed;

    private MicrosoftAccountLock(Semaphore local, FileChannel channel, Path gamesFile) {
        this.local = local;
        this.channel = channel;
        this.gamesFile = gamesFile;
    }

    static MicrosoftAccountLock account(String profileId) throws AuthException {
        return account(profileId, true);
    }

    static MicrosoftAccountLock tryAccount(String profileId) throws AuthException {
        return account(profileId, false);
    }

    private static MicrosoftAccountLock account(String profileId, boolean wait) throws AuthException {
        String id = profileId.replace("microsoft:", "").replace("-", "");
        if (!id.matches("[0-9a-fA-F]{32}")) throw new AuthException("Invalid Microsoft profile id.");
        Path file = LauncherPaths.authDirectory().resolve("locks").resolve(id.toLowerCase(java.util.Locale.ROOT) + ".lock");
        return acquire(file, file.resolveSibling(file.getFileName() + ".games.json"), wait);
    }

    static MicrosoftAccountLock sessions() throws AuthException {
        return acquire(LauncherPaths.authDirectory().resolve("microsoft-sessions.lock"), null, true);
    }

    private static MicrosoftAccountLock acquire(Path file, Path gamesFile, boolean wait) throws AuthException {
        Path path = file.toAbsolutePath().normalize();
        Semaphore local = LOCKS.computeIfAbsent(path, _ -> new Semaphore(1, true));
        FileChannel channel = null;
        boolean acquired = false;
        try {
            if (wait) local.acquire();
            else if (!local.tryAcquire()) return null;
            acquired = true;
            Files.createDirectories(path.getParent());
            channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            if (wait) channel.lock();
            else if (channel.tryLock() == null) {
                channel.close();
                local.release();
                return null;
            }
            return new MicrosoftAccountLock(local, channel, gamesFile);
        } catch (Exception exception) {
            if (channel != null) try { channel.close(); } catch (Exception ignored) { }
            if (acquired) local.release();
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new AuthException("Unable to lock Microsoft session.", exception);
        }
    }

    boolean inUse() throws AuthException {
        return !runningGames().isEmpty();
    }

    void gameStarted(ProcessHandle process) throws AuthException {
        JsonArray games = runningGames();
        JsonObject game = new JsonObject();
        game.put("pid", process.pid());
        game.put("startedAt", process.info().startInstant().map(Object::toString).orElse(""));
        games.add(game);
        JsonObject root = new JsonObject();
        root.put("games", games);
        try {
            BackendUtils.writeAtomic(gamesFile, JsonWriter.string().value(root).done());
        } catch (Exception exception) {
            throw new AuthException("Unable to remember running Minecraft account.", exception);
        }
    }

    private JsonArray runningGames() throws AuthException {
        JsonArray result = new JsonArray();
        if (gamesFile == null || !Files.exists(gamesFile)) return result;
        try {
            JsonArray games = JsonParser.object().from(Files.readString(gamesFile, StandardCharsets.UTF_8))
                    .getArray("games", new JsonArray());
            for (Object item : games) {
                if (!(item instanceof JsonObject game)) continue;
                ProcessHandle process = ProcessHandle.of(game.getLong("pid", -1)).orElse(null);
                if (process == null || !process.isAlive()) continue;
                String expected = game.getString("startedAt", "");
                String actual = process.info().startInstant().map(Object::toString).orElse("");
                if (expected.isEmpty() || actual.isEmpty() || expected.equals(actual)) result.add(game);
            }
            return result;
        } catch (Exception exception) {
            // Do not rotate a potentially active game token if its usage cannot be checked.
            throw new AuthException("Unable to check running Minecraft account.", exception);
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        // Closing the channel also releases its file lock.
        try { channel.close(); } catch (Exception ignored) { }
        local.release();
    }
}
