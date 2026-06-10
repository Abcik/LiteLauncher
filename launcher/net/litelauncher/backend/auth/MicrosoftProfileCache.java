package net.litelauncher.backend.auth;

import java.time.Instant;
import java.util.List;

record MicrosoftProfileCache(
        String playerName,
        String skinPng,
        boolean slim,
        List<MinecraftCape> capes,
        Instant savedAt
) {
    MicrosoftProfileCache {
        playerName = AuthUtils.text(playerName);
        skinPng = AuthUtils.text(skinPng);
        capes = capes == null ? List.of() : capes.stream()
                .filter(cape -> cape != null && cape.id() != null)
                .toList();
    }

    static MicrosoftProfileCache fromSession(MicrosoftSession session) {
        if (session == null) return null;
        return new MicrosoftProfileCache(usernameFallback(), null, false, List.of(), session.savedAt());
    }

    Profile profile(String profileId) {
        return Profile.microsoft(
                Profile.microsoftId(profileId),
                AuthUtils.firstText(playerName, usernameFallback()),
                skinPng,
                slim,
                capes
        );
    }

    private static String usernameFallback() {
        return "Unknown";
    }
}
