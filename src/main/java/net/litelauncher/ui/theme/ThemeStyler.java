package net.litelauncher.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public final class ThemeStyler {
    private ThemeStyler() {
    }

    public static void styleTextField(JTextField field, ThemePalette palette, int verticalPadding, int horizontalPadding) {
        field.setForeground(palette.textPrimary);
        field.setCaretColor(palette.textPrimary);
        field.setBackground(palette.inputSurface);
        field.setSelectionColor(palette.textSelectionFill);
        field.setSelectedTextColor(palette.textPrimary);
        field.setMargin(new Insets(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding));
        field.setOpaque(true);
    }

    public static void styleCheckBox(JCheckBox checkBox, ThemePalette palette) {
        checkBox.setForeground(palette.textPrimary);
        checkBox.setRolloverEnabled(true);
        checkBox.setOpaque(false);
        checkBox.setBackground(new Color(0, 0, 0, 0));
        checkBox.setBorder(BorderFactory.createEmptyBorder());
        checkBox.setFocusable(false);
        checkBox.setRequestFocusEnabled(false);
        checkBox.setIconTextGap(10);
        checkBox.setHorizontalAlignment(SwingConstants.LEFT);
        checkBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        checkBox.setVerticalTextPosition(SwingConstants.CENTER);
        LiteCheckBoxIcon icon = new LiteCheckBoxIcon();
        checkBox.setIcon(icon);
        checkBox.setSelectedIcon(icon);
        checkBox.setRolloverIcon(icon);
        checkBox.setRolloverSelectedIcon(icon);
        checkBox.setPressedIcon(icon);
        checkBox.setDisabledIcon(icon);
        checkBox.setDisabledSelectedIcon(icon);
        checkBox.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "none");
        checkBox.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("released SPACE"), "none");
        bindCheckBoxPostClickReset(checkBox);
        checkBox.revalidate();
        checkBox.repaint();
    }

    public static void styleComboBox(JComboBox<?> comboBox, ThemePalette palette) {
        comboBox.setForeground(palette.textPrimary);
        comboBox.setBackground(palette.inputSurface);
        comboBox.setOpaque(true);
        comboBox.setFocusable(false);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.border),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        comboBox.setRenderer(new ComboRenderer(palette));
        Object child = comboBox.getAccessibleContext() != null ? comboBox.getAccessibleContext().getAccessibleChild(0) : null;
        if (child instanceof BasicComboPopup popup) {
            popup.setBorder(BorderFactory.createLineBorder(palette.border));
            popup.setBackground(palette.elevatedSurface);
            popup.getList().setBackground(palette.elevatedSurface);
            popup.getList().setForeground(palette.textPrimary);
            popup.getList().setSelectionBackground(palette.selectionFill);
            popup.getList().setSelectionForeground(palette.textPrimary);
        }
        comboBox.revalidate();
        comboBox.repaint();
    }

    public static void styleScrollPane(JScrollPane scrollPane, ThemePalette palette, JComponent content, int rightGapWhenScrollable) {
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(palette.surfaceBackground);
        styleScrollBar(scrollPane.getVerticalScrollBar(), palette);
        styleScrollBar(scrollPane.getHorizontalScrollBar(), palette);
        if (content != null) {
            updateScrollableContentPadding(scrollPane, content, rightGapWhenScrollable);
        }
    }

    public static void styleScrollBar(JScrollBar scrollBar, ThemePalette palette) {
        if (scrollBar == null) {
            return;
        }
        scrollBar.setOpaque(false);
        scrollBar.setUnitIncrement(14);
        scrollBar.setBackground(palette.scrollbarTrack);
        scrollBar.setForeground(palette.scrollbarThumb);
        scrollBar.setBorder(BorderFactory.createEmptyBorder());
        if (!(scrollBar.getUI() instanceof LiteScrollBarUI)) {
            scrollBar.setUI(new LiteScrollBarUI());
        }
        if (scrollBar.getOrientation() == JScrollBar.VERTICAL) {
            scrollBar.setPreferredSize(new Dimension(12, Integer.MAX_VALUE));
        } else {
            scrollBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 12));
        }
        scrollBar.revalidate();
        scrollBar.repaint();
    }

    public static void bindScrollPaneSpacing(final JScrollPane scrollPane, final JComponent content, final int rightGapWhenScrollable) {
        if (content == null) {
            return;
        }
        ComponentAdapter updater = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateScrollableContentPadding(scrollPane, content, rightGapWhenScrollable);
            }

            @Override
            public void componentShown(ComponentEvent event) {
                updateScrollableContentPadding(scrollPane, content, rightGapWhenScrollable);
            }
        };
        scrollPane.addComponentListener(updater);
        scrollPane.getViewport().addComponentListener(updater);
        scrollPane.getVerticalScrollBar().addComponentListener(updater);
        SwingUtilities.invokeLater(() -> updateScrollableContentPadding(scrollPane, content, rightGapWhenScrollable));
    }

    private static void updateScrollableContentPadding(JScrollPane scrollPane, JComponent content, int rightGapWhenScrollable) {
        int rightInset = scrollPane.getVerticalScrollBar().isVisible() ? Math.max(0, rightGapWhenScrollable) : 0;
        Border current = content.getBorder();
        Insets insets = current != null ? current.getBorderInsets(content) : new Insets(0, 0, 0, 0);
        if (insets.top == 0 && insets.left == 0 && insets.bottom == 0 && insets.right == rightInset) {
            return;
        }
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, rightInset));
        content.revalidate();
        content.repaint();
    }

    private static void bindCheckBoxPostClickReset(final JCheckBox checkBox) {
        if (Boolean.TRUE.equals(checkBox.getClientProperty("lite.checkbox.resetBound"))) {
            return;
        }
        checkBox.putClientProperty("lite.checkbox.resetBound", Boolean.TRUE);
        checkBox.addActionListener(event -> SwingUtilities.invokeLater(() -> {
            checkBox.getModel().setArmed(false);
            checkBox.getModel().setPressed(false);
            checkBox.getModel().setRollover(checkBox.getMousePosition() != null);
            if (checkBox.isFocusOwner()) {
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
            checkBox.repaint();
        }));
    }

    private static final class ComboRenderer extends DefaultListCellRenderer {
        private final ThemePalette palette;

        private ComboRenderer(ThemePalette palette) {
            this.palette = palette;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            component.setForeground(palette.textPrimary);
            component.setBackground(isSelected ? palette.selectionFill : palette.elevatedSurface);
            if (component instanceof JComponent jc) {
                jc.setOpaque(true);
                jc.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            }
            setHorizontalAlignment(SwingConstants.LEFT);
            return component;
        }
    }
}
