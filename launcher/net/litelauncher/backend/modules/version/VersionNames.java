package net.litelauncher.backend.modules.version;

final class VersionNames {

    private VersionNames() {
    }

    static String title(String id, Version.Type type, boolean custom, String inheritsFrom) {
        String safeId = safe(id);
        if (!custom) return "Minecraft " + safeId;

        String kind = customKind(safeId);
        String version = safe(inheritsFrom).isBlank() ? safeId : safe(inheritsFrom);
        return kind + " " + version;
    }

    static String subtitle(String id) {
        return safe(id);
    }

    static String badge(String id, Version.Type type, boolean custom) {
        if (custom) return customKind(safe(id));
        return type == Version.Type.SNAPSHOT ? "Snapshot" : "Release";
    }

    private static String customKind(String id) {
        String lower = id.toLowerCase();
        if (lower.contains("optifine")) return "OptiFine";
        if (lower.contains("neoforge")) return "NeoForge";
        if (lower.contains("fabric")) return "Fabric";
        if (lower.contains("forge")) return "Forge";
        if (lower.contains("quilt")) return "Quilt";
        return "Custom";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
