package net.litelauncher.backend.auth;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MicrosoftAuthClient {

    private static final Duration TOKEN_REFRESH_MARGIN = Duration.ofMinutes(5);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final MinecraftProfileClient profiles = new MinecraftProfileClient(http);

    public MicrosoftAuthResult authenticate(String code, String codeVerifier, URI redirectUri) throws AuthException {
        MicrosoftTokens microsoft = requestMicrosoftTokens(code, codeVerifier, redirectUri);
        MinecraftToken minecraft = requestMinecraftToken(microsoft.accessToken());
        profiles.ensureMinecraftOwnership(minecraft.accessToken());
        MinecraftProfileClient.ProfileData profile = profiles.fetchProfile(minecraft.accessToken());

        return resultFromProfile(
                MicrosoftAuthConfig.CLIENT_ID,
                redirectUri.toString(),
                minecraft.accessToken(),
                minecraft.expiresAt(),
                microsoft.refreshToken(),
                minecraft.xuid(),
                profile,
                null
        );
    }

    public MicrosoftAuthResult refresh(MicrosoftSession session, MicrosoftProfileCache fallback) throws AuthException {
        if (session == null) throw new AuthException("Microsoft session was not found.");

        MicrosoftSession active = session;
        if (needsRefresh(active)) active = refreshMinecraftToken(active);

        MinecraftProfileClient.ProfileData profile = profiles.fetchProfile(active.minecraftAccessToken(), cachedCapes(fallback));
        return resultFromProfile(
                active.clientId(),
                active.redirectUri(),
                active.minecraftAccessToken(),
                active.minecraftAccessTokenExpiresAt(),
                active.microsoftRefreshToken(),
                active.xuid(),
                profile,
                fallback
        );
    }

    public MicrosoftAuthResult uploadSkin(MicrosoftSession session, MicrosoftProfileCache fallback, byte[] skinPng, boolean slim) throws AuthException {
        if (session == null) throw new AuthException("Microsoft session was not found.");

        MicrosoftSession active = session;
        if (needsRefresh(active)) active = refreshMinecraftToken(active);

        MinecraftProfileClient.ProfileData profile = profiles.uploadSkin(active.minecraftAccessToken(), skinPng, slim, cachedCapes(fallback));
        return resultFromProfile(
                active.clientId(),
                active.redirectUri(),
                active.minecraftAccessToken(),
                active.minecraftAccessTokenExpiresAt(),
                active.microsoftRefreshToken(),
                active.xuid(),
                profile,
                fallback,
                java.util.Base64.getEncoder().encodeToString(skinPng)
        );
    }

    public MicrosoftAuthResult setCape(MicrosoftSession session, MicrosoftProfileCache fallback, String capeId) throws AuthException {
        if (session == null) throw new AuthException("Microsoft session was not found.");

        MicrosoftSession active = session;
        if (needsRefresh(active)) active = refreshMinecraftToken(active);

        MinecraftProfileClient.ProfileData profile = profiles.setCape(active.minecraftAccessToken(), capeId, cachedCapes(fallback));
        return resultFromProfile(
                active.clientId(),
                active.redirectUri(),
                active.minecraftAccessToken(),
                active.minecraftAccessTokenExpiresAt(),
                active.microsoftRefreshToken(),
                active.xuid(),
                profile,
                fallback
        );
    }

    private boolean needsRefresh(MicrosoftSession session) {
        return !AuthUtils.hasText(session.minecraftAccessToken())
                || session.minecraftAccessTokenExpiresAt() == null
                || session.minecraftAccessTokenExpiresAt().isBefore(Instant.now().plus(TOKEN_REFRESH_MARGIN));
    }

    private MicrosoftSession refreshMinecraftToken(MicrosoftSession session) throws AuthException {
        MicrosoftTokens microsoft = refreshMicrosoftTokens(session.microsoftRefreshToken(), session.redirectUri());
        MinecraftToken minecraft = requestMinecraftToken(microsoft.accessToken());

        return new MicrosoftSession(
                session.clientId(),
                session.redirectUri(),
                session.profileId(),
                minecraft.accessToken(),
                minecraft.expiresAt(),
                AuthUtils.firstText(microsoft.refreshToken(), session.microsoftRefreshToken()),
                AuthUtils.firstText(minecraft.xuid(), session.xuid(), ""),
                Instant.now()
        );
    }

    private MinecraftToken requestMinecraftToken(String microsoftAccessToken) throws AuthException {
        XboxUserToken xboxUser = authenticateXbox(microsoftAccessToken);
        XboxSecurityToken xsts = authorizeXsts(xboxUser.token());
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
        return parseMicrosoftTokens(postForm(MicrosoftAuthConfig.TOKEN_URL, form), true);
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

    private XboxUserToken authenticateXbox(String microsoftAccessToken) throws AuthException {
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
        return new XboxUserToken(token, userHash);
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

    private MicrosoftAuthResult resultFromProfile(String clientId, String redirectUri, String accessToken, Instant expiresAt,
                                                  String refreshToken, String xuid, MinecraftProfileClient.ProfileData profile,
                                                  MicrosoftProfileCache fallback) {
        return resultFromProfile(clientId, redirectUri, accessToken, expiresAt, refreshToken, xuid, profile, fallback, null);
    }

    private MicrosoftAuthResult resultFromProfile(String clientId, String redirectUri, String accessToken, Instant expiresAt,
                                                  String refreshToken, String xuid, MinecraftProfileClient.ProfileData profile,
                                                  MicrosoftProfileCache fallback, String uploadedSkinPng) {
        Instant now = Instant.now();
        MicrosoftSession session = new MicrosoftSession(
                clientId,
                redirectUri,
                profile.id(),
                accessToken,
                expiresAt,
                refreshToken,
                AuthUtils.firstText(xuid, ""),
                now
        );
        MicrosoftProfileCache cache = new MicrosoftProfileCache(
                profile.name(),
                skinImage(profile, fallback, uploadedSkinPng),
                profile.slim(),
                mergeCapeImages(profile.capes(), fallback == null ? List.of() : fallback.capes()),
                now
        );
        return new MicrosoftAuthResult(session, cache);
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
            MinecraftCape old = findCape(fallback, cape.id());
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

    private MinecraftCape findCape(List<MinecraftCape> capes, String id) {
        if (capes == null || id == null) return null;
        for (MinecraftCape cape : capes) if (cape != null && id.equals(cape.id())) return cape;
        return null;
    }

    private JsonObject postForm(String url, Map<String, String> form) throws AuthException {
        return postForm(url, form, false);
    }

    private JsonObject postForm(String url, Map<String, String> form, boolean expiredOnFailure) throws AuthException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(AuthUtils.formEncode(form), StandardCharsets.UTF_8))
                .build();
        return sendJson(request, url, expiredOnFailure);
    }

    private JsonObject postJson(String url, JsonObject payload, Map<String, String> headers) throws AuthException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
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
                String details = errorDetails(response.body(), "HTTP " + response.statusCode());
                LauncherLog.error("Microsoft auth request failed (" + request.method() + ' ' + url + "): HTTP "
                        + response.statusCode() + " - " + details, null);
                if (sessionRejected(response.statusCode(), expiredOnFailure)) {
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

    private boolean sessionRejected(int statusCode, boolean refreshRequest) {
        return statusCode == 401 || statusCode == 403 || refreshRequest && statusCode == 400;
    }

    private JsonObject xui(JsonObject response) {
        JsonObject claims = response.getObject("DisplayClaims", new JsonObject());
        JsonArray items = claims.getArray("xui", new JsonArray());
        Object first = items.isEmpty() ? null : items.getFirst();
        return first instanceof JsonObject json ? json : new JsonObject();
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

    private record MicrosoftTokens(String accessToken, String refreshToken) {
    }

    private record XboxUserToken(String token, String userHash) {
    }

    private record XboxSecurityToken(String token, String userHash, String xuid) {
    }

    private record MinecraftToken(String accessToken, Instant expiresAt, String xuid) {
    }
}
