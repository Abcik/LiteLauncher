package net.litelauncher.ui.theme;

import java.awt.Color;

public final class ThemePalette {
    public final Color frameBase;
    public final Color surfaceBackground;
    public final Color elevatedSurface;
    public final Color inputSurface;
    public final Color border;
    public final Color separator;
    public final Color textPrimary;
    public final Color textSecondary;
    public final Color textMuted;
    public final Color accent;
    public final Color accentHover;
    public final Color accentPressed;
    public final Color danger;
    public final Color dangerHover;
    public final Color controlHover;
    public final Color controlPressed;
    public final Color windowShadow;
    public final Color popupShadow;
    public final Color avatarStart;
    public final Color avatarEnd;
    public final Color illustrationSky;
    public final Color illustrationSky2;
    public final Color illustrationGrass;
    public final Color illustrationGrass2;
    public final Color illustrationStone;
    public final Color illustrationStone2;
    public final Color scrollbarTrack;
    public final Color scrollbarThumb;
    public final Color scrollbarThumbHover;
    public final Color selectionFill;
    public final Color textSelectionFill;
    public final Color tooltipBackground;
    public final Color tooltipBorder;
    public final Color tooltipText;
    public final boolean dark;

    private ThemePalette(Color frameBase, Color surfaceBackground, Color elevatedSurface, Color inputSurface, Color border,
                         Color separator, Color textPrimary, Color textSecondary, Color textMuted, Color accent,
                         Color accentHover, Color accentPressed, Color danger, Color dangerHover, Color controlHover,
                         Color controlPressed, Color windowShadow, Color popupShadow, Color avatarStart, Color avatarEnd,
                         Color illustrationSky, Color illustrationSky2, Color illustrationGrass, Color illustrationGrass2,
                         Color illustrationStone, Color illustrationStone2, Color scrollbarTrack, Color scrollbarThumb,
                         Color scrollbarThumbHover, Color selectionFill, Color textSelectionFill, Color tooltipBackground,
                         Color tooltipBorder, Color tooltipText, boolean dark) {
        this.frameBase = frameBase;
        this.surfaceBackground = surfaceBackground;
        this.elevatedSurface = elevatedSurface;
        this.inputSurface = inputSurface;
        this.border = border;
        this.separator = separator;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textMuted = textMuted;
        this.accent = accent;
        this.accentHover = accentHover;
        this.accentPressed = accentPressed;
        this.danger = danger;
        this.dangerHover = dangerHover;
        this.controlHover = controlHover;
        this.controlPressed = controlPressed;
        this.windowShadow = windowShadow;
        this.popupShadow = popupShadow;
        this.avatarStart = avatarStart;
        this.avatarEnd = avatarEnd;
        this.illustrationSky = illustrationSky;
        this.illustrationSky2 = illustrationSky2;
        this.illustrationGrass = illustrationGrass;
        this.illustrationGrass2 = illustrationGrass2;
        this.illustrationStone = illustrationStone;
        this.illustrationStone2 = illustrationStone2;
        this.scrollbarTrack = scrollbarTrack;
        this.scrollbarThumb = scrollbarThumb;
        this.scrollbarThumbHover = scrollbarThumbHover;
        this.selectionFill = selectionFill;
        this.textSelectionFill = textSelectionFill;
        this.tooltipBackground = tooltipBackground;
        this.tooltipBorder = tooltipBorder;
        this.tooltipText = tooltipText;
        this.dark = dark;
    }

    public static ThemePalette forMode(ThemeMode mode) {
        if (mode == ThemeMode.DARK) {
            return new ThemePalette(
                    new Color(10, 10, 12), new Color(18, 18, 21), new Color(24, 24, 28), new Color(30, 30, 35),
                    new Color(255, 255, 255, 26), new Color(255, 255, 255, 34), new Color(245, 245, 247),
                    new Color(179, 179, 187), new Color(132, 132, 140), new Color(90, 180, 98),
                    new Color(102, 192, 110), new Color(114, 204, 122), new Color(188, 73, 83),
                    new Color(210, 88, 98), new Color(255, 255, 255, 20), new Color(255, 255, 255, 30),
                    new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(164, 164, 171),
                    new Color(118, 118, 126), new Color(60, 62, 68), new Color(40, 42, 47),
                    new Color(82, 85, 92), new Color(62, 64, 72), new Color(116, 118, 124),
                    new Color(82, 84, 90), new Color(24, 24, 28), new Color(76, 76, 84),
                    new Color(104, 104, 112), new Color(90, 180, 98, 64), new Color(255, 255, 255, 54),
                    new Color(31, 31, 36, 245), new Color(255, 255, 255, 32), new Color(245, 245, 247), true);
        }

        return new ThemePalette(
                new Color(244, 244, 246), new Color(251, 251, 252), new Color(245, 245, 247), new Color(241, 241, 244),
                new Color(20, 20, 24, 24), new Color(20, 20, 24, 34), new Color(20, 20, 24),
                new Color(94, 94, 103), new Color(132, 132, 140), new Color(90, 180, 98),
                new Color(102, 192, 110), new Color(114, 204, 122), new Color(196, 78, 88),
                new Color(214, 94, 104), new Color(20, 20, 24, 10), new Color(20, 20, 24, 16),
                new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(126, 126, 134),
                new Color(96, 96, 104), new Color(226, 228, 233), new Color(210, 213, 219),
                new Color(153, 157, 164), new Color(126, 130, 137), new Color(177, 181, 187),
                new Color(142, 146, 152), new Color(244, 244, 246), new Color(164, 164, 171),
                new Color(132, 132, 140), new Color(90, 180, 98, 54), new Color(20, 20, 24, 28),
                new Color(253, 253, 255, 245), new Color(20, 20, 24, 30), new Color(20, 20, 24), false);
    }
}
