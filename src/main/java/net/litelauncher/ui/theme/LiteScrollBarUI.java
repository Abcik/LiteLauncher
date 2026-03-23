package net.litelauncher.ui.theme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

final class LiteScrollBarUI extends BasicScrollBarUI {
    private static final int TRACK_INSET = 2;
    private static final int MIN_THUMB_LENGTH = 28;

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
        scrollbar.setFocusable(false);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(8, MIN_THUMB_LENGTH)
                : new Dimension(MIN_THUMB_LENGTH, 8);
    }

    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
        if (trackBounds.isEmpty()) {
            return;
        }
        ThemePalette palette = ThemeManager.getInstance().palette();
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(palette.scrollbarTrack);
        ShapeBounds bounds = createBounds(trackBounds);
        g2.fill(new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height, bounds.arc, bounds.arc));
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        ThemePalette palette = ThemeManager.getInstance().palette();
        Color fill = isDragging || isThumbRollover() ? palette.scrollbarThumbHover : palette.scrollbarThumb;
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        ShapeBounds bounds = createBounds(thumbBounds);
        g2.fill(new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height, bounds.arc, bounds.arc));
        g2.dispose();
    }

    private ShapeBounds createBounds(Rectangle raw) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            float x = raw.x + TRACK_INSET;
            float y = raw.y + TRACK_INSET;
            float width = Math.max(4f, raw.width - TRACK_INSET * 2f);
            float height = Math.max(8f, raw.height - TRACK_INSET * 2f);
            return new ShapeBounds(x, y, width, height, width);
        }
        float x = raw.x + TRACK_INSET;
        float y = raw.y + TRACK_INSET;
        float width = Math.max(8f, raw.width - TRACK_INSET * 2f);
        float height = Math.max(4f, raw.height - TRACK_INSET * 2f);
        return new ShapeBounds(x, y, width, height, height);
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setFocusable(false);
        button.setBorder(null);
        button.setPreferredSize(new Dimension());
        button.setMinimumSize(new Dimension());
        button.setMaximumSize(new Dimension());
        return button;
    }

    private record ShapeBounds(float x, float y, float width, float height, float arc) {
    }
}
