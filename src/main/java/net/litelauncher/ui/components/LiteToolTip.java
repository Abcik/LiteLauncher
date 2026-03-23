package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public final class LiteToolTip extends JToolTip {
    private static final Insets PADDING = new Insets(6, 10, 6, 10);
    private static final float ARC = 12f;

    public LiteToolTip(JComponent owner) {
        setComponent(owner);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(PADDING.top, PADDING.left, PADDING.bottom, PADDING.right));
        Font tooltipFont = UIManager.getFont("ToolTip.font");
        if (tooltipFont != null) {
            setFont(tooltipFont);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        String text = getTipText();
        if (text == null || text.isEmpty()) {
            return new Dimension(0, 0);
        }
        FontMetrics metrics = getFontMetrics(getFont());
        Insets insets = getInsets();
        return new Dimension(metrics.stringWidth(text) + insets.left + insets.right,
                metrics.getHeight() + insets.top + insets.bottom);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        String text = getTipText();
        if (text == null || text.isEmpty()) {
            return;
        }
        ThemePalette palette = ThemeManager.getInstance().palette();
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        UiTypography.applyCrispTextRendering(g2);
        g2.setColor(palette.tooltipBackground);
        g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC));
        g2.setColor(palette.tooltipBorder);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC));
        g2.setColor(palette.tooltipText);
        g2.setFont(getFont());
        Insets insets = getInsets();
        FontMetrics metrics = g2.getFontMetrics();
        int baseline = insets.top + metrics.getAscent();
        g2.drawString(text, insets.left, baseline);
        g2.dispose();
    }
}
