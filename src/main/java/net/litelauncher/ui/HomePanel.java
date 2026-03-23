package net.litelauncher.ui;

import net.litelauncher.app.ApplicationContext;
import net.litelauncher.ui.components.BuildInfoButton;
import net.litelauncher.ui.components.PlayButton;
import net.litelauncher.ui.modal.ModalCoordinator;
import net.litelauncher.ui.modal.ModalView;
import net.litelauncher.ui.popup.LaunchInfoPopup;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public final class HomePanel extends JPanel {
    private final JLabel titleLabel = new JLabel("LiteLauncher", SwingConstants.CENTER);
    private final JLabel subtitleLabel = new JLabel("Fast, minimal and ready to launch.", SwingConstants.CENTER);
    private final JLabel launcherVersionLabel = new JLabel("LiteLauncher v0.1.0");
    private final JLabel disclaimerLabel = new JLabel("<html><div style='text-align:right;'>NOT AN OFFICIAL MINECRAFT PRODUCT.<br>NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.</div></html>", SwingConstants.RIGHT);
    private final BuildInfoButton selectedBuildButton;
    private final LaunchInfoPopup launchInfoPopup = new LaunchInfoPopup();

    public HomePanel(ApplicationContext context, ModalCoordinator modalCoordinator, ModalView versionPopup) {
        selectedBuildButton = new BuildInfoButton(context.launchSelection().getSelectedBuild());
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(createCenterPanel(context, modalCoordinator, versionPopup), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        context.launchSelection().addListener(() -> selectedBuildButton.setBuildText(context.launchSelection().getSelectedBuild()));
        launchInfoPopup.setCloseAction(modalCoordinator::hide);
        refreshTheme();
        ThemeManager.getInstance().addListener(this::refreshTheme);
    }

    private JPanel createCenterPanel(ApplicationContext context, ModalCoordinator modalCoordinator, ModalView versionPopup) {
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(UiTypography.regular(UiTypography.HERO));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setFont(UiTypography.regular(UiTypography.BODY));

        JPanel heroSpacer = new JPanel();
        heroSpacer.setOpaque(false);
        heroSpacer.setPreferredSize(new Dimension(430, 230));
        heroSpacer.setMaximumSize(new Dimension(430, 230));

        PlayButton playButton = new PlayButton("PLAY");
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setPreferredSize(new Dimension(340, 64));
        playButton.setMaximumSize(new Dimension(340, 64));
        playButton.addActionListener(event -> {
            launchInfoPopup.setMessage(context.launchService().launch(
                    context.accountStore(),
                    context.launchSelection(),
                    context.launcherSettings()));
            modalCoordinator.toggle(launchInfoPopup);
        });

        selectedBuildButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectedBuildButton.addActionListener(event -> modalCoordinator.toggle(versionPopup));

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createVerticalStrut(24));
        centerPanel.add(heroSpacer);
        centerPanel.add(Box.createVerticalStrut(22));
        centerPanel.add(playButton);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(selectedBuildButton);
        centerPanel.add(Box.createVerticalGlue());
        return centerPanel;
    }

    private JPanel createFooter() {
        launcherVersionLabel.setFont(UiTypography.regular(UiTypography.BODY));
        disclaimerLabel.setFont(UiTypography.regular(UiTypography.MICRO));
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.add(launcherVersionLabel, BorderLayout.WEST);
        footer.add(disclaimerLabel, BorderLayout.EAST);
        return footer;
    }

    private void refreshTheme() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        titleLabel.setForeground(palette.textPrimary);
        subtitleLabel.setForeground(palette.textSecondary);
        launcherVersionLabel.setForeground(palette.textMuted);
        disclaimerLabel.setForeground(palette.textMuted);
        repaint();
    }
}
