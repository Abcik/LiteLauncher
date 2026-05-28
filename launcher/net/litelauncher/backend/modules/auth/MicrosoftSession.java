package net.litelauncher.backend.modules.auth;

import java.time.Instant;

record MicrosoftSession(
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
