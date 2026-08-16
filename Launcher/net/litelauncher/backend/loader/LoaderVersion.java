package net.litelauncher.backend.loader;

public record LoaderVersion(
        LoaderType type,
        String minecraftVersion,
        String version,
        String id,
        String artifactVersion,
        String downloadUrl
) {
    public LoaderVersion {
        if (type == null) throw new IllegalArgumentException("Loader type is required.");
        minecraftVersion = safe(minecraftVersion);
        version = safe(version);
        id = safe(id);
        artifactVersion = safe(artifactVersion);
        downloadUrl = safe(downloadUrl);
    }

    public String title() {
        return type.title() + " " + minecraftVersion;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
