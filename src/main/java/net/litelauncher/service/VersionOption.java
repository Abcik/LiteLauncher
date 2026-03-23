package net.litelauncher.service;

public final class VersionOption {
    private final String build;
    private final String description;
    private final boolean snapshot;
    private final boolean modified;

    public VersionOption(String build, String description, boolean snapshot, boolean modified) {
        this.build = build;
        this.description = description;
        this.snapshot = snapshot;
        this.modified = modified;
    }

    public String getBuild() {
        return build;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public boolean isModified() {
        return modified;
    }
}
