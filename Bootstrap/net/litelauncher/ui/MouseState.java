package net.litelauncher.ui;

import java.awt.Component;
import java.awt.event.MouseEvent;

public final class MouseState {

    private int logicalX;
    private int logicalY;
    private boolean leftDown;
    private boolean leftPressed;
    private boolean leftReleased;
    private boolean consumed;
    private MouseCursor requestedCursor = MouseCursor.DEFAULT;

    public void setLogicalPosition(int logicalX, int logicalY) {
        this.logicalX = logicalX;
        this.logicalY = logicalY;
    }

    public void onMousePressed(int button) {
        if (button != MouseEvent.BUTTON1) return;
        if (!leftDown) leftPressed = true;
        leftDown = true;
    }

    public void onMouseReleased(int button) {
        if (button != MouseEvent.BUTTON1) return;
        leftDown = false;
        leftReleased = true;
    }

    public void consume() {
        consumed = true;
    }

    public void beginFrame() {
        requestedCursor = MouseCursor.DEFAULT;
    }

    public void requestCursor(MouseCursor cursor) {
        if (cursor == null) return;
        if (cursor.strongerThan(requestedCursor)) requestedCursor = cursor;
    }

    public void applyCursor(Component component) {
        if (component == null) return;
        int currentType = component.getCursor().getType();
        int requestedType = requestedCursor.awtCursor().getType();
        if (currentType != requestedType) component.setCursor(requestedCursor.awtCursor());
    }

    public void finishFrame() {
        leftPressed = false;
        leftReleased = false;
        consumed = false;
    }

    public int getLogicalX() {
        return logicalX;
    }

    public int getLogicalY() {
        return logicalY;
    }

    public boolean isLeftDown() {
        return leftDown;
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isLeftReleased() {
        return leftReleased;
    }

    public boolean isConsumed() {
        return consumed;
    }
}
