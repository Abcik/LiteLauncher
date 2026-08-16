package net.litelauncher.frontend.modules.interaction;

import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;

public final class Hotspot {

    private int xMin;
    private int yMin;
    private int xMax;
    private int yMax;

    private MouseCursor hoverCursor = MouseCursor.DEFAULT;
    private MouseCursor dragCursor = MouseCursor.DEFAULT;

    private boolean hovered;
    private boolean pressedInside;
    private boolean clicked;

    public Hotspot(int xMin, int yMin, int xMax, int yMax) {
        setBounds(xMin, yMin, xMax, yMax);
    }

    public Hotspot(int xMin, int yMin, int xMax, int yMax, MouseCursor hoverCursor) {
        this(xMin, yMin, xMax, yMax);
        setCursor(hoverCursor);
    }

    public Hotspot(int xMin, int yMin, int xMax, int yMax, MouseCursor hoverCursor, MouseCursor dragCursor) {
        this(xMin, yMin, xMax, yMax);
        setCursor(hoverCursor, dragCursor);
    }

    public void setBounds(int xMin, int yMin, int xMax, int yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public Hotspot setCursor(MouseCursor cursor) {
        return setCursor(cursor, cursor);
    }

    public Hotspot setCursor(MouseCursor hoverCursor, MouseCursor dragCursor) {
        this.hoverCursor = hoverCursor == null ? MouseCursor.DEFAULT : hoverCursor;
        this.dragCursor = dragCursor == null ? this.hoverCursor : dragCursor;
        return this;
    }

    public void reset() {
        hovered = false;
        pressedInside = false;
        clicked = false;
    }

    public boolean update(MouseState mouse) {
        return update(mouse, true);
    }

    public boolean update(MouseState mouse, boolean enabled) {
        boolean previousHovered = hovered;
        hovered = enabled && contains(mouse.getLogicalX(), mouse.getLogicalY());

        if (enabled) requestCursor(mouse);

        if (!enabled || mouse.isConsumed()) {
            if (mouse.isLeftReleased()) pressedInside = false;
            return previousHovered != hovered;
        }

        if (mouse.isLeftPressed() && hovered) pressedInside = true;

        if (mouse.isLeftReleased()) {
            clicked = pressedInside && hovered;
            pressedInside = false;
        }

        requestCursor(mouse);
        return previousHovered != hovered || clicked;
    }

    public boolean consumeClick() {
        boolean value = clicked;
        clicked = false;
        return value;
    }

    public boolean consumeClick(MouseState mouse) {
        if (!clicked || mouse.isConsumed()) {
            clicked = false;
            return false;
        }

        clicked = false;
        mouse.consume();
        return true;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean contains(int x, int y) {
        return x >= xMin && x <= xMax && y >= yMin && y <= yMax;
    }

    private void requestCursor(MouseState mouse) {
        if (pressedInside && mouse.isLeftDown()) {
            mouse.requestCursor(dragCursor);
            return;
        }

        if (hovered) mouse.requestCursor(hoverCursor);
    }
}
