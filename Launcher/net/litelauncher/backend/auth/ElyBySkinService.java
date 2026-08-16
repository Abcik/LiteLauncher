package net.litelauncher.backend.auth;

import net.litelauncher.backend.BackendUtils;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ElyBySkinService {

    private static final String TEXTURES_URL = "https://skinsystem.ely.by/textures/";
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private final ElyBySkinCacheStore cacheStore = new ElyBySkinCacheStore();
    private final Map<String, Optional<ElyBySkin>> memoryCache = new ConcurrentHashMap<>();
    private final HttpClient http = BackendUtils.http1();

    public ElyBySkin cached(Profile profile) {
        if (!eligible(profile)) return null;
        return memoryCache.computeIfAbsent(profile.id(), id -> Optional.ofNullable(cacheStore.read(id))).orElse(null);
    }

    public ElyBySkin refresh(Profile profile) {
        if (!eligible(profile)) return null;
        ElyBySkin fallback = cached(profile);

        try {
            JsonObject textures = fetchTextures(profile.username());
            if (textures == null) {
                memoryCache.put(profile.id(), Optional.empty());
                cacheStore.delete(profile.id());
                LauncherLog.info("Ely.by skin was not found for " + profile.username() + ".");
                return null;
            }

            JsonObject skinJson = textures.getObject("SKIN");
            String url = skinJson == null ? null : AuthUtils.text(skinJson.getString("url"));
            if (url == null) {
                memoryCache.put(profile.id(), Optional.empty());
                cacheStore.delete(profile.id());
                LauncherLog.info("Ely.by skin was not found for " + profile.username() + ".");
                return null;
            }

            BufferedImage image = fetchSkin(url);
            boolean slim = "slim".equalsIgnoreCase(skinJson.getObject("metadata", new JsonObject()).getString("model", ""));
            ElyBySkin skin = new ElyBySkin(image, slim);
            memoryCache.put(profile.id(), Optional.of(skin));
            cacheStore.save(profile.id(), skin);
            LauncherLog.info("Ely.by skin updated for " + profile.username() + ": slim=" + slim);
            return skin;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            LauncherLog.info("Ely.by skin update was interrupted for " + profile.username() + ".");
            return fallback;
        } catch (Exception exception) {
            LauncherLog.error("Unable to update Ely.by skin for " + profile.username() + "; cached skin will be used.", exception);
            return fallback;
        }
    }

    public void prune(List<Profile> profiles) {
        cacheStore.prune(profiles);
        if (profiles == null) {
            memoryCache.clear();
            return;
        }
        memoryCache.keySet().removeIf(id -> profiles.stream().noneMatch(profile -> profile != null && id.equals(profile.id())));
    }

    private JsonObject fetchTextures(String username) throws Exception {
        String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8).replace("+", "%20");
        URI uri = URI.create(TEXTURES_URL + encoded);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "LiteLauncher/" + LauncherState.LAUNCHER_VERSION)
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 204 || response.statusCode() == 404) return null;
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("HTTP " + response.statusCode());
        byte[] body = response.body();
        if (body == null || body.length == 0) return null;
        if (body.length > MAX_RESPONSE_BYTES) throw new IOException("Ely.by texture response is too large.");
        return JsonParser.object().from(new String(body, StandardCharsets.UTF_8));
    }

    private BufferedImage fetchSkin(String url) throws Exception {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IOException("Unsupported Ely.by skin URL.");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "image/png")
                .header("User-Agent", "LiteLauncher/" + LauncherState.LAUNCHER_VERSION)
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] body = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("HTTP " + response.statusCode());
        if (body == null || body.length == 0 || body.length > MAX_RESPONSE_BYTES) throw new IOException("Invalid Ely.by skin size.");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(body));
        if (image == null || image.getWidth() != 64
                || (image.getHeight() != 64 && image.getHeight() != 32)) {
            throw new IOException("Ely.by returned an unsupported skin image.");
        }
        return image.getHeight() == 32 ? SkinImageUtils.convertLegacySkin(image) : image;
    }

    private boolean eligible(Profile profile) {
        return profile != null && !profile.microsoft() && profile.elyBy();
    }
}
