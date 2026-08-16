package net.litelauncher.backend.java;

import java.util.Locale;

record JavaRuntimePackage(int major, String name, String url, String sha1, long size) {

    String runtimeId() {
        return "jre-" + major;
    }

    boolean zipArchive() {
        return name.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    String archiveExtension() {
        return zipArchive() ? ".zip" : ".tar.gz";
    }
}
