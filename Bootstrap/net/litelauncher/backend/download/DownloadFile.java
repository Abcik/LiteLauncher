package net.litelauncher.backend.download;

import java.nio.file.Path;

public record DownloadFile(String url, Path path, String checksumAlgorithm, String checksum, long size, String label) {

    public DownloadFile {
        url = url == null ? "" : url;
        checksumAlgorithm = checksumAlgorithm == null ? "" : checksumAlgorithm;
        checksum = checksum == null ? "" : checksum;
        label = label == null ? "" : label;
    }
}
