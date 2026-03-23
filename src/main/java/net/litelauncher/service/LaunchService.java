package net.litelauncher.service;

import net.litelauncher.state.AccountStore;
import net.litelauncher.state.LaunchSelection;
import net.litelauncher.state.LauncherSettings;

public interface LaunchService {
    String launch(AccountStore accountStore, LaunchSelection selection, LauncherSettings settings);
}
