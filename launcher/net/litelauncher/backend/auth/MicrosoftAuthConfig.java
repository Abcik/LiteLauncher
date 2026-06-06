package net.litelauncher.backend.auth;

public final class MicrosoftAuthConfig {

    public static final String CLIENT_ID = clientId();
    public static final String REDIRECT_PATH = "/litelauncher";
    public static final String SCOPE = "XboxLive.signin XboxLive.offline_access";

    public static final String AUTHORIZE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    public static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    public static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    public static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    public static final String MINECRAFT_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    public static final String MINECRAFT_ENTITLEMENTS_URL = "https://api.minecraftservices.com/entitlements/mcstore";
    public static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private static String clientId() {
        String property = System.getProperty("litelauncher.ms.clientId");
        if (property != null && !property.isBlank()) return property.trim();

        String env = System.getenv("LITELAUNCHER_MS_CLIENT_ID");
        if (env != null && !env.isBlank()) return env.trim();

        return "4e8c6838-e825-4bb5-9e62-05a9210473d5";
    }
}
