package net.litelauncher.frontend;
import net.litelauncher.Theme;

import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;

import java.awt.Color;

public final class PixelPainter {

    private static final int[][] CLOSE_ICON = {
            {3, 3, 2, 2}, {11, 3, 2, 2},
            {5, 5, 2, 2}, {9, 5, 2, 2},
            {7, 7, 2, 2},
            {5, 9, 2, 2}, {9, 9, 2, 2},
            {3, 11, 2, 2}, {11, 11, 2, 2}
    };

    private static final int[][] MINIMIZE_ICON = {{3, 11, 10, 2}};

    private static final int[][] DARK_THEME_ICON = {
            {6, 2, 5, 1}, {4, 3, 5, 1}, {3, 4, 5, 2}, {12, 4, 1, 1},
            {11, 5, 3, 1}, {2, 6, 5, 4}, {12, 6, 1, 1}, {9, 7, 1, 1},
            {13, 9, 1, 1}, {3, 10, 5, 2}, {10, 10, 1, 1}, {4, 12, 5, 1}, {6, 13, 5, 1}
    };

    private static final int[][] LIGHT_THEME_ICON = {
            {7, 2, 2, 1},
            {3, 3, 2, 1}, {7, 3, 2, 1}, {11, 3, 2, 1},
            {3, 4, 3, 1}, {7, 4, 2, 1}, {10, 4, 3, 1},
            {4, 5, 2, 1}, {10, 5, 2, 1},
            {7, 6, 2, 1},
            {2, 7, 3, 2}, {6, 7, 4, 2}, {11, 7, 3, 2},
            {7, 9, 2, 1},
            {4, 10, 2, 1}, {10, 10, 2, 1},
            {3, 11, 3, 1}, {7, 11, 2, 1}, {10, 11, 3, 1},
            {3, 12, 2, 1}, {7, 12, 2, 1}, {11, 12, 2, 1},
            {7, 13, 2, 1}
    };

    private static final int[][] LANGUAGE_ICON = {
            {7, 2, 2, 2}, {3, 4, 10, 2}, {9, 6, 2, 4}, {5, 8, 2, 2},
            {7, 10, 2, 2}, {3, 12, 4, 2}, {9, 12, 4, 2}
    };

    private static final int[][] FOLDER_ICON = {
            {3, 3, 5, 1},
            {2, 4, 1, 1}, {7, 4, 6, 1},
            {2, 5, 1, 1}, {13, 5, 1, 1},
            {2, 6, 10, 1}, {13, 6, 1, 1},
            {2, 7, 12, 3},
            {2, 10, 4, 1}, {10, 10, 4, 1},
            {2, 11, 3, 1}, {6, 11, 4, 1}, {11, 11, 3, 1},
            {3, 12, 2, 1}, {6, 12, 4, 1}, {11, 12, 2, 1}
    };

    private static final int[][] PARAMETERS_ICON = {
            {7, 3, 2, 2}, {4, 4, 2, 1}, {10, 4, 2, 1}, {4, 5, 8, 1},
            {5, 6, 6, 1}, {3, 7, 4, 2}, {9, 7, 4, 2}, {5, 9, 6, 1},
            {4, 10, 8, 1}, {4, 11, 2, 1}, {7, 11, 2, 2}, {10, 11, 2, 1}
    };

    private static final int[][] DELETE_ICON = {
            {0, 0, 1, 1}, {1, 1, 1, 1}, {2, 2, 1, 1}, {3, 3, 1, 1}, {4, 4, 1, 1},
            {4, 0, 1, 1}, {3, 1, 1, 1}, {1, 3, 1, 1}, {0, 4, 1, 1}
    };


    private static final int[][] ENTER_ARROW = {
            {5, 0, 1, 5}, {0, 4, 5, 1}, {1, 3, 1, 3}, {2, 2, 1, 5}
    };

    private static final int[][] CHEVRON_DOWN = {
            {0, 0, 1, 2}, {1, 1, 1, 2}, {2, 2, 1, 2}, {3, 1, 1, 2}, {4, 0, 1, 2}
    };

    private static final int[][] CHEVRON_UP = {
            {0, 2, 1, 2}, {1, 1, 1, 2}, {2, 0, 1, 2}, {3, 1, 1, 2}, {4, 2, 1, 2}
    };

