package net.litelauncher.frontend.modules.overlay;

import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;

import java.awt.Graphics2D;

public interface PopupContent {

    int xMin();

    int yMin();

    int xMax();

    int yMax();

    void render(PixelGraphics graphics, MouseState mouse);

    default void renderOverlay(Graphics2D graphics, int scale) {
    }

    default boolean handleInput(MouseState mouse) {
        return false;
    }

    default void onOpen() {
    }

    default void onClose() {
    }

    default boolean closeOnBackdrop() {
        return true;
    }

    default boolean disposeOnClose() {
        return false;
    }

    default void dispose() {
    }

}
