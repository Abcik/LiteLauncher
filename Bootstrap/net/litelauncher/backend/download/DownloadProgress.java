package net.litelauncher.backend.download;

@FunctionalInterface
public interface DownloadProgress {
    void update(double progress, String action, String details);
}