    private static final int[][] MENU_ICON = {
            {0, 0, 5, 1},
            {0, 2, 5, 1},
            {0, 4, 5, 1}
    };

    private static final int[][] CHEVRON_LEFT = {
            {2, 0, 2, 1},
            {1, 1, 2, 1},
            {0, 2, 2, 1},
            {1, 3, 2, 1},
            {2, 4, 2, 1}
    };

    private static final int[][] CHEVRON_RIGHT = {
            {0, 0, 2, 1},
            {1, 1, 2, 1},
            {2, 2, 2, 1},
            {1, 3, 2, 1},
            {0, 4, 2, 1}
    };

    private PixelPainter() {
    }

    public static PixelSurface direct(PixelGraphics graphics) {
        return PixelSurface.direct(graphics);
    }

    public static void drawPopup(PixelGraphics graphics, int[] bounds, Theme theme) {
        PixelSurface surface = direct(graphics);
        drawPopupBackground(surface, bounds, Palette.GENERAL_BACKGROUND.color(theme));
        drawPopupOutline(surface, bounds, Palette.OUTLINE.color(theme));
    }

    public static void drawPopupBackground(PixelSurface surface, int[] bounds, Color color) {
        int xMin = bounds[0];
        int yMin = bounds[1];
        int xMax = bounds[2];
        int yMax = bounds[3];

        surface.paint(xMin + 2, yMin + 8, xMin + 3, yMax - 8, color);
        surface.paint(xMin + 4, yMin + 4, xMin + 7, yMax - 4, color);
        surface.paint(xMin + 8, yMin + 2, xMax - 8, yMax - 2, color);
        surface.paint(xMax - 7, yMin + 4, xMax - 4, yMax - 4, color);
        surface.paint(xMax - 3, yMin + 8, xMax - 2, yMax - 8, color);
    }

    public static void drawPopupOutline(PixelSurface surface, int[] bounds, Color color) {
        int xMin = bounds[0];
        int yMin = bounds[1];
        int xMax = bounds[2];
        int yMax = bounds[3];

        surface.paint(xMin, yMin + 8, xMin + 1, yMax - 8, color);
        surface.paint(xMin + 2, yMin + 4, xMin + 3, yMin + 7, color);
        surface.paint(xMin + 2, yMax - 7, xMin + 3, yMax - 4, color);
        surface.paint(xMin + 4, yMin + 2, xMin + 7, yMin + 3, color);
        surface.paint(xMin + 4, yMax - 3, xMin + 7, yMax - 2, color);
        surface.paint(xMin + 8, yMin, xMax - 8, yMin + 1, color);
        surface.paint(xMin + 8, yMax - 1, xMax - 8, yMax, color);
        surface.paint(xMax - 7, yMin + 2, xMax - 4, yMin + 3, color);
        surface.paint(xMax - 7, yMax - 3, xMax - 4, yMax - 2, color);
        surface.paint(xMax - 3, yMin + 4, xMax - 2, yMin + 7, color);
        surface.paint(xMax - 3, yMax - 7, xMax - 2, yMax - 4, color);
        surface.paint(xMax - 1, yMin + 8, xMax, yMax - 8, color);
    }


    public static void drawWindowBackground(PixelGraphics graphics, Color color) {
        graphics.paint(10, 2, 429, 3, color);
        graphics.paint(6, 4, 433, 5, color);
        graphics.paint(4, 6, 435, 9, color);
        graphics.paint(2, 10, 437, 319, color);
        graphics.paint(4, 320, 435, 323, color);
        graphics.paint(6, 324, 433, 325, color);
        graphics.paint(10, 326, 429, 327, color);
    }

    public static void drawElement(PixelSurface surface, int x, int y, int width, int height, Color background, Color outline) {
        drawElementBackground(surface, x, y, width, height, background);
        drawElementOutline(surface, x, y, width, height, outline);
    }

    public static void drawElement(PixelSurface surface, int[] bounds, Color background, Color outline) {
        drawElement(surface, bounds[0], bounds[1], bounds[2], bounds[3], background, outline);
    }

    public static void drawElements(PixelSurface surface, int[][] bounds, Color background, Color outline) {
        for (int[] item : bounds) drawElement(surface, item, background, outline);
    }

