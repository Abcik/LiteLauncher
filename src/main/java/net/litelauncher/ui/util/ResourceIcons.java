package net.litelauncher.ui.util;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ResourceIcons {
    private static final String ICON_ROOT = "/net/litelauncher/assets/icons/";
    private static final Map<String, BufferedImage> SOURCE_CACHE = new HashMap<>();
    private static final Map<String, ImageIcon> ICON_CACHE = new HashMap<>();

    private ResourceIcons() {
    }

    public static ImageIcon themedIcon(String baseName, boolean dark, int maxWidth, int maxHeight) {
        String suffix = dark ? "dark" : "light";
        String cacheKey = baseName + "|" + suffix + "|" + maxWidth + "x" + maxHeight;
        ImageIcon cached = ICON_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        BufferedImage image = themedImage(baseName, dark, maxWidth, maxHeight);
        ImageIcon icon = new ImageIcon(image);
        ICON_CACHE.put(cacheKey, icon);
        return icon;
    }

    public static BufferedImage themedImage(String baseName, boolean dark, int maxWidth, int maxHeight) {
        return scaleToFit(loadSource(resolvePath(baseName, dark)), maxWidth, maxHeight);
    }

    private static String resolvePath(String baseName, boolean dark) {
        String themedPath = ICON_ROOT + baseName + (dark ? "-dark.png" : "-light.png");
        if (resourceExists(themedPath)) {
            return themedPath;
        }
        return ICON_ROOT + baseName + ".png";
    }

    private static boolean resourceExists(String path) {
        try (InputStream stream = ResourceIcons.class.getResourceAsStream(path)) {
            return stream != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private static BufferedImage loadSource(String path) {
        BufferedImage cached = SOURCE_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        BufferedImage image = readImage(path);
        SOURCE_CACHE.put(path, image);
        return image;
    }

    private static BufferedImage readImage(String path) {
        try (InputStream stream = ResourceIcons.class.getResourceAsStream(path)) {
            if (stream == null) {
                return placeholderImage(24, 24);
            }
            BufferedImage read = ImageIO.read(stream);
            return read != null ? read : placeholderImage(24, 24);
        } catch (IOException exception) {
            return placeholderImage(24, 24);
        }
    }

    private static BufferedImage scaleToFit(BufferedImage source, int maxWidth, int maxHeight) {
        if (source == null) {
            return placeholderImage(maxWidth, maxHeight);
        }
        if (maxWidth <= 0 || maxHeight <= 0) {
            return source;
        }
        double widthScale = maxWidth / (double) source.getWidth();
        double heightScale = maxHeight / (double) source.getHeight();
        double scale = Math.min(widthScale, heightScale);
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        if (width == source.getWidth() && height == source.getHeight()) {
            return source;
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }

    private static BufferedImage placeholderImage(int width, int height) {
        int safeWidth = Math.max(8, width);
        int safeHeight = Math.max(8, height);
        BufferedImage image = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new java.awt.Color(255, 0, 140, 140));
        g2.fillRoundRect(0, 0, safeWidth, safeHeight, 6, 6);
        g2.setColor(new java.awt.Color(255, 255, 255, 220));
        g2.drawLine(2, 2, safeWidth - 3, safeHeight - 3);
        g2.drawLine(safeWidth - 3, 2, 2, safeHeight - 3);
        g2.dispose();
        return image;
    }
}
