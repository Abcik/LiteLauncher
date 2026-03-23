package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.ThemeStyler;
import net.litelauncher.ui.theme.UiTypography;
import net.litelauncher.ui.util.IconFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;

public final class PopupScaffold {
    public static final int ROOT_ARC = 28;
    public static final Insets ROOT_PADDING = new Insets(16, 16, 16, 16);
    public static final int ROOT_GAP = 18;
    public static final int HEADER_GAP = 12;
    public static final int HEADER_TEXT_GAP = 5;
    public static final int SCROLLBAR_CONTENT_GAP = 12;
    public static final int SCROLL_UNIT_INCREMENT = 14;
    public static final float TITLE_FONT_SIZE = UiTypography.TITLE;
    public static final float SUBTITLE_FONT_SIZE = UiTypography.BODY;

    private PopupScaffold() {
    }

    public static SurfacePanel createRootPanel() {
        SurfacePanel panel = new SurfacePanel(ROOT_ARC, ROOT_PADDING, false);
        panel.setLayout(new BorderLayout(0, ROOT_GAP));
        return panel;
    }

    public static JPanel createHeader(JLabel title, JLabel subtitle, HeaderIconButton closeButton) {
        JPanel header = new JPanel(new BorderLayout(HEADER_GAP, 0));
        header.setOpaque(false);
        header.add(createHeaderTextBlock(title, subtitle), BorderLayout.CENTER);
        header.add(wrapTrailingComponent(closeButton), BorderLayout.EAST);
        return header;
    }

    public static JScrollPane createScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        ThemeStyler.bindScrollPaneSpacing(scrollPane, content, SCROLLBAR_CONTENT_GAP);
        return scrollPane;
    }

    public static void applyHeaderTheme(JLabel title, JLabel subtitle, HeaderIconButton closeButton, ThemePalette palette) {
        title.setForeground(palette.textPrimary);
        subtitle.setForeground(palette.textSecondary);
        closeButton.setIcon(IconFactory.closeIcon(palette));
    }

    public static JPanel wrapTrailingComponent(Component component) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(component);
        return wrapper;
    }

    private static JPanel createHeaderTextBlock(JLabel title, JLabel subtitle) {
        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setFont(UiTypography.bold(TITLE_FONT_SIZE));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setFont(UiTypography.regular(SUBTITLE_FONT_SIZE));
        textBlock.add(title);
        textBlock.add(Box.createVerticalStrut(HEADER_TEXT_GAP));
        textBlock.add(subtitle);
        return textBlock;
    }
}
