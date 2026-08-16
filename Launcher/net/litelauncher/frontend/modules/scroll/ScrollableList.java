package net.litelauncher.frontend.modules.scroll;

import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScrollableList<T> {

    private final JComponent host;
    private final Source<T> source;
    private final PixelScrollView scrollView;
    private final int rowHeight;
    private final int rowGap;
    private final int scrollableRowWidth;
    private final int fullRowWidth;
    private final RowRenderer<T> rowRenderer;
    private final RowSync<T> rowSync;
    private final RowContentRenderer<T> rowContentRenderer;
    private RowEnabled<T> rowEnabled = (_, _) -> true;
    private final List<PixelButton> rowButtons = new ArrayList<>();

    private int lastCount = -1;
    private int lastRowWidth = -1;
    private int lastContentHeight = -1;
    private int lastInputStart = -1;
    private int lastInputEnd = -1;

    public ScrollableList(
            JComponent host,
            int x,
            int y,
            int width,
            int height,
            int rowHeight,
            int rowGap,
            int scrollableRowWidth,
            int fullRowWidth,
            Source<T> source,
            RowRenderer<T> rowRenderer,
            RowSync<T> rowSync,
            RowContentRenderer<T> rowContentRenderer
    ) {
        this.host = Objects.requireNonNull(host);
        this.source = Objects.requireNonNull(source);
        this.scrollView = new PixelScrollView(x, y, width, height)
                .setContentPadding(0, 0, 0, 0);
        this.rowHeight = Math.max(1, rowHeight);
        this.rowGap = Math.max(0, rowGap);
        this.scrollableRowWidth = Math.max(1, scrollableRowWidth);
        this.fullRowWidth = Math.max(1, fullRowWidth);
        this.rowRenderer = Objects.requireNonNull(rowRenderer);
        this.rowSync = rowSync;
        this.rowContentRenderer = rowContentRenderer;
    }

    public PixelScrollView scrollView() {
        return scrollView;
    }

    public ScrollableList<T> setRowEnabled(RowEnabled<T> rowEnabled) {
        this.rowEnabled = rowEnabled == null ? (_, _) -> true : rowEnabled;
        syncRowEnabledStates();
        return this;
    }

    public void setBounds(int x, int y, int width, int height) {
        scrollView.setBounds(x, y, width, height);
        markDirty();
    }

    public void setScrollOffset(int scrollOffset) {
        scrollView.setScrollOffset(scrollOffset);
    }

    public int size() {
        return source.size();
    }

    public int rowWidth() {
        return rowWidth(scrollableRowWidth, fullRowWidth);
    }

    public int rowWidth(int scrollableWidth, int fullWidth) {
        return scrollView.isScrollable() ? scrollableWidth : fullWidth;
    }

    public void markDirty() {
        lastCount = -1;
        lastInputStart = -1;
        lastInputEnd = -1;
        scrollView.markContentDirty();
    }

    public void render(PixelGraphics graphics) {
        syncRows();
        scrollView.render(graphics, this::buildRows);
    }

    public boolean handleInput(MouseState mouse) {
        return handleInput(mouse, true);
    }

    public boolean handleInput(MouseState mouse, boolean handleRows) {
        syncRows();

        boolean dirty = scrollView.handleInput(mouse, this::buildRows);
        if (handleRows) dirty |= handleRowsInput(mouse);
        return dirty;
    }

    public boolean handleRowsInput(MouseState mouse) {
        syncRows();

        int start = firstVisibleIndex();
        int end = lastVisibleIndexExclusive();
        boolean dirty = resetRowsOutside(start, end);

        for (int index = start; index < end; index++) {
            dirty |= rowButtons.get(index).handleInput(mouse, scrollView);
        }

        lastInputStart = start;
        lastInputEnd = end;
        return dirty;
    }

    public int consumeClickedIndex() {
        int start = firstVisibleIndex();
        int end = lastVisibleIndexExclusive();
        for (int index = start; index < end; index++) {
            if (rowButtons.get(index).consumeClick()) return index;
        }
        return -1;
    }

    private void buildRows(ScrollCanvas canvas) {
        syncRows();

        int start = firstVisibleIndex();
        int end = lastVisibleIndexExclusive();
        for (int index = start; index < end; index++) rowButtons.get(index).render(canvas);
        if (rowContentRenderer == null) return;
        for (int index = start; index < end; index++) rowContentRenderer.render(canvas, source.get(index), index);
    }

    private void syncRows() {
        int count = source.size();
        int contentHeight = listContentHeight(count, rowHeight, rowGap);
        scrollView.setExpectedContentHeight(contentHeight);

        int rowWidth = rowWidth();
        boolean geometryChanged = count != lastCount || rowWidth != lastRowWidth || contentHeight != lastContentHeight;
        if (!geometryChanged) {
            syncRowEnabledStates();
            return;
        }

        for (int index = 0; index < count; index++) {
            int rowIndex = index;
            int y = rowY(index);

            if (index >= rowButtons.size()) {
                rowButtons.add(new PixelButton(host, 0, y, rowWidth, rowHeight,
                        (surface, x, y1, width, height, state) ->
                                rowRenderer.render(surface, x, y1, width, height, state, source.get(rowIndex), rowIndex)
                ));
            } else {
                rowButtons.get(index).setBounds(0, y, rowWidth, rowHeight);
            }

            if (rowSync != null) rowSync.sync(source.get(index), index, y, rowWidth, this);
        }

        trimButtons(count);
        syncRowEnabledStates();
        lastCount = count;
        lastRowWidth = rowWidth;
        lastContentHeight = contentHeight;
    }

    private void syncRowEnabledStates() {
        int count = Math.min(source.size(), rowButtons.size());
        for (int index = 0; index < count; index++) {
            rowButtons.get(index).setEnabled(rowEnabled.enabled(source.get(index), index));
        }
    }

    private boolean resetRowsOutside(int start, int end) {
        if (lastInputStart < 0 || lastInputEnd < 0) return false;

        boolean dirty = false;
        int oldStart = lastInputStart;
        int oldEnd = Math.min(rowButtons.size(), lastInputEnd);
        for (int index = oldStart; index < oldEnd; index++) {
            if (index >= start && index < end) continue;
            rowButtons.get(index).reset();
            dirty = true;
        }
        return dirty;
    }

    private void trimButtons(int count) {
        while (rowButtons.size() > count) rowButtons.removeLast();
    }

    public boolean isRowVisible(int index) {
        return index >= firstVisibleIndex() && index < lastVisibleIndexExclusive();
    }

    public int firstVisibleIndex() {
        int count = source.size();
        if (count <= 0) return 0;

        int stride = rowStride();
        int index = scrollView.visibleContentTop() / stride;
        return Math.clamp(index, 0, count);
    }

    public int lastVisibleIndexExclusive() {
        int count = source.size();
        if (count <= 0) return 0;

        int stride = rowStride();
        int index = scrollView.visibleContentBottom() / stride + 1;
        return Math.clamp(index, 0, count);
    }

    private int rowY(int index) {
        return index * rowStride();
    }

    private int rowStride() {
        return rowHeight + rowGap;
    }

    private static int listContentHeight(int count, int itemHeight, int gap) {
        return count <= 0 ? 0 : count * (itemHeight + gap) - gap;
    }

    public interface Source<T> {
        int size();
        T get(int index);
    }

    @FunctionalInterface
    public interface RowRenderer<T> {
        void render(PixelSurface surface, int x, int y, int width, int height,
                    PixelButton.State state, T item, int index);
    }

    @FunctionalInterface
    public interface RowSync<T> {
        void sync(T item, int index, int y, int rowWidth, ScrollableList<T> list);
    }

    @FunctionalInterface
    public interface RowContentRenderer<T> {
        void render(ScrollCanvas canvas, T item, int index);
    }

    @FunctionalInterface
    public interface RowEnabled<T> {
        boolean enabled(T item, int index);
    }
}
