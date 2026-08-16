package net.litelauncher.backend.loader;

public enum LoaderType {
    OPTIFINE("OptiFine"),
    FABRIC("Fabric"),
    FORGE("Forge"),
    NEOFORGE("NeoForge"),
    QUILT("Quilt");

    private final String title;

    LoaderType(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
