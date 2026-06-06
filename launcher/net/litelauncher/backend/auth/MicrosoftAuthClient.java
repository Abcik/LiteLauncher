package net.litelauncher.backend.auth;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MicrosoftAuthClient {

    private static final Duration TOKEN_REFRESH_MARGIN = Duration.ofMinutes(5);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public MicrosoftAuthResult authenticate(String code, String codeVerifier, URI redirectUri) throws AuthException {
        MicrosoftTokens microsoft = requestMicrosoftTokens(code, codeVerifier, redirectUri);
        MinecraftToken minecraft = requestMinecraftToken(microsoft.accessToken());
        ensureMinecraftOwnership(minecraft.accessToken());
        MinecraftProfile profile = fetchMinecraftProfile(minecraft.accessToken());

        MicrosoftSession session = new MicrosoftSession(
                MicrosoftAuthConfig.CLIENT_ID,
                redirectUri.toString(),
                profile.name(),
                profile.id(),
                minecraft.accessToken(),
                minecraft.expiresAt(),
                microsoft.refreshToken(),
                minecraft.xuid(),
                fetchSkinPng(profile),
                profile.slim(),
                Instant.now()
        );
        return new MicrosoftAuthResult(session);
    }

    public MicrosoftAuthResult refresh(MicrosoftSession session) throws AuthException {
        if (session == null) throw new AuthException("Microsoft session was not found.");

        MicrosoftSession active = session;
        if (needsRefresh(active)) active = refreshMinecraftToken(active);

        MinecraftProfile profile = fetchMinecraftProfile(active.minecraftAccessToken());
        String skinPng = fetchSkinPng(profile);
        MicrosoftSession updated = new MicrosoftSession(
                active.clientId(),
                active.redirectUri(),
                profile.name(),
                profile.id(),
                active.minecraftAccessToken(),
                active.minecraftAccessTokenExpiresAt(),
                active.microsoftRefreshToken(),
                active.xuid(),
                AuthUtils.firstText(skinPng, active.skinPng()),
                skinPng == null ? active.slim() : profile.slim(),
                Instant.now()
        );
        return new MicrosoftAuthResult(updated);
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
                session.playerName(),
                session.profileId(),
                minecraft.accessToken(),
                minecraft.expiresAt(),
                AuthUtils.firstText(microsoft.refreshToken(), session.microsoftRefreshToken()),
                AuthUtils.firstText(minecraft.xuid(), session.xuid(), ""),
                session.skinPng(),
                session.slim(),
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
        return parseMicrosoftTokens(postForm(MicrosoftAuthConfig.TOKEN_URL, form), false);
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

    private void ensureMinecraftOwnership(String minecraftAccessToken) throws AuthException {
        JsonObject response = getJson(MicrosoftAuthConfig.MINECRAFT_ENTITLEMENTS_URL, Map.of("Authorization", "Bearer " + minecraftAccessToken));
        if (response.getArray("items", new JsonArray()).isEmpty()) {
            throw new AuthException("No Minecraft Java Edition license was found on this Microsoft account.");
        }
    }

    private MinecraftProfile fetchMinecraftProfile(String minecraftAccessToken) throws AuthException {
        JsonObject response = getJson(MicrosoftAuthConfig.MINECRAFT_PROFILE_URL, Map.of("Authorization", "Bearer " + minecraftAccessToken));
        String id = response.getString("id");
        String name = response.getString("name");
        if (!AuthUtils.hasText(id) || !AuthUtils.hasText(name)) throw new AuthException("Minecraft profile was not found or has not been created yet.");

        JsonObject skin = activeSkin(response.getArray("skins", new JsonArray()));
        return new MinecraftProfile(id, name, skin.getString("url"), "SLIM".equalsIgnoreCase(skin.getString("variant")));
    }

    private JsonObject activeSkin(JsonArray skins) {
        JsonObject first = new JsonObject();
        for (Object item : skins) {
            if (!(item instanceof JsonObject skin)) continue;
            if (first.isEmpty()) first = skin;
            if ("ACTIVE".equalsIgnoreCase(skin.getString("state"))) return skin;
        }
        return first;
    }

    private String fetchSkinPng(MinecraftProfile profile) {
        try {
            if (profile == null || !AuthUtils.hasText(profile.skinUrl())) return null;
            HttpRequest request = HttpRequest.newBuilder(URI.create(profile.skinUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "image/png")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length == 0) return null;
            return Base64.getEncoder().encodeToString(response.body());
        } catch (Exception exception) {
            return null;
        }
    }

    private JsonObject getJson(String url, Map<String, String> headers) throws AuthException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Accept", "application/json")
                .GET();
        headers.forEach(builder::header);
        return sendJson(builder.build(), url);
    }

    private JsonObject postForm(String url, Map<String, String> form) throws AuthException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(AuthUtils.formEncode(form), StandardCharsets.UTF_8))
                .build();
        return sendJson(request, url);
    }

    private JsonObject postJson(String url, JsonObject payload, Map<String, String> headers) throws AuthException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonWriter.string().value(payload).done(), StandardCharsets.UTF_8));
        headers.forEach(builder::header);
        return sendJson(builder.build(), url);
    }

    private JsonObject sendJson(HttpRequest request, String url) throws AuthException {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String details = errorDetails(response.body(), "HTTP " + response.statusCode());
                if (expiredSession(response.body())) throw AuthException.expiredSession("Microsoft session expired: " + details);
                throw new AuthException("Authorization failed: " + details);
            }
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

    private JsonObject xui(JsonObject response) {
        JsonObject claims = response.getObject("DisplayClaims", new JsonObject());
        JsonArray items = claims.getArray("xui", new JsonArray());
        Object first = items.isEmpty() ? null : items.getFirst();
        return first instanceof JsonObject json ? json : new JsonObject();
    }

    private boolean expiredSession(String body) {
        String text = body == null ? "" : body.toLowerCase();
        return text.contains("invalid_grant") || text.contains("expired") && text.contains("refresh");
    }

    private String errorDetails(String body, String fallback) {
        try {
            JsonObject json = JsonParser.object().from(body);
            return AuthUtils.firstText(json.getString("error_description"), json.getString("errorMessage"), json.getString("message"), json.getString("error"), fallback);
        } catch (Exception exception) {
            String text = AuthUtils.text(body);
            return text == null ? fallback : text.substring(0, Math.min(240, text.length()));
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

    private record MinecraftProfile(String id, String name, String skinUrl, boolean slim) {
    }
}
