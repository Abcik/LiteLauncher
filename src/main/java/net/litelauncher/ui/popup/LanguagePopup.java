package net.litelauncher.ui.popup;

import net.litelauncher.state.LauncherSettings;
import net.litelauncher.ui.components.HeaderIconButton;
import net.litelauncher.ui.components.PopupInteractiveRow;
import net.litelauncher.ui.components.PopupMetrics;
import net.litelauncher.ui.components.PopupScaffold;
import net.litelauncher.ui.components.SurfacePanel;
import net.litelauncher.ui.modal.BaseModalPanel;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.ThemeStyler;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public final class LanguagePopup extends BaseModalPanel {
    private static final String[] LANGUAGES = {"English", "Русский", "Українська", "Deutsch", "Español", "Français", "Português (Brasil)", "Italiano", "Polski", "Čeština", "Türkçe", "Română", "Magyar", "Қазақша", "ქართული", "Հայերեն"};

    private final LauncherSettings settings;
    private final JLabel titleLabel = new JLabel("Language");
    private final JLabel subtitleLabel = new JLabel("Choose launcher language.");
    private final JPanel listContainer = new JPanel();
    private final JScrollPane scrollPane;
    private final HeaderIconButton closeButton = new HeaderIconButton(null, "Close", false);
    private final List<LanguageRow> rows = new ArrayList<>();
    private String selectedLanguage;

    public LanguagePopup(LauncherSettings settings) {
        super("language", new Dimension(392, 392));
        this.settings = settings;
        this.selectedLanguage = settings.getLanguage();
        setLayout(new BorderLayout());

        SurfacePanel panel = PopupScaffold.createRootPanel();
        panel.add(PopupScaffold.createHeader(titleLabel, subtitleLabel, closeButton), BorderLayout.NORTH);
        listContainer.setOpaque(false);
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        scrollPane = PopupScaffold.createScrollPane(listContainer);
        panel.add(scrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);

        closeButton.addActionListener(event -> requestClose());
        buildRows();
        ThemeManager.getInstance().addListener(this::applyTheme);
        applyTheme();
    }

    @Override
    public void beforeOpen() {
        selectedLanguage = settings.getLanguage();
        refreshRowSelection();
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    @Override
    public void onDismiss() {
        settings.setLanguage(selectedLanguage);
    }

    private void buildRows() {
        rows.clear();
        for (String language : LANGUAGES) {
            rows.add(new LanguageRow(language, () -> {
                selectedLanguage = language;
                refreshRowSelection();
            }));
        }
        listContainer.removeAll();
        boolean first = true;
        for (LanguageRow row : rows) {
            if (!first) {
                listContainer.add(Box.createVerticalStrut(PopupMetrics.STACK_GAP));
            }
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            listContainer.add(row);
            first = false;
        }
        refreshRowSelection();
    }

    private void refreshRowSelection() {
        for (LanguageRow row : rows) {
            row.setSelected(row.language.equals(selectedLanguage));
        }
        listContainer.repaint();
    }

    private void applyTheme() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        PopupScaffold.applyHeaderTheme(titleLabel, subtitleLabel, closeButton, palette);
        ThemeStyler.styleScrollPane(scrollPane, palette, listContainer, PopupScaffold.SCROLLBAR_CONTENT_GAP);
        for (LanguageRow row : rows) {
            row.refreshTheme();
        }
        repaint();
    }

    private static final class LanguageRow extends PopupInteractiveRow {
        private final String language;
        private final JLabel titleLabel = new JLabel();

        private LanguageRow(String language, Runnable onClick) {
            super(PopupMetrics.DEFAULT_ARC, PopupMetrics.COMPACT_ROW_PADDING, true);
            this.language = language;
            setLayout(new BorderLayout());
            PopupMetrics.applyStandardSize(this, PopupMetrics.compactRowSize(), PopupMetrics.COMPACT_ROW_HEIGHT);
            titleLabel.setFont(UiTypography.regular(PopupMetrics.ROW_TITLE_SIZE));
            add(PopupMetrics.createCenteredSingleLineText(titleLabel), BorderLayout.CENTER);
            setOnClick(onClick);
            refreshTheme();
        }

        private void setSelected(boolean selected) {
            setSelectedState(selected);
            refreshTheme();
        }

        private void refreshTheme() {
            ThemePalette palette = ThemeManager.getInstance().palette();
            titleLabel.setText(language);
            titleLabel.setForeground(isSelectedState() ? PopupMetrics.ACCENT_TEXT : palette.textPrimary);
            repaint();
        }
    }
}
