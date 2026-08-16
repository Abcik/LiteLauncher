package net.litelauncher.frontend.modules.input;

import java.awt.Component;
import java.awt.event.MouseEvent;

public final class MouseState {

    private int logicalX;
    private int logicalY;

    private boolean leftDown;
    private boolean leftPressed;
    private boolean leftReleased;
    private boolean rightDown;
    private boolean rightPressed;
    private boolean rightReleased;
    private boolean consumed;

    private double wheelDelta;
    private MouseCursor requestedCursor = MouseCursor.DEFAULT;

    public void setLogicalPosition(int logicalX, int logicalY) {
        this.logicalX = logicalX;
        this.logicalY = logicalY;
    }

    public void onMousePressed(int button) {
        if (button == MouseEvent.BUTTON1) {
            if (!leftDown) leftPressed = true;
            leftDown = true;
        } else if (button == MouseEvent.BUTTON3) {
            if (!rightDown) rightPressed = true;
            rightDown = true;
        }
    }

    public void onMouseReleased(int button) {
        if (button == MouseEvent.BUTTON1) {
            leftDown = false;
            leftReleased = true;
        } else if (button == MouseEvent.BUTTON3) {
            rightDown = false;
            rightReleased = true;
        }
    }

    public void onMouseWheel(double wheelRotation) {
        wheelDelta += wheelRotation;
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

    public void clearCursorRequest() {
        requestedCursor = MouseCursor.DEFAULT;
    }

    public void applyCursor(Component component) {
        if (component == null) return;
        component.getCursor();
        int currentType = component.getCursor().getType();
        int requestedType = requestedCursor.awtCursor().getType();
        if (currentType != requestedType) component.setCursor(requestedCursor.awtCursor());
    }

    public void finishFrame() {
        leftPressed = false;
        leftReleased = false;
        rightPressed = false;
        rightReleased = false;
        wheelDelta = 0.0;
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

    public boolean isRightDown() {
        return rightDown;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }


    public boolean isConsumed() {
        return consumed;
    }

    public double getWheelDelta() {
        return wheelDelta;
    }
}
