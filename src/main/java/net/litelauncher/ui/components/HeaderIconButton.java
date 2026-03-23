package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolTip;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class HeaderIconButton extends JButton {
    private static final Dimension BUTTON_SIZE = new Dimension(36, 36);
    private static final float HOVER_ARC = 12f;

    private final boolean danger;

    public HeaderIconButton(Icon icon, String tooltip, boolean danger) {
        this.danger = danger;
        setIcon(icon);
        setToolTipText(tooltip);
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
        setPreferredSize(BUTTON_SIZE);
        setMinimumSize(BUTTON_SIZE);
        setMaximumSize(BUTTON_SIZE);
    }

    @Override
    public JToolTip createToolTip() {
        return new LiteToolTip(this);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ThemePalette palette = ThemeManager.getInstance().palette();
        if (getModel().isPressed()) {
            g2.setColor(danger ? palette.danger : palette.controlPressed);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), HOVER_ARC, HOVER_ARC));
        } else if (getModel().isRollover()) {
            g2.setColor(danger ? palette.dangerHover : palette.controlHover);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), HOVER_ARC, HOVER_ARC));
        }
        if (isFocusOwner()) {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(palette.border);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, HOVER_ARC, HOVER_ARC));
        }
        g2.dispose();
        super.paintComponent(graphics);
    }
}
