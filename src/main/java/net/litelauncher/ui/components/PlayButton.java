package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class PlayButton extends JButton {
    public PlayButton(String text) {
        super(text);
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UiTypography.regular(UiTypography.EMPHASIS));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        ThemePalette palette = ThemeManager.getInstance().palette();
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getModel().isPressed() ? palette.accentPressed : getModel().isRollover() ? palette.accentHover : palette.accent);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 22, 22));
        g2.setColor(PopupMetrics.ACCENT_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 22, 22));
        g2.dispose();
        setForeground(PopupMetrics.ACCENT_TEXT);
        super.paintComponent(graphics);
    }
}
