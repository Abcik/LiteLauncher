package net.litelauncher.ui.popup;

import net.litelauncher.service.VersionCatalogService;
import net.litelauncher.service.VersionOption;
import net.litelauncher.state.LaunchSelection;
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
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public final class VersionPopup extends BaseModalPanel {
    private final LauncherSettings settings;
    private final LaunchSelection launchSelection;
    private final JLabel titleLabel = new JLabel("Versions");
    private final JLabel subtitleLabel = new JLabel("Choose version.");
    private final JPanel versionsContainer = new JPanel();
    private final JCheckBox snapshotsCheckBox = new JCheckBox("Show snapshots");
    private final JCheckBox modificationsCheckBox = new JCheckBox("Show modifications");
    private final JScrollPane scrollPane;
    private final HeaderIconButton closeButton = new HeaderIconButton(null, "Close", false);
    private final List<VersionOption> entries;
    private final List<VersionRow> rows = new ArrayList<>();
    private String selectedVersion;

    public VersionPopup(VersionCatalogService versionCatalogService, LauncherSettings settings, LaunchSelection launchSelection) {
        super("versions", new Dimension(500, 490));
        this.settings = settings;
        this.launchSelection = launchSelection;
        this.entries = versionCatalogService.getVersions();
        this.selectedVersion = launchSelection.getSelectedBuild();
        setLayout(new BorderLayout());

        SurfacePanel panel = PopupScaffold.createRootPanel();
        panel.add(PopupScaffold.createHeader(titleLabel, subtitleLabel, closeButton), BorderLayout.NORTH);
        versionsContainer.setOpaque(false);
        versionsContainer.setLayout(new BoxLayout(versionsContainer, BoxLayout.Y_AXIS));
        scrollPane = PopupScaffold.createScrollPane(versionsContainer);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createFooter(), BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);

        closeButton.addActionListener(event -> requestClose());
        snapshotsCheckBox.addActionListener(event -> refreshRows());
        modificationsCheckBox.addActionListener(event -> refreshRows());

        buildRows();
        ThemeManager.getInstance().addListener(this::applyTheme);
        applyTheme();
        refreshRows();
    }

    @Override
    public void beforeOpen() {
        selectedVersion = launchSelection.getSelectedBuild();
        snapshotsCheckBox.setSelected(settings.isSnapshotsEnabled());
        modificationsCheckBox.setSelected(settings.isModificationsEnabled());
        scrollPane.getVerticalScrollBar().setValue(0);
        refreshRows();
    }

    @Override
    public void onDismiss() {
        settings.setSnapshotsEnabled(snapshotsCheckBox.isSelected());
        settings.setModificationsEnabled(modificationsCheckBox.isSelected());
        launchSelection.setSelectedBuild(selectedVersion);
    }

    private JPanel createFooter() {
        SurfacePanel footer = new SurfacePanel(18, new Insets(12, 16, 12, 16), true);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        snapshotsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        modificationsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(snapshotsCheckBox);
        footer.add(Box.createVerticalStrut(8));
        footer.add(modificationsCheckBox);
        return footer;
    }

    private void buildRows() {
        rows.clear();
        for (VersionOption entry : entries) {
            rows.add(new VersionRow(entry, () -> {
                selectedVersion = entry.getBuild();
                refreshRowSelection();
            }));
        }
    }

    private void refreshRows() {
        versionsContainer.removeAll();
        boolean showSnapshots = snapshotsCheckBox.isSelected();
        boolean showMods = modificationsCheckBox.isSelected();
        boolean first = true;
        for (VersionRow row : rows) {
            VersionOption entry = row.entry;
            boolean visible = (!entry.isSnapshot() || showSnapshots) && (!entry.isModified() || showMods);
            if (!visible) {
                continue;
            }
            if (!first) {
                versionsContainer.add(Box.createVerticalStrut(PopupMetrics.STACK_GAP));
            }
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setSelected(entry.getBuild().equals(selectedVersion));
            versionsContainer.add(row);
            first = false;
        }
        versionsContainer.revalidate();
        versionsContainer.repaint();
        applyTheme();
    }

    private void refreshRowSelection() {
        for (VersionRow row : rows) {
            row.setSelected(row.entry.getBuild().equals(selectedVersion));
        }
        versionsContainer.repaint();
    }

    private void applyTheme() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        PopupScaffold.applyHeaderTheme(titleLabel, subtitleLabel, closeButton, palette);
        ThemeStyler.styleCheckBox(snapshotsCheckBox, palette);
        ThemeStyler.styleCheckBox(modificationsCheckBox, palette);
        ThemeStyler.styleScrollPane(scrollPane, palette, versionsContainer, PopupScaffold.SCROLLBAR_CONTENT_GAP);
        for (VersionRow row : rows) {
            row.refreshTheme();
        }
        repaint();
    }

    private static final class VersionRow extends PopupInteractiveRow {
        private final VersionOption entry;
        private final JLabel titleLabel = new JLabel();
        private final JLabel descriptionLabel = new JLabel();

        private VersionRow(VersionOption entry, Runnable onClick) {
            super(PopupMetrics.DEFAULT_ARC, PopupMetrics.STANDARD_ROW_PADDING, true);
            this.entry = entry;
            setLayout(new BorderLayout());
            PopupMetrics.applyStandardSize(this, PopupMetrics.standardRowSize(), PopupMetrics.STANDARD_ROW_HEIGHT);
            titleLabel.setFont(UiTypography.regular(PopupMetrics.ROW_TITLE_SIZE));
            descriptionLabel.setFont(UiTypography.regular(PopupMetrics.ROW_SUBTITLE_SIZE));
            add(PopupMetrics.createCenteredTwoLineText(titleLabel, descriptionLabel), BorderLayout.CENTER);
            setOnClick(onClick);
            refreshTheme();
        }

        private void setSelected(boolean selected) {
            setSelectedState(selected);
            refreshTheme();
        }

        void refreshTheme() {
            ThemePalette palette = ThemeManager.getInstance().palette();
            Color textColor = isSelectedState() ? PopupMetrics.ACCENT_TEXT : palette.textPrimary;
            Color subtitleColor = isSelectedState() ? PopupMetrics.ACCENT_TEXT : palette.textSecondary;
            titleLabel.setText(entry.getBuild());
            descriptionLabel.setText(entry.getDescription());
            titleLabel.setForeground(textColor);
            descriptionLabel.setForeground(subtitleColor);
            repaint();
        }
    }
}
