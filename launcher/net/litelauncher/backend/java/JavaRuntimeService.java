package net.litelauncher.backend.java;

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
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public final class JavaRuntimeService {

    private static final int FALLBACK_JAVA_MAJOR = 25;

    private final DownloadService downloads;
    private final AdoptiumRuntimeProvider provider = new AdoptiumRuntimeProvider();
    private final JavaRuntimeLocator locator = new JavaRuntimeLocator();

    public JavaRuntimeService(DownloadService downloads) {
        this.downloads = downloads;
    }

    public Path ensureJava(int majorVersion, LaunchProgress progress) throws GameLaunchException {
        int major = majorVersion <= 0 ? OSUtils.currentJavaMajor() : majorVersion;
        try {
            return ensureJavaRuntime(major, progress);
        } catch (GameLaunchException exception) {
            if (major == FALLBACK_JAVA_MAJOR) throw exception;
            LauncherLog.error("Java runtime failed, using fallback Java " + FALLBACK_JAVA_MAJOR + ": jre-" + major, exception);
            return ensureJavaRuntime(FALLBACK_JAVA_MAJOR, progress);
        }
    }

    private Path ensureJavaRuntime(int major, LaunchProgress progress) throws GameLaunchException {
        String runtimeId = provider.runtimeId(major);
        Path root = OSUtils.javaRuntimeDirectory(runtimeId);
        Path tempRoot = root.resolveSibling(runtimeId + ".tmp");
        Path archive = provider.archivePath(runtimeId);

        cleanupRuntimeTemps(runtimeId, tempRoot, archive);

        Path java = locator.findJava(root);
        if (java != null && Files.isRegularFile(java)) {
            LauncherLog.info("Java runtime already installed: " + runtimeId + " -> " + java);
            return java;
        }

        LauncherLog.info("Java runtime missing, installing: " + runtimeId + " -> " + root);
        deleteDirectory(root);
        try {
            downloadRuntime(major, runtimeId, archive, progress);
        } catch (GameLaunchException exception) {
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        }
        return installRuntime(runtimeId, root, tempRoot, archive, progress);
    }

    private void downloadRuntime(int major, String runtimeId, Path archive, LaunchProgress progress) throws GameLaunchException {
        try {
            Files.createDirectories(OSUtils.javaDirectory());
            String url = provider.downloadUrl(major);
            LauncherLog.info("Java download URL: " + url);
            downloads.download(List.of(new DownloadFile(url, archive, "", 0, I18n.text("progress.downloadingJava"))), progress::update);
        } catch (DownloadException exception) {
            LauncherLog.error("Java runtime download failed: " + runtimeId, exception);
            throw new GameLaunchException("Unable to download Java runtime.", exception, InformationMessages.DOWNLOAD_ERROR);
        } catch (GameLaunchException exception) {
            LauncherLog.error("Java runtime download failed: " + runtimeId, exception);
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Java runtime download failed: " + runtimeId, exception);
            throw new GameLaunchException("Unable to download Java runtime.", exception, InformationMessages.DOWNLOAD_ERROR);
        }
    }

    private Path installRuntime(String runtimeId, Path root, Path tempRoot, Path archive, LaunchProgress progress) throws GameLaunchException {
        try {
            progress.update(0.96, I18n.text("progress.installingJava"), runtimeId);
            LauncherLog.info("Extracting Java archive: " + archive + " -> " + tempRoot);
            deleteDirectory(tempRoot);
            extractArchive(archive, tempRoot);

            Path java = locator.findJava(tempRoot);
            if (java == null || !Files.isRegularFile(java))
                throw new GameLaunchException("Unable to extract Java runtime.", InformationMessages.JAVA_ERROR);
            locator.makeExecutable(java);

            deleteDirectory(root);
            moveDirectory(tempRoot, root);
            deleteQuietly(archive);

            java = locator.findJava(root);
            locator.makeExecutable(java);
            if (java == null || !Files.isRegularFile(java))
                throw new GameLaunchException("Unable to extract Java runtime.", InformationMessages.JAVA_ERROR);
            LauncherLog.info("Java runtime installed: " + runtimeId + " -> " + java);
            return java;
        } catch (GameLaunchException exception) {
            LauncherLog.error("Java runtime extraction failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Java runtime extraction failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw new GameLaunchException("Unable to extract Java runtime.", exception, InformationMessages.JAVA_ERROR);
        } finally {
            deleteQuietly(archive);
            deleteQuietly(archive.resolveSibling(archive.getFileName() + ".litelauncher-download"));
        }
    }

    private void extractArchive(Path archive, Path target) throws Exception {
        Files.createDirectories(target);
        if (OSUtils.os().windows()) extractZip(archive, target);
        else extractTarGz(archive, target);
    }

    private void extractZip(Path archive, Path target) throws Exception {
        Path root = target.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                Path out = root.resolve(stripArchiveRoot(entry.getName())).toAbsolutePath().normalize();
                if (!out.startsWith(root) || out.equals(root)) continue;
                if (entry.isDirectory()) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void extractTarGz(Path archive, Path target) throws Exception {
        Path root = target.toAbsolutePath().normalize();
        try (InputStream input = new GZIPInputStream(Files.newInputStream(archive))) {
            byte[] header = new byte[512];
            while (readFully(input, header) == 512) {
                if (emptyBlock(header)) break;
                String name = stripArchiveRoot(tarString(header, 0, 100));
                long mode = tarOctal(header, 100, 8);
                long size = tarOctal(header, 124, 12);
                int type = header[156];
                Path out = root.resolve(name).toAbsolutePath().normalize();
                if (!out.startsWith(root) || out.equals(root)) skip(input, size);
                else if (type == '5') Files.createDirectories(out);
                else if (type == '0' || type == 0) {
                    Files.createDirectories(out.getParent());
                    try (var output = Files.newOutputStream(out)) {
                        copy(input, output, size);
                    }
                    if ((mode & 0b001_001_001) != 0) locator.makeExecutable(out);
                } else skip(input, size);
                skip(input, (512 - (size % 512)) % 512);
            }
        }
    }

    private String stripArchiveRoot(String name) {
        if (name == null) return "";
        name = name.replace('\\', '/');
        int slash = name.indexOf('/');
        return slash < 0 ? "" : name.substring(slash + 1);
    }

    private int readFully(InputStream input, byte[] buffer) throws Exception {
        int offset = 0;
        while (offset < buffer.length) {
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

    private void copy(InputStream input, java.io.OutputStream output, long bytes) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        long left = bytes;
        while (left > 0) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) throw new java.io.EOFException();
            output.write(buffer, 0, read);
            left -= read;
        }
    }

    private void skip(InputStream input, long bytes) throws Exception {
        byte[] buffer = new byte[8192];
        long left = bytes;
        while (left > 0) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) throw new java.io.EOFException();
            left -= read;
        }
    }

    private void moveDirectory(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void cleanupRuntimeTemps(String runtimeId, Path tempRoot, Path archive) {
        deleteDirectory(tempRoot);
        deleteQuietly(archive);
        deleteQuietly(archive.resolveSibling(archive.getFileName() + ".litelauncher-download"));
        deleteQuietly(OSUtils.javaDirectory().resolve(runtimeId + ".zip"));
        deleteQuietly(OSUtils.javaDirectory().resolve(runtimeId + ".tar.gz"));
        deleteQuietly(OSUtils.javaDirectory().resolve(runtimeId + ".zip.litelauncher-download"));
        deleteQuietly(OSUtils.javaDirectory().resolve(runtimeId + ".tar.gz.litelauncher-download"));
    }

    private void deleteDirectory(Path directory) {
        try {
            if (!Files.exists(directory)) return;
            try (var stream = Files.walk(directory)) {
                stream.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
            }
        } catch (Exception ignored) {
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
        }
    }
}
