package net.litelauncher.frontend.modules.checkbox;

import net.litelauncher.frontend.Palette;
import net.litelauncher.LauncherStore;
import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.interaction.Hotspot;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.PixelScrollView;

import javax.swing.JComponent;
import java.awt.Color;

public final class CheckBox {

    private static final int SIZE = 9;

    private final JComponent host;
    private final Hotspot hotspot = new Hotspot(0, 0, 0, 0, MouseCursor.HAND);

    private final int x;
    private final int y;
    private boolean checked;
    private boolean pressed;
    private Runnable changeAction;

    public CheckBox(JComponent host, int x, int y, boolean checked) {
        this.host = host;
        this.x = x;
        this.y = y;
        this.checked = checked;
    }

    public boolean value() {
        return checked;
    }

    public void setValue(boolean checked) {
        if (this.checked == checked) return;
        this.checked = checked;
        if (changeAction != null) changeAction.run();
        host.repaint();
    }

    public void setChangeAction(Runnable changeAction) {
        this.changeAction = changeAction;
    }

    private void toggle() {
        setValue(!checked);
    }

    public boolean handleInput(MouseState mouse) {
        return handleInput(mouse, x, y, true, null);
    }

    public boolean handleInput(MouseState mouse, PixelScrollView scrollView) {
        boolean visible = scrollView.isContentBoundsVisible(x, y, x + SIZE - 1, y + SIZE - 1);
        return handleInput(mouse, scrollView.screenX(x), scrollView.screenY(y), visible, scrollView);
    }

    public void render(PixelGraphics graphics) {
        render(PixelSurface.direct(graphics));
    }

    public void render(PixelSurface surface) {
        drawBox(surface, x, y);
    }

    private boolean handleInput(MouseState mouse, int screenX, int screenY, boolean inputEnabled, PixelScrollView scrollView) {
        boolean insideScrollContent = scrollView == null || scrollView.containsContentPoint(mouse.getLogicalX(), mouse.getLogicalY());
        boolean canUseInput = inputEnabled && insideScrollContent;

        hotspot.setBounds(screenX, screenY, screenX + SIZE - 1, screenY + SIZE - 1);

        boolean previousPressed = pressed;
        boolean dirty = hotspot.update(mouse, canUseInput);
        pressed = canUseInput && hotspot.isHovered() && mouse.isLeftDown();

        if (canUseInput && hotspot.consumeClick(mouse)) {
            toggle();
            return true;
        }

        return dirty || previousPressed != pressed;
    }

    private void drawBox(PixelSurface surface, int x, int y) {

        if (checked) drawCheckedBackground(surface, x, y, Palette.ACCENT.color(LauncherStore.get().theme()));
        drawFrame(surface, x, y);

        Color animation = animationColor();
        if (animation != null) drawBoxOverlay(surface, x, y, animation);

        if (checked) drawCheckMark(surface, x, y, Palette.ACCENT_TITLE.color(LauncherStore.get().theme()));
    }

    private Color animationColor() {
        if (pressed) return Palette.ACCENT_PRESSED.color(LauncherStore.get().theme());
        if (hotspot.isHovered()) return Palette.ACCENT_HOVERED.color(LauncherStore.get().theme());
        return null;
    }

    private void drawCheckedBackground(PixelSurface surface, int x, int y, Color color) {
        surface.paint(x + 1, y + 1, x + 7, y + 7, color);
    }

    private void drawFrame(PixelSurface surface, int x, int y) {
        surface.paint(x + 1, y, x + 7, y, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        surface.paint(x, y + 1, x + 1, y + 1, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        surface.paint(x + 7, y + 1, x + 8, y + 1, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        surface.paint(x, y + 2, x, y + 6, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));

        surface.paint(x, y + 7, x + 1, y + 7, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        surface.paint(x + 1, y + 8, x + 7, y + 8, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        surface.paint(x + 7, y + 7, x + 7, y + 8, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        surface.paint(x + 8, y + 2, x + 8, y + 7, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
    }

    private void drawBoxOverlay(PixelSurface surface, int x, int y, Color color) {
        if (checked) drawCheckedBackground(surface, x, y, color);

        surface.paint(x + 1, y, x + 7, y, color);
        surface.paint(x, y + 1, x + 1, y + 1, color);
        surface.paint(x + 7, y + 1, x + 8, y + 1, color);
        surface.paint(x, y + 2, x, y + 7, color);
        surface.paint(x + 8, y + 2, x + 8, y + 7, color);
        surface.paint(x + 1, y + 8, x + 7, y + 8, color);
        surface.paint(x + 7, y + 7, x + 7, y + 8, color);
    }

    private void drawCheckMark(PixelSurface surface, int x, int y, Color color) {
        surface.paint(x + 2, y + 4, x + 2, y + 5, color);
        surface.paint(x + 3, y + 5, x + 3, y + 6, color);
        surface.paint(x + 4, y + 4, x + 4, y + 5, color);
        surface.paint(x + 5, y + 3, x + 5, y + 4, color);
        surface.paint(x + 6, y + 2, x + 6, y + 3, color);
    }
}
