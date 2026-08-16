package net.litelauncher.frontend.modules.scroll;

import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;

public final class ScrollCanvas extends PixelSurface {

    private final PixelScrollView owner;

    ScrollCanvas(PixelScrollView owner) {
        super(owner::touchBottom);
        this.owner = owner;
    }

    void beginMeasure() {
        begin(null, owner.contentX(), owner.contentY(), false);
    }

    void beginRender(PixelGraphics graphics) {
        begin(graphics, owner.contentX(), owner.contentY(), true);
    }

    public void hotspot(Object key, int xMin, int yMin, int xMax, int yMax) {
        hotspot(key, xMin, yMin, xMax, yMax, MouseCursor.DEFAULT);
    }

    public void hotspot(Object key, int xMin, int yMin, int xMax, int yMax, MouseCursor cursor) {
        touchBottom(yMax);
        owner.hotspot(key, screenX(xMin), screenY(yMin), screenX(xMax), screenY(yMax), cursor);
    }

    public void hotspot(Object key, int xMin, int yMin, int xMax, int yMax, MouseCursor hoverCursor, MouseCursor dragCursor) {
        touchBottom(yMax);
        owner.hotspot(key, screenX(xMin), screenY(yMin), screenX(xMax), screenY(yMax), hoverCursor, dragCursor);
    }

    public boolean hovered(Object key) {
        return owner.isHovered(key);
    }
}
