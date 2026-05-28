package net.litelauncher.backend;

import net.litelauncher.backend.modules.auth.AuthException;
import net.litelauncher.backend.modules.launch.GameLaunchException;

public final class InformationMessages {

    public static final String SELECT_PROFILE = "Select profile";
    public static final String SELECT_VERSION = "Select version";
    public static final String SIGN_IN_ERROR = "Sign-in error";
    public static final String WEB_AUTH_ERROR = "Web authorization error";
    public static final String DOWNLOAD_ERROR = "Download error";
    public static final String JAVA_ERROR = "Java runtime error";
    public static final String LAUNCH_ERROR = "Launch error";

    private InformationMessages() {
    }

    public static String launch(Exception exception) {
        if (exception instanceof AuthException) return SIGN_IN_ERROR;
        if (exception instanceof GameLaunchException launchException) return launchException.informationMessage();
        return LAUNCH_ERROR;
    }
}
