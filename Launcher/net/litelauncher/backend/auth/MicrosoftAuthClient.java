package net.litelauncher.backend.auth;

import net.litelauncher.backend.BackendUtils;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MicrosoftAuthClient {

    private final HttpClient http;
    private final MinecraftProfileClient profiles;

    public MicrosoftAuthClient() {
        this(BackendUtils.http());
    }

    MicrosoftAuthClient(HttpClient http) {
        this.http = http;
        this.profiles = new MinecraftProfileClient(http);
    }

    public MicrosoftAuthResult authenticate(String code, String codeVerifier, URI redirectUri) throws AuthException {
        MicrosoftTokens microsoft = requestMicrosoftTokens(code, codeVerifier, redirectUri);
        MinecraftToken minecraft = requestMinecraftToken(microsoft.accessToken());
        profiles.ensureMinecraftOwnership(minecraft.accessToken());
        MinecraftProfileClient.ProfileData profile = profiles.fetchProfile(minecraft.accessToken());

        MicrosoftSession session = new MicrosoftSession(MicrosoftAuthConfig.CLIENT_ID, redirectUri.toString(), profile.id(),
                minecraft.accessToken(), minecraft.expiresAt(), microsoft.refreshToken(), minecraft.xuid(), Instant.now());
        return new MicrosoftAuthResult(session, profileCache(profile, null, null));
    }

    MicrosoftProfileCache refresh(MicrosoftSession session, MicrosoftProfileCache fallback) throws AuthException {
        return profileCache(profiles.fetchProfile(session.minecraftAccessToken(), cachedCapes(fallback)), fallback, null);
    }

    MicrosoftProfileCache uploadSkin(MicrosoftSession session, MicrosoftProfileCache fallback, byte[] skinPng, boolean slim) throws AuthException {
        MinecraftProfileClient.ProfileData profile = profiles.uploadSkin(session.minecraftAccessToken(), skinPng, slim, cachedCapes(fallback));
        return profileCache(profile, fallback, Base64.getEncoder().encodeToString(skinPng));
    }

    MicrosoftProfileCache setCape(MicrosoftSession session, MicrosoftProfileCache fallback, String capeId) throws AuthException {
        return profileCache(profiles.setCape(session.minecraftAccessToken(), capeId, cachedCapes(fallback)), fallback, null);
    }

    MicrosoftSession refreshMinecraftToken(MicrosoftSession session, SessionSaver saver) throws AuthException {
        MicrosoftTokens microsoft = refreshMicrosoftTokens(session.microsoftRefreshToken(), session.redirectUri());
        // Keep rotated Microsoft credentials even if a later Xbox/Minecraft request fails.
        MicrosoftSession credentials = new MicrosoftSession(session.clientId(), session.redirectUri(), session.profileId(),
                session.minecraftAccessToken(), session.minecraftAccessTokenExpiresAt(),
                AuthUtils.firstText(microsoft.refreshToken(), session.microsoftRefreshToken()), session.xuid(), Instant.now());
        saver.save(credentials);
        MinecraftToken minecraft = requestMinecraftToken(microsoft.accessToken());

        MicrosoftSession updated = new MicrosoftSession(
                session.clientId(),
                session.redirectUri(),
                session.profileId(),
                minecraft.accessToken(),
                minecraft.expiresAt(),
                credentials.microsoftRefreshToken(),
                AuthUtils.firstText(minecraft.xuid(), session.xuid(), ""),
                Instant.now()
        );
        saver.save(updated);
        return updated;
    }

    private MinecraftToken requestMinecraftToken(String microsoftAccessToken) throws AuthException {
        XboxSecurityToken xsts = authorizeXsts(authenticateXbox(microsoftAccessToken));
        MinecraftToken minecraft = authenticateMinecraft(xsts.userHash(), xsts.token());
        return new MinecraftToken(minecraft.accessToken(), minecraft.expiresAt(), xsts.xuid());
    }

    private MicrosoftTokens requestMicrosoftTokens(String code, String codeVerifier, URI redirectUri) throws AuthException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", MicrosoftAuthConfig.CLIENT_ID);
        form.put("code", code);
        form.put("redirect_uri", redirectUri.toString());
        form.put("grant_type", "authorization_code");
        form.put("code_verifier", codeVerifier);
        form.put("scope", MicrosoftAuthConfig.SCOPE);
        return parseMicrosoftTokens(postForm(MicrosoftAuthConfig.TOKEN_URL, form, false), true);
    }

    private MicrosoftTokens refreshMicrosoftTokens(String refreshToken, String redirectUri) throws AuthException {
        if (!AuthUtils.hasText(refreshToken)) throw AuthException.expiredSession("Microsoft refresh token is missing.");

        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", MicrosoftAuthConfig.CLIENT_ID);
        form.put("refresh_token", refreshToken);
        form.put("redirect_uri", AuthUtils.firstText(redirectUri, "http://localhost" + MicrosoftAuthConfig.REDIRECT_PATH));
        form.put("grant_type", "refresh_token");
        form.put("scope", MicrosoftAuthConfig.SCOPE);
        return parseMicrosoftTokens(postForm(MicrosoftAuthConfig.TOKEN_URL, form, true), false);
    }

    private MicrosoftTokens parseMicrosoftTokens(JsonObject json, boolean requireRefreshToken) throws AuthException {
        String accessToken = json.getString("access_token");
        String refreshToken = json.getString("refresh_token");
        if (!AuthUtils.hasText(accessToken)) throw new AuthException("Microsoft did not return access_token.");
        if (requireRefreshToken && !AuthUtils.hasText(refreshToken)) throw new AuthException("Microsoft did not return refresh_token.");
        return new MicrosoftTokens(accessToken, refreshToken);
    }

    private String authenticateXbox(String microsoftAccessToken) throws AuthException {
        JsonObject properties = new JsonObject();
        properties.put("AuthMethod", "RPS");
        properties.put("SiteName", "user.auth.xboxlive.com");
        properties.put("RpsTicket", "d=" + microsoftAccessToken);

        JsonObject request = new JsonObject();
        request.put("RelyingParty", "http://auth.xboxlive.com");
        request.put("TokenType", "JWT");
        request.put("Properties", properties);

        JsonObject response = postJson(MicrosoftAuthConfig.XBOX_AUTH_URL, request, Map.of("x-xbl-contract-version", "1"));
        String token = response.getString("Token");
        String userHash = xui(response).getString("uhs");
        if (!AuthUtils.hasText(token) || !AuthUtils.hasText(userHash)) throw new AuthException("Xbox Live did not return the required sign-in data.");
        return token;
    }

    private XboxSecurityToken authorizeXsts(String xboxUserToken) throws AuthException {
        JsonArray tokens = new JsonArray();
        tokens.add(xboxUserToken);

        JsonObject properties = new JsonObject();
        properties.put("SandboxId", "RETAIL");
        properties.put("UserTokens", tokens);

        JsonObject request = new JsonObject();
        request.put("RelyingParty", "rp://api.minecraftservices.com/");
        request.put("TokenType", "JWT");
        request.put("Properties", properties);

        JsonObject response = postJson(MicrosoftAuthConfig.XSTS_URL, request, Map.of("x-xbl-contract-version", "1"));
        JsonObject xui = xui(response);
        String token = response.getString("Token");
        String userHash = xui.getString("uhs");
        String xuid = AuthUtils.firstText(xui.getString("xid"), xui.getString("xuid"), "");
        if (!AuthUtils.hasText(token) || !AuthUtils.hasText(userHash)) throw new AuthException("XSTS did not return a token for Minecraft Services.");
        return new XboxSecurityToken(token, userHash, xuid);
    }

    private MinecraftToken authenticateMinecraft(String userHash, String xstsToken) throws AuthException {
        JsonObject request = new JsonObject();
        request.put("identityToken", "XBL3.0 x=" + userHash + ';' + xstsToken);

        JsonObject response = postJson(MicrosoftAuthConfig.MINECRAFT_LOGIN_URL, request, Map.of());
        String accessToken = response.getString("access_token");
        int expiresIn = response.getInt("expires_in", 86400);
        if (!AuthUtils.hasText(accessToken)) throw new AuthException("Minecraft Services did not return access_token.");
        return new MinecraftToken(accessToken, Instant.now().plusSeconds(Math.max(1, expiresIn)), "");
    }

    private List<MinecraftCape> cachedCapes(MicrosoftProfileCache fallback) {
        return fallback == null ? List.of() : fallback.capes();
    }

    private MicrosoftProfileCache profileCache(MinecraftProfileClient.ProfileData profile,
                                               MicrosoftProfileCache fallback, String uploadedSkinPng) {
        return new MicrosoftProfileCache(
                profile.name(),
                skinImage(profile, fallback, uploadedSkinPng),
                profile.slim(),
                mergeCapeImages(profile.capes(), fallback == null ? List.of() : fallback.capes()),
                Instant.now()
        );
    }

    private String skinImage(MinecraftProfileClient.ProfileData profile, MicrosoftProfileCache fallback, String uploadedSkinPng) {
        String skin = AuthUtils.firstText(profile.skinPng(), uploadedSkinPng);
        if (skin != null) return skin;
        return profile.skinPresent() && fallback != null ? fallback.skinPng() : null;
    }

    private List<MinecraftCape> mergeCapeImages(List<MinecraftCape> fresh, List<MinecraftCape> fallback) {
        if (fresh == null) return fallback == null ? List.of() : List.copyOf(fallback);
        if (fresh.isEmpty()) return List.of();

        ArrayList<MinecraftCape> capes = new ArrayList<>();
        for (MinecraftCape cape : fresh) {
            if (cape == null || cape.id() == null) continue;
            MinecraftCape old = AuthUtils.findCape(fallback, cape.id());
            capes.add(new MinecraftCape(
                    cape.id(),
                    AuthUtils.firstText(cape.name(), old == null ? null : old.name(), "Cape"),
                    AuthUtils.firstText(cape.png(), old == null ? null : old.png()),
                    cape.active(),
                    AuthUtils.firstText(cape.textureUrl(), old == null ? null : old.textureUrl())
            ));
        }
        return List.copyOf(capes);
    }


    private JsonObject postForm(String url, Map<String, String> form, boolean expiredOnFailure) throws AuthException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(AuthUtils.formEncode(form), StandardCharsets.UTF_8))
                .build();
        return sendJson(request, url, expiredOnFailure);
    }

    private JsonObject postJson(String url, JsonObject payload, Map<String, String> headers) throws AuthException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonWriter.string().value(payload).done(), StandardCharsets.UTF_8));
        headers.forEach(builder::header);
        return sendJson(builder.build(), url, false);
    }

    private JsonObject sendJson(HttpRequest request, String url, boolean expiredOnFailure) throws AuthException {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String details = AuthUtils.errorDetails(response.body(), "HTTP " + response.statusCode());
                LauncherLog.error("Microsoft auth request failed (" + request.method() + ' ' + url + "): HTTP "
                        + response.statusCode() + " - " + details, null);
                if (sessionRejected(response.body(), expiredOnFailure)) {
                    throw AuthException.expiredSession("Microsoft session expired or was rejected. See launcher log for details.");
                }
                throw new AuthException("Authorization failed. See launcher log for details.");
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

    private boolean sessionRejected(String body, boolean refreshRequest) {
        if (!refreshRequest) return false;
        try {
            String error = JsonParser.object().from(body).getString("error", "");
            return "invalid_grant".equals(error) || "interaction_required".equals(error)
                    || "login_required".equals(error) || "consent_required".equals(error);
        } catch (Exception ignored) {
            return false;
        }
    }

    @FunctionalInterface
    interface SessionSaver {
        void save(MicrosoftSession session) throws AuthException;
    }

    private JsonObject xui(JsonObject response) {
        JsonObject claims = response.getObject("DisplayClaims", new JsonObject());
        JsonArray items = claims.getArray("xui", new JsonArray());
        Object first = items.isEmpty() ? null : items.getFirst();
        return first instanceof JsonObject json ? json : new JsonObject();
    }



    private record MicrosoftTokens(String accessToken, String refreshToken) { }

    private record XboxSecurityToken(String token, String userHash, String xuid) { }

    private record MinecraftToken(String accessToken, Instant expiresAt, String xuid) { }
}
