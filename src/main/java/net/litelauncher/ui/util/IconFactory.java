package net.litelauncher.ui.util;

import net.litelauncher.ui.theme.ThemePalette;

import javax.swing.Icon;
import java.awt.image.BufferedImage;

public final class IconFactory {
    private static final int LOGO_MAX_WIDTH = 196;
    private static final int LOGO_MAX_HEIGHT = 28;
    private static final int HEADER_ICON_SIZE = 20;
    private static final int WINDOW_ICON_SIZE = 16;
    private static final int PROFILE_ICON_SIZE = 24;
    private static final int TRASH_ICON_SIZE = 16;

    private IconFactory() {
    }

    public static Icon logoWordmarkIcon(ThemePalette palette) {
        return ResourceIcons.themedIcon("logo", palette.dark, LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT);
    }

    public static Icon minimizeIcon(ThemePalette palette) {
        return ResourceIcons.themedIcon("minimize", palette.dark, WINDOW_ICON_SIZE, WINDOW_ICON_SIZE);
    }

    public static Icon closeIcon(ThemePalette palette) {
        return ResourceIcons.themedIcon("close", palette.dark, WINDOW_ICON_SIZE, WINDOW_ICON_SIZE);
    }

    public static Icon settingsIcon(ThemePalette palette) {
        return ResourceIcons.themedIcon("settings", palette.dark, HEADER_ICON_SIZE, HEADER_ICON_SIZE);
    }

    public static Icon globeIcon(ThemePalette palette) {
        return ResourceIcons.themedIcon("language", palette.dark, HEADER_ICON_SIZE, HEADER_ICON_SIZE);
    }

    public static Icon bulbIcon(ThemePalette palette) {
        return ResourceIcons.themedIcon("theme", palette.dark, HEADER_ICON_SIZE, HEADER_ICON_SIZE);
    }

    public static Icon trashIcon(ThemePalette palette) {
        return ResourceIcons.themedIcon("trash", palette.dark, TRASH_ICON_SIZE, TRASH_ICON_SIZE);
    }

    public static BufferedImage profileAvatarImage(ThemePalette palette) {
        return ResourceIcons.themedImage("profile", palette.dark, PROFILE_ICON_SIZE, PROFILE_ICON_SIZE);
    }
}
