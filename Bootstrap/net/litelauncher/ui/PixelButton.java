package net.litelauncher.ui;

import javax.swing.JComponent;
import java.util.Objects;

public final class PixelButton {

    private final JComponent host;
    private final Hotspot hotspot = new Hotspot(0, 0, 0, 0, MouseCursor.HAND);
    private final Renderer renderer;

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean enabled = true;
    private boolean pressed;
    private boolean clicked;

    public PixelButton(JComponent host, int x, int y, int width, int height, Renderer renderer) {
        this.host = Objects.requireNonNull(host);
        this.renderer = Objects.requireNonNull(renderer);
        setBounds(x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        hotspot.setBounds(x, y, x + width - 1, y + height - 1);
        host.repaint();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        reset();
    }

    public boolean handleInput(MouseState mouse) {
        boolean previousPressed = pressed;
        boolean dirty = hotspot.update(mouse, enabled);
        pressed = enabled && hotspot.isHovered() && mouse.isLeftDown();

        if (enabled && hotspot.consumeClick(mouse)) {
            clicked = true;
            host.repaint();
            return true;
        }

        return dirty || previousPressed != pressed;
    }

    public boolean consumeClick() {
        boolean value = clicked;
        clicked = false;
        return value;
    }

    public boolean contains(int logicalX, int logicalY) {
        return logicalX >= x && logicalX < x + width && logicalY >= y && logicalY < y + height;
    }

    public void reset() {
        hotspot.reset();
        pressed = false;
        clicked = false;
        host.repaint();
    }

    public void render(PixelSurface surface) {
        renderer.render(surface, x, y, width, height, state());
    }

    private State state() {
        if (!enabled) return State.DISABLED;
        if (pressed) return State.PRESSED;
        if (hotspot.isHovered()) return State.HOVERED;
        return State.NORMAL;
    }

    public enum State {
        NORMAL, HOVERED, PRESSED, DISABLED
    }

    @FunctionalInterface
    public interface Renderer {
        void render(PixelSurface surface, int x, int y, int width, int height, State state);
    }
}
