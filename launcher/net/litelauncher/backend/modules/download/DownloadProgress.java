package net.litelauncher.backend.modules.download;

@FunctionalInterface
public interface DownloadProgress {
    void update(double progress, String action, String details);
}
