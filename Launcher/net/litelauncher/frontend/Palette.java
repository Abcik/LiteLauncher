package net.litelauncher.frontend;

import net.litelauncher.Theme;

import java.awt.Color;
import java.util.Locale;
import java.nio.file.Path;

public enum Palette {

    GENERAL_BACKGROUND(new Color(251, 251, 252), new Color(18, 18, 21)),
    POPUP_FOGGING(new Color(251, 251, 252, 230), new Color(18, 18, 21, 230)),
    ELEMENT_BACKGROUND(new Color(242, 242, 244), new Color(30, 30, 35)),
    OUTLINE(new Color(222, 222, 225), new Color(52, 52, 57)),
    SUBTITLE(new Color(94, 94, 103), new Color(154, 154, 160)),
    TITLE(new Color(18, 18, 21), new Color(245, 245, 247)),

    ACCENT_TITLE(new Color(245, 245, 247), new Color(245, 245, 247)),
    ACCENT_SUBTITLE(new Color(222, 222, 225), new Color(222, 222, 225)),
    ACCENT_GLARE(new Color(165, 198, 99), new Color(165, 198, 99)),
    ACCENT(new Color(122, 159, 53), new Color(122, 159, 53)),
    ACCENT_SHADOW(new Color(86, 119, 20), new Color(86, 119, 20)),

    DANGER_GLARE(new Color(242, 163, 155), new Color(242, 163, 155)),
    DANGER(new Color(204, 108, 97), new Color(204, 108, 97)),
    DANGER_SHADOW(new Color(163, 76, 66), new Color(163, 76, 66)),

    ACCENT_HOVERED(new Color(251, 251, 252, 60), new Color(18, 18, 21, 60)),
    ACCENT_PRESSED(new Color(251, 251, 252, 120), new Color(18, 18, 21, 120)),

    HOVERED(new Color(251, 251, 252, 120), new Color(18, 18, 21, 120)),
    PRESSED(new Color(251, 251, 252, 200), new Color(18, 18, 21, 200));

    private final Color light;
    private final Color dark;

    Palette(Color light, Color dark) {
        this.light = light;
        this.dark = dark;
    }

    public Color color(Theme theme) {
        Theme safeTheme = theme == null ? Theme.LIGHT : theme;
        return ThemeColorOverrides.get().color(this, safeTheme, defaultColor(safeTheme));
    }

    public Color defaultColor(Theme theme) {
        return theme == Theme.DARK ? dark : light;
    }

    public String defaultHex(Theme theme) {
        Color color = defaultColor(theme);
        return String.format(Locale.ROOT, "%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    public String customHex(Theme theme) {
        return ThemeColorOverrides.get().customHex(this, theme);
    }

    public Color customizationColor(Theme theme) {
        Theme safeTheme = theme == null ? Theme.LIGHT : theme;
        return ThemeColorOverrides.get().customizationColor(this, safeTheme, defaultColor(safeTheme));
    }

    public boolean setCustomHex(Theme theme, String hex) {
        return ThemeColorOverrides.get().setCustomHex(this, theme, hex);
    }

    public static void importCustomThemeColors(Path file) throws Exception {
        ThemeColorOverrides.get().importFile(file);
    }

    public static void setCustomThemesEnabled(boolean enabled) {
        ThemeColorOverrides.get().setEnabled(enabled);
    }

    public static void deleteCustomThemeColors() {
        ThemeColorOverrides.get().deleteFileAndClear();
    }

    public static long revision() {
        return ThemeColorOverrides.get().revision();
    }
}
