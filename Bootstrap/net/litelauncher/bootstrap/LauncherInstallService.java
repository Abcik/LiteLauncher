package net.litelauncher.bootstrap;

import net.litelauncher.backend.BootstrapLog;
import net.litelauncher.backend.download.DownloadException;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.TaskProgress;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class LauncherInstallService {

    private final DownloadService downloads;
    private final BootstrapLog log;

    LauncherInstallService(DownloadService downloads, BootstrapLog log) {
        this.downloads = downloads;
        this.log = log;
    }

    Path launcherPath(LauncherManifest manifest) {
        return OSUtils.launcherDirectory().resolve(launcherFileName(manifest));
    }

    boolean isLauncherValid(Path launcher, LauncherManifest manifest) throws BootstrapException {
        try {
            return downloads.isValid(downloadFile(manifest, launcher, "Checking launcher"));
        } catch (DownloadException exception) {
            throw new BootstrapException("File error.", exception);
        }
    }

    Path ensureLauncher(LauncherManifest manifest, TaskProgress progress) throws BootstrapException {
        try {
            Files.createDirectories(OSUtils.launcherDirectory());
        } catch (Exception exception) {
            throw new BootstrapException("File error.", exception);
        }

        Path target = launcherPath(manifest);
        if (isLauncherValid(target, manifest)) {
            log.info("Launcher already valid: " + target);
            cleanupOldLaunchers(target);
            return target;
        }

        log.info("Launcher missing or invalid. Downloading: " + target);
        try {
            downloads.download(List.of(downloadFile(manifest, target, "Downloading launcher")),
                    (val, action, details) -> report(progress, 0.24 + 0.44 * val, action));
            if (!isLauncherValid(target, manifest)) throw new BootstrapException("Download error.");
            cleanupOldLaunchers(target);
            return target;
        } catch (BootstrapException exception) {
            OSUtils.deleteQuietly(DownloadService.tempPath(target));
            throw exception;
        } catch (Exception exception) {
            OSUtils.deleteQuietly(DownloadService.tempPath(target));
            throw new BootstrapException("Download error.", exception);
        }
    }

    void cleanupOldLaunchers(Path keep) {
        if (!Files.isDirectory(OSUtils.launcherDirectory())) return;
        try (var stream = Files.list(OSUtils.launcherDirectory())) {
            stream.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.equals(keep))
                    .forEach(path -> {
                        OSUtils.deleteQuietly(path);
                        log.info("Removed old launcher: " + path);
                    });
        } catch (Exception exception) {
            log.error("Unable to cleanup old launcher files.", exception);
        }
    }

    private DownloadFile downloadFile(LauncherManifest manifest, Path target, String label) {
        return new DownloadFile(manifest.file(), target, manifest.checksumAlgorithm(), manifest.checksum(), manifest.size(), label);
    }

    private String launcherFileName(LauncherManifest manifest) {
        try {
            String path = URI.create(manifest.file()).getPath();
            if (path != null) {
                int slash = path.lastIndexOf('/');
                String name = slash < 0 ? path : path.substring(slash + 1);
                name = sanitize(name);
                if (!name.isBlank()) return name;
            }
        } catch (Exception ignored) {
        }
        return "LiteLauncher-v" + sanitize(manifest.version()) + ".jar";
    }

    private String sanitize(String name) {
        if (name == null) return "";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void report(TaskProgress progress, double value, String details) {
        if (progress != null) progress.update(Math.max(0.0, Math.min(1.0, value)), details);
    }
}
