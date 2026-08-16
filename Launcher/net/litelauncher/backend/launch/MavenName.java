package net.litelauncher.backend.launch;

record MavenName(String group, String artifact, String version, String classifier, String extension) {

    static MavenName parse(String name) {
        String[] parts = name == null ? new String[0] : name.split(":");
        if (parts.length < 3) return new MavenName("", "", "", "", "");

        String version = parts[2];
        String classifier = parts.length > 3 ? parts[3] : "";
        String extension = "jar";

        int versionAt = version.indexOf('@');
        if (versionAt >= 0) {
            extension = version.substring(versionAt + 1);
            version = version.substring(0, versionAt);
        }

        int classifierAt = classifier.indexOf('@');
        if (classifierAt >= 0) {
            extension = classifier.substring(classifierAt + 1);
            classifier = classifier.substring(0, classifierAt);
        }

        return new MavenName(parts[0], parts[1], version, classifier, extension.isBlank() ? "jar" : extension);
    }

    boolean valid() {
        return !group.isBlank() && !artifact.isBlank() && !version.isBlank();
    }

    String moduleKey() {
        return classifier.isBlank() ? group + ":" + artifact : group + ":" + artifact + ":" + classifier;
    }

    String path() {
        String suffix = classifier.isBlank() ? "" : "-" + classifier;
        return group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version + suffix + "." + extension;
    }
}
