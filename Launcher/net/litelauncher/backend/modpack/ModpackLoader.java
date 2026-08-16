package net.litelauncher.backend.modpack;

public enum ModpackLoader {
    VANILLA(""),
    FABRIC("fabric-loader"),
    QUILT("quilt-loader"),
    FORGE("forge"),
    NEOFORGE("neoforge");

    private final String dependencyId;

    ModpackLoader(String dependencyId) {
        this.dependencyId = dependencyId;
    }

    public static ModpackLoader fromDependencyId(String id) {
        if (id == null || id.isBlank()) return null;
        for (ModpackLoader loader : values()) {
            if (loader.dependencyId.equals(id)) return loader;
        }
        return null;
    }
}
