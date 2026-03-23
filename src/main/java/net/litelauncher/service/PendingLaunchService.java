package net.litelauncher.service;

import net.litelauncher.state.AccountStore;
import net.litelauncher.state.LaunchSelection;
import net.litelauncher.state.LauncherSettings;

public final class PendingLaunchService implements LaunchService {
    @Override
    public String launch(AccountStore accountStore, LaunchSelection selection, LauncherSettings settings) {
        return "Launch execution and account authorization are not enabled in this build yet.\n\n"
                + "Account: " + accountStore.getCurrentAccount() + "\n"
                + "Version: " + selection.getSelectedBuild() + "\n"
                + "Memory: " + settings.getMemoryMb() + " MB";
    }
}
