package net.litelauncher.backend.platform;

import java.util.Locale;

public enum OperatingSystem {
    WINDOWS("windows"),
    MACOS("mac"),
    LINUX("linux"),
    UNKNOWN("");

    private static final OperatingSystem CURRENT = detect(System.getProperty("os.name", ""));

    private final String javaManifestName;

    OperatingSystem(String javaManifestName) {
        this.javaManifestName = javaManifestName;
    }

    public static OperatingSystem current() {
        return CURRENT;
    }

    public boolean windows() {
        return this == WINDOWS;
    }

    public boolean macos() {
        return this == MACOS;
    }

    public boolean linux() {
        return this == LINUX;
    }

    public String javaManifestName() {
        return javaManifestName;
    }

    private static OperatingSystem detect(String osName) {
        String value = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (value.contains("win")) return WINDOWS;
        if (value.contains("mac")) return MACOS;
        if (value.contains("linux")) return LINUX;
        return UNKNOWN;
    }
}
