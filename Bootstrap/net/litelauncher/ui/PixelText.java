package net.litelauncher.ui;

import net.litelauncher.ui.text.GlyphLayout;
import net.litelauncher.ui.text.TextRasterizer;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;

public final class PixelText {

    private final JComponent host;
    private int x;
    private int y;
    private int boxWidth;
    private String text;
    private Alignment alignment;
    private Color color;
    private int scale;
    private int gap;

    private boolean dirty = true;
    private BufferedImage frame = TextRasterizer.emptyImage(1, 1);

    public PixelText(JComponent host, int x, int y, int boxWidth, String text, Alignment alignment, Color color) {
        this.host = Objects.requireNonNull(host);
        this.x = x;
        this.y = y;
        this.boxWidth = Math.max(1, boxWidth);
        this.text = text == null ? "" : text;
        this.alignment = Objects.requireNonNull(alignment);
        this.color = Objects.requireNonNull(color);
        this.scale = 1;
        this.gap = 0;
    }

    public void setText(String text) {
        String value = text == null ? "" : text;
        if (this.text.equals(value)) return;
        this.text = value;
        dirty = true;
        host.repaint();
    }

    public void setColor(Color color) {
        Color value = Objects.requireNonNull(color);
        if (this.color.equals(value)) return;
        this.color = value;
        dirty = true;
        host.repaint();
    }

    public void setBounds(int x, int y, int boxWidth) {
        int width = Math.max(1, boxWidth);
        if (this.x == x && this.y == y && this.boxWidth == width) return;
        this.x = x;
        this.y = y;
        this.boxWidth = width;
        dirty = true;
        host.repaint();
    }

    public void render(PixelSurface surface) {
        if (dirty) rebuild();
        surface.image(x, y, frame);
    }

    private void rebuild() {
        String line = text.replace('\n', ' ');
        int contentWidth = Math.max(1, GlyphLayout.width(line, scale, gap));
        BufferedImage content = TextRasterizer.rasterizeLines(new String[]{line}, contentWidth, color, scale, gap);
        frame = TextRasterizer.emptyImage(boxWidth, content.getHeight());

        Graphics2D graphics = frame.createGraphics();
        prepareGraphics(graphics);
        graphics.drawImage(content, startX(contentWidth), 0, null);
        graphics.dispose();

        dirty = false;
    }

    private int startX(int contentWidth) {
        if (alignment == Alignment.CENTER) return (boxWidth - contentWidth) / 2;
        if (alignment == Alignment.RIGHT) return boxWidth - contentWidth;
        return 0;
    }

    private static void prepareGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }
}
