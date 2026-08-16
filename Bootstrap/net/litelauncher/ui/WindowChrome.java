package net.litelauncher.ui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowEvent;

public final class WindowChrome {

    private final JComponent host;
    private final PixelButton minimizeButton;
    private final PixelButton closeButton;
    private final PixelText titleText;

    public WindowChrome(JComponent host) {
        this.host = host;
        titleText = new PixelText(host, 34, 14, 120, "LiteLauncher", PixelText.Alignment.LEFT, Palette.TITLE);
        minimizeButton = new PixelButton(host, 174, 10, 16, 16, this::drawMinimizeButton);
        closeButton = new PixelButton(host, 194, 10, 16, 16, this::drawCloseButton);
    }

    public boolean handleInput(MouseState mouse) {
        boolean dirty = minimizeButton.handleInput(mouse);
        dirty |= closeButton.handleInput(mouse);

        if (minimizeButton.consumeClick()) {
            minimizeWindow();
            return true;
        }

        if (closeButton.consumeClick()) {
            closeWindow();
            return true;
        }

        return dirty;
    }

    public boolean containsButton(int x, int y) {
        return minimizeButton.contains(x, y) || closeButton.contains(x, y);
    }

    public void render(PixelSurface surface) {
        surface.image(10, 10, 27, 27, "assets/light/logo.png");
        titleText.render(surface);
        minimizeButton.render(surface);
        closeButton.render(surface);
    }

    private void drawMinimizeButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        if (state == PixelButton.State.HOVERED) surface.paint(x, y, x + width - 1, y + height - 1, Palette.HOVERED);
        if (state == PixelButton.State.PRESSED) surface.paint(x, y, x + width - 1, y + height - 1, Palette.PRESSED);
        PixelPainter.drawMinimizeIcon(surface, x, y, Palette.OUTLINE);
    }

    private void drawCloseButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        if (state == PixelButton.State.HOVERED) surface.paint(x, y, x + width - 1, y + height - 1, Palette.HOVERED);
        if (state == PixelButton.State.PRESSED) surface.paint(x, y, x + width - 1, y + height - 1, Palette.PRESSED);
        PixelPainter.drawCloseIcon(surface, x, y, Palette.OUTLINE);
    }

    private void minimizeWindow() {
        Window window = SwingUtilities.getWindowAncestor(host);
        if (window instanceof Frame frame) frame.setState(Frame.ICONIFIED);
    }

    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(host);
        if (window != null) window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }
}
