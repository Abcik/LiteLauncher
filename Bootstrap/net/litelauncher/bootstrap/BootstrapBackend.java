package net.litelauncher.bootstrap;

import net.litelauncher.backend.BootstrapLog;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.java.JavaRuntimeService;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.TaskProgress;

import java.nio.file.Files;
import java.nio.file.Path;

public final class BootstrapBackend {

    private final BootstrapLog log;
    private final LauncherManifestService manifests;
    private final LauncherInstallService launchers;
    private final JavaRuntimeService javaRuntimes;

    private BootstrapBackend(BootstrapLog log) {
        this.log = log;
        DownloadService downloads = new DownloadService(log);
        this.manifests = new LauncherManifestService(log);
        this.launchers = new LauncherInstallService(downloads, log);
        this.javaRuntimes = new JavaRuntimeService(downloads, log);
    }

    public static void updateAndLaunch(TaskProgress progress, BootstrapLog log) throws Exception {
        new BootstrapBackend(log).updateAndLaunch(progress);
    }

    private void updateAndLaunch(TaskProgress progress) throws Exception {
        cleanupStartupTemps();

        ManifestLoad manifestLoad = manifests.loadManifest();
        LauncherManifest manifest = manifestLoad.manifest();
        log.info("Launcher manifest: version=" + manifest.version()
                + ", file=" + manifest.file()
                + ", checksum=" + manifest.checksumAlgorithm() + ":" + manifest.checksum()
                + ", javaMajor=" + manifest.javaMajor());

        Path launcher = launchers.launcherPath(manifest);
        boolean launcherReady = launchers.isLauncherValid(launcher, manifest);

        Path java = javaRuntimes.findJava(manifest.javaMajor());
        boolean javaReady = java != null && Files.isRegularFile(java);

        if (launcherReady && javaReady) {
            log.info("Launcher and Java runtime are already valid. Starting without bootstrap window.");
            if (manifestLoad.cached()) log.info("Network manifest was unavailable; cached manifest was used for startup.");
            launch(java, launcher);
            return;
        }

        report(progress, 0.04, "Checking files...");

        if (!launcherReady) {
            report(progress, 0.20, "Checking launcher...");
            launcher = launchers.ensureLauncher(manifest, progress);
        } else {
            log.info("Launcher already valid: " + launcher);
            launchers.cleanupOldLaunchers(launcher);
        }

        if (!javaReady) {
            report(progress, 0.74, "Checking Java...");
            java = javaRuntimes.ensureJava(manifest.javaMajor(), progress);
        } else {
            log.info("Java runtime already installed: " + java);
        }

        report(progress, 0.98, "Starting launcher...");
        launch(java, launcher);
        report(progress, 1.0, "Done. 100%");
    }

    private void launch(Path java, Path launcher) throws BootstrapException {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    java.toString(),
                    "-Xms16m",
                    "-Xmx128m",
                    "-XX:+UseSerialGC",
                    "-jar",
                    launcher.toString()
            );

            builder.directory(OSUtils.launcherDirectory().toFile());
            builder.start();
            log.info("Launcher process started: " + launcher);
        } catch (Exception exception) {
            throw new BootstrapException("Launch error.", exception);
        }
    }

    private void cleanupStartupTemps() {
        try {
            cleanupTempFiles(OSUtils.bootstrapDirectory());
            cleanupTempFiles(OSUtils.launcherDirectory());
        } catch (Exception exception) {
            log.error("Unable to cleanup bootstrap temp files.", exception);
        }
    }

    private void cleanupTempFiles(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) return;
        try (var stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(DownloadService.TEMP_SUFFIX)).forEach(OSUtils::deleteQuietly);
        }
    }

    private void report(TaskProgress progress, double value, String details) {
        if (progress != null) progress.update(Math.max(0.0, Math.min(1.0, value)), details);
    }
}
