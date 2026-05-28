package net.litelauncher.backend.modules.java;

import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.modules.download.DownloadFile;
import net.litelauncher.backend.modules.download.DownloadService;
import net.litelauncher.backend.modules.launch.GameLaunchException;
import net.litelauncher.backend.modules.launch.LaunchProgress;
import net.litelauncher.backend.platform.OSUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public final class JavaRuntimeService {

    private final DownloadService downloads;

    public JavaRuntimeService(DownloadService downloads) {
        this.downloads = downloads;
    }

    public Path ensureJava(int majorVersion, LaunchProgress progress) throws GameLaunchException {
        int major = majorVersion <= 0 ? currentJavaMajor() : majorVersion;
        String runtimeId = "jre-" + major;
        Path root = OSUtils.javaRuntimeDirectory(runtimeId);
        Path tempRoot = root.resolveSibling(runtimeId + ".tmp");
        Path archive = OSUtils.javaDirectory().resolve(runtimeId + archiveExtension());

        cleanupRuntimeTemps(runtimeId, tempRoot, archive);

        Path java = findJava(root);
        if (java != null && Files.isRegularFile(java)) {
            LauncherLog.info("Java runtime already installed: " + runtimeId + " -> " + java);
            return java;
        }

        LauncherLog.info("Java runtime missing, installing: " + runtimeId + " -> " + root);
        deleteDirectory(root);

        try {
            Files.createDirectories(OSUtils.javaDirectory());
            String url = adoptiumUrl(major);
            LauncherLog.info("Java download URL: " + url);
            downloads.download(List.of(new DownloadFile(url, archive, "", 0, "Downloading Java")), progress::update);
        } catch (GameLaunchException exception) {
            LauncherLog.error("Java runtime download failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Java runtime download failed: " + runtimeId, exception);
            cleanupRuntimeTemps(runtimeId, tempRoot, archive);
            throw new GameLaunchException("Unable to download Java runtime.", exception, InformationMessages.DOWNLOAD_ERROR);
        }

        try {
            progress.update(0.96, "Installing Java...", runtimeId);
            LauncherLog.info("Extracting Java archive: " + archive + " -> " + tempRoot);
            deleteDirectory(tempRoot);
            extractRuntime(archive, tempRoot);

            java = findJava(tempRoot);
            if (java == null || !Files.isRegularFile(java)) throw new GameLaunchException("Unable to extract Java runtime.", InformationMessages.JAVA_ERROR);
            makeExecutable(java);

            deleteDirectory(root);
            moveDirectory(tempRoot, root);
            deleteQuietly(archive);

            java = findJava(root);
            makeExecutable(java);
            if (java == null || !Files.isRegularFile(java)) throw new GameLaunchException("Unable to extract Java runtime.", InformationMessages.JAVA_ERROR);
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

    private String adoptiumUrl(int major) throws GameLaunchException {
        return "https://api.adoptium.net/v3/binary/latest/" + major + "/ga/" + adoptiumOs() + "/" + adoptiumArch() + "/jre/hotspot/normal/eclipse?project=jdk";
    }

    private Path javaExecutable(Path root) {
        return root.resolve("bin").resolve(isWindows() ? "java.exe" : "java");
    }

    private Path findJava(Path root) {
        Path direct = javaExecutable(root);
        if (Files.isRegularFile(direct)) return direct;
        try (var stream = Files.find(root, 8, (path, attrs) -> {
            if (!attrs.isRegularFile() || path.getParent() == null || path.getFileName() == null) return false;
            String name = path.getFileName().toString();
            return name.equals(isWindows() ? "java.exe" : "java") && "bin".equals(path.getParent().getFileName().toString());
        })) {
            return stream.findFirst().orElse(direct);
        } catch (Exception ignored) {
            return direct;
        }
    }

    private void extractRuntime(Path archive, Path target) throws GameLaunchException {
        try {
            Files.createDirectories(target);
            if (isWindows()) extractZip(archive, target);
            else extractTarGz(archive, target);
        } catch (GameLaunchException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to extract Java runtime.", exception, InformationMessages.JAVA_ERROR);
        }
    }

    private void extractZip(Path archive, Path target) throws Exception {
        Path root = target.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                Path out = root.resolve(stripRoot(entry.getName())).toAbsolutePath().normalize();
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
        try (InputStream input = new GZIPInputStream(Files.newInputStream(archive))) {
            byte[] header = new byte[512];
            while (readFully(input, header) == 512) {
                if (emptyBlock(header)) break;
                String name = stripRoot(tarString(header, 0, 100));
                long mode = tarSize(header, 100, 8);
                long size = tarSize(header, 124, 12);
                int type = header[156];
                Path root = target.toAbsolutePath().normalize();
                Path out = root.resolve(name).toAbsolutePath().normalize();
                if (!out.startsWith(root) || out.equals(root)) {
                    skip(input, size);
                } else if (type == '5') {
                    Files.createDirectories(out);
                } else if (type == '0' || type == 0) {
                    Files.createDirectories(out.getParent());
                    try (var output = Files.newOutputStream(out)) {
                        copy(input, output, size);
                    }
                    applyTarMode(out, mode);
                } else {
                    skip(input, size);
                }
                long padding = (512 - (size % 512)) % 512;
                skip(input, padding);
            }
        }
    }

    private void applyTarMode(Path file, long mode) {
        if ((mode & 0111) != 0) makeExecutable(file);
    }

    private String stripRoot(String name) {
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

    private long tarSize(byte[] data, int offset, int length) {
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

    private void makeExecutable(Path file) {
        try {
            if (file == null || isWindows() || !Files.exists(file)) return;
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
            );
            Files.setPosixFilePermissions(file, permissions);
        } catch (Exception ignored) {
        }
    }

    private String adoptiumOs() throws GameLaunchException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "mac";
        if (os.contains("linux")) return "linux";
        throw new GameLaunchException("Unable to download Java runtime.", InformationMessages.JAVA_ERROR);
    }

    private String adoptiumArch() throws GameLaunchException {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
        if (arch.contains("amd64") || arch.contains("x86_64") || arch.equals("x64")) return "x64";
        throw new GameLaunchException("Unable to download Java runtime.", InformationMessages.JAVA_ERROR);
    }

    private String archiveExtension() {
        return isWindows() ? ".zip" : ".tar.gz";
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private int currentJavaMajor() {
        String version = System.getProperty("java.version", "17");
        try {
            if (version.startsWith("1.")) return Integer.parseInt(version.substring(2, 3));
            int dot = version.indexOf('.');
            String major = dot < 0 ? version : version.substring(0, dot);
            return Math.max(1, Integer.parseInt(major));
        } catch (Exception ignored) {
            return 17;
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
