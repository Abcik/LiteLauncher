package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.UiTypography;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class PopupMetrics {
    public static final int DEFAULT_ARC = 18;
    public static final int STANDARD_ROW_HEIGHT = 78;
    public static final int COMPACT_ROW_HEIGHT = 64;
    public static final int STANDARD_ROW_WIDTH = 424;
    public static final int COMPACT_ROW_WIDTH = 344;
    public static final int ICON_BUTTON_SIZE = 36;
    public static final int EXTERNAL_ACTION_GAP = 10;
    public static final int PROFILE_ROW_WRAPPER_WIDTH = STANDARD_ROW_WIDTH + EXTERNAL_ACTION_GAP + ICON_BUTTON_SIZE;
    public static final Insets STANDARD_ROW_PADDING = new Insets(14, 18, 14, 18);
    public static final Insets COMPACT_ROW_PADDING = new Insets(12, 18, 12, 18);
    public static final int STACK_GAP = 12;
    public static final int TEXT_LINE_GAP = 6;
    public static final float ROW_TITLE_SIZE = UiTypography.BODY;
    public static final float ROW_SUBTITLE_SIZE = UiTypography.BODY;
    public static final Color ACCENT_TEXT = Color.WHITE;
    public static final Color ACCENT_BORDER = new Color(255, 255, 255, 28);

    private PopupMetrics() {
    }

    public static Dimension standardRowSize() { return new Dimension(STANDARD_ROW_WIDTH, STANDARD_ROW_HEIGHT); }
    public static Dimension compactRowSize() { return new Dimension(COMPACT_ROW_WIDTH, COMPACT_ROW_HEIGHT); }
    public static Dimension profileRowWrapperSize() { return new Dimension(PROFILE_ROW_WRAPPER_WIDTH, STANDARD_ROW_HEIGHT); }

    public static void applyStandardSize(JComponent component, Dimension size, int height) {
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    public static JPanel createCenteredTwoLineText(JLabel title, JLabel subtitle) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        GridBagConstraints titleGbc = new GridBagConstraints();
        titleGbc.gridx = 0; titleGbc.gridy = 0; titleGbc.weightx = 1.0; titleGbc.weighty = 0.5; titleGbc.anchor = GridBagConstraints.SOUTHWEST; titleGbc.insets = new Insets(0, 0, TEXT_LINE_GAP / 2, 0);
        GridBagConstraints subtitleGbc = new GridBagConstraints();
        subtitleGbc.gridx = 0; subtitleGbc.gridy = 1; subtitleGbc.weightx = 1.0; subtitleGbc.weighty = 0.5; subtitleGbc.anchor = GridBagConstraints.NORTHWEST; subtitleGbc.insets = new Insets(TEXT_LINE_GAP / 2, 0, 0, 0);
        wrapper.add(title, titleGbc);
        wrapper.add(subtitle, subtitleGbc);
        return wrapper;
    }

    public static JPanel createCenteredSingleLineText(JLabel label) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 1.0; gbc.weighty = 1.0;
        wrapper.add(label, gbc);
        return wrapper;
    }
}
