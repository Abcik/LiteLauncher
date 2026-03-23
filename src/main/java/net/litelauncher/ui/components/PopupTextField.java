package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public final class PopupTextField extends JTextField {
    private static final Insets CONTENT_INSETS = new Insets(0, 14, 0, 14);
    private String placeholderText;

    public PopupTextField() {
        setOpaque(false);
        setMargin(new Insets(0, 0, 0, 0));
        setBorder(BorderFactory.createEmptyBorder(CONTENT_INSETS.top, CONTENT_INSETS.left, CONTENT_INSETS.bottom, CONTENT_INSETS.right));
    }

    public void setPlaceholderText(String placeholderText) {
        this.placeholderText = placeholderText;
        repaint();
    }

    public static void installOutsideBlur(java.awt.Component root) {
        java.awt.event.MouseAdapter blurHandler = new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent event) { clearTextFieldFocus(event.getComponent()); }
        };
        attachBlurHandler(root, blurHandler);
    }

    private static void attachBlurHandler(java.awt.Component component, java.awt.event.MouseAdapter blurHandler) {
        if (!(component instanceof PopupTextField)) {
            component.addMouseListener(blurHandler);
        }
        if (component instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
                attachBlurHandler(child, blurHandler);
            }
        }
    }

    private static void clearTextFieldFocus(java.awt.Component source) {
        java.awt.Component focusOwner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner instanceof JTextComponent && !SwingUtilities.isDescendingFrom(source, focusOwner)) {
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        }
    }

    @Override public Insets getInsets() { return new Insets(CONTENT_INSETS.top, CONTENT_INSETS.left, CONTENT_INSETS.bottom, CONTENT_INSETS.right); }

    @Override
    protected void paintComponent(Graphics graphics) {
        ThemePalette palette = ThemeManager.getInstance().palette();
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isEnabled() ? palette.elevatedSurface : palette.controlHover);
        g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, PopupMetrics.DEFAULT_ARC, PopupMetrics.DEFAULT_ARC));
        g2.setColor(isEnabled() ? palette.border : palette.separator);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, PopupMetrics.DEFAULT_ARC, PopupMetrics.DEFAULT_ARC));
        g2.dispose();
        super.paintComponent(graphics);
        paintPlaceholder(graphics, palette);
    }

    private void paintPlaceholder(Graphics graphics, ThemePalette palette) {
        if (placeholderText == null || placeholderText.isEmpty() || !getText().isEmpty()) {
            return;
        }
        Insets insets = getInsets();
        FontMetrics metrics = graphics.getFontMetrics(getFont());
        int textX = switch (getHorizontalAlignment()) {
            case CENTER -> (getWidth() - metrics.stringWidth(placeholderText)) / 2;
            case RIGHT, TRAILING -> getWidth() - insets.right - metrics.stringWidth(placeholderText);
            default -> insets.left;
        };
        int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        Graphics2D g2 = (Graphics2D) graphics.create();
        UiTypography.applyCrispTextRendering(g2);
        g2.setColor(isEnabled() ? palette.textMuted : palette.textSecondary);
        g2.setFont(getFont());
        g2.drawString(placeholderText, textX, textY);
        g2.dispose();
    }
}
