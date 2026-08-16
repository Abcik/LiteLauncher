package net.litelauncher.ui;

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

    private PixelPainter() {
    }

    public static void drawWindow(PixelSurface surface, int width, int height) {
        drawWindowShape(surface, width, height, Palette.OUTLINE);
        drawWindowFill(surface, width, height, Palette.GENERAL_BACKGROUND);
        surface.paint(10, 34, width - 11, 35, Palette.OUTLINE);
    }

    private static void drawWindowFill(PixelSurface surface, int width, int height, Color color) {
        int right = width - 1;
        int bottom = height - 1;
        surface.paint(10, 2, right - 10, 3, color);
        surface.paint(6, 4, right - 6, 5, color);
        surface.paint(4, 6, right - 4, 9, color);
        surface.paint(2, 10, right - 2, bottom - 10, color);
        surface.paint(4, bottom - 9, right - 4, bottom - 6, color);
        surface.paint(6, bottom - 5, right - 6, bottom - 4, color);
        surface.paint(10, bottom - 3, right - 10, bottom - 2, color);
    }

    private static void drawWindowShape(PixelSurface surface, int width, int height, Color color) {
        int right = width - 1;
        int bottom = height - 1;
        surface.paint(10, 0, right - 10, 1, color);
        surface.paint(6, 2, right - 6, 3, color);
        surface.paint(4, 4, right - 4, 5, color);
        surface.paint(2, 6, right - 2, 9, color);
        surface.paint(0, 10, right, bottom - 10, color);
        surface.paint(2, bottom - 9, right - 2, bottom - 6, color);
        surface.paint(4, bottom - 5, right - 4, bottom - 4, color);
        surface.paint(6, bottom - 3, right - 6, bottom - 2, color);
        surface.paint(10, bottom - 1, right - 10, bottom, color);
    }

    public static void drawAccentButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        drawBeveledElementOutline(surface, x, y, width, height, Palette.ACCENT_GLARE, Palette.ACCENT_SHADOW);
        drawElementBackground(surface, x, y, width, height, Palette.ACCENT);

        Color animation = accentStateColor(state);
        if (animation != null) drawElementOverlay(surface, x, y, width, height, animation);
    }

    public static void drawProgressTrack(PixelSurface surface, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        surface.paint(x + 1, y, x + width - 2, y, Palette.OUTLINE);
        surface.paint(x, y + 1, x + width - 1, y + height - 2, Palette.OUTLINE);
        surface.paint(x + 1, y + height - 1, x + width - 2, y + height - 1, Palette.OUTLINE);
    }

    public static void drawProgressFill(PixelSurface surface, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        if (width == 1) {
            surface.paint(x, y + 1, x, y + height - 2, Palette.ACCENT_GLARE);
            return;
        }

        surface.paint(x, y + 1, x, y + height - 2, Palette.ACCENT_GLARE);
        surface.paint(x + 1, y, x + width - 2, y, Palette.ACCENT_GLARE);
        surface.paint(x + 1, y + 1, x + width - 2, y + height - 2, Palette.ACCENT);
        surface.paint(x + 1, y + height - 1, x + width - 2, y + height - 1, Palette.ACCENT_SHADOW);
        surface.paint(x + width - 1, y + 1, x + width - 1, y + height - 2, Palette.ACCENT_SHADOW);
    }

    public static void drawElement(PixelSurface surface, int x, int y, int width, int height, Color background, Color outline) {
        drawElementBackground(surface, x, y, width, height, background);
        drawElementOutline(surface, x, y, width, height, outline);
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

    public static void drawCloseIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, CLOSE_ICON, color);
    }

    public static void drawMinimizeIcon(PixelSurface surface, int x, int y, Color color) {
        drawPattern(surface, x, y, MINIMIZE_ICON, color);
    }

    private static Color accentStateColor(PixelButton.State state) {
        if (state == PixelButton.State.HOVERED) return Palette.ACCENT_HOVERED;
        if (state == PixelButton.State.PRESSED) return Palette.ACCENT_PRESSED;
        return null;
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

    private static void drawPattern(PixelSurface surface, int x, int y, int[][] pattern, Color color) {
        for (int[] rect : pattern) {
            surface.paint(x + rect[0], y + rect[1], x + rect[0] + rect[2] - 1, y + rect[1] + rect[3] - 1, color);
        }
    }
}
