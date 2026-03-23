package net.litelauncher.ui.popup;

import net.litelauncher.state.LauncherSettings;
import net.litelauncher.ui.components.HeaderIconButton;
import net.litelauncher.ui.components.PopupMetrics;
import net.litelauncher.ui.components.PopupScaffold;
import net.litelauncher.ui.components.PopupSlider;
import net.litelauncher.ui.components.PopupTextField;
import net.litelauncher.ui.components.SurfacePanel;
import net.litelauncher.ui.modal.BaseModalPanel;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.ThemeStyler;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public final class SettingsPopup extends BaseModalPanel {
    private static final int MIN_MEMORY_MB = 1024;
    private static final int MAX_MEMORY_MB = 8192;
    private static final int MIN_MEMORY_FIELD_MB = 1;
    private static final int MEMORY_STEP_MB = 256;
    private static final int MIN_GAME_WIDTH = 640;
    private static final int MAX_GAME_WIDTH = 7680;
    private static final int MIN_GAME_HEIGHT = 360;
    private static final int MAX_GAME_HEIGHT = 4320;
    private static final int SECTION_ARC = 18;
    private static final Insets SECTION_PADDING = new Insets(16, 18, 16, 18);
    private static final int SECTION_GAP = 12;
    private static final int FIELD_HEIGHT = 52;
    private static final int MEMORY_FIELD_WIDTH = 110;
    private static final int RESOLUTION_FIELD_WIDTH = 122;
    private static final int RESOLUTION_DIVIDER_WIDTH = 16;
    private static final int RESOLUTION_FIELD_GAP = 8;
    private static final int DISPLAY_ROW_GAP = 14;
    private static final int RESOLUTION_LABEL_GAP = 10;

    private final LauncherSettings settings;
    private final JLabel titleLabel = new JLabel("Settings");
    private final JLabel subtitleLabel = new JLabel("Launcher and game settings.");
    private final JLabel memorySectionLabel = new JLabel("Memory");
    private final JLabel launchSectionLabel = new JLabel("Launcher");
    private final JLabel displaySectionLabel = new JLabel("Screen");
    private final JLabel advancedSectionLabel = new JLabel("JVM Arguments");
    private final JLabel memoryValueSuffixLabel = new JLabel("MB");
    private final JLabel resolutionValueLabel = new JLabel("Resolution:");
    private final JLabel resolutionDividerLabel = new JLabel("x", SwingConstants.CENTER);
    private final PopupSlider memorySlider = new PopupSlider(MIN_MEMORY_MB, MAX_MEMORY_MB, 4096);
    private final PopupTextField memoryField = new PopupTextField();
    private final PopupTextField widthField = new PopupTextField();
    private final PopupTextField heightField = new PopupTextField();
    private final PopupTextField launchArgumentsField = new PopupTextField();
    private final JCheckBox closeLauncherCheckBox = new JCheckBox("Close launcher after launching the game");
    private final JCheckBox fullscreenCheckBox = new JCheckBox("Launch in fullscreen");
    private final JScrollPane bodyScrollPane;
    private final HeaderIconButton closeButton = new HeaderIconButton(null, "Close", false);
    private boolean syncingMemoryControls;

    public SettingsPopup(LauncherSettings settings) {
        super("settings", new Dimension(590, 590));
        this.settings = settings;
        setLayout(new BorderLayout());

        SurfacePanel panel = PopupScaffold.createRootPanel();
        panel.add(PopupScaffold.createHeader(titleLabel, subtitleLabel, closeButton), BorderLayout.NORTH);
        bodyScrollPane = PopupScaffold.createScrollPane(createBody());
        panel.add(bodyScrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);

        configureFields();
        configureMemoryControls();
        configureDisplayControls();
        PopupTextField.installOutsideBlur(this);
        closeButton.addActionListener(event -> requestClose());

        syncFromSettings();
        applyTheme();
        ThemeManager.getInstance().addListener(this::applyTheme);
    }

    @Override
    public void beforeOpen() {
        syncFromSettings();
    }

    @Override
    public void onDismiss() {
        settings.setMemoryMb(clampToSlider(parseMemoryFieldValue()));
        settings.setCloseLauncherAfterLaunch(closeLauncherCheckBox.isSelected());
        settings.setGameWidth(parseDimensionField(widthField, settings.getGameWidth(), MIN_GAME_WIDTH, MAX_GAME_WIDTH));
        settings.setGameHeight(parseDimensionField(heightField, settings.getGameHeight(), MIN_GAME_HEIGHT, MAX_GAME_HEIGHT));
        settings.setFullscreen(fullscreenCheckBox.isSelected());
        settings.setLaunchArguments(launchArgumentsField.getText());
    }

    private void configureFields() {
        configureNumericField(memoryField, MEMORY_FIELD_WIDTH, SwingConstants.CENTER, "4096");
        configureNumericField(widthField, RESOLUTION_FIELD_WIDTH, SwingConstants.CENTER, "1280");
        configureNumericField(heightField, RESOLUTION_FIELD_WIDTH, SwingConstants.CENTER, "720");
        launchArgumentsField.setFont(UiTypography.regular(UiTypography.BODY));
        launchArgumentsField.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
        launchArgumentsField.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        launchArgumentsField.setPlaceholderText("Extra JVM or game arguments");

        ((AbstractDocument) memoryField.getDocument()).setDocumentFilter(new DigitsDocumentFilter(7));
        ((AbstractDocument) widthField.getDocument()).setDocumentFilter(new DigitsDocumentFilter(4));
        ((AbstractDocument) heightField.getDocument()).setDocumentFilter(new DigitsDocumentFilter(4));

        memoryField.addActionListener(event -> commitMemoryField());
        memoryField.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent event) { commitMemoryField(); }});
        widthField.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent event) { widthField.setText(String.valueOf(parseDimensionField(widthField, settings.getGameWidth(), MIN_GAME_WIDTH, MAX_GAME_WIDTH))); }});
        heightField.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent event) { heightField.setText(String.valueOf(parseDimensionField(heightField, settings.getGameHeight(), MIN_GAME_HEIGHT, MAX_GAME_HEIGHT))); }});
    }

    private void configureDisplayControls() {
        fullscreenCheckBox.addActionListener(event -> updateDisplayFieldState());
    }

    private void configureNumericField(PopupTextField field, int width, int alignment, String placeholder) {
        field.setFont(UiTypography.regular(UiTypography.BODY));
        field.setPreferredSize(new Dimension(width, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(width, FIELD_HEIGHT));
        field.setHorizontalAlignment(alignment);
        field.setPlaceholderText(placeholder);
    }

    private void configureMemoryControls() {
        memorySlider.setMajorTickSpacing(1024);
        memorySlider.setMinorTickSpacing(MEMORY_STEP_MB);
        memorySlider.setSnapToTicks(true);
        memorySlider.addChangeListener(event -> {
            if (!syncingMemoryControls) {
                syncingMemoryControls = true;
                memoryField.setText(String.valueOf(memorySlider.getValue()));
                syncingMemoryControls = false;
            }
        });
    }

    private JPanel createBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(createMemorySection());
        body.add(Box.createVerticalStrut(SECTION_GAP));
        body.add(createDisplaySection());
        body.add(Box.createVerticalStrut(SECTION_GAP));
        body.add(createAdvancedSection());
        body.add(Box.createVerticalStrut(SECTION_GAP));
        body.add(createLaunchSection());
        return body;
    }

    private JPanel createMemorySection() {
        SurfacePanel section = createSectionPanel();
        section.add(createSectionTitle(memorySectionLabel), BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(memorySlider, BorderLayout.CENTER);
        row.add(createValueField(memoryField, memoryValueSuffixLabel), BorderLayout.EAST);
        section.add(row, BorderLayout.CENTER);
        return section;
    }

    private JPanel createLaunchSection() {
        SurfacePanel section = createSectionPanel();
        section.add(createSectionTitle(launchSectionLabel), BorderLayout.NORTH);
        section.add(closeLauncherCheckBox, BorderLayout.CENTER);
        return section;
    }

    private JPanel createDisplaySection() {
        SurfacePanel section = createSectionPanel();
        section.add(createSectionTitle(displaySectionLabel), BorderLayout.NORTH);
        section.add(createDisplayRow(), BorderLayout.CENTER);
        return section;
    }

    private JPanel createAdvancedSection() {
        SurfacePanel section = createSectionPanel();
        section.add(createSectionTitle(advancedSectionLabel), BorderLayout.NORTH);
        section.add(launchArgumentsField, BorderLayout.CENTER);
        return section;
    }

    private SurfacePanel createSectionPanel() {
        SurfacePanel section = new SurfacePanel(SECTION_ARC, SECTION_PADDING, true);
        section.setLayout(new BorderLayout(0, 12));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return section;
    }

    private JComponent createSectionTitle(JLabel label) {
        label.setFont(UiTypography.regular(UiTypography.BODY));
        return label;
    }

    private JPanel createDisplayRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel resolutionRow = new JPanel();
        resolutionRow.setOpaque(false);
        resolutionRow.setLayout(new BoxLayout(resolutionRow, BoxLayout.X_AXIS));
        resolutionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        resolutionValueLabel.setFont(UiTypography.regular(UiTypography.BODY));
        resolutionValueLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        resolutionRow.add(resolutionValueLabel);
        resolutionRow.add(Box.createHorizontalStrut(RESOLUTION_LABEL_GAP));
        JPanel resolutionFields = createResolutionFields();
        resolutionFields.setAlignmentY(Component.CENTER_ALIGNMENT);
        resolutionRow.add(resolutionFields);
        resolutionRow.add(Box.createHorizontalGlue());

        fullscreenCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(resolutionRow);
        row.add(Box.createVerticalStrut(DISPLAY_ROW_GAP));
        row.add(fullscreenCheckBox);
        return row;
    }

    private JPanel createResolutionFields() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.add(widthField);
        wrapper.add(Box.createHorizontalStrut(RESOLUTION_FIELD_GAP));
        wrapper.add(createDividerPanel());
        wrapper.add(Box.createHorizontalStrut(RESOLUTION_FIELD_GAP));
        wrapper.add(heightField);
        return wrapper;
    }

    private JPanel createDividerPanel() {
        JPanel dividerPanel = new JPanel(null);
        dividerPanel.setOpaque(false);
        dividerPanel.setPreferredSize(new Dimension(RESOLUTION_DIVIDER_WIDTH, FIELD_HEIGHT));
        dividerPanel.setMaximumSize(new Dimension(RESOLUTION_DIVIDER_WIDTH, FIELD_HEIGHT));
        resolutionDividerLabel.setFont(UiTypography.regular(UiTypography.EMPHASIS));
        Dimension labelSize = resolutionDividerLabel.getPreferredSize();
        int labelX = Math.max(0, (RESOLUTION_DIVIDER_WIDTH - labelSize.width) / 2);
        int labelY = Math.max(0, (FIELD_HEIGHT - labelSize.height) / 2 - 3);
        resolutionDividerLabel.setBounds(labelX, labelY, labelSize.width, labelSize.height);
        dividerPanel.add(resolutionDividerLabel);
        return dividerPanel;
    }

    private JPanel createValueField(PopupTextField field, JLabel suffixLabel) {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setOpaque(false);
        wrapper.add(field, BorderLayout.CENTER);
        suffixLabel.setFont(UiTypography.regular(UiTypography.BODY));
        wrapper.add(suffixLabel, BorderLayout.EAST);
        return wrapper;
    }

    private void syncFromSettings() {
        syncingMemoryControls = true;
        memorySlider.setValue(clampToSlider(settings.getMemoryMb()));
        memoryField.setText(String.valueOf(clampToSlider(settings.getMemoryMb())));
        syncingMemoryControls = false;
        closeLauncherCheckBox.setSelected(settings.isCloseLauncherAfterLaunch());
        widthField.setText(String.valueOf(settings.getGameWidth()));
        heightField.setText(String.valueOf(settings.getGameHeight()));
        fullscreenCheckBox.setSelected(settings.isFullscreen());
        updateDisplayFieldState();
        launchArgumentsField.setText(settings.getLaunchArguments());
        bodyScrollPane.getVerticalScrollBar().setValue(0);
    }

    private void commitMemoryField() {
        int memoryValue = parseMemoryFieldValue();
        syncingMemoryControls = true;
        memoryField.setText(String.valueOf(memoryValue));
        memorySlider.setValue(clampToSlider(memoryValue));
        syncingMemoryControls = false;
    }

    private int parseMemoryFieldValue() {
        return parseNumericValue(memoryField.getText(), Math.max(MIN_MEMORY_FIELD_MB, settings.getMemoryMb()), MIN_MEMORY_FIELD_MB, Integer.MAX_VALUE);
    }

    private int parseDimensionField(PopupTextField field, int fallback, int min, int max) {
        return parseNumericValue(field.getText(), fallback, min, max);
    }

    private int parseNumericValue(String value, int fallback, int min, int max) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return clamp(fallback, min, max);
        }
        try {
            return clamp(Integer.parseInt(digits), min, max);
        } catch (NumberFormatException exception) {
            return clamp(fallback, min, max);
        }
    }

    private int clampToSlider(int value) {
        int clamped = clamp(value, MIN_MEMORY_MB, MAX_MEMORY_MB);
        return clamp(Math.round((clamped - MIN_MEMORY_MB) / (float) MEMORY_STEP_MB) * MEMORY_STEP_MB + MIN_MEMORY_MB, MIN_MEMORY_MB, MAX_MEMORY_MB);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateDisplayFieldState() {
        boolean enabled = !fullscreenCheckBox.isSelected();
        widthField.setEnabled(enabled);
        heightField.setEnabled(enabled);
        resolutionValueLabel.setEnabled(enabled);
        resolutionDividerLabel.setEnabled(enabled);
    }

    private void applyTheme() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        PopupScaffold.applyHeaderTheme(titleLabel, subtitleLabel, closeButton, palette);
        memorySectionLabel.setForeground(palette.textPrimary);
        launchSectionLabel.setForeground(palette.textPrimary);
        displaySectionLabel.setForeground(palette.textPrimary);
        advancedSectionLabel.setForeground(palette.textPrimary);
        memoryValueSuffixLabel.setForeground(palette.textSecondary);
        resolutionValueLabel.setForeground(resolutionValueLabel.isEnabled() ? palette.textSecondary : palette.textMuted);
        resolutionDividerLabel.setForeground(resolutionDividerLabel.isEnabled() ? palette.textSecondary : palette.textMuted);
        applyPopupFieldTheme(memoryField, palette);
        applyPopupFieldTheme(widthField, palette);
        applyPopupFieldTheme(heightField, palette);
        applyPopupFieldTheme(launchArgumentsField, palette);
        ThemeStyler.styleCheckBox(closeLauncherCheckBox, palette);
        ThemeStyler.styleCheckBox(fullscreenCheckBox, palette);
        ThemeStyler.styleScrollPane(bodyScrollPane, palette, (JPanel) bodyScrollPane.getViewport().getView(), PopupScaffold.SCROLLBAR_CONTENT_GAP);
        repaint();
    }

    private void applyPopupFieldTheme(PopupTextField field, ThemePalette palette) {
        field.setForeground(palette.textPrimary);
        field.setDisabledTextColor(palette.textMuted);
        field.setCaretColor(palette.textPrimary);
        field.setSelectionColor(palette.textSelectionFill);
        field.setSelectedTextColor(palette.textPrimary);
        field.repaint();
    }

    private static final class DigitsDocumentFilter extends DocumentFilter {
        private final int maxLength;
        private DigitsDocumentFilter(int maxLength) { this.maxLength = maxLength; }
        @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException { replace(fb, offset, 0, string, attr); }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String insertion = text == null ? "" : text.replaceAll("[^0-9]", "");
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String candidate = current.substring(0, offset) + insertion + current.substring(offset + length);
            if (candidate.length() <= maxLength) {
                super.replace(fb, offset, length, insertion, attrs);
            }
        }
    }
}
