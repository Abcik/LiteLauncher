package net.litelauncher.ui.theme;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

final class LiteCheckBoxIcon implements Icon {
    private static final int SIZE = 18;
    private static final float ARC = 7f;

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.translate(x, y);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ThemePalette palette = ThemeManager.getInstance().palette();
        AbstractButton button = component instanceof AbstractButton ? (AbstractButton) component : null;
        ButtonModel model = button != null ? button.getModel() : null;
        boolean selected = button != null && button.isSelected();
        boolean enabled = button == null || button.isEnabled();
        boolean pressed = model != null && model.isPressed() && model.isArmed();
        boolean hovered = model != null && model.isRollover();

        Color fill = selected ? palette.accent : palette.inputSurface;
        Color border = selected ? palette.accent : palette.border;
        if (pressed) {
            fill = selected ? palette.accentPressed : palette.controlPressed;
            border = selected ? palette.accentPressed : palette.border;
        } else if (hovered) {
            fill = selected ? palette.accentHover : palette.controlHover;
            border = selected ? palette.accentHover : palette.border;
        }
        if (!enabled) {
            fill = withAlpha(fill, palette.dark ? 120 : 92);
            border = withAlpha(border, palette.dark ? 110 : 84);
        }

        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, SIZE - 1f, SIZE - 1f, ARC, ARC));
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, SIZE - 1f, SIZE - 1f, ARC, ARC));

        if (selected) {
            g2.setColor(enabled ? Color.WHITE : withAlpha(Color.WHITE, 170));
            g2.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float check = new Path2D.Float();
            check.moveTo(4.5f, 9.5f);
            check.lineTo(7.6f, 12.5f);
            check.lineTo(13.5f, 6.2f);
            g2.draw(check);
        }

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
