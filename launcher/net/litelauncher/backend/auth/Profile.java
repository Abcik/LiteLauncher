package net.litelauncher.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record Profile(String id, String username, boolean microsoft, String skinPng, boolean slim) {

    public Profile {
        username = safeUsername(username);
        id = microsoft ? safeMicrosoftId(id) : offlineId(username);
        skinPng = microsoft ? AuthUtils.text(skinPng) : null;
        slim = microsoft && slim;
    }

    public static Profile offline(String username) {
        return new Profile(null, username, false, null, false);
    }

    public static Profile microsoft(String id, String username) {
        return microsoft(id, username, null, false);
    }

    public static Profile microsoft(String id, String username, String skinPng, boolean slim) {
        return new Profile(id, username, true, skinPng, slim);
    }

    public static String microsoftId(String profileId) {
        String id = profileId == null || profileId.isBlank() ? UUID.randomUUID().toString() : profileId.trim();
        return id.startsWith("microsoft:") ? id : "microsoft:" + id;
    }

    private static String offlineId(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String safeMicrosoftId(String id) {
        if (id != null && !id.isBlank()) return id.trim();
        return microsoftId(UUID.randomUUID().toString());
    }

    private static String safeUsername(String username) {
        if (username == null) return "Player";
        for (char c : username.toCharArray()) if (!isOfflineUsernameCharacter(c)) return "Player";
        return username;
    }

    private static boolean isOfflineUsernameCharacter(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9' || character == '_';
    }

}
