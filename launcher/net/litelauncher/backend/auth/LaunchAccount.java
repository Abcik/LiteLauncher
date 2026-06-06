package net.litelauncher.backend.auth;

public record LaunchAccount(
        String uuid,
        String username,
        String accessToken,
        String xuid,
        String userType,
        Profile updatedProfile
) {

    public static LaunchAccount offline(Profile profile) {
        String username = profile == null ? "Player" : profile.username();
        String uuid = profile == null ? Profile.offline(username).id() : cleanUuid(profile.id());
        return new LaunchAccount(uuid, username, "offline-" + uuid, "", "legacy", null);
    }

    public static LaunchAccount online(Profile profile, String accessToken, String xuid) {
        return new LaunchAccount(cleanUuid(profile.id()), profile.username(), accessToken, xuid == null ? "" : xuid, "msa", profile);
    }

    public String legacySession() {
        if (!"msa".equals(userType)) return accessToken;
        return "token:" + accessToken + ":" + uuid;
    }

    private static String cleanUuid(String value) {
        return value == null ? "" : value.replace("microsoft:", "").replace("-", "");
    }
}
