package net.litelauncher.backend.java;

import java.util.Locale;

record JavaRuntimePackage(String runtimeId, String name, String url, String sha1, long size) {

    boolean zipArchive() {
        return name.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    String archiveExtension() {
        return zipArchive() ? ".zip" : ".tar.gz";
    }
}
