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

    public static Path versionsDirectory() {
        return minecraftDirectory().resolve("versions");
    }

    public static Path librariesDirectory() {
        return minecraftDirectory().resolve("libraries");
    }

    public static Path assetsDirectory() {
        return minecraftDirectory().resolve("assets");
    }

    public static Path assetIndexesDirectory() {
        return assetsDirectory().resolve("indexes");
    }

    public static Path assetObjectsDirectory() {
        return assetsDirectory().resolve("objects");
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

    public static Path nativesDirectory(String versionId) {
        String safeId = versionId == null || versionId.isBlank() ? "default" : versionId.replaceAll("[^A-Za-z0-9._-]", "_");
        return versionsDirectory().resolve(safeId).resolve("natives");
    }

    public static Path logsDirectory() {
        return minecraftDirectory().resolve("logs");
    }

    public static Path launcherProfileFile() {
        return minecraftDirectory().resolve("launcher_profiles.json");
    }

    public static Path launcherProfileMicrosoftStoreFile() {
        return minecraftDirectory().resolve("launcher_profiles_microsoft_store.json");
    }

    public static Path launcherDataDirectory() {
        return liteLauncherDirectory().resolve("data");
    }

    public static Path authDirectory() {
        return launcherDataDirectory().resolve("auth");
    }

    public static Path launcherStateFile() {
        return launcherDataDirectory().resolve("launcher-state.json");
    }

    public static Path versionManifestFile() {
        return versionsDirectory().resolve("version_manifest_v2.json");
    }

    public static Path offlineSessionsFile() {
        return authDirectory().resolve("offline-sessions.json");
    }

    public static Path microsoftSessionsFile() {
        return authDirectory().resolve("microsoft-sessions.json");
    }

    private static Path windowsMinecraftDirectory(String home) {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) return Path.of(appData, ".minecraft");
        return Path.of(home, "AppData", "Roaming", ".minecraft");
    }
}