    public static void drawElementBackground(PixelSurface surface, int x, int y, int width, int height, Color color) {
        surface.paint(x + 4, y + 1, x + width - 5, y + 1, color);
        surface.paint(x + 2, y + 2, x + width - 3, y + 3, color);
        surface.paint(x + 1, y + 4, x + width - 2, y + height - 5, color);
        surface.paint(x + 2, y + height - 4, x + width - 3, y + height - 3, color);
        surface.paint(x + 4, y + height - 2, x + width - 5, y + height - 2, color);
    }

    public static void drawElementOutline(PixelSurface surface, int x, int y, int width, int height, Color color) {
        surface.paint(x + 4, y, x + width - 5, y, color);
        surface.paint(x + 2, y + 1, x + 3, y + 1, color);
        surface.paint(x + width - 4, y + 1, x + width - 3, y + 1, color);
        surface.paint(x + 1, y + 2, x + 1, y + 3, color);
        surface.paint(x + width - 2, y + 2, x + width - 2, y + 3, color);
        surface.paint(x, y + 4, x, y + height - 5, color);
        surface.paint(x + width - 1, y + 4, x + width - 1, y + height - 5, color);
        surface.paint(x + 1, y + height - 4, x + 1, y + height - 3, color);
        surface.paint(x + width - 2, y + height - 4, x + width - 2, y + height - 3, color);
        surface.paint(x + 2, y + height - 2, x + 3, y + height - 2, color);
        surface.paint(x + width - 4, y + height - 2, x + width - 3, y + height - 2, color);
        surface.paint(x + 4, y + height - 1, x + width - 5, y + height - 1, color);
    }

    public static void drawElementOverlay(PixelSurface surface, int x, int y, int width, int height, Color color) {
        surface.paint(x + 4, y, x + width - 5, y, color);
        surface.paint(x + 2, y + 1, x + width - 3, y + 1, color);
        surface.paint(x + 1, y + 2, x + width - 2, y + 3, color);
        surface.paint(x, y + 4, x + width - 1, y + height - 5, color);
        surface.paint(x + 1, y + height - 4, x + width - 2, y + height - 3, color);
        surface.paint(x + 2, y + height - 2, x + width - 3, y + height - 2, color);
        surface.paint(x + 4, y + height - 1, x + width - 5, y + height - 1, color);
    }

    public static void drawStateElement(PixelSurface surface, int x, int y, int width, int height,
                                        PixelButton.State state, Theme theme) {
        drawElement(surface, x, y, width, height,
                Palette.ELEMENT_BACKGROUND.color(theme), Palette.OUTLINE.color(theme));

        Color animation = defaultStateColor(state, theme);
        if (animation != null) drawElement(surface, x, y, width, height, animation, animation);
    }

    public static void drawSelectedElement(PixelSurface surface, int x, int y, int width, int height, Theme theme) {
        drawElementOutline(surface, x, y, width, height, Palette.ELEMENT_BACKGROUND.color(theme));
    }

    public static Color defaultStateColor(PixelButton.State state, Theme theme) {
        if (state == PixelButton.State.HOVERED) return Palette.HOVERED.color(theme);
        if (state == PixelButton.State.PRESSED) return Palette.PRESSED.color(theme);
        return null;
    }

    public static Color accentStateColor(PixelButton.State state, Theme theme) {
        if (state == PixelButton.State.HOVERED) return Palette.ACCENT_HOVERED.color(theme);
        if (state == PixelButton.State.PRESSED) return Palette.ACCENT_PRESSED.color(theme);
        return null;
    }

    public static void drawAccentButton(PixelSurface surface, int x, int y, int width, int height,
                                        PixelButton.State state, Theme theme) {
        drawBeveledElementOutline(surface, x, y, width, height,
                Palette.ACCENT_GLARE.color(theme), Palette.ACCENT_SHADOW.color(theme));
        drawElementBackground(surface, x, y, width, height, Palette.ACCENT.color(theme));

        Color animation = accentStateColor(state, theme);
        if (animation != null) drawElementOverlay(surface, x, y, width, height, animation);
    }

