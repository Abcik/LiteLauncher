package net.litelauncher.backend.version;

import net.litelauncher.i18n.I18n;

final class VersionNames {

    static String title(String id, boolean custom, String inheritsFrom) {
        return title(id, custom, inheritsFrom, "");
    }

    static String title(String id, boolean custom, String inheritsFrom, String explicitKind) {
        String safeId = safe(id);
        if (!custom) return "Minecraft " + safeId;

        String kind = safe(explicitKind).isBlank() ? customKind(safeId) : safe(explicitKind);
        String version = safe(inheritsFrom).isBlank() ? minecraftVersionFromId(safeId) : safe(inheritsFrom);
        return kind + " " + version;
    }

    static String subtitle(String id, boolean custom, String inheritsFrom) {
        String safeId = safe(id);
        if (!custom) return safeId;
        String parent = safe(inheritsFrom);
        String lower = safeId.toLowerCase();

        if (lower.startsWith("fabric-loader-") && !parent.isBlank()) {
            return stripBetween(safeId, "fabric-loader-", "-" + parent);
        }
        if (lower.startsWith("quilt-loader-") && !parent.isBlank()) {
            return stripBetween(safeId, "quilt-loader-", "-" + parent);
        }
        if (lower.startsWith("neoforge-")) return safeId.substring("neoforge-".length());
        if (lower.contains("-optifine_")) return safeId.substring(lower.indexOf("-optifine_") + "-optifine_".length());
        if (!parent.isBlank() && safeId.startsWith(parent + "-forge-")) {
            return safeId.substring((parent + "-forge-").length());
        }
        return safeId;
    }

    private static String customKind(String id) {
        String lower = id.toLowerCase();
        if (lower.contains("optifine")) return "OptiFine";
        if (lower.contains("neoforge")) return "NeoForge";
        if (lower.contains("fabric")) return "Fabric";
        if (lower.contains("forge")) return "Forge";
        if (lower.contains("quilt")) return "Quilt";
        return I18n.text("versions.custom");
    }

    private static String minecraftVersionFromId(String id) {
        String lower = id.toLowerCase();
        int optifine = lower.indexOf("-optifine_");
        if (optifine > 0) return id.substring(0, optifine);
        return id;
    }

    private static String stripBetween(String value, String prefix, String suffix) {
        if (!value.startsWith(prefix) || !value.endsWith(suffix) || value.length() <= prefix.length() + suffix.length()) return value;
        return value.substring(prefix.length(), value.length() - suffix.length());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
