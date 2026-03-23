package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class PopupInteractiveRow extends SurfacePanel {
    private static final String LISTENER_BOUND_KEY = "lite.popupRow.listenerBound";

    private final int arc;
    private Runnable onClick;
    private boolean hovered;
    private boolean pressed;
    private boolean armed;
    private boolean selectedState;

    private final MouseInputAdapter interactionHandler = new MouseInputAdapter() {
        @Override public void mouseEntered(MouseEvent event) { updateHoverState(event); }
        @Override public void mouseExited(MouseEvent event) { updateHoverState(event); }
        @Override public void mouseMoved(MouseEvent event) { updateHoverState(event); }
        @Override public void mouseDragged(MouseEvent event) { updateHoverState(event); }

        @Override
        public void mousePressed(MouseEvent event) {
            if (!isEnabled() || !SwingUtilities.isLeftMouseButton(event)) {
                return;
            }
            boolean inside = isInsideRow(event);
            armed = inside;
            pressed = inside;
            hovered = inside;
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (!SwingUtilities.isLeftMouseButton(event)) {
                return;
            }
            boolean inside = isInsideRow(event);
            boolean activate = armed && inside && isEnabled();
            armed = false;
            pressed = false;
            hovered = inside;
            repaint();
            if (activate && onClick != null) {
                onClick.run();
            }
        }
    };

    protected PopupInteractiveRow(int arc, Insets padding, boolean elevated) {
        super(arc, padding, elevated);
        this.arc = arc;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bindMouseHandlers(this);
    }

    protected void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    protected void setSelectedState(boolean selectedState) {
        this.selectedState = selectedState;
        repaint();
    }

    protected boolean isSelectedState() {
        return selectedState;
    }

    public void syncHoverFromPointer() {
        try {
            if (!isShowing()) {
                return;
            }
            Point location = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
            if (location == null) {
                return;
            }
            SwingUtilities.convertPointFromScreen(location, this);
            boolean inside = contains(location);
            if (hovered != inside) {
                hovered = inside;
                repaint();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        bindRecursively(comp);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        ThemePalette palette = ThemeManager.getInstance().palette();
        java.awt.Color fill = palette.elevatedSurface;
        java.awt.Color border = palette.border;
        if (selectedState) {
            fill = pressed ? palette.accentPressed : hovered ? palette.accentHover : palette.accent;
            border = PopupMetrics.ACCENT_BORDER;
        } else if (pressed) {
            fill = palette.controlPressed;
        } else if (hovered) {
            fill = palette.controlHover;
        }
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
        g2.setColor(border);
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
        g2.dispose();
    }

    private void bindRecursively(Component component) {
        if (component instanceof AbstractButton) {
            return;
        }
        bindMouseHandlers(component);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                bindRecursively(child);
            }
        }
    }

    private void bindMouseHandlers(Component component) {
        if (component instanceof JComponent) {
            JComponent jc = (JComponent) component;
            if (Boolean.TRUE.equals(jc.getClientProperty(LISTENER_BOUND_KEY))) {
                return;
            }
            jc.putClientProperty(LISTENER_BOUND_KEY, Boolean.TRUE);
        }
        component.addMouseListener(interactionHandler);
        component.addMouseMotionListener(interactionHandler);
    }

    private void updateHoverState(MouseEvent event) {
        boolean inside = isInsideRow(event);
        if (hovered != inside) {
            hovered = inside;
            repaint();
        }
    }

    private boolean isInsideRow(MouseEvent event) {
        Component source = event.getComponent();
        Point point = SwingUtilities.convertPoint(source, event.getPoint(), this);
        return contains(point);
    }
}
