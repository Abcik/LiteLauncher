package net.litelauncher.backend.java;

import net.litelauncher.backend.download.DownloadException;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.bootstrap.BootstrapException;
import net.litelauncher.ui.TaskProgress;
import net.litelauncher.backend.BootstrapLog;

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

    private final BootstrapLog log;
    private final DownloadService downloads;
    private final JavaRuntimeManifestService manifests = new JavaRuntimeManifestService();
    private final JavaRuntimeLocator locator = new JavaRuntimeLocator();

    public JavaRuntimeService(DownloadService downloads, BootstrapLog log) {
        this.downloads = downloads;
        this.log = log;
    }


    public Path findJava(int majorVersion) {
        int major = majorVersion <= 0 ? OSUtils.currentJavaMajor() : majorVersion;
        Path java = locator.findJava(OSUtils.javaRuntimeDirectory(manifests.runtimeId(major)));
        if (java != null && Files.isRegularFile(java)) locator.makeExecutable(java);
        return java;
    }

    public Path ensureJava(int majorVersion, TaskProgress progress) throws BootstrapException {
        int major = majorVersion <= 0 ? OSUtils.currentJavaMajor() : majorVersion;
        return ensureJavaRuntime(major, progress);
    }

    private Path ensureJavaRuntime(int major, TaskProgress progress) throws BootstrapException {
        String runtimeId = manifests.runtimeId(major);
        Path root = OSUtils.javaRuntimeDirectory(runtimeId);
        Path tempRoot = root.resolveSibling(runtimeId + ".tmp");

        cleanupRuntimeTemps(runtimeId, tempRoot, null);

        Path java = locator.findJava(root);
        if (java != null && Files.isRegularFile(java)) {
            locator.makeExecutable(java);
            log.info("Java runtime already installed: " + runtimeId + " -> " + java);
            return java;
        }

        JavaRuntimePackage runtime;
        try {
            log.info("Loading Java runtime manifest: " + JavaRuntimeManifestService.MANIFEST_URL);
            runtime = manifests.resolve(major);
        } catch (Exception exception) {
            log.error("Java runtime manifest failed: " + runtimeId, exception);
            throw new BootstrapException(exception.getMessage(), exception);
        }

        Path archive = OSUtils.javaDirectory().resolve(runtime.runtimeId() + runtime.archiveExtension());
        cleanupRuntimeTemps(runtimeId, tempRoot, archive);

        log.info("Java runtime missing, installing: " + runtimeId + " -> " + root);
        deleteDirectory(root);
        try {
            downloadRuntime(runtime, archive, progress);
        } catch (BootstrapException exception) {
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        }
        return installRuntime(runtime, root, tempRoot, archive, progress);
    }

    private void downloadRuntime(JavaRuntimePackage runtime, Path archive, TaskProgress progress) throws BootstrapException {
        String runtimeId = runtime.runtimeId();
        try {
            Files.createDirectories(OSUtils.javaDirectory());
            log.info("Java download URL: " + runtime.url());
            downloads.download(List.of(new DownloadFile(
                            runtime.url(), archive, "SHA-1", runtime.sha1(), runtime.size(), "Downloading Java")),
                    (val, action, details) -> report(progress, 0.76 + 0.14 * val, action));
        } catch (Exception exception) {
            log.error("Java runtime download failed: " + runtimeId, exception);
            throw new BootstrapException("Download error.", exception);
        }
    }

    private Path installRuntime(JavaRuntimePackage runtime, Path root, Path tempRoot, Path archive, TaskProgress progress) throws BootstrapException {
        String runtimeId = runtime.runtimeId();
        try {
            report(progress, 0.93, "Installing Java...");
            log.info("Extracting Java archive: " + archive + " -> " + tempRoot);
            deleteDirectory(tempRoot);
            extractArchive(runtime, archive, tempRoot);

            Path java = locator.findJava(tempRoot);
            if (java == null || !Files.isRegularFile(java))
                throw new BootstrapException("Java error.");
            locator.makeExecutable(java);

            deleteDirectory(root);
            moveDirectory(tempRoot, root);
            deleteQuietly(archive);

            java = locator.findJava(root);
            if (java == null || !Files.isRegularFile(java))
                throw new BootstrapException("Java error.");
            locator.makeExecutable(java);
            log.info("Java runtime installed: " + runtimeId + " -> " + java);
            return java;
        } catch (BootstrapException exception) {
            log.error("Java runtime extraction failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        } catch (Exception exception) {
            log.error("Java runtime extraction failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw new BootstrapException("Java error.", exception);
        } finally {
            deleteQuietly(archive);
            deleteQuietly(archive.resolveSibling(archive.getFileName() + ".litelauncher-download"));
        }
    }

    private void extractArchive(JavaRuntimePackage runtime, Path archive, Path target) throws Exception {
        Files.createDirectories(target);
        if (runtime.zipArchive()) extractZip(archive, target);
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
        if (archive != null) {
            deleteQuietly(archive);
            deleteQuietly(archive.resolveSibling(archive.getFileName() + ".litelauncher-download"));
        }
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

    private void report(TaskProgress progress, double value, String details) {
        if (progress != null) progress.update(Math.max(0.0, Math.min(1.0, value)), details);
    }
}