    public static void drawSmallAccentButton(PixelSurface surface, int x, int y, PixelButton.State state, Theme theme) {
        Color glare = Palette.ACCENT_GLARE.color(theme);
        Color accent = Palette.ACCENT.color(theme);
        Color shadow = Palette.ACCENT_SHADOW.color(theme);

        surface.paint(x + 1, y + 1, x + 7, y + 7, accent);
        surface.paint(x + 1, y, x + 7, y, glare);
        surface.paint(x, y + 1, x + 1, y + 1, glare);
        surface.paint(x + 7, y + 1, x + 8, y + 1, glare);
        surface.paint(x, y + 2, x, y + 6, glare);
        surface.paint(x, y + 7, x + 1, y + 7, shadow);
        surface.paint(x + 1, y + 8, x + 7, y + 8, shadow);
        surface.paint(x + 7, y + 7, x + 7, y + 8, shadow);
        surface.paint(x + 8, y + 2, x + 8, y + 7, shadow);

        Color animation = accentStateColor(state, theme);
        if (animation != null) drawSmallButtonOverlay(surface, x, y, animation);
    }

    public static void drawPlayButton(PixelSurface surface, int x, int y, int width, int height,
                                      PixelButton.State state, Theme theme) {
        drawLargeActionButton(surface, x, y, width, height, state, theme,
                Palette.ACCENT_GLARE, Palette.ACCENT, Palette.ACCENT_SHADOW);
    }

    public static void drawDangerButton(PixelSurface surface, int x, int y, int width, int height,
                                        PixelButton.State state, Theme theme) {
        drawLargeActionButton(surface, x, y, width, height, state, theme,
                Palette.DANGER_GLARE, Palette.DANGER, Palette.DANGER_SHADOW);
    }

    private static void drawLargeActionButton(PixelSurface surface, int x, int y, int width, int height,
                                              PixelButton.State state, Theme theme,
                                              Palette glarePalette, Palette fillPalette, Palette shadowPalette) {
        Color glare = glarePalette.color(theme);
        Color fill = fillPalette.color(theme);
        Color shadow = shadowPalette.color(theme);

        surface.paint(x + 2, y + 14, x + 3, y + 27, glare);
        surface.paint(x, y + 8, x + 1, y + 23, glare);
        surface.paint(x + 2, y + 4, x + 3, y + 7, glare);
        surface.paint(x + 4, y + 2, x + 7, y + 3, glare);
        surface.paint(x + 8, y, x + width - 9, y + 1, glare);
        surface.paint(x + width - 8, y + 2, x + width - 5, y + 3, glare);
        surface.paint(x + 4, y + height - 4, x + 7, y + height - 3, shadow);
        surface.paint(x + 8, y + height - 2, x + width - 9, y + height - 1, shadow);
        surface.paint(x + width - 8, y + height - 4, x + width - 5, y + height - 3, shadow);
        surface.paint(x + width - 4, y + height - 8, x + width - 3, y + height - 5, shadow);
        surface.paint(x + width - 2, y + 8, x + width - 1, y + height - 9, shadow);
        surface.paint(x + width - 4, y + 4, x + width - 3, y + 7, shadow);
        surface.paint(x + 2, y + 8, x + 3, y + height - 9, fill);
        surface.paint(x + 4, y + 4, x + 7, y + height - 5, fill);
        surface.paint(x + 8, y + 2, x + width - 9, y + height - 3, fill);
        surface.paint(x + width - 8, y + 4, x + width - 5, y + height - 5, fill);
        surface.paint(x + width - 4, y + 8, x + width - 3, y + height - 9, fill);

        Color animation = accentStateColor(state, theme);
        if (animation != null) drawPlayOverlay(surface, x, y, width, height, animation);
    }

