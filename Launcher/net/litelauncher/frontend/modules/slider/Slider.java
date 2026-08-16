package net.litelauncher.frontend.modules.slider;

import net.litelauncher.frontend.Palette;
import net.litelauncher.LauncherStore;
import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.interaction.Hotspot;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.PixelScrollView;

import javax.swing.JComponent;
import java.awt.Color;

public final class Slider {

    private static final int TRACK_HEIGHT = 4;
    private static final int KNOB_WIDTH = 8;
    private static final int KNOB_HEIGHT = 8;
    private static final int KNOB_CENTER_X = 4;
    private static final int TRACK_Y = 2;

    private final JComponent host;
    private final Hotspot trackHotspot = new Hotspot(0, 0, 0, 0, MouseCursor.HAND, MouseCursor.MOVE_HORIZONTAL);
    private final Hotspot knobHotspot = new Hotspot(0, 0, 0, 0, MouseCursor.HAND, MouseCursor.MOVE_HORIZONTAL);

    private final int x;
    private final int y;
    private final int trackLength;
    private double value = 0.5;
    private boolean dragging;
    private Runnable changeAction;
    private int dragCenterOffset;

    public Slider(JComponent host, int x, int y, int trackLength) {
        this.host = host;
        this.x = x;
        this.y = y;
        this.trackLength = Math.max(1, trackLength);
    }

    public double value() {
        return value;
    }

    public void setValue(double value) {
        double next = Math.clamp(value, 0.0, 1.0);
        if (next == this.value) return;
        this.value = next;
        if (changeAction != null) changeAction.run();
        host.repaint();
    }

    public void setChangeAction(Runnable changeAction) {
        this.changeAction = changeAction;
    }

    public boolean resetInput() {
        boolean dirty = dragging || trackHotspot.isHovered() || knobHotspot.isHovered();
        dragging = false;
        trackHotspot.reset();
        knobHotspot.reset();
        if (dirty) host.repaint();
        return dirty;
    }

    private int width() {
        return trackLength + KNOB_WIDTH - 1;
    }

    private int height() {
        return KNOB_HEIGHT;
    }

    public boolean handleInput(MouseState mouse) {
        return handleInput(mouse, x, y, true, null);
    }

    public boolean handleInput(MouseState mouse, PixelScrollView scrollView) {
        boolean visible = scrollView.isContentBoundsVisible(x, y, x + width() - 1, y + height() - 1);
        return handleInput(mouse, scrollView.screenX(x), scrollView.screenY(y), visible, scrollView);
    }

    public void render(PixelGraphics graphics) {
        render(PixelSurface.direct(graphics));
    }

    public void render(PixelSurface surface) {
        drawTrack(surface, trackX(), trackY(), trackLength);
        drawBar(surface, trackX(), trackY(), fillWidth());
        drawKnob(surface, knobX(), y);

        Color animation = animationColor();
        if (animation != null) {
            drawBarOverlay(surface, trackX(), trackY(), fillWidth()-KNOB_CENTER_X, animation);
            drawKnobOverlay(surface, knobX(), y, animation);
        }
    }

    private boolean handleInput(MouseState mouse, int screenX, int screenY, boolean inputEnabled, PixelScrollView scrollView) {
        syncHotspots(screenX, screenY);

        int mouseX = mouse.getLogicalX();
        int mouseY = mouse.getLogicalY();
        boolean insideScrollContent = scrollView == null || scrollView.containsContentPoint(mouseX, mouseY);
        boolean canUpdate = inputEnabled && (insideScrollContent || dragging);
        boolean dirty = trackHotspot.update(mouse, canUpdate) | knobHotspot.update(mouse, canUpdate);
        boolean mouseCanStartDrag = inputEnabled && insideScrollContent && !mouse.isConsumed();

        int knobScreenX = screenX + knobOffset();
        int trackScreenX = screenX + KNOB_CENTER_X;

        if (mouse.isLeftPressed() && mouseCanStartDrag) {
            if (insideKnob(mouseX, mouseY, knobScreenX, screenY)) {
                dragging = true;
                dragCenterOffset = mouseX - (knobScreenX + KNOB_CENTER_X);
                mouse.consume();
                return true;
            }

            if (trackHotspot.contains(mouseX, mouseY)) {
                dragging = true;
                dragCenterOffset = 0;
                mouse.consume();
                setValueFromCenterX(mouseX, trackScreenX);
                return true;
            }
        }

        if (mouse.isLeftReleased() && dragging) {
            dragging = false;
            mouse.consume();
            return true;
        }

        if (dragging && mouse.isLeftDown()) {
            mouse.consume();
            return setValueFromCenterX(mouseX - dragCenterOffset, trackScreenX) || dirty;
        }

        return dirty;
    }

