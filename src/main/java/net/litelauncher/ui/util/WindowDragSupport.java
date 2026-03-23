package net.litelauncher.ui.util;

import javax.swing.JComponent;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class WindowDragSupport extends MouseAdapter {
    private final Window window;
    private Point clickScreenPoint;
    private Point windowOrigin;

    public WindowDragSupport(Window window) {
        this.window = window;
    }

    public void install(JComponent component) {
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        clickScreenPoint = event.getLocationOnScreen();
        windowOrigin = window.getLocation();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (clickScreenPoint == null || windowOrigin == null) {
            return;
        }
        Point screenPoint = event.getLocationOnScreen();
        int dx = screenPoint.x - clickScreenPoint.x;
        int dy = screenPoint.y - clickScreenPoint.y;
        window.setLocation(windowOrigin.x + dx, windowOrigin.y + dy);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        clickScreenPoint = null;
        windowOrigin = null;
    }
}