    public static void drawCloseIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, CLOSE_ICON, color);
    }

    public static void drawMinimizeIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, MINIMIZE_ICON, color);
    }

    public static void drawThemeIcon(PixelSurface surface, int x, int y, Color color, Theme theme) {
        drawPattern(surface, x, y, theme == Theme.LIGHT ? LIGHT_THEME_ICON : DARK_THEME_ICON, color);
    }

    public static void drawLanguageIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, LANGUAGE_ICON, color);
    }

    public static void drawFolderIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, FOLDER_ICON, color);
    }

    public static void drawParametersIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, PARAMETERS_ICON, color);
    }

    public static void drawDeleteIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, DELETE_ICON, color);
    }

    public static void drawMenuIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, MENU_ICON, color);
    }

    public static void drawChevronLeft(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, CHEVRON_LEFT, color);
    }

    public static void drawChevronRight(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, CHEVRON_RIGHT, color);
    }


    public static void drawEnterArrow(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, ENTER_ARROW, color);
    }

    public static void drawChevronDown(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, CHEVRON_DOWN, color);
    }

    public static void drawChevronUp(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, CHEVRON_UP, color);
    }

    public static void drawProfileOverlay(PixelSurface surface, int x, int y, Color color) {
        surface.paint(x + 4, y, x + 11, y + 7, color);
        surface.paint(x, y + 8, x + 15, y + 15, color);
    }

    public static void drawGameRunningIndicator(PixelSurface surface, int x, int y, Theme theme) {
        Color glare = Palette.ACCENT_GLARE.color(theme);
        Color accent = Palette.ACCENT.color(theme);
        Color shadow = Palette.ACCENT_SHADOW.color(theme);

        surface.paint(x + 1, y, x + 4, y, glare);
        surface.paint(x, y + 1, x + 1, y + 1, glare);
        surface.paint(x + 4, y + 1, x + 5, y + 1, glare);
        surface.paint(x, y + 2, x, y + 3, glare);
        surface.paint(x + 2, y + 1, x + 3, y + 1, accent);
        surface.paint(x + 1, y + 2, x + 4, y + 3, accent);
        surface.paint(x + 2, y + 4, x + 3, y + 4, accent);
        surface.paint(x + 5, y + 2, x + 5, y + 3, shadow);
        surface.paint(x, y + 4, x + 1, y + 4, shadow);
        surface.paint(x + 4, y + 4, x + 5, y + 4, shadow);
        surface.paint(x + 1, y + 5, x + 4, y + 5, shadow);
    }

    private static void drawBeveledElementOutline(PixelSurface surface, int x, int y, int width, int height,
                                                  Color glare, Color shadow) {
        surface.paint(x + 4, y, x + width - 5, y, glare);
        surface.paint(x + 2, y + 1, x + 3, y + 1, glare);
        surface.paint(x + width - 4, y + 1, x + width - 3, y + 1, shadow);
        surface.paint(x + 1, y + 2, x + 1, y + 3, glare);
        surface.paint(x + width - 2, y + 2, x + width - 2, y + 3, shadow);
        surface.paint(x, y + 4, x, y + height - 5, glare);
        surface.paint(x + width - 1, y + 4, x + width - 1, y + height - 5, shadow);
        surface.paint(x + 1, y + height - 4, x + 1, y + height - 3, glare);
        surface.paint(x + width - 2, y + height - 4, x + width - 2, y + height - 3, shadow);
        surface.paint(x + 2, y + height - 2, x + 3, y + height - 2, shadow);
        surface.paint(x + width - 4, y + height - 2, x + width - 3, y + height - 2, shadow);
        surface.paint(x + 4, y + height - 1, x + width - 5, y + height - 1, shadow);
    }

    private static void drawSmallButtonOverlay(PixelSurface surface, int x, int y, Color color) {
        surface.paint(x + 1, y, x + 7, y, color);
        surface.paint(x, y + 1, x + 8, y + 7, color);
        surface.paint(x + 1, y + 8, x + 7, y + 8, color);
    }

    private static void drawPlayOverlay(PixelSurface surface, int x, int y, int width, int height, Color color) {
        surface.paint(x + 8, y, x + width - 9, y + 1, color);
        surface.paint(x + 4, y + 2, x + width - 5, y + 3, color);
        surface.paint(x + 2, y + 4, x + width - 3, y + 7, color);
        surface.paint(x, y + 8, x + width - 1, y + height - 9, color);
        surface.paint(x + 2, y + height - 8, x + width - 3, y + height - 5, color);
        surface.paint(x + 4, y + height - 4, x + width - 5, y + height - 3, color);
        surface.paint(x + 8, y + height - 2, x + width - 9, y + height - 1, color);
    }

    private static void drawPattern(PixelSurface surface, int x, int y, int[][] pattern, Color color) {
        for (int[] rect : pattern) {
            surface.paint(x + rect[0], y + rect[1], x + rect[0] + rect[2] - 1, y + rect[1] + rect[3] - 1, color);
        }
    }
}
