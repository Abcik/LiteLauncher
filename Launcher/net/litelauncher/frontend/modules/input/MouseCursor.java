package net.litelauncher.frontend.modules.input;

import java.awt.Cursor;

public enum MouseCursor {
    DEFAULT(0, Cursor.DEFAULT_CURSOR),
    HAND(10, Cursor.HAND_CURSOR),
    TEXT(20, Cursor.TEXT_CURSOR),
    MOVE_HORIZONTAL(30, Cursor.W_RESIZE_CURSOR),
    MOVE(30, Cursor.MOVE_CURSOR);

    private final int priority;
    private final int awtCursorType;

    MouseCursor(int priority, int awtCursorType) {
        this.priority = priority;
        this.awtCursorType = awtCursorType;
    }

    public boolean strongerThan(MouseCursor other) {
        return other == null || priority > other.priority;
    }

    public Cursor awtCursor() {
        return Cursor.getPredefinedCursor(awtCursorType);
    }
}
