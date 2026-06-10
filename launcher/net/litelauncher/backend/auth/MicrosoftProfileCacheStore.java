package net.litelauncher.backend.auth;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

final class MicrosoftProfileCacheStore {

    private static final int VERSION = 1;

    MicrosoftProfileCache read(String profileId) {
        Path file = cacheFile(profileId);
        if (file == null || !Files.isRegularFile(file)) return null;

        try {
            JsonObject json = JsonParser.object().from(Files.readString(file, StandardCharsets.UTF_8));
            if (json.getInt("version", 0) != VERSION) {
                AuthUtils.deleteQuietly(file);
                return null;
            }
            return new MicrosoftProfileCache(
                    json.getString("playerName"),
                    json.getString("skinPng"),
                    json.getBoolean("slim", false),
                    readCapes(json.getArray("capes", new JsonArray())),
                    AuthUtils.instant(json.getString("savedAt"))
            );
        } catch (Exception exception) {
            LauncherLog.error("Unable to read Microsoft profile cache: " + file, exception);
            AuthUtils.deleteQuietly(file);
            return null;
        }
    }

    void save(String profileId, MicrosoftProfileCache cache) throws AuthException {
        if (cache == null || !AuthUtils.hasText(profileId)) return;
        Path file = cacheFile(profileId);
        if (file == null) return;

        try {
            JsonObject json = new JsonObject();
            json.put("version", VERSION);
            json.put("playerName", cache.playerName());
            json.put("skinPng", cache.skinPng());
            json.put("slim", cache.slim());
            json.put("capes", writeCapes(cache.capes()));
            json.put("savedAt", (cache.savedAt() == null ? Instant.now() : cache.savedAt()).toString());
            AuthUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(json).done());
        } catch (Exception exception) {
            throw new AuthException("Unable to save Microsoft profile cache.", exception);
        }
    }

    void delete(String profileId) {
        Path file = cacheFile(profileId);
        if (file != null) AuthUtils.deleteQuietly(file);
    }

    void prune(Set<String> keep) {
        try {
            Files.createDirectories(OSUtils.microsoftProfileCacheDirectory());
            Set<String> allowed = new HashSet<>();
            if (keep != null) {
                for (String id : keep) {
                    String name = cacheFileName(id);
                    if (name != null) allowed.add(name);
                }
            }
            try (Stream<Path> files = Files.list(OSUtils.microsoftProfileCacheDirectory())) {
                files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".json"))
                        .filter(file -> !allowed.contains(file.getFileName().toString()))
                        .forEach(AuthUtils::deleteQuietly);
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to prune Microsoft profile cache.", exception);
        }
    }

    void ensureDirectory() {
        try {
            Files.createDirectories(OSUtils.microsoftProfileCacheDirectory());
        } catch (Exception exception) {
            LauncherLog.error("Unable to prepare Microsoft profile cache directory.", exception);
        }
    }

    private Path cacheFile(String profileId) {
        String name = cacheFileName(profileId);
        return name == null ? null : OSUtils.microsoftProfileCacheDirectory().resolve(name);
    }

    private String cacheFileName(String profileId) {
        String id = normalizedProfileId(profileId);
        return id == null ? null : id + ".json";
    }

    private String normalizedProfileId(String profileId) {
        String id = AuthUtils.text(profileId);
        if (id == null) return null;
        if (id.startsWith("microsoft:")) id = id.substring("microsoft:".length());
        id = id.trim().toLowerCase(Locale.ROOT).replace("-", "");
        if (id.matches("[0-9a-f]{32}")) {
            return id.substring(0, 8) + '-' + id.substring(8, 12) + '-' + id.substring(12, 16) + '-'
                    + id.substring(16, 20) + '-' + id.substring(20);
        }
        return id.replaceAll("[^a-z0-9._-]", "_");
    }

    private List<MinecraftCape> readCapes(JsonArray items) {
        ArrayList<MinecraftCape> capes = new ArrayList<>();
        if (items == null) return List.of();
        for (Object item : items) {
            if (!(item instanceof JsonObject json)) continue;
            MinecraftCape cape = new MinecraftCape(
                    json.getString("id"),
                    AuthUtils.firstText(json.getString("name"), json.getString("alias")),
                    json.getString("png"),
                    json.getBoolean("active", false),
                    AuthUtils.firstText(json.getString("textureUrl"), json.getString("url"))
            );
            if (cape.id() != null) capes.add(cape);
        }
        return List.copyOf(capes);
    }

    private JsonArray writeCapes(List<MinecraftCape> capes) {
        JsonArray items = new JsonArray();
        if (capes == null) return items;
        for (MinecraftCape cape : capes) {
            if (cape == null || cape.id() == null) continue;
            JsonObject json = new JsonObject();
            json.put("id", cape.id());
            json.put("name", cape.name());
            json.put("png", cape.png());
            json.put("active", cape.active());
            json.put("url", cape.textureUrl());
            items.add(json);
        }
        return items;
    }
}
