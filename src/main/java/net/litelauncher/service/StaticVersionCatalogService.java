package net.litelauncher.service;

import java.util.Arrays;
import java.util.List;

public final class StaticVersionCatalogService implements VersionCatalogService {
    private final List<VersionOption> versions = Arrays.asList(
            new VersionOption("1.21.11 Vanilla", "Last stable release", false, false),
            new VersionOption("1.21.8 Vanilla", "Stable release", false, false),
            new VersionOption("24w11a Snapshot", "Last experimental snapshot", true, false),
            new VersionOption("24w05a Snapshot", "Experimental snapshot", true, false),
            new VersionOption("1.21.11 Fabric", "Modified release", false, true),
            new VersionOption("1.20.1 Forge", "Modified release", false, true)
    );

    @Override
    public List<VersionOption> getVersions() {
        return versions;
    }
}
