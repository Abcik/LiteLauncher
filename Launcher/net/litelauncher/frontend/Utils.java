package net.litelauncher.frontend;

import net.litelauncher.Theme;

public final class Utils {

    public static String getLocalIcon(String icon) {
        return "assets/common/" + icon + ".png";
    }

    public static String getLocalIcon(String icon, Theme theme) {
        return "assets/" + theme.identifier() + "/" + icon + ".png";
    }
}