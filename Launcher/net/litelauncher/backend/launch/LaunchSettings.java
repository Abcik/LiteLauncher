package net.litelauncher.backend.launch;

import net.litelauncher.Language;

public record LaunchSettings(
        Language language,
        int memoryAmount,
        int screenWidth,
        int screenHeight,
        boolean fullscreen,
        String jvmArguments,
        boolean closeAfterLaunch,
        boolean instancesStorageSystem
) {
    public LaunchSettings {
        language = language == null ? Language.ENGLISH : language;
        memoryAmount = Math.max(512, memoryAmount);
        screenWidth = Math.max(1, screenWidth);
        screenHeight = Math.max(1, screenHeight);
        jvmArguments = jvmArguments == null ? "" : jvmArguments;
    }
}
