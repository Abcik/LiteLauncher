package net.litelauncher.frontend.modules.overlay;

import net.litelauncher.LauncherStore;
import net.litelauncher.Theme;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;

import javax.swing.JComponent;
import java.awt.Graphics2D;

public final class PopupLayer {

    public interface BackdropPainter {
        void paint(PixelGraphics graphics, Theme theme);
    }

    private final JComponent host;
    private final BackdropPainter backdropPainter;

    private PopupContent content;
    private boolean backdropArmed;

    public PopupLayer(JComponent host, BackdropPainter backdropPainter) {
        this.host = host;
        this.backdropPainter = backdropPainter;
    }

    public boolean isOpen() {
        return content != null;
    }

    public void open(PopupContent content) {
        open(content, null);
    }

    public void open(PopupContent content, Runnable onOpened) {
        close();
        this.content = content;
        this.backdropArmed = true;
        content.onOpen();
        if (onOpened != null) onOpened.run();
        host.requestFocusInWindow();
        host.repaint();
    }

    public void close() {
        closeCurrent(false);
    }

    public void dispose() {
        closeCurrent(true);
    }

    private void closeCurrent(boolean forceDispose) {
        if (content == null) return;

        PopupContent closed = content;
        content = null;
        backdropArmed = false;

        closed.onClose();
        if (forceDispose || closed.disposeOnClose()) closed.dispose();

        host.repaint();
    }

    public boolean handleInput(MouseState mouse) {
        if (content == null) return false;
        if (!mouse.isLeftDown()) backdropArmed = true;
        if (backdropArmed && mouse.isLeftPressed() && outside(mouse)) {
            if (content.closeOnBackdrop()) close();
            mouse.consume();
            return true;
        }
        return content.handleInput(mouse);
    }

    public void render(PixelGraphics graphics, MouseState mouse) {
        if (content == null) return;
        backdropPainter.paint(graphics, LauncherStore.get().theme());
        content.render(graphics, mouse);
    }

    public void renderOverlay(Graphics2D graphics, int scale) {
        if (content == null) return;
        content.renderOverlay(graphics, scale);
    }

    private boolean outside(MouseState mouse) {
        return mouse.getLogicalX() < content.xMin()
                || mouse.getLogicalX() > content.xMax()
                || mouse.getLogicalY() < content.yMin()
                || mouse.getLogicalY() > content.yMax();
    }
}
