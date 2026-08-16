package net.litelauncher.backend.auth;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class SkinImageUtils {

    private SkinImageUtils() {
    }

    public static BufferedImage convertLegacySkin(BufferedImage image) {
        if (image == null || image.getWidth() != 64 || image.getHeight() != 32) {
            throw new IllegalArgumentException("Legacy skin must be 64x32.");
        }

        BufferedImage converted = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);

            copyMirrored(graphics, image, 4, 16, 4, 4, 20, 48);
            copyMirrored(graphics, image, 8, 16, 4, 4, 24, 48);
            copyMirrored(graphics, image, 8, 20, 4, 12, 16, 52);
            copyMirrored(graphics, image, 4, 20, 4, 12, 20, 52);
            copyMirrored(graphics, image, 0, 20, 4, 12, 24, 52);
            copyMirrored(graphics, image, 12, 20, 4, 12, 28, 52);

            copyMirrored(graphics, image, 44, 16, 4, 4, 36, 48);
            copyMirrored(graphics, image, 48, 16, 4, 4, 40, 48);
            copyMirrored(graphics, image, 48, 20, 4, 12, 32, 52);
            copyMirrored(graphics, image, 44, 20, 4, 12, 36, 52);
            copyMirrored(graphics, image, 40, 20, 4, 12, 40, 52);
            copyMirrored(graphics, image, 52, 20, 4, 12, 44, 52);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    private static void copyMirrored(Graphics2D graphics, BufferedImage source,
                                     int sourceX, int sourceY, int width, int height,
                                     int targetX, int targetY) {
        graphics.drawImage(source,
                targetX, targetY, targetX + width, targetY + height,
                sourceX + width, sourceY, sourceX, sourceY + height,
                null);
    }
}
