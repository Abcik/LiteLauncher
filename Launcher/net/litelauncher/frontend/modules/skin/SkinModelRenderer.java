package net.litelauncher.frontend.modules.skin;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class SkinModelRenderer {
    private static final int SSAA = 2;
    private static final double MODEL_CENTER_Y = 16.5;
    private static final double MODEL_VERTICAL_OFFSET = -2.0;
    private static final double DEFAULT_SCALE = 4.35;
    private static final double FACE_LAYER_DEPTH_BIAS = 0.004;

    private final ArrayList<ProjectedFace> projectedFaces = new ArrayList<>(96);
    private int projectedFaceCount;
    private BufferedImage skin = SkinModelGeometry.fallbackSkin();
    private BufferedImage cape;
    private boolean slim;
    private SkinRenderBuffer surface;
    private List<SkinModelFace> classicFaces;
    private List<SkinModelFace> slimFaces;

    void setAppearance(BufferedImage skin, boolean slim, BufferedImage cape) {
        this.skin = SkinModelGeometry.normalizeSkin(skin);
        this.cape = SkinModelGeometry.validCape(cape) ? cape : null;
        this.slim = slim;
        clearModelCache();
    }

    void setSkin(BufferedImage skin, boolean slim) {
        this.skin = SkinModelGeometry.normalizeSkin(skin);
        this.slim = slim;
        clearModelCache();
    }

    void setCape(BufferedImage cape) {
        this.cape = SkinModelGeometry.validCape(cape) ? cape : null;
        clearModelCache();
    }

    void clearSurface() {
        surface = null;
    }

    void render(Graphics2D graphics, int viewW, int viewH, int canvasScale, double yaw, double pitch) {
        int highW = viewW * SSAA;
        int highH = viewH * SSAA;
        Projector projector = new Projector(
                highW * 0.5,
                (viewH * 0.5 + MODEL_VERTICAL_OFFSET * canvasScale) * SSAA,
                DEFAULT_SCALE * canvasScale * SSAA,
                yaw,
                pitch
        );

        RenderBounds bounds = projectVisibleFaces(currentFaces(), projector, highW, highH);
        if (bounds == null) return;

        surface = SkinRenderBuffer.ensure(surface, bounds.width(), bounds.height());
        surface.nextFrame();
        rasterModel(surface, bounds);
        drawSurface(graphics, surface.image(), bounds);
    }

    private void clearModelCache() {
        classicFaces = null;
        slimFaces = null;
        surface = null;
    }

    private List<SkinModelFace> currentFaces() {
        if (slim) {
            if (slimFaces == null) slimFaces = SkinModelGeometry.build(skin, true, cape);
            return slimFaces;
        }
        if (classicFaces == null) classicFaces = SkinModelGeometry.build(skin, false, cape);
        return classicFaces;
    }

    private RenderBounds projectVisibleFaces(List<SkinModelFace> faces, Projector projector, int highW, int highH) {
        projectedFaceCount = 0;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (SkinModelFace face : faces) {
            SkinModelVector rotatedNormal = projector.rotate(face.normal());
            if (rotatedNormal.z() <= 0.0001) continue;

            SkinModelVector r0 = projector.rotateModelPoint(face.v0());
            SkinModelVector r1 = projector.rotateModelPoint(face.v1());
            SkinModelVector r2 = projector.rotateModelPoint(face.v2());
            SkinModelVector r3 = projector.rotateModelPoint(face.v3());

            ProjectedFace projected = nextProjectedFace();
            projected.face = face;
            projected.x0 = projector.screenX(r0);
            projected.y0 = projector.screenY(r0);
            projected.z0 = biasedDepth(r0, face.layer());
            projected.x1 = projector.screenX(r1);
            projected.y1 = projector.screenY(r1);
            projected.z1 = biasedDepth(r1, face.layer());
            projected.x2 = projector.screenX(r2);
            projected.y2 = projector.screenY(r2);
            projected.z2 = biasedDepth(r2, face.layer());
            projected.x3 = projector.screenX(r3);
            projected.y3 = projector.screenY(r3);
            projected.z3 = biasedDepth(r3, face.layer());

            minX = Math.min(minX, Math.min(Math.min(projected.x0, projected.x1), Math.min(projected.x2, projected.x3)));
            minY = Math.min(minY, Math.min(Math.min(projected.y0, projected.y1), Math.min(projected.y2, projected.y3)));
            maxX = Math.max(maxX, Math.max(Math.max(projected.x0, projected.x1), Math.max(projected.x2, projected.x3)));
            maxY = Math.max(maxY, Math.max(Math.max(projected.y0, projected.y1), Math.max(projected.y2, projected.y3)));
        }

        if (projectedFaceCount == 0) return null;

        int padding = 3 * SSAA;
        int x0 = Math.max(0, (int) Math.floor(minX) - padding);
        int y0 = Math.max(0, (int) Math.floor(minY) - padding);
        int x1 = Math.min(highW, (int) Math.ceil(maxX) + padding);
        int y1 = Math.min(highH, (int) Math.ceil(maxY) + padding);
        return new RenderBounds(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    private ProjectedFace nextProjectedFace() {
        int index = projectedFaceCount++;
        if (index < projectedFaces.size()) return projectedFaces.get(index);
        ProjectedFace face = new ProjectedFace();
        projectedFaces.add(face);
        return face;
    }

    private void rasterModel(SkinRenderBuffer target, RenderBounds bounds) {
        int split = Math.max(1, bounds.height() / 2);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread top = renderWorker("skin-render-top", failure, () -> rasterRows(target, bounds, 0, split));
        Thread bottom = renderWorker("skin-render-bottom", failure, () -> rasterRows(target, bounds, split, bounds.height()));
        waitFor(top);
        waitFor(bottom);
        if (failure.get() != null) throw new IllegalStateException("Skin renderer failed", failure.get());
    }

    private static Thread renderWorker(String name, AtomicReference<Throwable> failure, Runnable task) {
        return Thread.ofVirtual().name(name).start(() -> {
            try {
                task.run();
            } catch (Throwable exception) {
                failure.compareAndSet(null, exception);
            }
        });
    }

    private void rasterRows(SkinRenderBuffer target, RenderBounds bounds, int yStart, int yEnd) {
        if (yStart >= yEnd) return;
        target.clearRows(0, bounds.width(), yStart, yEnd);

        for (int i = 0; i < projectedFaceCount; i++) {
            ProjectedFace projected = projectedFaces.get(i);
            SkinModelFace face = projected.face;
            rasterTriangle(target, bounds, yStart, yEnd, face,
                    projected.x0, projected.y0, projected.x1, projected.y1, projected.x2, projected.y2,
                    projected.z0, projected.z1, projected.z2,
                    0.0, 0.0, face.texW(), 0.0, face.texW(), face.texH());
            rasterTriangle(target, bounds, yStart, yEnd, face,
                    projected.x0, projected.y0, projected.x2, projected.y2, projected.x3, projected.y3,
                    projected.z0, projected.z2, projected.z3,
                    0.0, 0.0, face.texW(), face.texH(), 0.0, face.texH());
        }
    }

    private static void waitFor(Thread thread) {
        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException _) {
                interrupted = true;
                thread.interrupt();
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private static void rasterTriangle(SkinRenderBuffer target, RenderBounds bounds, int yStart, int yEnd, SkinModelFace face,
                                       double x0, double y0, double x1, double y1, double x2, double y2,
                                       float z0, float z1, float z2,
                                       double u0, double v0, double u1, double v1, double u2, double v2) {
        x0 -= bounds.x(); y0 -= bounds.y();
        x1 -= bounds.x(); y1 -= bounds.y();
        x2 -= bounds.x(); y2 -= bounds.y();

        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
        int maxX = Math.min(bounds.width() - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(yStart, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
        int maxY = Math.min(yEnd - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
        if (minX > maxX || minY > maxY) return;

        double area = edge(x0, y0, x1, y1, x2, y2);
        if (Math.abs(area) < 0.000001) return;
        double invArea = 1.0 / area;

        double startX = minX + 0.5;
        double startY = minY + 0.5;
        double aRow = edge(x1, y1, x2, y2, startX, startY) * invArea;
        double bRow = edge(x2, y2, x0, y0, startX, startY) * invArea;
        double aStepX = (y2 - y1) * invArea;
        double aStepY = -(x2 - x1) * invArea;
        double bStepX = (y0 - y2) * invArea;
        double bStepY = -(x0 - x2) * invArea;

        int[] texture = face.texture();
        int texW = face.texW();
        int texH = face.texH();
        int[] pixels = target.pixels();
        float[] zBuffer = target.zBuffer();
        int[] zStamp = target.zStamp();
        int stride = target.stride();
        int stamp = target.frameStamp();

        for (int y = minY; y <= maxY; y++) {
            double a = aRow;
            double b = bRow;
            int index = y * stride + minX;
            for (int x = minX; x <= maxX; x++, index++) {
                double c = 1.0 - a - b;
                if (a >= -0.0001 && b >= -0.0001 && c >= -0.0001) {
                    float z = (float) (z0 * a + z1 * b + z2 * c);
                    if (zStamp[index] != stamp || z >= zBuffer[index]) {
                        int tx = clampToTexture((int) (u0 * a + u1 * b + u2 * c), texW);
                        int ty = clampToTexture((int) (v0 * a + v1 * b + v2 * c), texH);
                        int argb = texture[ty * texW + tx];
                        int alpha = argb >>> 24;
                        if (alpha != 0) {
                            zStamp[index] = stamp;
                            zBuffer[index] = z;
                            pixels[index] = alpha == 255 ? argb : blendPremultiplied(argb, pixels[index]);
                        }
                    }
                }
                a += aStepX;
                b += bStepX;
            }
            aRow += aStepY;
            bRow += bStepY;
        }
    }

    private static int clampToTexture(int value, int size) {
        if (value < 0) return 0;
        if (value >= size) return size - 1;
        return value;
    }

    private static int blendPremultiplied(int src, int dst) {
        int alpha = src >>> 24;
        int inv = 255 - alpha;
        int outA = alpha + (((dst >>> 24) * inv + 127) / 255);
        int outR = ((src >>> 16) & 0xff) + ((((dst >>> 16) & 0xff) * inv + 127) / 255);
        int outG = ((src >>> 8) & 0xff) + ((((dst >>> 8) & 0xff) * inv + 127) / 255);
        int outB = (src & 0xff) + (((dst & 0xff) * inv + 127) / 255);
        return (Math.min(255, outA) << 24)
                | (Math.min(255, outR) << 16)
                | (Math.min(255, outG) << 8)
                | Math.min(255, outB);
    }

    private static double edge(double ax, double ay, double bx, double by, double cx, double cy) {
        return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
    }

    private static float biasedDepth(SkinModelVector rotatedPoint, int layer) {
        return (float) (rotatedPoint.z() + layer * FACE_LAYER_DEPTH_BIAS);
    }

    private static void drawSurface(Graphics2D graphics, BufferedImage image, RenderBounds bounds) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.translate(bounds.x() / (double) SSAA, bounds.y() / (double) SSAA);
        graphics.scale(1.0 / SSAA, 1.0 / SSAA);
        graphics.drawImage(image, 0, 0, bounds.width(), bounds.height(), 0, 0, bounds.width(), bounds.height(), null);
    }

    private record RenderBounds(int x, int y, int width, int height) { }

    private static final class ProjectedFace {
        SkinModelFace face;
        double x0, y0, x1, y1, x2, y2, x3, y3;
        float z0, z1, z2, z3;
    }

    private static final class Projector {
        private final double centerX;
        private final double centerY;
        private final double scale;
        private final double sinYaw;
        private final double cosYaw;
        private final double sinPitch;
        private final double cosPitch;

        Projector(double centerX, double centerY, double scale, double yaw, double pitch) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.scale = scale;
            sinYaw = Math.sin(yaw);
            cosYaw = Math.cos(yaw);
            sinPitch = Math.sin(pitch);
            cosPitch = Math.cos(pitch);
        }

        SkinModelVector rotateModelPoint(SkinModelVector point) {
            return rotate(point.x(), point.y() - MODEL_CENTER_Y, point.z());
        }

        SkinModelVector rotate(SkinModelVector vector) {
            return rotate(vector.x(), vector.y(), vector.z());
        }

        double screenX(SkinModelVector rotated) {
            return centerX + rotated.x() * scale;
        }

        double screenY(SkinModelVector rotated) {
            return centerY - rotated.y() * scale;
        }

        private SkinModelVector rotate(double x, double y, double z) {
            double x1 = x * cosYaw + z * sinYaw;
            double z1 = -x * sinYaw + z * cosYaw;
            double y2 = y * cosPitch - z1 * sinPitch;
            double z2 = y * sinPitch + z1 * cosPitch;
            return new SkinModelVector(x1, y2, z2);
        }
    }
}
