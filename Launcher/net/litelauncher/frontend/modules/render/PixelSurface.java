package net.litelauncher.frontend.modules.render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;

public class PixelSurface {

    private final IntConsumer bottomTracker;
    private PixelGraphics graphics;
    private int originX;
    private int originY;
    private boolean rendering;

    protected PixelSurface() {
        this((IntConsumer) null);
    }

    protected PixelSurface(IntConsumer bottomTracker) {
        this.bottomTracker = bottomTracker;
    }

    private PixelSurface(PixelGraphics graphics) {
        this((IntConsumer) null);
        begin(graphics, 0, 0, true);
    }

    public static PixelSurface direct(PixelGraphics graphics) {
        return new PixelSurface(graphics);
    }

    protected final void begin(PixelGraphics graphics, int originX, int originY, boolean rendering) {
        this.graphics = graphics;
        this.originX = originX;
        this.originY = originY;
        this.rendering = rendering;
    }

    public final int screenX(int localX) {
        return originX + localX;
    }

    public final int screenY(int localY) {
        return originY + localY;
    }

    public void paint(int xMin, int yMin, int xMax, int yMax, Color color) {
        touchBottom(yMax);
        if (!rendering || graphics == null) return;
        graphics.paint(screenX(xMin), screenY(yMin), screenX(xMax), screenY(yMax), color);
    }

    public void image(int xMin, int yMin, int xMax, int yMax, String path) {
        touchBottom(yMax);
        if (!rendering || graphics == null) return;
        graphics.image(screenX(xMin), screenY(yMin), screenX(xMax), screenY(yMax), path);
    }

    public void image(int x, int y, BufferedImage image) {
        if (image == null) return;
        touchBottom(y + image.getHeight() - 1);
        if (!rendering || graphics == null) return;
        graphics.image(screenX(x), screenY(y), image);
    }

    public void pushClip(int xMin, int yMin, int xMax, int yMax) {
        touchBottom(yMax);
        if (!rendering || graphics == null) return;
        graphics.pushClip(screenX(xMin), screenY(yMin), screenX(xMax), screenY(yMax));
    }

    public void popClip() {
        if (!rendering || graphics == null) return;
        graphics.popClip();
    }

    protected final void touchBottom(int localBottomY) {
        if (bottomTracker != null) bottomTracker.accept(localBottomY);
    }
}
