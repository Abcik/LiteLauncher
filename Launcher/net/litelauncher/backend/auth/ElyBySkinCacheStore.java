package net.litelauncher.backend.auth;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

final class ElyBySkinCacheStore {

    private static final int VERSION = 1;
    private static final long MAX_CACHE_BYTES = 512L * 1024L;

    ElyBySkin read(String profileId) {
        Path file = cacheFile(profileId);
        if (file == null || !Files.isRegularFile(file)) return null;

        try {
            long size = Files.size(file);
            if (size <= 0L || size > MAX_CACHE_BYTES) throw new IllegalStateException("Invalid Ely.by skin cache size.");
            JsonObject json = JsonParser.object().from(Files.readString(file, StandardCharsets.UTF_8));
            if (json.getInt("version", 0) != VERSION) {
                BackendUtils.deleteQuietly(file);
                return null;
            }
            String png = AuthUtils.text(json.getString("skinPng"));
            if (png == null) return null;
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(png)));
            if (!validSkin(image)) throw new IllegalStateException("Invalid cached Ely.by skin image.");
            return new ElyBySkin(image, json.getBoolean("slim", false));
        } catch (Exception exception) {
            LauncherLog.error("Unable to read Ely.by skin cache: " + file, exception);
            BackendUtils.deleteQuietly(file);
            return null;
        }
    }

    void save(String profileId, ElyBySkin skin) {
        Path file = cacheFile(profileId);
        if (file == null || skin == null || !validSkin(skin.image())) return;

        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            if (!ImageIO.write(skin.image(), "png", png)) throw new IllegalStateException("PNG encoder is unavailable.");

            JsonObject json = new JsonObject();
            json.put("version", VERSION);
            json.put("skinPng", Base64.getEncoder().encodeToString(png.toByteArray()));
            json.put("slim", skin.slim());
            BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(json).done());
        } catch (Exception exception) {
            LauncherLog.error("Unable to save Ely.by skin cache: " + file, exception);
        }
    }

    void delete(String profileId) {
        Path file = cacheFile(profileId);
        if (file != null) BackendUtils.deleteQuietly(file);
    }

    void prune(List<Profile> profiles) {
        try {
            Files.createDirectories(LauncherPaths.elyBySkinCacheDirectory());
            Set<String> keep = new HashSet<>();
            if (profiles != null) {
                for (Profile profile : profiles) {
                    if (profile == null || profile.microsoft()) continue;
                    String name = cacheFileName(profile.id());
                    if (name != null) keep.add(name);
                }
            }

            try (Stream<Path> files = Files.list(LauncherPaths.elyBySkinCacheDirectory())) {
                files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".json"))
                        .filter(file -> !keep.contains(file.getFileName().toString()))
                        .forEach(BackendUtils::deleteQuietly);
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to prune Ely.by skin cache.", exception);
        }
    }

    private Path cacheFile(String profileId) {
        String name = cacheFileName(profileId);
        return name == null ? null : LauncherPaths.elyBySkinCacheDirectory().resolve(name);
    }

    private String cacheFileName(String profileId) {
        String id = AuthUtils.text(profileId);
        if (id == null) return null;
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_") + ".json";
    }

    private boolean validSkin(BufferedImage image) {
        return image != null && image.getWidth() == 64 && image.getHeight() == 64;
    }
}
