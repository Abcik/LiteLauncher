package net.litelauncher.backend.auth;

import java.util.Objects;

public record MicrosoftAuthResult(MicrosoftSession session) {

    public MicrosoftAuthResult(MicrosoftSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    public Profile profile() {
        return Profile.microsoft(Profile.microsoftId(session.profileId()), session.playerName(), session.skinPng(), session.slim());
    }

    public LaunchAccount launchAccount() {
        return LaunchAccount.online(profile(), session.minecraftAccessToken(), session.xuid());
    }
}
