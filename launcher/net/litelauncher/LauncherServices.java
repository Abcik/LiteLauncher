package net.litelauncher;

import net.litelauncher.backend.BackendExecutor;
import net.litelauncher.backend.auth.AuthService;
import net.litelauncher.backend.launch.GameLaunchService;
import net.litelauncher.backend.version.VersionService;

final class LauncherServices {

    private final BackendExecutor backendExecutor = new BackendExecutor();
    private final AuthService authService = new AuthService(null);
    private final VersionService versionService = new VersionService();
    private final GameLaunchService gameLaunchService = new GameLaunchService(authService, versionService);

    BackendExecutor backendExecutor() {
        return backendExecutor;
    }

    AuthService authService() {
        return authService;
    }

    VersionService versionService() {
        return versionService;
    }

    GameLaunchService gameLaunchService() {
        return gameLaunchService;
    }

}
