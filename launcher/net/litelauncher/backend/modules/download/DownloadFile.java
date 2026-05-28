package net.litelauncher.backend.modules.download;

import java.nio.file.Path;

public record DownloadFile(String url, Path path, String sha1, long size, String label) {

    public DownloadFile {
        url = url == null ? "" : url;
        sha1 = sha1 == null ? "" : sha1;
        label = label == null ? "" : label;
    }
}
