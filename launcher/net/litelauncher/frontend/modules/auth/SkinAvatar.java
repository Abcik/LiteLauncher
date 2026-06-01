package net.litelauncher.frontend.modules.auth;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

public final class SkinAvatar {

    private static final int SIZE = 16;

    private SkinAvatar() {
    }

    public static BufferedImage create(String skinPng, boolean slim) {
        try {
            if (skinPng == null || skinPng.isBlank()) return null;
            BufferedImage skin = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(skinPng)));
            return create(skin, slim);
        } catch (Exception exception) {
            return null;
        }
    }

    public static BufferedImage create(BufferedImage skin, boolean slim) {
        if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 32) return null;

        BufferedImage avatar = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = avatar.createGraphics();
        prepare(graphics);

        drawPart(graphics, skin, 8, 8, 8, 8, 4, 0, 8, 8);
        drawLayer(graphics, skin, 40, 8, 8, 8, 4, 0, 8, 8);

        drawPart(graphics, skin, 20, 20, 8, 8, 4, 8, 8, 8);
        if (skin.getHeight() >= 64) drawLayer(graphics, skin, 20, 36, 8, 8, 4, 8, 8, 8);

        int armWidth = slim ? 3 : 4;
        int rightX = 44;
        int leftX = 36;
        int rightOverlayX = 44;
        int leftOverlayX = 52;
        int rightDestX = slim ? 1 : 0;
        int leftDestX = 12;

        drawPart(graphics, skin, rightX, 20, armWidth, 8, rightDestX, 8, armWidth, 8);
        drawPart(graphics, skin, skin.getHeight() >= 64 ? leftX : rightX, skin.getHeight() >= 64 ? 52 : 20,
                armWidth, 8, leftDestX, 8, armWidth, 8);

        if (skin.getHeight() >= 64) {
            drawLayer(graphics, skin, rightOverlayX, 36, armWidth, 8, rightDestX, 8, armWidth, 8);
            drawLayer(graphics, skin, leftOverlayX, 52, armWidth, 8, leftDestX, 8, armWidth, 8);
        }

        graphics.dispose();
        return avatar;
    }

    private static void drawPart(Graphics2D graphics, BufferedImage skin,
                                 int sx, int sy, int sw, int sh,
                                 int dx, int dy, int dw, int dh) {
        graphics.drawImage(skin, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
    }

    private static void drawLayer(Graphics2D graphics, BufferedImage skin,
                                  int sx, int sy, int sw, int sh,
                                  int dx, int dy, int dw, int dh) {
        if (isEmpty(skin, sx, sy, sw, sh)) return;
        drawPart(graphics, skin, sx, sy, sw, sh, dx, dy, dw, dh);
    }

    private static boolean isEmpty(BufferedImage image, int x, int y, int width, int height) {
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                if (((image.getRGB(xx, yy) >>> 24) & 0xff) != 0) return false;
            }
        }
        return true;
    }

    private static void prepare(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }
}