    private void syncHotspots(int screenX, int screenY) {
        int trackScreenX = screenX + KNOB_CENTER_X;
        int trackScreenY = screenY + TRACK_Y;
        int knobScreenX = screenX + knobOffset();

        trackHotspot.setBounds(trackScreenX, trackScreenY, trackScreenX + trackLength - 1, trackScreenY + TRACK_HEIGHT - 1);
        knobHotspot.setBounds(knobScreenX, screenY, knobScreenX + KNOB_WIDTH - 1, screenY + KNOB_HEIGHT - 1);
    }

    private int fillWidth() {
        return knobCenterX() - trackX() + 1;
    }

    private int knobX() {
        return x + knobOffset();
    }

    private int knobOffset() {
        return (int) Math.round(value * (trackLength - 1));
    }

    private int knobCenterX() {
        return knobX() + KNOB_CENTER_X;
    }

    private int trackX() {
        return x + KNOB_CENTER_X;
    }

    private int trackY() {
        return y + TRACK_Y;
    }

    private Color animationColor() {
        if (dragging) return Palette.ACCENT_PRESSED.color(LauncherStore.get().theme());
        if (trackHotspot.isHovered() || knobHotspot.isHovered()) return Palette.ACCENT_HOVERED.color(LauncherStore.get().theme());
        return null;
    }

    private boolean setValueFromCenterX(int centerX, int trackScreenX) {
        int maxX = trackScreenX + trackLength - 1;
        centerX = Math.clamp(centerX, trackScreenX, maxX);

        double next = trackLength <= 1 ? 0.0 : (double) (centerX - trackScreenX) / (double) (trackLength - 1);
        if (next == value) return false;
        setValue(next);
        return true;
    }

    private boolean insideKnob(int pointX, int pointY, int x, int y) {
        if (pointY < y || pointY > y + KNOB_HEIGHT - 1) return false;

        int localY = pointY - y;
        int left = x;
        int right = x + KNOB_WIDTH - 1;

        if (localY == 0 || localY == KNOB_HEIGHT - 1) {
            left = x + 1;
            right = x + KNOB_WIDTH - 2;
        }

        return pointX >= left && pointX <= right;
    }



    private void drawBar(PixelSurface surface, int x, int y, int width) {
        if (width <= 0) return;

        surface.paint(x, y + 1, x, y + 2, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));

        if (width > 1) {
            surface.paint(x + 1, y, x + width - 1, y, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
            surface.paint(x + 1, y + 1, x + width - 1, y + 2, Palette.ACCENT.color(LauncherStore.get().theme()));
            surface.paint(x + 1, y + 3, x + width - 1, y + 3, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        }
    }

    private void drawBarOverlay(PixelSurface surface, int x, int y, int width, Color color) {
        if (width <= 0) return;

        surface.paint(x, y + 1, x, y + 2, color);

        if (width > 1) {
            surface.paint(x + 1, y, x + width - 1, y, color);
            surface.paint(x + 1, y + 1, x + width - 1, y + 3, color);
        }
    }

    private void drawTrack(PixelSurface surface, int x, int y, int width) {
        Color outline = Palette.OUTLINE.color(LauncherStore.get().theme());
        if (width <= 0) return;
        if (width == 1) {
            surface.paint(x, y + 1, x, y + 2, outline);
            return;
        }
        surface.paint(x + 1, y, x + width - 2, y, outline);
        surface.paint(x, y + 1, x + width - 1, y + 2, outline);
        surface.paint(x + 1, y + 3, x + width - 2, y + 3, outline);
    }

    private void drawKnob(PixelSurface surface, int x, int y) {
        surface.paint(x + 1, y + 1, x + 6, y + 6, Palette.ACCENT.color(LauncherStore.get().theme()));

        surface.paint(x + 1, y, x + 6, y, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        surface.paint(x, y + 1, x + 1, y + 1, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        surface.paint(x + 6, y + 1, x + 7, y + 1, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        surface.paint(x, y + 2, x, y + 5, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));

        surface.paint(x + 7, y + 2, x + 7, y + 5, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        surface.paint(x, y + 6, x + 1, y + 6, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        surface.paint(x + 6, y + 6, x + 7, y + 6, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        surface.paint(x + 1, y + 7, x + 6, y + 7, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
    }

    private void drawKnobOverlay(PixelSurface surface, int x, int y, Color color) {
        surface.paint(x + 1, y, x + 6, y, color);
        surface.paint(x, y + 1, x + 7, y + 6, color);
        surface.paint(x + 1, y + 7, x + 6, y + 7, color);
    }
}
