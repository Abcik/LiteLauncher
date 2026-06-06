package net.litelauncher.backend.auth;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.platform.OSUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MicrosoftSessionStore {

    private static final String CIPHER_NAME = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecureRandom random = new SecureRandom();

    public Map<String, MicrosoftSession> read() {
        Map<String, MicrosoftSession> sessions = new LinkedHashMap<>();
        Path file = OSUtils.microsoftSessionsFile();
        if (!Files.isRegularFile(file)) return sessions;

        try {
            JsonObject envelope = JsonParser.object().from(Files.readString(file, StandardCharsets.UTF_8));
            String plaintext = decrypt(envelope.getString("iv"), envelope.getString("payload"));
            JsonArray entries = JsonParser.object().from(plaintext).getArray("entries", new JsonArray());
            for (Object item : entries) {
                if (!(item instanceof JsonObject json)) continue;
                String accountId = AuthUtils.text(json.getString("accountId"));
                MicrosoftSession session = readSession(json);
                if (accountId != null && session != null) sessions.put(accountId, session);
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to read Microsoft sessions.", exception);
            AuthUtils.deleteQuietly(file);
        }
        return sessions;
    }


    public MicrosoftSession read(String accountId) {
        if (!AuthUtils.hasText(accountId)) return null;
        return read().get(accountId);
    }

    public void save(MicrosoftSession session) throws AuthException {
        Map<String, MicrosoftSession> sessions = read();
        sessions.put(Profile.microsoftId(session.profileId()), session);
        write(sessions);
    }

    public void prune(java.util.Set<String> keep) throws AuthException {
        Map<String, MicrosoftSession> sessions = read();
        if (sessions.keySet().removeIf(id -> keep == null || !keep.contains(id))) write(sessions);
    }

    public void write(Map<String, MicrosoftSession> sessions) throws AuthException {
        try {
            JsonArray entries = new JsonArray();
            for (Map.Entry<String, MicrosoftSession> entry : sessions.entrySet()) {
                entries.add(writeSession(entry.getKey(), entry.getValue()));
            }

            JsonObject payload = new JsonObject();
            payload.put("entries", entries);
            String plaintext = JsonWriter.string().value(payload).done();

            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            JsonObject envelope = new JsonObject();
            envelope.put("version", 1);
            envelope.put("cipher", CIPHER_NAME);
            envelope.put("iv", Base64.getEncoder().encodeToString(iv));
            envelope.put("payload", Base64.getEncoder().encodeToString(ciphertext));
            envelope.put("savedAt", java.time.Instant.now().toString());

            AuthUtils.writeAtomic(OSUtils.microsoftSessionsFile(), JsonWriter.indent("  ").string().value(envelope).done());
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException("Unable to save Microsoft session.", exception);
        }
    }

    private MicrosoftSession readSession(JsonObject json) {
        String clientId = AuthUtils.text(json.getString("clientId"));
        String playerName = AuthUtils.text(json.getString("playerName"));
        String profileId = AuthUtils.text(json.getString("profileId"));
        String refreshToken = AuthUtils.text(json.getString("microsoftRefreshToken"));
        if (clientId == null || playerName == null || profileId == null || refreshToken == null) return null;

        return new MicrosoftSession(
                clientId,
                AuthUtils.firstText(json.getString("redirectUri"), "http://localhost" + MicrosoftAuthConfig.REDIRECT_PATH),
                playerName,
                profileId,
                AuthUtils.text(json.getString("minecraftAccessToken")),
                AuthUtils.instant(json.getString("minecraftAccessTokenExpiresAt")),
                refreshToken,
                AuthUtils.firstText(json.getString("xuid"), ""),
                AuthUtils.text(json.getString("skinPng")),
                json.getBoolean("slim", false),
                AuthUtils.instant(json.getString("savedAt"))
        );
    }

    private JsonObject writeSession(String accountId, MicrosoftSession session) {
        JsonObject json = new JsonObject();
        json.put("accountId", accountId);
        json.put("clientId", session.clientId());
        json.put("redirectUri", session.redirectUri());
        json.put("playerName", session.playerName());
        json.put("profileId", session.profileId());
        json.put("minecraftAccessToken", session.minecraftAccessToken());
        json.put("minecraftAccessTokenExpiresAt", session.minecraftAccessTokenExpiresAt() == null ? null : session.minecraftAccessTokenExpiresAt().toString());
        json.put("microsoftRefreshToken", session.microsoftRefreshToken());
        json.put("xuid", session.xuid());
        json.put("skinPng", session.skinPng());
        json.put("slim", session.slim());
        json.put("savedAt", session.savedAt() == null ? null : session.savedAt().toString());
        return json;
    }

    private String decrypt(String encodedIv, String encodedPayload) throws AuthException {
        try {
            if (!AuthUtils.hasText(encodedIv) || !AuthUtils.hasText(encodedPayload)) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance(CIPHER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.getDecoder().decode(encodedIv)));
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(encodedPayload));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new AuthException("Unable to read Microsoft sessions.", exception);
        }
    }

    private SecretKeySpec secretKey() throws AuthException {
        try {
            String machineId = OSUtils.machineId();
            if (!AuthUtils.hasText(machineId)) throw new AuthException("Unable to resolve system machine id for Microsoft session encryption.");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(LauncherState.TITLE.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(MicrosoftAuthConfig.CLIENT_ID.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(machineId.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest.digest(), "AES");
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException("Unable to prepare Microsoft session encryption.", exception);
        }
    }
}
