package net.litelauncher.ui.text;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class TextRasterizer {

    private static final TextFont FONT = TextFont.PIXELS;

    public static int lineHeight(int scale) {
        return FONT.lineHeight() * scale;
    }

    public static BufferedImage rasterizeLines(String[] lines, int width, Color color, int scale, int gap) {
        int lineHeight = lineHeight(scale);
        BufferedImage image = emptyImage(width, Math.max(1, lines.length * lineHeight));
        Graphics2D graphics = image.createGraphics();
        prepare(graphics);

        for (int index = 0; index < lines.length; index++) {
            drawLineMask(graphics, lines[index], 0, index * lineHeight, scale, gap);
        }

        tintInPlace(graphics, image, color);
        graphics.dispose();
        return image;
    }

    public static BufferedImage emptyImage(int width, int height) {
        return new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
    }

    private static void drawLineMask(Graphics2D graphics, String text, int x, int y, int scale, int gap) {
        int cursorX = x;
        int baselineY = y + FONT.ascender() * scale;

        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);

            TextFont.Glyph glyph = FONT.glyph(codePoint);
            int targetX = cursorX + glyph.bearingX() * scale;
            int targetY = baselineY - glyph.bearingY() * scale;

            graphics.drawImage(
                    FONT.atlas(),
                    targetX,
                    targetY,
                    targetX + glyph.width() * scale,
                    targetY + glyph.height() * scale,
                    glyph.x(),
                    glyph.y(),
                    glyph.x() + glyph.width(),
                    glyph.y() + glyph.height(),
                    null
            );

            cursorX += glyph.advance() * scale;
            if (index < text.length()) cursorX += gap;
        }
    }

    private static void tintInPlace(Graphics2D graphics, BufferedImage image, Color color) {
        graphics.setComposite(AlphaComposite.SrcAtop);
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setComposite(AlphaComposite.SrcOver);
    }

    private static void prepare(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }
}
