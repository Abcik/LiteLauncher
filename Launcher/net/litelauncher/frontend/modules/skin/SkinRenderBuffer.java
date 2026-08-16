package net.litelauncher.frontend.modules.skin;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public final class SkinRenderBuffer {
    private static final int BUFFER_QUANTUM = 64;

    private final int width;
    private final int height;
    private final BufferedImage image;
    private final int[] pixels;
    private final float[] zBuffer;
    private final int[] zStamp;
    private int frameStamp = 1;

    private SkinRenderBuffer(int width, int height) {
        this.width = roundUp(width);
        this.height = roundUp(height);
        this.image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB_PRE);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        this.zBuffer = new float[this.width * this.height];
        this.zStamp = new int[this.width * this.height];
    }

    static SkinRenderBuffer ensure(SkinRenderBuffer current, int width, int height) {
        if (current == null || current.width < width || current.height < height) {
            int nextW = Math.max(width, current == null ? 0 : current.width);
            int nextH = Math.max(height, current == null ? 0 : current.height);
            return new SkinRenderBuffer(nextW, nextH);
        }
        return current;
    }

    void nextFrame() {
        frameStamp++;
        if (frameStamp == 0) {
            Arrays.fill(zStamp, 0);
            frameStamp = 1;
        }
    }

    void clearRows(int x, int rowWidth, int yStart, int yEnd) {
        for (int y = yStart; y < yEnd; y++) {
            int offset = y * width + x;
            Arrays.fill(pixels, offset, offset + rowWidth, 0);
        }
    }

    BufferedImage image() { return image; }
    int[] pixels() { return pixels; }
    float[] zBuffer() { return zBuffer; }
    int[] zStamp() { return zStamp; }
    int frameStamp() { return frameStamp; }
    int stride() { return width; }

    private static int roundUp(int value) {
        return ((value + BUFFER_QUANTUM - 1) / BUFFER_QUANTUM) * BUFFER_QUANTUM;
    }
}
