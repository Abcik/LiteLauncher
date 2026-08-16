package net.litelauncher.ui;

import java.awt.Color;
import java.awt.image.BufferedImage;

public final class PixelSurface {

    private final PixelGraphics graphics;
    private final int originX;
    private final int originY;

    private PixelSurface(PixelGraphics graphics, int originX, int originY) {
        this.graphics = graphics;
        this.originX = originX;
        this.originY = originY;
    }

    public static PixelSurface direct(PixelGraphics graphics) {
        return new PixelSurface(graphics, 0, 0);
    }

    public void paint(int xMin, int yMin, int xMax, int yMax, Color color) {
        graphics.paint(originX + xMin, originY + yMin, originX + xMax, originY + yMax, color);
    }

    public void image(int xMin, int yMin, int xMax, int yMax, String path) {
        graphics.image(originX + xMin, originY + yMin, originX + xMax, originY + yMax, path);
    }

    public void image(int x, int y, BufferedImage image) {
        graphics.image(originX + x, originY + y, image);
    }

}
