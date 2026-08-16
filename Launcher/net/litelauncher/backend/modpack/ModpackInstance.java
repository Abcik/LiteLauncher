package net.litelauncher.backend.modpack;

import java.nio.file.Path;
import java.util.Set;

public record ModpackInstance(
        String id,
        String name,
        String versionId,
        String minecraftVersion,
        ModpackLoader loader,
        String loaderVersion,
        String launchVersionId,
        boolean installed,
        Set<String> overrideFiles,
        Path directory
) {
    public ModpackInstance {
        id = safe(id);
        name = safe(name);
        versionId = safe(versionId);
        minecraftVersion = safe(minecraftVersion);
        loader = loader == null ? ModpackLoader.VANILLA : loader;
        loaderVersion = safe(loaderVersion);
        launchVersionId = safe(launchVersionId);
        overrideFiles = overrideFiles == null ? Set.of() : Set.copyOf(overrideFiles);
    }

    public Path manifestDirectory() {
        return directory.resolve("manifest");
    }

    public Path indexFile() {
        return manifestDirectory().resolve("modrinth.index.json");
    }

    public Path versionsDirectory() {
        return directory.resolve("versions");
    }

    public ModpackInstance withLaunchVersion(String versionId) {
        return new ModpackInstance(id, name, this.versionId, minecraftVersion, loader, loaderVersion,
                versionId, installed, overrideFiles, directory);
    }

    public ModpackInstance withInstalled(boolean value) {
        return new ModpackInstance(id, name, versionId, minecraftVersion, loader, loaderVersion,
                launchVersionId, value, overrideFiles, directory);
    }

    public ModpackInstance withOverrideFiles(Set<String> files) {
        return new ModpackInstance(id, name, versionId, minecraftVersion, loader, loaderVersion,
                launchVersionId, installed, files, directory);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
