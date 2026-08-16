package net.litelauncher.backend.java;

import net.litelauncher.backend.platform.LauncherPaths;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.CancellationToken;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.download.DownloadException;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.launch.GameLaunchException;
import net.litelauncher.backend.launch.LaunchProgress;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.i18n.I18n;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public final class JavaRuntimeService {

    private final DownloadService downloads;
    private final JavaRuntimeManifestService manifests = new JavaRuntimeManifestService();
    private final JavaRuntimeLocator locator = new JavaRuntimeLocator();

    public JavaRuntimeService(DownloadService downloads) {
        this.downloads = downloads;
    }

    public Path ensureJava(int majorVersion, LaunchProgress progress, CancellationToken cancellation) throws GameLaunchException {
        checkCancelled(cancellation);
        int major = majorVersion <= 0 ? OSUtils.currentJavaMajor() : majorVersion;
        return ensureJavaRuntime(major, progress, cancellation);
    }

    private Path ensureJavaRuntime(int major, LaunchProgress progress, CancellationToken cancellation) throws GameLaunchException {
        checkCancelled(cancellation);
        String runtimeId = manifests.runtimeId(major);
        Path root = LauncherPaths.javaRuntimeDirectory(runtimeId);
        Path tempRoot = root.resolveSibling(runtimeId + ".tmp");

        cleanupRuntimeTemps(runtimeId, tempRoot, null);

        Path java = locator.findJava(root);
        if (java != null && Files.isRegularFile(java)) {
            locator.makeExecutable(java);
            LauncherLog.info("Java runtime already installed: " + runtimeId + " -> " + java);
            return java;
        }

        JavaRuntimePackage runtime;
        try {
            checkCancelled(cancellation);
            LauncherLog.info("Loading Java runtime manifest: " + JavaRuntimeManifestService.MANIFEST_URL);
            runtime = manifests.resolve(major);
            checkCancelled(cancellation);
        } catch (CancellationException exception) {
            throw exception;
        } catch (Exception exception) {
            checkCancelled(cancellation);
            LauncherLog.error("Java runtime manifest failed: " + runtimeId, exception);
            throw new GameLaunchException(exception.getMessage(), exception, InformationMessages.JAVA_ERROR);
        }

        Path archive = LauncherPaths.javaDirectory().resolve(runtime.runtimeId() + runtime.archiveExtension());
        cleanupRuntimeTemps(runtimeId, tempRoot, archive);

        LauncherLog.info("Java runtime missing, installing: " + runtimeId + " -> " + root);
        deleteDirectory(root);
        try {
            downloadRuntime(runtime, archive, progress, cancellation);
        } catch (CancellationException | GameLaunchException exception) {
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        }
        return installRuntime(runtime, root, tempRoot, archive, progress, cancellation);
    }

    private void downloadRuntime(JavaRuntimePackage runtime, Path archive, LaunchProgress progress, CancellationToken cancellation) throws GameLaunchException {
        String runtimeId = runtime.runtimeId();
        try {
            checkCancelled(cancellation);
            Files.createDirectories(LauncherPaths.javaDirectory());
            LauncherLog.info("Java download URL: " + runtime.url());
            downloads.download(List.of(new DownloadFile(
                    runtime.url(), archive, runtime.sha1(), runtime.size(), I18n.text("progress.downloadingJava"))),
                    progress::update, cancellation);
            checkCancelled(cancellation);
        } catch (CancellationException exception) {
            LauncherLog.info("Java runtime download cancelled: " + runtimeId);
            throw exception;
        } catch (DownloadException exception) {
            LauncherLog.error("Java runtime download failed: " + runtimeId, exception);
            throw new GameLaunchException("Unable to download Java runtime.", exception, InformationMessages.DOWNLOAD_ERROR);
        } catch (Exception exception) {
            LauncherLog.error("Java runtime download failed: " + runtimeId, exception);
            throw new GameLaunchException("Unable to download Java runtime.", exception, InformationMessages.DOWNLOAD_ERROR);
        }
    }

    private Path installRuntime(JavaRuntimePackage runtime, Path root, Path tempRoot, Path archive, LaunchProgress progress, CancellationToken cancellation) throws GameLaunchException {
        String runtimeId = runtime.runtimeId();
        try {
            checkCancelled(cancellation);
            progress.update(0.96, I18n.text("progress.installingJava"), runtimeId);
            LauncherLog.info("Extracting Java archive: " + archive + " -> " + tempRoot);
            deleteDirectory(tempRoot);
            extractArchive(runtime, archive, tempRoot, cancellation);

            checkCancelled(cancellation);
            Path java = locator.findJava(tempRoot);
            if (java == null || !Files.isRegularFile(java))
                throw new GameLaunchException("Unable to extract Java runtime.", InformationMessages.JAVA_ERROR);
            locator.makeExecutable(java);

            deleteDirectory(root);
            checkCancelled(cancellation);
            moveDirectory(tempRoot, root);
            BackendUtils.deleteQuietly(archive);

            checkCancelled(cancellation);
            java = locator.findJava(root);
            if (java == null || !Files.isRegularFile(java))
                throw new GameLaunchException("Unable to extract Java runtime.", InformationMessages.JAVA_ERROR);
            locator.makeExecutable(java);
            LauncherLog.info("Java runtime installed: " + runtimeId + " -> " + java);
            return java;
        } catch (CancellationException exception) {
            LauncherLog.info("Java runtime extraction cancelled: " + runtimeId);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        } catch (GameLaunchException exception) {
            LauncherLog.error("Java runtime extraction failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Java runtime extraction failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw new GameLaunchException("Unable to extract Java runtime.", exception, InformationMessages.JAVA_ERROR);
        } finally {
            BackendUtils.deleteQuietly(archive);
            BackendUtils.deleteQuietly(archive.resolveSibling(archive.getFileName() + ".litelauncher-download"));
        }
    }

    private void extractArchive(JavaRuntimePackage runtime, Path archive, Path target, CancellationToken cancellation) throws Exception {
        checkCancelled(cancellation);
        Files.createDirectories(target);
        if (runtime.zipArchive()) extractZip(archive, target, cancellation);
        else extractTarGz(archive, target, cancellation);
    }

    private void extractZip(Path archive, Path target, CancellationToken cancellation) throws Exception {
        Path root = target.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                checkCancelled(cancellation);
                Path out = root.resolve(stripArchiveRoot(entry.getName())).toAbsolutePath().normalize();
                if (!out.startsWith(root) || out.equals(root)) continue;
                if (entry.isDirectory()) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    try (var output = Files.newOutputStream(out)) {
                        copyUntilEntryEnd(zip, output, cancellation);
                    }
                }
            }
        }
    }

    private void extractTarGz(Path archive, Path target, CancellationToken cancellation) throws Exception {
        Path root = target.toAbsolutePath().normalize();
        try (InputStream input = new GZIPInputStream(Files.newInputStream(archive))) {
            byte[] header = new byte[512];
            while (readFully(input, header, cancellation) == 512) {
                checkCancelled(cancellation);
                if (emptyBlock(header)) break;
                String name = stripArchiveRoot(tarString(header, 0, 100));
                long mode = tarOctal(header, 100, 8);
                long size = tarOctal(header, 124, 12);
                int type = header[156];
                Path out = root.resolve(name).toAbsolutePath().normalize();
                if (!out.startsWith(root) || out.equals(root)) skip(input, size, cancellation);
                else if (type == '5') Files.createDirectories(out);
                else if (type == '0' || type == 0) {
                    Files.createDirectories(out.getParent());
                    try (var output = Files.newOutputStream(out)) {
                        copy(input, output, size, cancellation);
                    }
                    if ((mode & 0b001_001_001) != 0) locator.makeExecutable(out);
                } else skip(input, size, cancellation);
                skip(input, (512 - (size % 512)) % 512, cancellation);
            }
        }
    }

    private void copyUntilEntryEnd(InputStream input, java.io.OutputStream output, CancellationToken cancellation) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            checkCancelled(cancellation);
            if (read > 0) output.write(buffer, 0, read);
        }
    }

    private String stripArchiveRoot(String name) {
        if (name == null) return "";
        name = name.replace('\\', '/');
        int slash = name.indexOf('/');
        return slash < 0 ? "" : name.substring(slash + 1);
    }

    private int readFully(InputStream input, byte[] buffer, CancellationToken cancellation) throws Exception {
        int offset = 0;
        while (offset < buffer.length) {
            checkCancelled(cancellation);
            int read = input.read(buffer, offset, buffer.length - offset);
            if (read < 0) break;
            offset += read;
        }
        return offset;
    }

    private boolean emptyBlock(byte[] block) {
        for (byte b : block) if (b != 0) return false;
        return true;
    }

    private String tarString(byte[] data, int offset, int length) {
        int end = offset;
        while (end < offset + length && data[end] != 0) end++;
        return new String(data, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private long tarOctal(byte[] data, int offset, int length) {
        String text = tarString(data, offset, length).trim();
        if (text.isEmpty()) return 0L;
        return Long.parseLong(text, 8);
    }

    private void copy(InputStream input, java.io.OutputStream output, long bytes, CancellationToken cancellation) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        long left = bytes;
        while (left > 0) {
            checkCancelled(cancellation);
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) throw new java.io.EOFException();
            output.write(buffer, 0, read);
            left -= read;
        }
    }

    private void skip(InputStream input, long bytes, CancellationToken cancellation) throws Exception {
        byte[] buffer = new byte[8192];
        long left = bytes;
        while (left > 0) {
            checkCancelled(cancellation);
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) throw new java.io.EOFException();
            left -= read;
        }
    }

    private void checkCancelled(CancellationToken cancellation) {
        if (cancellation != null) cancellation.throwIfCancelled();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Java runtime installation cancelled.");
    }

    private void moveDirectory(Path source, Path target) throws Exception {
        BackendUtils.moveReplace(source, target);
    }

    private void cleanupRuntimeTemps(String runtimeId, Path tempRoot, Path archive) {
        deleteDirectory(tempRoot);
        if (archive != null) {
            BackendUtils.deleteQuietly(archive);
            BackendUtils.deleteQuietly(archive.resolveSibling(archive.getFileName() + ".litelauncher-download"));
        }
        BackendUtils.deleteQuietly(LauncherPaths.javaDirectory().resolve(runtimeId + ".zip"));
        BackendUtils.deleteQuietly(LauncherPaths.javaDirectory().resolve(runtimeId + ".tar.gz"));
        BackendUtils.deleteQuietly(LauncherPaths.javaDirectory().resolve(runtimeId + ".zip.litelauncher-download"));
        BackendUtils.deleteQuietly(LauncherPaths.javaDirectory().resolve(runtimeId + ".tar.gz.litelauncher-download"));
    }

    private void deleteDirectory(Path directory) {
        BackendUtils.deleteTreeQuietly(directory);
    }

}
