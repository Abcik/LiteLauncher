package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class SurfacePanel extends JPanel {
    private final int arc;
    private final Insets padding;
    private final boolean elevated;

    public SurfacePanel(int arc, Insets padding, boolean elevated) {
        this.arc = arc;
        this.padding = padding;
        this.elevated = elevated;
        setOpaque(false);
    }

    @Override
    public Insets getInsets() {
        return padding;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ThemePalette palette = ThemeManager.getInstance().palette();
        Color fill = elevated ? palette.elevatedSurface : palette.surfaceBackground;
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
        g2.setColor(palette.border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
        g2.dispose();
        super.paintComponent(graphics);
    }
}
