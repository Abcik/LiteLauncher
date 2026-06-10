package net.litelauncher.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public record Profile(String id, String username, boolean microsoft, String skinPng, boolean slim,
                      List<MinecraftCape> capes) {

    public Profile {
        username = safeUsername(username);
        id = microsoft ? safeMicrosoftId(id) : offlineId(username);
        skinPng = microsoft ? AuthUtils.text(skinPng) : null;
        slim = microsoft && slim;
        capes = microsoft ? safeCapes(capes) : List.of();
    }

    public Profile(String id, String username, boolean microsoft, String skinPng, boolean slim) {
        this(id, username, microsoft, skinPng, slim, List.of());
    }

    public static Profile offline(String username) {
        return new Profile(null, username, false, null, false, List.of());
    }

    public static Profile microsoft(String id, String username) {
        return microsoft(id, username, null, false, List.of());
    }

    public static Profile microsoft(String id, String username, String skinPng, boolean slim) {
        return microsoft(id, username, skinPng, slim, List.of());
    }

    public static Profile microsoft(String id, String username, String skinPng, boolean slim, List<MinecraftCape> capes) {
        return new Profile(id, username, true, skinPng, slim, capes);
    }

    public MinecraftCape activeCape() {
        for (MinecraftCape cape : capes) if (cape != null && cape.active()) return cape;
        return null;
    }

    public String activeCapeId() {
        MinecraftCape cape = activeCape();
        return cape == null ? null : cape.id();
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

    private static List<MinecraftCape> safeCapes(List<MinecraftCape> capes) {
        if (capes == null || capes.isEmpty()) return List.of();
        return capes.stream().filter(cape -> cape != null && cape.id() != null).toList();
    }

    private static boolean isOfflineUsernameCharacter(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9' || character == '_';
    }

}
