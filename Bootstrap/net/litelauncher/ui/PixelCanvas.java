package net.litelauncher.ui;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public abstract class PixelCanvas extends JPanel {

    private final int logicalWidth;
    private final int logicalHeight;
    private final int scale;
    private final BufferedImage backBuffer;
    private final MouseState mouseState;

    private boolean windowDragging;
    private int windowDragOffsetX;
    private int windowDragOffsetY;

    protected PixelCanvas(int logicalWidth, int logicalHeight, int scale) {
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.scale = scale;
        this.backBuffer = new BufferedImage(logicalWidth, logicalHeight, BufferedImage.TYPE_INT_ARGB);
        this.mouseState = new MouseState();

        setOpaque(false);
        setDoubleBuffered(true);
        setFocusable(true);
        setPreferredSize(new Dimension(logicalWidth * scale, logicalHeight * scale));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());

        InputHandler input = new InputHandler();
        addMouseListener(input);
        addMouseMotionListener(input);
    }

    protected abstract void render(PixelSurface surface, MouseState mouse);

    protected boolean handleInput(MouseState mouse) {
        return false;
    }

    protected boolean canStartWindowDrag(int logicalX, int logicalY) {
        return false;
    }

    @Override
    protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        PixelGraphics pixelGraphics = new PixelGraphics(backBuffer);
        pixelGraphics.clear();
        render(PixelSurface.direct(pixelGraphics), mouseState);
        pixelGraphics.dispose();

        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.drawImage(backBuffer, 0, 0, logicalWidth * scale, logicalHeight * scale, null);
        g2d.dispose();
    }

    private boolean startWindowDrag(MouseEvent event) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) return false;

        windowDragging = true;
        windowDragOffsetX = event.getXOnScreen() - window.getX();
        windowDragOffsetY = event.getYOnScreen() - window.getY();
        return true;
    }

    private void dragWindow(MouseEvent event) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) return;

        window.setLocation(event.getXOnScreen() - windowDragOffsetX, event.getYOnScreen() - windowDragOffsetY);
    }

    private void dispatchInput() {
        mouseState.beginFrame();
        if (handleInput(mouseState)) repaint();
        mouseState.applyCursor(this);
        mouseState.finishFrame();
    }

    private void updateMousePosition(MouseEvent event) {
        int logicalX = event.getX() / scale;
        int logicalY = event.getY() / scale;
        mouseState.setLogicalPosition(logicalX, logicalY);
    }

    private final class InputHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent event) {
            updateMousePosition(event);
            mouseState.onMousePressed(event.getButton());

            if (event.getButton() == MouseEvent.BUTTON1
                    && canStartWindowDrag(mouseState.getLogicalX(), mouseState.getLogicalY())
                    && startWindowDrag(event)) {
                mouseState.finishFrame();
                return;
            }

            dispatchInput();
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            updateMousePosition(event);
            mouseState.onMouseReleased(event.getButton());

            if (event.getButton() == MouseEvent.BUTTON1 && windowDragging) {
                windowDragging = false;
                mouseState.finishFrame();
                return;
            }

            dispatchInput();
        }

        @Override
        public void mouseEntered(MouseEvent event) {
            updateMousePosition(event);
            dispatchInput();
        }

        @Override
        public void mouseExited(MouseEvent event) {
            updateMousePosition(event);
            dispatchInput();
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            updateMousePosition(event);

            if (windowDragging) {
                dragWindow(event);
                return;
            }

            dispatchInput();
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            updateMousePosition(event);
            dispatchInput();
        }
    }
}
