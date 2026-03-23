package net.litelauncher.app;

import net.litelauncher.ui.LauncherFrame;
import net.litelauncher.ui.theme.ThemeManager;

import javax.swing.ToolTipManager;

public final class ApplicationBootstrap {
    public void launch() {
        ThemeManager.getInstance().installCurrentTheme();
        configureTooltips();

        LauncherFrame frame = new LauncherFrame(new ApplicationContext());
        frame.setVisible(true);
    }

    private void configureTooltips() {
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.setInitialDelay(700);
        toolTipManager.setReshowDelay(250);
        toolTipManager.setDismissDelay(6000);
    }
}
