package net.litelauncher.frontend.modules.button;

import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.interaction.Hotspot;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.PixelScrollView;

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

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public PixelButton(JComponent host, int x, int y, int width, int height, CompactRenderer renderer) {
        this(host, x, y, width, height,
                (surface, buttonX, buttonY, _, _, state) -> renderer.render(surface, buttonX, buttonY, state));
    }

    public void setBounds(int x, int y, int width, int height) {

        if (this.x == x && this.y == y && this.width == width && this.height == height) return;

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        hotspot.reset();
        host.repaint();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        reset();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean handleInput(MouseState mouse) {
        return handleInput(mouse, 0, 0, true, null);
    }

    public boolean handleInput(MouseState mouse, PixelScrollView scrollView) {
        boolean visible = scrollView.isContentBoundsVisible(x, y, x + width-1, y + height-1);
        return handleInput(mouse, scrollView.screenX(0), scrollView.screenY(0), visible, scrollView);
    }

    public boolean consumeClick() {
        boolean value = clicked;
        clicked = false;
        return value;
    }

    public void reset() {
        hotspot.reset();
        pressed = false;
        clicked = false;
        host.repaint();
    }

    public boolean contains(int logicalX, int logicalY) {
        return logicalX >= x && logicalX < x + width && logicalY >= y && logicalY < y + height;
    }

    public void render(PixelGraphics graphics) {
        render(PixelSurface.direct(graphics));
    }

    public void render(PixelSurface surface) {
        renderer.render(surface, x, y, width, height, state());
    }

    private boolean handleInput(MouseState mouse, int screenOriginX, int screenOriginY, boolean inputVisible, PixelScrollView scrollView) {
        boolean inputEnabled = enabled && inputVisible;
        boolean insideScrollContent = scrollView == null || scrollView.containsContentPoint(mouse.getLogicalX(), mouse.getLogicalY());
        boolean canUseInput = inputEnabled && insideScrollContent;

        hotspot.setBounds(screenOriginX + x, screenOriginY + y, screenOriginX + x + width-1, screenOriginY + y + height-1);

        boolean previousPressed = pressed;
        boolean dirty = hotspot.update(mouse, canUseInput);
        pressed = canUseInput && hotspot.isHovered() && mouse.isLeftDown();

        if (canUseInput && hotspot.consumeClick(mouse)) {
            clicked = true;
            host.repaint();
            return true;
        }

        return dirty || previousPressed != pressed;
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

    @FunctionalInterface
    public interface CompactRenderer {
        void render(PixelSurface surface, int x, int y, State state);
    }
}
