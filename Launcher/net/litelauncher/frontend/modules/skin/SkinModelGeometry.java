package net.litelauncher.frontend.modules.skin;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class SkinModelGeometry {
    private static final int SKIN_SIZE = 64;
    private static final int ALPHA_CUTOFF = 16;
    private static final int ALPHA_SKIN_BASE = 0;
    private static final int ALPHA_BLEND = 1;

    private static final double CAPE_CENTER_Y = 16.25;
    private static final double CAPE_CENTER_Z = -3.15;
    private static final double CAPE_ROT_X = Math.toRadians(10.0);
    private static final double TEXTURE_BRIGHTNESS = 1.00;
    private static final double TEXTURE_CONTRAST = 1.02;

    static BufferedImage normalizeSkin(BufferedImage source) {
        return source != null && source.getWidth() == SKIN_SIZE && source.getHeight() == SKIN_SIZE ? source : fallbackSkin();
    }

    static BufferedImage fallbackSkin() {
        return new BufferedImage(SKIN_SIZE, SKIN_SIZE, BufferedImage.TYPE_INT_RGB);
    }

    static boolean validCape(BufferedImage cape) {
        return cape != null && cape.getWidth() >= 22 && cape.getHeight() >= 17;
    }

    static List<SkinModelFace> build(BufferedImage skin, boolean slimModel, BufferedImage cape) {
        ArrayList<SkinModelFace> faces = new ArrayList<>(78);
        double armW = slimModel ? 3.0 : 4.0;
        double armX = 4.0 + armW * 0.5;

        addSkinPart(faces, skin, 0, 28, 8, 8, 8, 0, 0, 32, 0, 0.55);
        addSkinPart(faces, skin, 0, 18, 8, 12, 4, 16, 16, 16, 32, 0.32);
        addSkinPart(faces, skin, -armX, 18, armW, 12, 4, 40, 16, 40, 32, 0.28);
        addSkinPart(faces, skin, armX, 18, armW, 12, 4, 32, 48, 48, 48, 0.28);
        addSkinPart(faces, skin, -2, 6, 4, 12, 4, 0, 16, 0, 32, 0.26);
        addSkinPart(faces, skin, 2, 6, 4, 12, 4, 16, 48, 0, 48, 0.26);

        if (validCape(cape)) {
            Box capeBox = new Box(0, CAPE_CENTER_Y, CAPE_CENTER_Z, 10, 16, 1, 0, CAPE_ROT_X);
            addBox(faces, cape, capeBox, capeUv(), 0, ALPHA_BLEND);
        }
        return faces;
    }

    private static void addSkinPart(List<SkinModelFace> faces, BufferedImage skin,
                                    double cx, double cy, double w, double h, double d,
                                    int baseU, int baseV, int overlayU, int overlayV, double overlayInflate) {
        int iw = (int) w, ih = (int) h, id = (int) d;
        addBox(faces, skin, new Box(cx, cy, 0, w, h, d, 0, 0), boxUv(baseU, baseV, iw, ih, id), 0, ALPHA_SKIN_BASE);
        addBox(faces, skin, new Box(cx, cy, 0, w, h, d, overlayInflate, 0), boxUv(overlayU, overlayV, iw, ih, id), 1, ALPHA_BLEND);
    }

    private static void addBox(List<SkinModelFace> faces, BufferedImage image, Box box, UvSet uv, int layer, int alphaMode) {
        double hw = box.w * 0.5 + box.inflate;
        double hh = box.h * 0.5 + box.inflate;
        double hd = box.d * 0.5 + box.inflate;

        SkinModelVector p000 = box.apply(new SkinModelVector(-hw, -hh, -hd));
        SkinModelVector p001 = box.apply(new SkinModelVector(-hw, -hh,  hd));
        SkinModelVector p010 = box.apply(new SkinModelVector(-hw,  hh, -hd));
        SkinModelVector p011 = box.apply(new SkinModelVector(-hw,  hh,  hd));
        SkinModelVector p100 = box.apply(new SkinModelVector( hw, -hh, -hd));
        SkinModelVector p101 = box.apply(new SkinModelVector( hw, -hh,  hd));
        SkinModelVector p110 = box.apply(new SkinModelVector( hw,  hh, -hd));
        SkinModelVector p111 = box.apply(new SkinModelVector( hw,  hh,  hd));

        addFace(faces, image, uv.up,    box.normal(new SkinModelVector( 0,  1,  0)), p010, p110, p111, p011, layer, alphaMode);
        addFace(faces, image, uv.down,  box.normal(new SkinModelVector( 0, -1,  0)), p000, p100, p101, p001, layer, alphaMode);
        addFace(faces, image, uv.right, box.normal(new SkinModelVector(-1,  0,  0)), p010, p011, p001, p000, layer, alphaMode);
        addFace(faces, image, uv.front, box.normal(new SkinModelVector( 0,  0,  1)), p011, p111, p101, p001, layer, alphaMode);
        addFace(faces, image, uv.left,  box.normal(new SkinModelVector( 1,  0,  0)), p111, p110, p100, p101, layer, alphaMode);
        addFace(faces, image, uv.back,  box.normal(new SkinModelVector( 0,  0, -1)), p110, p010, p000, p100, layer, alphaMode);
    }

    private static void addFace(List<SkinModelFace> faces, BufferedImage image, Uv uv, SkinModelVector normal,
                                SkinModelVector v0, SkinModelVector v1, SkinModelVector v2, SkinModelVector v3, int layer, int alphaMode) {
        if (!isValidUv(image, uv) || !hasVisiblePixels(image, uv, alphaMode)) return;
        faces.add(new SkinModelFace(shadedTexture(image, uv, light(normal), alphaMode), uv.w, uv.h, normal, v0, v1, v2, v3, layer));
    }

    private static UvSet boxUv(int u, int v, int w, int h, int d) {
        return new UvSet(
                new Uv(u + d,         v,     w, d),
                new Uv(u + d + w,     v,     w, d),
                new Uv(u,             v + d, d, h),
                new Uv(u + d,         v + d, w, h),
                new Uv(u + d + w,     v + d, d, h),
                new Uv(u + d + w + d, v + d, w, h));
    }

    private static UvSet capeUv() {
        return new UvSet(
                new Uv(1,  0, 10, 1),
                new Uv(11, 0, 10, 1),
                new Uv(0,  1, 1, 16),
                new Uv(12, 1, 10, 16),
                new Uv(11, 1, 1, 16),
                new Uv(1,  1, 10, 16));
    }

    private static boolean isValidUv(BufferedImage image, Uv uv) {
        return uv.x >= 0 && uv.y >= 0 && uv.x + uv.w <= image.getWidth() && uv.y + uv.h <= image.getHeight();
    }

    private static boolean hasVisiblePixels(BufferedImage image, Uv uv, int alphaMode) {
        if (alphaMode == ALPHA_SKIN_BASE) return true;
        for (int y = uv.y; y < uv.y + uv.h; y++) {
            for (int x = uv.x; x < uv.x + uv.w; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xff) > ALPHA_CUTOFF) return true;
            }
        }
        return false;
    }

    private static int[] shadedTexture(BufferedImage image, Uv uv, double light, int alphaMode) {
        int[] texture = new int[uv.w * uv.h];
        int i = 0;
        for (int y = 0; y < uv.h; y++) {
            for (int x = 0; x < uv.w; x++, i++) {
                int argb = image.getRGB(uv.x + x, uv.y + y);
                int a = (argb >>> 24) & 0xff;
                if (a <= ALPHA_CUTOFF) {
                    texture[i] = alphaMode == ALPHA_SKIN_BASE ? 0xff000000 : 0;
                    continue;
                }

                int r = tone((argb >>> 16) & 0xff, light);
                int g = tone((argb >>> 8) & 0xff, light);
                int b = tone(argb & 0xff, light);
                texture[i] = alphaMode == ALPHA_SKIN_BASE ? 0xff000000 | (r << 16) | (g << 8) | b : premultiply(a, r, g, b);
            }
        }
        return texture;
    }

    private static double light(SkinModelVector normal) {
        SkinModelVector n = normal.unit();
        SkinModelVector key = new SkinModelVector(-0.32, 0.80, 0.66).unit();
        SkinModelVector fill = new SkinModelVector(0.62, 0.30, 0.28).unit();
        double value = 0.90 + 0.08 * Math.max(0, n.dot(key)) + 0.04 * Math.max(0, n.dot(fill));
        return Math.clamp(value, 0.86, 1.04);
    }

    private static int tone(int channel, double light) {
        double value = (channel - 128.0) * TEXTURE_CONTRAST + 128.0;
        value *= TEXTURE_BRIGHTNESS * light;
        return Math.clamp(Math.round(value), 0, 255);
    }

    private static int premultiply(int a, int r, int g, int b) {
        if (a >= 255) return 0xff000000 | (r << 16) | (g << 8) | b;
        return (a << 24) | (((r * a + 127) / 255) << 16) | (((g * a + 127) / 255) << 8) | ((b * a + 127) / 255);
    }


    private static final class Box {
        final double cx, cy, cz, w, h, d, inflate, rotX;

        Box(double cx, double cy, double cz, double w, double h, double d, double inflate, double rotX) {
            this.cx = cx; this.cy = cy; this.cz = cz; this.w = w; this.h = h; this.d = d; this.inflate = inflate; this.rotX = rotX;
        }

        SkinModelVector apply(SkinModelVector point) {
            SkinModelVector rotated = rotate(point);
            return new SkinModelVector(rotated.x() + cx, rotated.y() + cy, rotated.z() + cz);
        }

        SkinModelVector normal(SkinModelVector normal) {
            return rotate(normal).unit();
        }

        private SkinModelVector rotate(SkinModelVector point) {
            double cos = Math.cos(rotX);
            double sin = Math.sin(rotX);
            return new SkinModelVector(point.x(), point.y() * cos - point.z() * sin, point.y() * sin + point.z() * cos);
        }
    }

    private static final class Uv {
        final int x, y, w, h;
        Uv(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    private static final class UvSet {
        final Uv up, down, right, front, left, back;
        UvSet(Uv up, Uv down, Uv right, Uv front, Uv left, Uv back) {
            this.up = up; this.down = down; this.right = right; this.front = front; this.left = left; this.back = back;
        }
    }
}
