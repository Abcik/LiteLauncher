package net.litelauncher.app;

import net.litelauncher.ui.theme.UiTypography;

import javax.swing.SwingUtilities;

public final class LiteLauncherApplication {
    private LiteLauncherApplication() {
    }

    public static void main(String[] args) {
        UiTypography.configureGlobalRendering();
        SwingUtilities.invokeLater(() -> new ApplicationBootstrap().launch());
    }
}
