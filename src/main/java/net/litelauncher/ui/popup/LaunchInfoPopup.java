package net.litelauncher.ui.popup;

import net.litelauncher.ui.components.ActionButton;
import net.litelauncher.ui.components.PopupScaffold;
import net.litelauncher.ui.components.SurfacePanel;
import net.litelauncher.ui.modal.BaseModalPanel;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

public final class LaunchInfoPopup extends BaseModalPanel {
    private static final int BUTTON_WIDTH = 280;
    private static final int BUTTON_HEIGHT = 52;
    private static final int MESSAGE_WIDTH = 404;

    private final JLabel titleLabel = new JLabel("Information", SwingConstants.CENTER);
    private final JTextPane messagePane = new JTextPane();
    private final ActionButton okButton = new ActionButton("Okay", ActionButton.Variant.SECONDARY);
    private final SimpleAttributeSet centeredText = new SimpleAttributeSet();

    public LaunchInfoPopup() {
        super("launch-info", new Dimension(560, 320));
        setLayout(new BorderLayout());

        SurfacePanel rootPanel = PopupScaffold.createRootPanel();
        rootPanel.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(UiTypography.bold(PopupScaffold.TITLE_FONT_SIZE));

        StyleConstants.setAlignment(centeredText, StyleConstants.ALIGN_CENTER);
        messagePane.setAlignmentX(Component.CENTER_ALIGNMENT);
        messagePane.setEditable(false);
        messagePane.setFocusable(false);
        messagePane.setOpaque(false);
        messagePane.setHighlighter(null);
        messagePane.setFont(UiTypography.regular(UiTypography.BODY));
        messagePane.setBorder(BorderFactory.createEmptyBorder());
        updateMessageLayout();

        content.add(Box.createVerticalGlue());
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(14));
        content.add(messagePane);
        content.add(Box.createVerticalGlue());

        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        okButton.setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        okButton.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        okButton.addActionListener(event -> requestClose());

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        footer.add(okButton);

        rootPanel.add(content, BorderLayout.CENTER);
        rootPanel.add(footer, BorderLayout.SOUTH);
        add(rootPanel, BorderLayout.CENTER);

        ThemeManager.getInstance().addListener(this::applyTheme);
        applyTheme();
    }

    public void setMessage(String message) {
        messagePane.setText(message == null ? "" : message);
        StyledDocument document = messagePane.getStyledDocument();
        document.setParagraphAttributes(0, document.getLength(), centeredText, false);
        updateMessageLayout();
        revalidate();
        repaint();
    }

    private void applyTheme() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        titleLabel.setForeground(palette.textPrimary);
        messagePane.setForeground(palette.textSecondary);
        repaint();
    }

    private void updateMessageLayout() {
        messagePane.setSize(new Dimension(MESSAGE_WIDTH, Short.MAX_VALUE));
        Dimension preferred = messagePane.getPreferredSize();
        messagePane.setPreferredSize(new Dimension(MESSAGE_WIDTH, preferred.height));
        messagePane.setMinimumSize(new Dimension(MESSAGE_WIDTH, preferred.height));
        messagePane.setMaximumSize(new Dimension(MESSAGE_WIDTH, Integer.MAX_VALUE));
    }
}
