package net.litelauncher.ui;

public final class Hotspot {

    private int xMin;
    private int yMin;
    private int xMax;
    private int yMax;
    private MouseCursor hoverCursor = MouseCursor.DEFAULT;
    private boolean hovered;
    private boolean pressedInside;
    private boolean clicked;

    public Hotspot(int xMin, int yMin, int xMax, int yMax, MouseCursor hoverCursor) {
        setBounds(xMin, yMin, xMax, yMax);
        this.hoverCursor = hoverCursor == null ? MouseCursor.DEFAULT : hoverCursor;
    }

    public void setBounds(int xMin, int yMin, int xMax, int yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public void reset() {
        hovered = false;
        pressedInside = false;
        clicked = false;
    }

    public boolean update(MouseState mouse, boolean enabled) {
        boolean previousHovered = hovered;
        hovered = enabled && contains(mouse.getLogicalX(), mouse.getLogicalY());

        if (hovered) mouse.requestCursor(hoverCursor);

        if (!enabled || mouse.isConsumed()) {
            if (mouse.isLeftReleased()) pressedInside = false;
            return previousHovered != hovered;
        }

        if (mouse.isLeftPressed() && hovered) pressedInside = true;

        if (mouse.isLeftReleased()) {
            clicked = pressedInside && hovered;
            pressedInside = false;
        }

        return previousHovered != hovered || clicked;
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
}
