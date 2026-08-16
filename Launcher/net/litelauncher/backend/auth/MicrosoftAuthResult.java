package net.litelauncher.backend.auth;

import java.util.Objects;

public record MicrosoftAuthResult(MicrosoftSession session, MicrosoftProfileCache cache) {

    public MicrosoftAuthResult {
        session = Objects.requireNonNull(session, "session");
        cache = cache == null ? MicrosoftProfileCache.fromSession(session) : cache;
    }

    public Profile profile() {
        return cache.profile(session.profileId());
    }

    public LaunchAccount launchAccount() {
        return LaunchAccount.online(profile(), session.minecraftAccessToken(), session.xuid());
    }
}
