package net.litelauncher.backend.platform;

import java.nio.file.Path;

public final class LauncherPaths {

    public static Path minecraftDirectory() {
        String home = System.getProperty("user.home", ".");
        OperatingSystem os = OperatingSystem.current();
        if (os.windows()) return windowsMinecraftDirectory(home);
        if (os.macos()) return Path.of(home, "Library", "Application Support", "minecraft");
        return Path.of(home, ".minecraft");
    }

    public static Path liteLauncherDirectory() {
        return minecraftDirectory().resolve("litelauncher");
    }

    public static Path javaDirectory() {
        return liteLauncherDirectory().resolve("java");
    }

    public static Path javaRuntimeDirectory(String runtimeId) {
        return javaDirectory().resolve(runtimeId == null || runtimeId.isBlank() ? "jre-8" : runtimeId);
    }

    public static Path bootstrapDirectory() {
        return liteLauncherDirectory().resolve("bootstrap");
    }

    public static Path launcherDirectory() {
        return bootstrapDirectory().resolve("launcher");
    }

    public static Path logsDirectory() {
        return minecraftDirectory().resolve("logs");
    }

    public static Path bootstrapManifestFile() {
        return bootstrapDirectory().resolve("version_manifest.json");
    }

    private static Path windowsMinecraftDirectory(String home) {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) return Path.of(appData, ".minecraft");
        return Path.of(home, "AppData", "Roaming", ".minecraft");
    }

    private LauncherPaths() {
    }
}
