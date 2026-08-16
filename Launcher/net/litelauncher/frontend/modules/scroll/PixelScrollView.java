package net.litelauncher.frontend.modules.scroll;

import net.litelauncher.frontend.Palette;
import net.litelauncher.LauncherStore;
import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.interaction.Hotspot;
import net.litelauncher.frontend.modules.interaction.HotspotRegistry;
import net.litelauncher.frontend.modules.render.PixelGraphics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class PixelScrollView {

    private record ScrollVote(long time, double weight) {
    }

    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_SLOT_WIDTH = 6;
    private static final int SCROLLBAR_RIGHT_PADDING = 2;
    private static final int CONTENT_SCROLLBAR_GAP = 6;
    private static final int MIN_THUMB_HEIGHT = 16;
    private static final int DEFAULT_WHEEL_STEP = 8;
    private static final long SCROLL_VOTE_WINDOW_MS = 100L;

    private int x;
    private int y;
    private int width;
    private int height;
    private final ScrollCanvas canvas = new ScrollCanvas(this);
    private final Hotspot thumbHotspot = new Hotspot(0, 0, -1, -1, MouseCursor.DEFAULT, MouseCursor.DEFAULT);
    private final Hotspot scrollbarHotspot = new Hotspot(0, 0, -1, -1, MouseCursor.DEFAULT, MouseCursor.DEFAULT);
    private final HotspotRegistry contentHotspots = new HotspotRegistry();
    private final List<ScrollVote> scrolls = new ArrayList<>();

    private int contentPaddingTop;
    private int contentPaddingBottom;
    private int contentPaddingLeft;
    private int contentPaddingRight;

    private int measuredContentBottom = -1;
    private int measuredContentHeight;
    private int expectedContentHeight = -1;
    private int scrollOffset;
    private boolean draggingThumb;
    private int dragThumbOffset;
    private boolean contentDirty = true;
    private boolean layoutValid;
    private int layoutScrollOffset = Integer.MIN_VALUE;
    private int layoutViewportWidth = Integer.MIN_VALUE;

    public PixelScrollView(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public PixelScrollView setBounds(int x, int y, int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (this.x == x && this.y == y && this.width == safeWidth && this.height == safeHeight) return this;
        this.x = x;
        this.y = y;
        this.width = safeWidth;
        this.height = safeHeight;
        draggingThumb = false;
        thumbHotspot.reset();
        scrollbarHotspot.reset();
        markContentDirty();
        clampScroll();
        return this;
    }

    private int getViewportWidth() {
        return Math.max(1, width - reservedRightSpace());
    }

    public boolean isScrollable() {
        return maxScroll() > 0;
    }

    public int screenX(int localContentX) {
        return contentX() + localContentX;
    }

    public int screenY(int localContentY) {
        return contentY() + localContentY;
    }

    public boolean containsContentPoint(int screenX, int screenY) {
        return screenX >= x && screenX <= viewportRight() && screenY >= y && screenY <= viewportBottom();
    }

    public boolean isContentBoundsVisible(int localXMin, int localYMin, int localXMax, int localYMax) {
        int screenXMin = screenX(localXMin);
        int screenYMin = screenY(localYMin);
        int screenXMax = screenX(localXMax);
        int screenYMax = screenY(localYMax);
        return screenXMax >= x && screenXMin <= viewportRight() && screenYMax >= y && screenYMin <= viewportBottom();
    }

    public PixelScrollView setContentPadding(int left, int top, int right, int bottom) {
        this.contentPaddingLeft = Math.max(0, left);
        this.contentPaddingTop = Math.max(0, top);
        this.contentPaddingRight = Math.max(0, right);
        this.contentPaddingBottom = Math.max(0, bottom);
        return this;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int visibleContentTop() {
        return Math.max(0, scrollOffset - contentPaddingTop);
    }

    public int visibleContentBottom() {
        return Math.max(0, scrollOffset - contentPaddingTop + height - 1);
    }

    public PixelScrollView setScrollOffset(int scrollOffset) {
        int value = Math.max(0, scrollOffset);
        if (this.scrollOffset == value) return this;

        this.scrollOffset = value;
        markContentDirty();
        return this;
    }

    public PixelScrollView setExpectedContentHeight(int contentHeight) {
        int value = Math.max(0, contentHeight);
        if (this.expectedContentHeight == value) {
            clampScroll();
            return this;
        }

        this.expectedContentHeight = value;
        markContentDirty();
        clampScroll();
        return this;
    }

    public void markContentDirty() {
        contentDirty = true;
        layoutValid = false;
    }

    public boolean handleInput(MouseState mouse, ScrollContent builder) {
        layoutIfNeeded(builder);

        int previousOffset = scrollOffset;
        boolean dirty = handleWheel(mouse);
        dirty |= handleScrollbar(mouse);
        clampScroll();

        if (scrollOffset != previousOffset) layoutIfNeeded(builder);
        dirty |= contentHotspots.update(mouse);

        return dirty || previousOffset != scrollOffset;
    }

    public void render(PixelGraphics graphics, ScrollContent builder) {
        layoutIfNeeded(builder);
        clampScroll();

        graphics.pushClip(x, y, viewportRight(), viewportBottom());
        canvas.beginRender(graphics);
        builder.build(canvas);
        graphics.popClip();

        drawScrollbar(graphics);
    }

    public boolean consumeClick(Object key) {
        return contentHotspots.consumeClick(key);
    }

    public boolean consumeClick(Object key, MouseState mouse) {
        return contentHotspots.consumeClick(key, mouse);
    }

    public boolean isHovered(Object key) {
        return contentHotspots.hovered(key);
    }

    private void layoutIfNeeded(ScrollContent builder) {
        int viewportWidth = getViewportWidth();
        if (layoutValid && !contentDirty && layoutScrollOffset == scrollOffset && layoutViewportWidth == viewportWidth) return;

        layout(builder);
        contentDirty = false;
        layoutValid = true;
        layoutScrollOffset = scrollOffset;
        layoutViewportWidth = viewportWidth;
    }

    private void layout(ScrollContent builder) {
        contentHotspots.begin();
        measuredContentBottom = -1;

        if (expectedContentHeight >= 0) {
            measuredContentHeight = expectedContentHeight;
        } else {
            canvas.beginMeasure();
            builder.build(canvas);
            measuredContentHeight = Math.max(0, measuredContentBottom + 1);
        }

        contentHotspots.end();
    }

    private boolean handleWheel(MouseState mouse) {
        double wheelDelta = mouse.getWheelDelta();
        if (wheelDelta == 0.0 || mouse.isConsumed()) return false;
        if (!insideRect(mouse.getLogicalX(), mouse.getLogicalY(), x, y, width, height)) return false;

        long now = System.currentTimeMillis();
        scrolls.add(new ScrollVote(now, wheelDelta));
        scrolls.removeIf(scroll -> now - scroll.time() > SCROLL_VOTE_WINDOW_MS);

        double previousDirection = 0.0;
        for (ScrollVote scroll : scrolls) previousDirection += scroll.weight();

        if (Math.signum(wheelDelta) != Math.signum(previousDirection)) {
            mouse.consume();
            return false;
        }

        int scrollPixels = (int) Math.round(wheelDelta * DEFAULT_WHEEL_STEP);
        if (scrollPixels == 0) {
            mouse.consume();
            return false;
        }

        scrollOffset += scrollPixels;
        mouse.consume();
        return true;
    }

    private boolean handleScrollbar(MouseState mouse) {
        if (!isScrollable()) {
            draggingThumb = false;
            thumbHotspot.reset();
            scrollbarHotspot.reset();
            return false;
        }

        int previousOffset = scrollOffset;
        syncThumbHotspot();
        boolean dirty = scrollbarHotspot.update(mouse);
        dirty |= thumbHotspot.update(mouse);

        int mouseX = mouse.getLogicalX();
        int mouseY = mouse.getLogicalY();
        int thumbHeight = thumbHeight();
        int thumbY = thumbY(thumbHeight);
        boolean canUseMouse = !mouse.isConsumed();

        if (mouse.isLeftPressed() && canUseMouse) {
            if (insideVerticalPill(mouseX, mouseY, scrollbarX(), thumbY, thumbHeight)) {
                draggingThumb = true;
                dragThumbOffset = mouseY - thumbY;
                mouse.consume();
                dirty = true;
            } else if (insideVerticalPill(mouseX, mouseY, scrollbarX(), y, height)) {
                draggingThumb = true;
                dragThumbOffset = thumbHeight / 2;
                setScrollFromThumbY(mouseY - dragThumbOffset, thumbHeight);
                mouse.consume();
                dirty = true;
            }
        }

        if (mouse.isLeftReleased() && draggingThumb) {
            draggingThumb = false;
            mouse.consume();
            dirty = true;
        }

        if (draggingThumb && mouse.isLeftDown()) {
            setScrollFromThumbY(mouseY - dragThumbOffset, thumbHeight);
            mouse.consume();
            dirty = true;
        }

        clampScroll();
        syncThumbHotspot();
        scrollbarHotspot.update(mouse);
        thumbHotspot.update(mouse);
        return dirty || previousOffset != scrollOffset;
    }

    void hotspot(Object key, int screenXMin, int screenYMin, int screenXMax, int screenYMax) {
        hotspot(key, screenXMin, screenYMin, screenXMax, screenYMax, MouseCursor.DEFAULT);
    }

    void hotspot(Object key, int screenXMin, int screenYMin, int screenXMax, int screenYMax, MouseCursor cursor) {
        hotspot(key, screenXMin, screenYMin, screenXMax, screenYMax, cursor, cursor);
    }

    void hotspot(Object key, int screenXMin, int screenYMin, int screenXMax, int screenYMax, MouseCursor hoverCursor, MouseCursor dragCursor) {
        int clippedXMin = Math.max(screenXMin, x);
        int clippedYMin = Math.max(screenYMin, y);
        int clippedXMax = Math.min(screenXMax, viewportRight());
        int clippedYMax = Math.min(screenYMax, viewportBottom());

        if (clippedXMin > clippedXMax || clippedYMin > clippedYMax) {
            contentHotspots.hide(key);
            return;
        }

        contentHotspots.set(key, clippedXMin, clippedYMin, clippedXMax, clippedYMax, hoverCursor, dragCursor);
    }

    void touchBottom(int localBottomY) {
        if (localBottomY > measuredContentBottom) measuredContentBottom = localBottomY;
    }

    private void drawScrollbar(PixelGraphics graphics) {
        if (!isScrollable()) return;

        syncThumbHotspot();
        int thumbHeight = thumbHeight();
        int thumbY = thumbY(thumbHeight);

        drawTrack(graphics, scrollbarX(), y, height);
        drawThumb(graphics, scrollbarX(), thumbY, thumbHeight);

        Color animation = scrollbarAnimationColor();
        if (animation != null) drawThumbOverlay(graphics, scrollbarX(), thumbY, thumbHeight, animation);
    }

    private Color scrollbarAnimationColor() {
        if (draggingThumb) return Palette.ACCENT_PRESSED.color(LauncherStore.get().theme());
        if (thumbHotspot.isHovered()) return Palette.ACCENT_HOVERED.color(LauncherStore.get().theme());
        return null;
    }

    private void drawThumb(PixelGraphics graphics, int x, int y, int height) {
        if (height <= 0) return;

        if (height == 1) {
            graphics.paint(x + 1, y, x + 2, y, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
            return;
        }
        graphics.paint(x + 1, y, x + 2, y, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        graphics.paint(x, y + 1, x, y + height - 2, Palette.ACCENT_GLARE.color(LauncherStore.get().theme()));
        graphics.paint(x + 1, y + 1, x + 2, y + height - 2, Palette.ACCENT.color(LauncherStore.get().theme()));
        graphics.paint(x + 3, y + 1, x + 3, y + height - 2, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
        graphics.paint(x + 1, y + height - 1, x + 2, y + height - 1, Palette.ACCENT_SHADOW.color(LauncherStore.get().theme()));
    }

    private void drawThumbOverlay(PixelGraphics graphics, int x, int y, int height, Color color) {
        if (height <= 0) return;
        if (height == 1) {
            graphics.paint(x + 1, y, x + 2, y, color);
            return;
        }

        graphics.paint(x + 1, y, x + 2, y, color);
        graphics.paint(x, y + 1, x + 3, y + height - 2, color);
        graphics.paint(x + 1, y + height - 1, x + 2, y + height - 1, color);
    }

    private void drawTrack(PixelGraphics graphics, int x, int y, int height) {
        Color outline = Palette.OUTLINE.color(LauncherStore.get().theme());
        graphics.paint(x + 1, y, x + 2, y + height - 1, outline);
        graphics.paint(x, y + 1, x + 3, y + height - 2, outline);
    }

    private boolean insideVerticalPill(int pointX, int pointY, int x, int y, int height) {
        if (height <= 0) return false;
        if (pointY < y || pointY > y + height - 1) return false;

        int localY = pointY - y;
        int left = x;
        int right = x + SCROLLBAR_WIDTH - 1;

        if (height == 1 || localY == 0 || localY == height - 1) {
            left = x + 1;
            right = x + 2;
        }

        return pointX >= left && pointX <= right;
    }

    private void syncThumbHotspot() {
        if (!isScrollable()) {
            thumbHotspot.setBounds(0, 0, -1, -1);
            scrollbarHotspot.setBounds(0, 0, -1, -1);
            return;
        }

        int barX = scrollbarX();
        int thumbHeight = thumbHeight();
        int thumbY = thumbY(thumbHeight);
        scrollbarHotspot.setBounds(barX, y, barX + SCROLLBAR_WIDTH - 1, y + height - 1);
        thumbHotspot.setBounds(barX, thumbY, barX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight - 1);
    }

    int contentX() {
        return x + contentPaddingLeft;
    }

    int contentY() {
        return y + contentPaddingTop - scrollOffset;
    }

    private int viewportRight() {
        return x + getViewportWidth() - 1;
    }

    private int viewportBottom() {
        return y + height - 1;
    }

    private int scrollbarX() {
        return x + width - SCROLLBAR_SLOT_WIDTH - SCROLLBAR_RIGHT_PADDING;
    }

    private int reservedRightSpace() {
        if (isScrollable()) return SCROLLBAR_SLOT_WIDTH + CONTENT_SCROLLBAR_GAP;
        return SCROLLBAR_WIDTH;
    }

    private int totalContentHeight() {
        return contentPaddingTop + scrollContentHeight() + contentPaddingBottom;
    }

    private int scrollContentHeight() {
        if (expectedContentHeight >= 0) return expectedContentHeight;
        return measuredContentHeight;
    }

    private int maxScroll() {
        return Math.max(0, totalContentHeight() - height);
    }

    private int thumbHeight() {
        int contentHeight = Math.max(1, totalContentHeight());
        int thumbHeight = height * height / contentHeight;
        thumbHeight = Math.max(MIN_THUMB_HEIGHT, thumbHeight);
        return Math.min(height, thumbHeight);
    }

    private int thumbY(int thumbHeight) {
        int maxScroll = maxScroll();
        int thumbTravel = height - thumbHeight;
        if (maxScroll <= 0 || thumbTravel <= 0) return y;
        return y + scrollOffset * thumbTravel / maxScroll;
    }

    private void setScrollFromThumbY(int thumbY, int thumbHeight) {
        int minY = y;
        int maxY = y + height - thumbHeight;
        thumbY = Math.clamp(thumbY, minY, maxY);

        int thumbTravel = maxY - minY;
        if (thumbTravel <= 0) {
            scrollOffset = 0;
            return;
        }

        scrollOffset = (thumbY - minY) * maxScroll() / thumbTravel;
    }

    private void clampScroll() {
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll());
    }


    private boolean insideRect(int pointX, int pointY, int areaX, int areaY, int areaWidth, int areaHeight) {
        return pointX >= areaX && pointX < areaX + areaWidth && pointY >= areaY && pointY < areaY + areaHeight;
    }
}
