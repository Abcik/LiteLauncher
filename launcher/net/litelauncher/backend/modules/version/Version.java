package net.litelauncher.backend.modules.version;

public record Version(
        String id,
        String title,
        String subtitle,
        Type type,
        boolean modded,
        boolean loaded,
        boolean custom,
        String url,
        long releaseTime
) {

    private static final long LEGACY_RELEASE_TIME_CUTOFF = 1_610_640_332_000L;

    public enum Type {
        SNAPSHOT, RELEASE, BETA, ALPHA, CUSTOM
    }

    public Version {
        id = safe(id);
        title = safe(title);
        subtitle = safe(subtitle);
        type = type == null ? Type.RELEASE : type;
        url = url == null ? "" : url;
    }

    public static Version official(String id, Type type, boolean loaded, boolean modded, String url, long releaseTime) {
        return new Version(id, VersionNames.title(id, type, false, ""), VersionNames.subtitle(id), type, modded, loaded, false, url, releaseTime);
    }

    public static Version custom(String id, Type type, boolean modded, long releaseTime, String inheritsFrom) {
        Type safeType = type == null || type == Type.CUSTOM ? Type.CUSTOM : type;
        return new Version(id, VersionNames.title(id, safeType, true, inheritsFrom), VersionNames.subtitle(id), safeType, modded, true, true, "", releaseTime);
    }

    public boolean snapshot() {
        return type == Type.SNAPSHOT;
    }

    public boolean releaseLike() {
        return type == Type.RELEASE || type == Type.BETA || type == Type.ALPHA;
    }

    public boolean legacy() {
        return releaseTime > 0L && releaseTime < LEGACY_RELEASE_TIME_CUTOFF;
    }

    public String kindText() {
        return VersionNames.badge(id, type, custom);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
