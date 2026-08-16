package net.litelauncher.frontend.modules.skin;

import net.litelauncher.frontend.modules.animation.Animated;
import net.litelauncher.frontend.modules.animation.AnimationTicker;
import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;

import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;

public final class SkinModelView implements Animated {
    private static final double IDLE_YAW_SPEED_PER_MS = 0.0005;

    private final JComponent host;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final SkinModelRenderer renderer = new SkinModelRenderer();

    private int inputX;
    private int inputY;
    private int inputWidth;
    private int inputHeight;
    private BufferedImage frame;
    private boolean frameDirty = true;
    private boolean dragging;
    private boolean disposed;
    private int lastMouseX;
    private int lastMouseY;
    private double yaw = Math.toRadians(32.0);
    private double pitch = Math.toRadians(9.0);

    public SkinModelView(JComponent host, int x, int y, int width, int height,
                         BufferedImage skin, boolean slim, BufferedImage cape) {
        this.host = Objects.requireNonNull(host);
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        setInputBounds(x, y, width, height);
        renderer.setAppearance(skin, slim, cape);
        AnimationTicker.register(this);
    }

    public void setInputBounds(int x, int y, int width, int height) {
        inputX = x;
        inputY = y;
        inputWidth = Math.max(1, width);
        inputHeight = Math.max(1, height);
    }

    public void setSkin(BufferedImage skin, boolean slim) {
        renderer.setSkin(skin, slim);
        markDirty();
    }

    public void setCape(BufferedImage cape) {
        renderer.setCape(cape);
        markDirty();
    }

    public boolean handleInput(MouseState mouse) {
        if (disposed || mouse == null) return false;

        boolean inside = contains(mouse.getLogicalX(), mouse.getLogicalY());
        if (inside || dragging) mouse.requestCursor(MouseCursor.MOVE);

        boolean dirty = false;
        boolean pressed = mouse.isLeftPressed() || mouse.isRightPressed();
        boolean down = mouse.isLeftDown() || mouse.isRightDown();

        if (!mouse.isConsumed() && pressed && inside) {
            dragging = true;
            lastMouseX = mouse.getLogicalX();
            lastMouseY = mouse.getLogicalY();
            mouse.consume();
            dirty = true;
        }

        if (dragging && down) {
            int dx = mouse.getLogicalX() - lastMouseX;
            int dy = mouse.getLogicalY() - lastMouseY;
            if (dx != 0 || dy != 0) {
                yaw += dx * 0.012;
                pitch = Math.clamp(pitch + dy * 0.010, Math.toRadians(-38.0), Math.toRadians(28.0));
                lastMouseX = mouse.getLogicalX();
                lastMouseY = mouse.getLogicalY();
                dirty = true;
                frameDirty = true;
            }
            mouse.consume();
        }

        if (dragging && !down) {
            dragging = false;
            dirty = true;
        }

        if (dirty) host.repaint();
        return dirty;
    }

    public void renderOverlay(Graphics2D graphics, int canvasScale) {
        if (disposed || graphics == null) return;

        int scale = Math.max(1, canvasScale);
        int pixelWidth = width * scale;
        int pixelHeight = height * scale;
        prepareFrame(pixelWidth, pixelHeight, scale);

        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            configureSharpGraphics(copy);
            copy.setClip(x * scale, y * scale, pixelWidth, pixelHeight);
            copy.drawImage(frame, x * scale, y * scale, null);
        } finally {
            copy.dispose();
        }
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        AnimationTicker.unregister(this);
    }

    @Override public boolean needsAnimation() {
        return !disposed;
    }

    @Override public void advance(long deltaMs) {
        if (disposed) return;
        if (!dragging) {
            yaw += Math.max(1, deltaMs) * IDLE_YAW_SPEED_PER_MS;
            frameDirty = true;
            host.repaint();
        }
    }

    private boolean contains(int logicalX, int logicalY) {
        return logicalX >= inputX && logicalX < inputX + inputWidth
                && logicalY >= inputY && logicalY < inputY + inputHeight;
    }

    private void markDirty() {
        frameDirty = true;
        host.repaint();
    }

    private void prepareFrame(int pixelWidth, int pixelHeight, int canvasScale) {
        if (frame == null || frame.getWidth() != pixelWidth || frame.getHeight() != pixelHeight) {
            frame = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
            renderer.clearSurface();
            frameDirty = true;
        }
        if (!frameDirty) return;

        Graphics2D graphics = frame.createGraphics();
        try {
            clearFrame(graphics, pixelWidth, pixelHeight);
            configureSharpGraphics(graphics);
            renderer.render(graphics, pixelWidth, pixelHeight, canvasScale, yaw, pitch);
        } finally {
            graphics.dispose();
        }
        frameDirty = false;
    }

    private static void clearFrame(Graphics2D graphics, int width, int height) {
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);
    }

    private static void configureSharpGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }

}
