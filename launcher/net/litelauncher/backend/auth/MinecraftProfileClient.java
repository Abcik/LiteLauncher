package net.litelauncher.backend.auth;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class MinecraftProfileClient {

    private static final int MAX_SKIN_BYTES = 1024 * 1024;
    private static final int MAX_PROFILE_IMAGE_BYTES = 2 * 1024 * 1024;

    private final HttpClient http;

    MinecraftProfileClient(HttpClient http) {
        this.http = http;
    }

    void ensureMinecraftOwnership(String minecraftAccessToken) throws AuthException {
        JsonObject response = getJson(MicrosoftAuthConfig.MINECRAFT_ENTITLEMENTS_URL, bearer(minecraftAccessToken));
        if (response.getArray("items", new JsonArray()).isEmpty()) {
            throw new AuthException("No Minecraft Java Edition license was found on this Microsoft account.");
        }
    }

    ProfileData fetchProfile(String minecraftAccessToken) throws AuthException {
        return fetchProfile(minecraftAccessToken, List.of());
    }

    ProfileData fetchProfile(String minecraftAccessToken, List<MinecraftCape> cachedCapes) throws AuthException {
        return parseProfile(getJson(MicrosoftAuthConfig.MINECRAFT_PROFILE_URL, bearer(minecraftAccessToken)), cachedCapes);
    }

    ProfileData uploadSkin(String minecraftAccessToken, byte[] skinPng, boolean slim, List<MinecraftCape> cachedCapes) throws AuthException {
        validateSkinPng(skinPng);

        String boundary = multipartBoundary();
        JsonObject response = sendJson(profileRequestBuilder(MicrosoftAuthConfig.MINECRAFT_PROFILE_SKINS_URL, minecraftAccessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(multipartSkin(slim ? "slim" : "classic", skinPng, boundary))
                .build(), MicrosoftAuthConfig.MINECRAFT_PROFILE_SKINS_URL);
        return response.isEmpty() ? fetchProfile(minecraftAccessToken, cachedCapes) : parseProfile(response, cachedCapes);
    }

    ProfileData setCape(String minecraftAccessToken, String capeId, List<MinecraftCape> cachedCapes) throws AuthException {
        HttpRequest.Builder builder = profileRequestBuilder(MicrosoftAuthConfig.MINECRAFT_PROFILE_CAPES_ACTIVE_URL, minecraftAccessToken);
        HttpRequest request;
        if (AuthUtils.hasText(capeId)) {
            JsonObject payload = new JsonObject();
            payload.put("capeId", capeId.trim());
            request = builder.header("Content-Type", "application/json")
                    .method("PUT", HttpRequest.BodyPublishers.ofString(JsonWriter.string().value(payload).done(), StandardCharsets.UTF_8))
                    .build();
        } else {
            request = builder.DELETE().build();
        }
        JsonObject response = sendJson(request, MicrosoftAuthConfig.MINECRAFT_PROFILE_CAPES_ACTIVE_URL);
        return response.isEmpty() ? fetchProfile(minecraftAccessToken, cachedCapes) : parseProfile(response, cachedCapes);
    }

    private void validateSkinPng(byte[] skinPng) throws AuthException {
        if (skinPng == null || skinPng.length == 0) throw new AuthException("Skin image is empty.");
        if (skinPng.length > MAX_SKIN_BYTES) throw new AuthException("Skin image is too large.");
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(skinPng));
            if (image == null || image.getWidth() != 64 || image.getHeight() != 64) {
                throw new AuthException("Only 64x64 PNG skins are supported.");
            }
        } catch (IOException exception) {
            throw new AuthException("Unable to read skin image.", exception);
        }
    }

    private ProfileData parseProfile(JsonObject response, List<MinecraftCape> cachedCapes) throws AuthException {
        String id = response.getString("id");
        String name = response.getString("name");
        if (!AuthUtils.hasText(id) || !AuthUtils.hasText(name)) throw new AuthException("Minecraft profile was not found or has not been created yet.");

        JsonObject skin = activeItem(response.getArray("skins", new JsonArray()));
        String skinUrl = skin.getString("url");
        boolean skinPresent = AuthUtils.hasText(skinUrl);
        return new ProfileData(
                id,
                name,
                fetchPng(skinUrl),
                "SLIM".equalsIgnoreCase(skin.getString("variant")),
                skinPresent,
                parseCapes(response.getArray("capes", new JsonArray()), cachedCapes)
        );
    }

    private JsonObject activeItem(JsonArray items) {
        JsonObject first = new JsonObject();
        for (Object item : items) {
            if (!(item instanceof JsonObject json)) continue;
            if (first.isEmpty()) first = json;
            if ("ACTIVE".equalsIgnoreCase(json.getString("state"))) return json;
        }
        return first;
    }

    private List<MinecraftCape> parseCapes(JsonArray items, List<MinecraftCape> cachedCapes) {
        ArrayList<MinecraftCape> capes = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof JsonObject json)) continue;

            String id = json.getString("id");
            if (!AuthUtils.hasText(id)) continue;

            MinecraftCape cached = findCape(cachedCapes, id);

            String newTextureUrl = AuthUtils.text(json.getString("url"));
            String textureUrl = AuthUtils.hasText(newTextureUrl) ? newTextureUrl : cached == null ? null : cached.textureUrl();

            String png = cachedCapePng(cachedCapes, cached, newTextureUrl);
            if (!AuthUtils.hasText(png) && AuthUtils.hasText(newTextureUrl)) png = fetchPng(newTextureUrl);

            capes.add(new MinecraftCape(
                    id,
                    AuthUtils.firstText(json.getString("name"), json.getString("alias"), cached == null ? null : cached.name(), "Cape"),
                    png,
                    "ACTIVE".equalsIgnoreCase(json.getString("state")),
                    textureUrl
            ));
        }
        return List.copyOf(capes);
    }

    private String cachedCapePng(List<MinecraftCape> capes, MinecraftCape preferred, String newTextureUrl) {
        if (!AuthUtils.hasText(newTextureUrl)) {
            return preferred != null && AuthUtils.hasText(preferred.png()) ? preferred.png() : null;
        }

        if (preferred != null && AuthUtils.hasText(preferred.png()) && newTextureUrl.equals(preferred.textureUrl())) return preferred.png();

        if (capes == null) return null;

        for (MinecraftCape cape : capes) {
            if (cape != null && AuthUtils.hasText(cape.png()) && newTextureUrl.equals(cape.textureUrl())) return cape.png();
        }

        return null;
    }

    private MinecraftCape findCape(List<MinecraftCape> capes, String id) {
        if (capes == null || id == null) return null;
        for (MinecraftCape cape : capes) if (cape != null && id.equals(cape.id())) return cape;
        return null;
    }

    private String fetchPng(String url) {
        try {
            if (!AuthUtils.hasText(url)) return null;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "image/png")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300 || body == null || body.length == 0) return null;
            if (body.length > MAX_PROFILE_IMAGE_BYTES) return null;
            return Base64.getEncoder().encodeToString(body);
        } catch (Exception exception) {
            return null;
        }
    }

    private Map<String, String> bearer(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }

    private HttpRequest.Builder profileRequestBuilder(String url, String token) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token);
    }

    private String multipartBoundary() {
        return "LiteLauncherBoundary" + UUID.randomUUID().toString().replace("-", "");
    }

    private HttpRequest.BodyPublisher multipartSkin(String variant, byte[] skin, String boundary) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writePart(out, boundary, "variant", null, null, variant.getBytes(StandardCharsets.UTF_8));
            writePart(out, boundary, "file", "skin.png", "image/png", skin);
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return HttpRequest.BodyPublishers.ofByteArray(out.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to build skin upload request.", exception);
        }
    }

    private void writePart(ByteArrayOutputStream out, String boundary, String name, String filename, String contentType, byte[] body) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"").getBytes(StandardCharsets.UTF_8));
        if (filename != null) out.write(("; filename=\"" + filename + "\"").getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        if (contentType != null) out.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private JsonObject getJson(String url, Map<String, String> headers) throws AuthException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Accept", "application/json")
                .GET();
        headers.forEach(builder::header);
        return sendJson(builder.build(), url);
    }

    private JsonObject sendJson(HttpRequest request, String url) throws AuthException {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String details = errorDetails(response.body(), "HTTP " + response.statusCode());
                LauncherLog.error("Minecraft profile request failed (" + request.method() + ' ' + url + "): HTTP "
                        + response.statusCode() + " - " + details, null);
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    throw AuthException.expiredSession("Minecraft session expired or was rejected. See launcher log for details.");
                }
                throw new AuthException("Minecraft profile request failed. See launcher log for details.");
            }
            if (response.body() == null || response.body().isBlank()) return new JsonObject();
            return JsonParser.object().from(response.body());
        } catch (AuthException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AuthException("Network error while accessing " + url + '.', exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Request to " + url + " was interrupted.", exception);
        } catch (Exception exception) {
            throw new AuthException("Service " + url + " returned invalid JSON.", exception);
        }
    }

    private String errorDetails(String body, String fallback) {
        try {
            JsonObject json = JsonParser.object().from(body);
            return AuthUtils.firstText(json.getString("error_description"), json.getString("errorMessage"), json.getString("message"), json.getString("error"), fallback);
        } catch (Exception exception) {
            String text = AuthUtils.text(body);
            return text == null ? fallback : text.substring(0, Math.min(500, text.length()));
        }
    }

    record ProfileData(String id, String name, String skinPng, boolean slim, boolean skinPresent, List<MinecraftCape> capes) {
    }
}
