package net.litelauncher;

import net.litelauncher.backend.auth.AuthService;
import net.litelauncher.backend.auth.ElyBySkinService;
import net.litelauncher.backend.auth.MicrosoftCallbackPageRenderer;
import net.litelauncher.backend.launch.ElyByAuthlibCatalog;
import net.litelauncher.backend.launch.GameLaunchService;
import net.litelauncher.backend.modpack.ModpackService;
import net.litelauncher.backend.loader.LoaderCatalog;
import net.litelauncher.backend.loader.LoaderInstaller;
import net.litelauncher.backend.version.VersionService;
import net.litelauncher.frontend.modules.auth.MicrosoftCallbackPage;
import net.litelauncher.backend.discord.DiscordRpcService;
import net.litelauncher.backend.download.DownloadService;

final class LauncherServices {

    private final DownloadService downloadService = new DownloadService();
    private final AuthService authService = new AuthService(callbackPages());
    private final ElyBySkinService elyBySkinService = new ElyBySkinService();
    private final LoaderCatalog loaderCatalog = new LoaderCatalog();
    private final LoaderInstaller loaderInstaller = new LoaderInstaller(downloadService);
    private final ModpackService modpackService = new ModpackService(loaderInstaller, downloadService);
    private final VersionService versionService = new VersionService(modpackService);
    private final ElyByAuthlibCatalog elyByAuthlibCatalog = new ElyByAuthlibCatalog();
    private final GameLaunchService gameLaunchService = new GameLaunchService(authService, versionService, modpackService, elyByAuthlibCatalog, loaderInstaller, downloadService);
    private final DiscordRpcService discordRpcService = new DiscordRpcService();

    AuthService authService() {
        return authService;
    }

    VersionService versionService() {
        return versionService;
    }

    ModpackService modpackService() {
        return modpackService;
    }

    LoaderCatalog loaderCatalog() {
        return loaderCatalog;
    }

    ElyBySkinService elyBySkinService() {
        return elyBySkinService;
    }

    ElyByAuthlibCatalog elyByAuthlibCatalog() {
        return elyByAuthlibCatalog;
    }

    GameLaunchService gameLaunchService() {
        return gameLaunchService;
    }

    DiscordRpcService discordRpcService() {
        return discordRpcService;
    }

    private static MicrosoftCallbackPageRenderer callbackPages() {
        return new MicrosoftCallbackPageRenderer() {
            @Override
            public String successHtml(Theme theme, Language language, String profileName, String skinPng, boolean slim) {
                return MicrosoftCallbackPage.successHtml(theme, language, profileName, skinPng, slim);
            }

            @Override
            public String errorHtml(Theme theme, Language language, String message) {
                return MicrosoftCallbackPage.errorHtml(theme, language, message);
            }
        };
    }
}
