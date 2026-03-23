package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public final class WindowSurfacePanel extends JPanel {
    public WindowSurfacePanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ThemePalette palette = ThemeManager.getInstance().palette();
        RoundRectangle2D.Float surface = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 34, 34);
        g2.setColor(palette.surfaceBackground);
        g2.fill(surface);
        g2.setColor(palette.border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(surface);
        g2.dispose();
        super.paintComponent(graphics);
    }
}
