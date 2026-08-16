package net.litelauncher.backend.launch;

import net.litelauncher.backend.platform.LauncherPaths;
import net.litelauncher.backend.modpack.ModpackInstance;

import java.nio.file.Path;

record LaunchEnvironment(
        Path gameDirectory,
        Path versionsDirectory,
        String launchVersionId,
        boolean modpack,
        boolean instance
) {
    static LaunchEnvironment minecraft(String versionId) {
        return new LaunchEnvironment(LauncherPaths.minecraftDirectory(), LauncherPaths.versionsDirectory(), versionId, false, false);
    }

    static LaunchEnvironment instance(String instanceId, String launchVersionId) {
        return new LaunchEnvironment(LauncherPaths.instanceDirectory(instanceId), LauncherPaths.instanceVersionsDirectory(instanceId),
                launchVersionId, false, true);
    }

    static LaunchEnvironment modpack(ModpackInstance instance) {
        return new LaunchEnvironment(instance.directory(), instance.versionsDirectory(), instance.launchVersionId(), true, false);
    }

    boolean sharedVersionFallback() {
        return modpack;
    }

    Path nativesDirectory() {
        String id = launchVersionId == null || launchVersionId.isBlank() ? "pending" : launchVersionId;
        return versionsDirectory.resolve(id).resolve("natives");
    }
}
