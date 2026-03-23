package net.litelauncher.ui.theme;

public enum ThemeMode {
    DARK,
    LIGHT;

    public ThemeMode toggle() {
        return this == DARK ? LIGHT : DARK;
    }
}
