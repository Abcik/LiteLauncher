package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public final class PopupSlider extends JSlider {
    private static final int TRACK_HEIGHT = 6;
    private static final int THUMB_SIZE = 16;
    private static final int HALO_SIZE = 24;

    private final SliderMouseHandler mouseHandler = new SliderMouseHandler();
    private boolean hovered;
    private boolean dragging;

    public PopupSlider(int min, int max, int value) {
        super(min, max, value);
        setOpaque(false);
        setFocusable(false);
        setRequestFocusEnabled(false);
        setFocusTraversalKeysEnabled(false);
        setPaintTicks(false);
        setPaintLabels(false);
        setPaintTrack(true);
        setSnapToTicks(false);
        setBorder(null);
        setPreferredSize(new Dimension(0, 28));
        setUI(new PopupSliderUi(this));
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        disableKeyboardActions();
    }

    @Override
    public void updateUI() {
        setUI(new PopupSliderUi(this));
    }

    @Override
    public void setValue(int value) {
        super.setValue(normalizeValue(value));
    }

    @Override
    public void setSnapToTicks(boolean snap) {
        super.setSnapToTicks(snap);
        super.setValue(normalizeValue(getValue()));
    }

    private int normalizeValue(int value) {
        int normalized = value;
        if (getSnapToTicks()) {
            int step = resolveStepSize();
            if (step > 0) {
                normalized = getMinimum() + Math.round((value - getMinimum()) / (float) step) * step;
            }
        }
        return Math.max(getMinimum(), Math.min(getMaximum(), normalized));
    }

    private int resolveStepSize() {
        int step = getMinorTickSpacing() > 0 ? getMinorTickSpacing() : getMajorTickSpacing();
        return Math.max(step, 1);
    }

    private void disableKeyboardActions() {
        int[] conditions = new int[]{JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_IN_FOCUSED_WINDOW};
        String[] keys = new String[]{"LEFT", "RIGHT", "UP", "DOWN", "KP_LEFT", "KP_RIGHT", "KP_UP", "KP_DOWN", "HOME", "END", "PAGE_UP", "PAGE_DOWN", "TAB", "shift TAB", "SPACE"};
        for (int condition : conditions) {
            for (String key : keys) {
                getInputMap(condition).put(KeyStroke.getKeyStroke(key), "none");
            }
        }
    }

    private void updateHover(Point point) {
        boolean thumbHovered = false;
        if (getUI() instanceof PopupSliderUi) {
            Rectangle thumbBounds = ((PopupSliderUi) getUI()).getThumbBounds();
            thumbHovered = thumbBounds != null && thumbBounds.contains(point);
        }
        if (hovered != thumbHovered) {
            hovered = thumbHovered;
            repaint();
        }
    }

    private Color resolveThumbColor(ThemePalette palette) {
        if (dragging) {
            return palette.accentPressed;
        }
        if (hovered) {
            return palette.accentHover;
        }
        return palette.accent;
    }

    private final class SliderMouseHandler extends MouseInputAdapter {
        @Override public void mouseEntered(MouseEvent event) { updateHover(event.getPoint()); }
        @Override public void mouseExited(MouseEvent event) { hovered = false; repaint(); }
        @Override public void mouseMoved(MouseEvent event) { updateHover(event.getPoint()); }
        @Override public void mouseDragged(MouseEvent event) { updateHover(event.getPoint()); repaint(); }
        @Override public void mousePressed(MouseEvent event) { dragging = SwingUtilities.isLeftMouseButton(event); updateHover(event.getPoint()); repaint(); }
        @Override public void mouseReleased(MouseEvent event) { dragging = false; updateHover(event.getPoint()); repaint(); }
    }

    private final class PopupSliderUi extends BasicSliderUI {
        private PopupSliderUi(JSlider slider) { super(slider); }
        @Override protected Dimension getThumbSize() { return new Dimension(THUMB_SIZE, THUMB_SIZE); }
        @Override public void paintFocus(Graphics graphics) { }

        @Override
        protected TrackListener createTrackListener(JSlider slider) {
            return new QuantizedTrackListener();
        }

        @Override
        public void paintTrack(Graphics graphics) {
            ThemePalette palette = ThemeManager.getInstance().palette();
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int trackY = trackRect.y + (trackRect.height - TRACK_HEIGHT) / 2;
            float trackArc = TRACK_HEIGHT;
            g2.setColor(palette.controlHover);
            g2.fill(new RoundRectangle2D.Float(trackRect.x, trackY, trackRect.width, TRACK_HEIGHT, trackArc, trackArc));
            int thumbCenter = thumbRect.x + thumbRect.width / 2;
            int filledWidth = Math.max(0, thumbCenter - trackRect.x);
            if (filledWidth > 0) {
                g2.setColor(resolveThumbColor(palette));
                g2.fill(new RoundRectangle2D.Float(trackRect.x, trackY, filledWidth, TRACK_HEIGHT, trackArc, trackArc));
            }
            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics graphics) {
            ThemePalette palette = ThemeManager.getInstance().palette();
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float thumbX = thumbRect.x + (thumbRect.width - THUMB_SIZE) / 2f;
            float thumbY = thumbRect.y + (thumbRect.height - THUMB_SIZE) / 2f;
            if (hovered || dragging) {
                float haloX = thumbRect.x + (thumbRect.width - HALO_SIZE) / 2f;
                float haloY = thumbRect.y + (thumbRect.height - HALO_SIZE) / 2f;
                g2.setColor(new Color(palette.accent.getRed(), palette.accent.getGreen(), palette.accent.getBlue(), dragging ? 68 : 40));
                g2.fill(new RoundRectangle2D.Float(haloX, haloY, HALO_SIZE, HALO_SIZE, HALO_SIZE, HALO_SIZE));
            }
            g2.setColor(resolveThumbColor(palette));
            g2.fill(new RoundRectangle2D.Float(thumbX, thumbY, THUMB_SIZE, THUMB_SIZE, THUMB_SIZE, THUMB_SIZE));
            g2.setColor(PopupMetrics.ACCENT_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(thumbX + 0.5f, thumbY + 0.5f, THUMB_SIZE - 1f, THUMB_SIZE - 1f, THUMB_SIZE, THUMB_SIZE));
            g2.dispose();
        }

        @Override protected void calculateTrackRect() { super.calculateTrackRect(); trackRect.y = contentRect.y + (contentRect.height - TRACK_HEIGHT) / 2; trackRect.height = TRACK_HEIGHT; }
        @Override protected void calculateThumbLocation() { super.calculateThumbLocation(); thumbRect.y = contentRect.y + (contentRect.height - THUMB_SIZE) / 2; }
        Rectangle getThumbBounds() { return thumbRect; }
        @Override protected void calculateTrackBuffer() { trackBuffer = THUMB_SIZE / 2 + 2; }

        private void snapToMousePosition(int mouseX) {
            int boundedX = Math.max(trackRect.x, Math.min(trackRect.x + trackRect.width, mouseX));
            slider.setValue(normalizeValue(valueForXPosition(boundedX)));
        }

        private final class QuantizedTrackListener extends TrackListener {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!slider.isEnabled() || !SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                currentMouseX = event.getX();
                currentMouseY = event.getY();
                slider.setValueIsAdjusting(true);
                snapToMousePosition(event.getX());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (!slider.isEnabled() || !slider.getValueIsAdjusting()) {
                    return;
                }
                currentMouseX = event.getX();
                currentMouseY = event.getY();
                snapToMousePosition(event.getX());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (!slider.isEnabled()) {
                    return;
                }
                currentMouseX = event.getX();
                currentMouseY = event.getY();
                snapToMousePosition(event.getX());
                slider.setValueIsAdjusting(false);
            }
        }
    }
}
