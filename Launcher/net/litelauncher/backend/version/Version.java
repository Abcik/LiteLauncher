package net.litelauncher.backend.version;

import net.litelauncher.backend.loader.LoaderVersion;

public record Version(
        String id,
        String title,
        String subtitle,
        Type type,
        boolean modded,
        boolean loaded,
        boolean custom,
        String url,
        long releaseTime,
        LoaderVersion loader
) {

    private static final long LEGACY_RELEASE_TIME_CUTOFF = 1_321_567_200_000L; // Minecraft 1.0

    public enum Type {
        SNAPSHOT, RELEASE, BETA, ALPHA, CUSTOM, MODPACK
    }

    public Version {
        id = safe(id);
        title = safe(title);
        subtitle = safe(subtitle);
        type = type == null ? Type.RELEASE : type;
        url = url == null ? "" : url;
    }

    public static Version official(String id, Type type, boolean loaded, boolean modded, String url, long releaseTime) {
        return new Version(id, VersionNames.title(id, false, ""), VersionNames.subtitle(id, false, ""),
                type, modded, loaded, false, url, releaseTime, null);
    }

    public static Version custom(String id, Type type, boolean modded, long releaseTime, String inheritsFrom) {
        return custom(id, type, modded, releaseTime, inheritsFrom, "");
    }

    public static Version custom(String id, Type type, boolean modded, long releaseTime, String inheritsFrom, String loaderKind) {
        Type safeType = type == null || type == Type.CUSTOM ? Type.CUSTOM : type;
        return new Version(id, VersionNames.title(id, true, inheritsFrom, loaderKind), VersionNames.subtitle(id, true, inheritsFrom),
                safeType, modded, true, true, "", releaseTime, null);
    }

    public static Version modified(LoaderVersion loader, boolean loaded, long releaseTime) {
        if (loader == null) throw new IllegalArgumentException("Loader version is required.");
        return new Version(loader.id(), loader.title(), loader.version(), Type.RELEASE, true, loaded, loaded,
                "", releaseTime, loader);
    }

    public static Version modpack(String id, String name, String minecraftVersion, boolean loaded, long releaseTime) {
        String title = (safe(name) + " " + safe(minecraftVersion)).trim();
        return new Version(id, title, id, Type.MODPACK, true, loaded, false, "", releaseTime, null);
    }

    public boolean snapshot() {
        return type == Type.SNAPSHOT;
    }

    public boolean releaseLike() {
        return type == Type.RELEASE || type == Type.BETA || type == Type.ALPHA;
    }

    public boolean modpack() {
        return type == Type.MODPACK;
    }

    public boolean pendingLoader() {
        return loader != null && !loaded;
    }


    public boolean modificationInstallersAvailable() {
        return type == Type.RELEASE && !custom && !modded && !modpack()
                && supportsModifications(id);
    }

    private static boolean supportsModifications(String versionId) {
        String[] parts = safe(versionId).split("\\.");
        if (parts.length < 2) return false;

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (major != 1) return major > 1;
            if (minor != 12) return minor > 12;
            return patch >= 2;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    public boolean legacy() {
        return !modpack() && releaseTime > 0L && releaseTime < LEGACY_RELEASE_TIME_CUTOFF;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
