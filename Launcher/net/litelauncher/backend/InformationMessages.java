package net.litelauncher.backend;

import net.litelauncher.backend.auth.AuthException;
import net.litelauncher.backend.launch.GameLaunchException;
import net.litelauncher.i18n.I18n;

public final class InformationMessages {

    public static final String SELECT_PROFILE = "error.selectProfile";
    public static final String SELECT_VERSION = "error.selectVersion";
    public static final String SIGN_IN_ERROR = "error.signIn";
    public static final String WEB_AUTH_ERROR = "error.webAuth";
    public static final String DOWNLOAD_ERROR = "error.download";
    public static final String JAVA_ERROR = "error.java";
    public static final String LAUNCH_ERROR = "error.launch";
    public static final String SKIN_ERROR = "error.skin";
    public static final String CAPE_ERROR = "error.cape";

    public static String text(String key) {
        return I18n.text(key);
    }

    public static String launch(Exception exception) {
        if (exception instanceof AuthException) return text(SIGN_IN_ERROR);
        if (exception instanceof GameLaunchException launchException) return text(launchException.informationMessage());
        return text(LAUNCH_ERROR);
    }
}
