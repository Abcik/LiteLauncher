package net.litelauncher.backend.java;

final class JavaRuntimePackage {

    private final int major;
    private final String name;
    private final String url;
    private final String sha1;
    private final long size;

    JavaRuntimePackage(int major, String name, String url, String sha1, long size) {
        this.major = major;
        this.name = name;
        this.url = url;
        this.sha1 = sha1;
        this.size = size;
    }

    String runtimeId() {
        return "jre-" + major;
    }

    String url() {
        return url;
    }

    String sha1() {
        return sha1;
    }

    long size() {
        return size;
    }

    boolean zipArchive() {
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip");
    }

    String archiveExtension() {
        return zipArchive() ? ".zip" : ".tar.gz";
    }
}
