package net.litelauncher.frontend.modules.render;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PixelGraphics implements AutoCloseable {

    private static final Map<String, BufferedImage> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final BufferedImage MISSING_TEXTURE = createMissingTexture();

    private final int canvasWidth;
    private final int[] pixels;
    private final Graphics2D graphics;
    private final Deque<int[]> clipStack = new ArrayDeque<>();

    private int clipXMin;
    private int clipYMin;
    private int clipXMaxExclusive;
    private int clipYMaxExclusive;

    public PixelGraphics(BufferedImage canvas) {
        Objects.requireNonNull(canvas, "canvas");

        DataBuffer dataBuffer = canvas.getRaster().getDataBuffer();
        if (!(dataBuffer instanceof DataBufferInt)) {
            throw new IllegalArgumentException("Canvas must use DataBufferInt.");
        }

        this.canvasWidth = canvas.getWidth();
        this.pixels = ((DataBufferInt) dataBuffer).getData();
        this.graphics = canvas.createGraphics();

        this.clipXMin = 0;
        this.clipYMin = 0;
        this.clipXMaxExclusive = canvas.getWidth();
        this.clipYMaxExclusive = canvas.getHeight();

        configureGraphics(graphics);
        syncGraphicsClip();
    }

    public void clear() {
        Arrays.fill(pixels, 0);
    }

    public void pushClip(int xMin, int yMin, int xMax, int yMax) {
        clipStack.push(new int[]{clipXMin, clipYMin, clipXMaxExclusive, clipYMaxExclusive});

        int xMaxExclusive = xMax + 1;
        int yMaxExclusive = yMax + 1;

        if (xMin > clipXMin) clipXMin = xMin;
        if (yMin > clipYMin) clipYMin = yMin;
        if (xMaxExclusive < clipXMaxExclusive) clipXMaxExclusive = xMaxExclusive;
        if (yMaxExclusive < clipYMaxExclusive) clipYMaxExclusive = yMaxExclusive;

        if (clipXMin > clipXMaxExclusive) clipXMin = clipXMaxExclusive;
        if (clipYMin > clipYMaxExclusive) clipYMin = clipYMaxExclusive;

        syncGraphicsClip();
    }

    public void popClip() {
        if (clipStack.isEmpty()) return;

        int[] previous = clipStack.pop();
        clipXMin = previous[0];
        clipYMin = previous[1];
        clipXMaxExclusive = previous[2];
        clipYMaxExclusive = previous[3];

        syncGraphicsClip();
    }

    public void paint(int xMin, int yMin, int xMax, int yMax, Color color) {
        if (color.getAlpha() == 0) return;

        if (xMin < clipXMin) xMin = clipXMin;
        if (yMin < clipYMin) yMin = clipYMin;
        if (xMax >= clipXMaxExclusive) xMax = clipXMaxExclusive - 1;
        if (yMax >= clipYMaxExclusive) yMax = clipYMaxExclusive - 1;

        if (xMin > xMax || yMin > yMax) return;

        if (color.getAlpha() == 255) {
            for (int y = yMin; y <= yMax; y++) {
                int rowStart = y * canvasWidth + xMin;
                Arrays.fill(pixels, rowStart, rowStart + (xMax - xMin + 1), color.getRGB());
            }
            return;
        }

        graphics.setColor(color);
        graphics.fillRect(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
    }

    public void image(int xMin, int yMin, int xMax, int yMax, String path) {
        int targetWidth = xMax - xMin + 1;
        int targetHeight = yMax - yMin + 1;

        if (targetWidth <= 0 || targetHeight <= 0) return;
        if (xMax < clipXMin || yMax < clipYMin || xMin >= clipXMaxExclusive || yMin >= clipYMaxExclusive) return;

        graphics.drawImage(IMAGE_CACHE.computeIfAbsent(path, PixelGraphics::loadImage),
                xMin, yMin, targetWidth, targetHeight, null);
    }

    public void image(int x, int y, BufferedImage image) {
        graphics.drawImage(image, x, y, null);
    }

    public void dispose() {
        close();
    }

    @Override
    public void close() {
        graphics.dispose();
    }

    private void syncGraphicsClip() {
        int width = Math.max(0, clipXMaxExclusive - clipXMin);
        int height = Math.max(0, clipYMaxExclusive - clipYMin);
        graphics.setClip(clipXMin, clipYMin, width, height);
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }

    private static BufferedImage loadImage(String path) {
        try {
            if (path.startsWith("assets")) {
                try (InputStream stream = PixelGraphics.class.getClassLoader().getResourceAsStream(path)) {
                    if (stream == null) return MISSING_TEXTURE;

                    BufferedImage image = ImageIO.read(stream);
                    return image != null ? image : MISSING_TEXTURE;
                }
            }

            Path file = Paths.get(path);
            if (!Files.exists(file)) return MISSING_TEXTURE;

            BufferedImage image = ImageIO.read(file.toFile());
            return image != null ? image : MISSING_TEXTURE;
        } catch (IOException _) {
            return MISSING_TEXTURE;
        }
    }

    private static BufferedImage createMissingTexture() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        int magenta = new Color(255, 0, 255).getRGB();
        int black = new Color(0, 0, 0).getRGB();

        for (int y = 0; y < 8; y++) {
            int rowOffset = y * 8;
            for (int x = 0; x < 8; x++) pixels[rowOffset + x] = ((x + y) & 1) == 0 ? magenta : black;
        }

        return image;
    }
}
