package net.litelauncher.backend.auth;

import java.time.Instant;

public record MicrosoftSession(
        String clientId,
        String redirectUri,
        String playerName,
        String profileId,
        String minecraftAccessToken,
        Instant minecraftAccessTokenExpiresAt,
        String microsoftRefreshToken,
        String xuid,
        String skinPng,
        boolean slim,
        Instant savedAt
) {
}
