package net.litelauncher.backend.download;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public record DownloadFile(String url, Path path, String sha1, long size, String label,
                           Map<String, String> headers) {

    public DownloadFile(String url, Path path, String sha1, long size, String label) {
        this(url, path, sha1, size, label, Map.of());
    }

    public DownloadFile {
        url = url == null ? "" : url;
        sha1 = sha1 == null ? "" : sha1;
        label = label == null ? "" : label;
        headers = sanitizeHeaders(headers);
    }

    private static Map<String, String> sanitizeHeaders(Map<String, String> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((name, value) -> {
            if (name == null || value == null) return;
            String cleanName = name.trim();
            String cleanValue = value.trim();
            if (cleanName.isEmpty() || cleanValue.isEmpty()) return;
            if (cleanName.indexOf('\r') >= 0 || cleanName.indexOf('\n') >= 0) return;
            if (cleanValue.indexOf('\r') >= 0 || cleanValue.indexOf('\n') >= 0) return;
            result.put(cleanName, cleanValue);
        });
        return Map.copyOf(result);
    }
}
