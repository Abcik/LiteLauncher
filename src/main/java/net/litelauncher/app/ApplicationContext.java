package net.litelauncher.app;

import net.litelauncher.service.LaunchService;
import net.litelauncher.service.PendingLaunchService;
import net.litelauncher.service.StaticVersionCatalogService;
import net.litelauncher.service.VersionCatalogService;
import net.litelauncher.state.AccountStore;
import net.litelauncher.state.LaunchSelection;
import net.litelauncher.state.LauncherSettings;

public final class ApplicationContext {
    private final LauncherSettings launcherSettings = new LauncherSettings();
    private final AccountStore accountStore = new AccountStore();
    private final LaunchSelection launchSelection = new LaunchSelection("1.21.11 Vanilla");
    private final VersionCatalogService versionCatalogService = new StaticVersionCatalogService();
    private final LaunchService launchService = new PendingLaunchService();

    public LauncherSettings launcherSettings() {
        return launcherSettings;
    }

    public AccountStore accountStore() {
        return accountStore;
    }

    public LaunchSelection launchSelection() {
        return launchSelection;
    }

    public VersionCatalogService versionCatalogService() {
        return versionCatalogService;
    }

    public LaunchService launchService() {
        return launchService;
    }
}
