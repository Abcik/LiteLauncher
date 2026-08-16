package net.litelauncher.backend.loader;

public record LoaderOption(LoaderType type, LoaderVersion version) {
    public LoaderOption {
        if (type == null) throw new IllegalArgumentException("Loader type is required.");
    }

    public boolean available() {
        return version != null && !version.id().isBlank() && !version.version().isBlank();
    }
}
