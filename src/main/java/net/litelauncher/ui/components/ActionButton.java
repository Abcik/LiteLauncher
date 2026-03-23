package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public final class ActionButton extends JButton {
    public enum Variant { ACCENT, SECONDARY, DANGER }

    private final Variant variant;

    public ActionButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 22, 0, 22));
        setPreferredSize(new Dimension(196, 48));
        setFont(UiTypography.regular(UiTypography.BODY));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        ThemePalette palette = ThemeManager.getInstance().palette();
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill;
        Color border;
        Color text;
        if (variant == Variant.ACCENT) {
            fill = getModel().isPressed() ? palette.accentPressed : getModel().isRollover() ? palette.accentHover : palette.accent;
            border = PopupMetrics.ACCENT_BORDER;
            text = PopupMetrics.ACCENT_TEXT;
        } else if (variant == Variant.DANGER) {
            fill = getModel().isPressed() ? palette.danger : getModel().isRollover() ? palette.dangerHover : palette.danger;
            border = new Color(255, 255, 255, 24);
            text = Color.WHITE;
        } else {
            fill = getModel().isPressed() ? palette.controlPressed : getModel().isRollover() ? palette.controlHover : palette.elevatedSurface;
            border = palette.border;
            text = palette.textPrimary;
        }
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 18, 18));
        g2.dispose();

        setForeground(text);
        super.paintComponent(graphics);
    }
}
