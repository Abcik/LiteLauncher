package net.litelauncher.backend.auth;

import java.time.Instant;

public record MicrosoftSession(
        String clientId,
        String redirectUri,
        String profileId,
        String minecraftAccessToken,
        Instant minecraftAccessTokenExpiresAt,
        String microsoftRefreshToken,
        String xuid,
        Instant savedAt
) {
    public MicrosoftSession {
        clientId = AuthUtils.text(clientId);
        redirectUri = AuthUtils.text(redirectUri);
        profileId = AuthUtils.text(profileId);
        minecraftAccessToken = AuthUtils.text(minecraftAccessToken);
        microsoftRefreshToken = AuthUtils.text(microsoftRefreshToken);
        xuid = AuthUtils.firstText(xuid, "");
    }
}
