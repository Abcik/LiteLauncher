package net.litelauncher.bootstrap;

import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.TaskProgress;
import net.litelauncher.ui.UtilityLog;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public final class BootstrapBackend {

    private static final String PUBLIC_KEY = "MCowBQYDK2VwAyEA3Paqfi8Xb+fEZBAxGy1LjMH4YI3SlPW3rKT6kZH7APA=";
    private static final String VERSION_MANIFEST_URL = "https://litelauncher.net/api/v1/launcher/version_manifest.json";
    private static final String TEMP_SUFFIX = ".litelauncher-download";
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final Duration MANIFEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(MANIFEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static void updateAndLaunch(TaskProgress progress, UtilityLog log) throws Exception {
        cleanupStartupTemps(log);

        ManifestLoad manifestLoad = loadManifest(log);
        LauncherManifest manifest = manifestLoad.manifest;
        log.info("Launcher manifest: version=" + manifest.version + ", file=" + manifest.file + ", javaMajor=" + manifest.javaMajor);

        Path launcher = launcherPath(manifest);
        boolean launcherReady = isValid(launcher, manifest.size, manifest.checksumAlgorithm(), manifest.checksum());

        Path java = findJava(OSUtils.javaRuntimeDirectory(runtimeId(manifest.javaMajor)));
        boolean javaReady = Files.isRegularFile(java);
        if (javaReady) OSUtils.makeExecutable(java);

        if (launcherReady && javaReady) {
            log.info("Launcher and Java runtime are already valid. Starting without bootstrap window.");
            if (manifestLoad.cached) log.info("Network manifest was unavailable; cached manifest was used for startup.");
            launch(java, launcher, log);
            return;
        }

        report(progress, 0.04, "Checking files...");

        if (!launcherReady) {
            report(progress, 0.20, "Checking launcher...");
            launcher = ensureLauncher(manifest, progress, log);
        } else {
            log.info("Launcher already valid: " + launcher);
            cleanupOldLaunchers(launcher, log);
        }

        if (!javaReady) {
            report(progress, 0.74, "Checking Java...");
            java = ensureJava(manifest.javaMajor, progress, log);
        } else {
            log.info("Java runtime already installed: " + java);
        }

        report(progress, 0.98, "Starting launcher...");
        launch(java, launcher, log);
        report(progress, 1.0, "Done. 100%");
    }

    private static ManifestLoad loadManifest(UtilityLog log) throws BootstrapException {
        Path manifest = OSUtils.bootstrapDirectory().resolve("version_manifest.json");
        Path temp = tempPath(manifest);

        try {
            Files.createDirectories(manifest.getParent());
            Files.deleteIfExists(temp);

            downloadRaw(VERSION_MANIFEST_URL, temp, null, MANIFEST_TIMEOUT);

            String rawManifest = Files.readString(temp, StandardCharsets.UTF_8);
            LauncherManifest parsed = LauncherManifest.parse(rawManifest);

            if (!parsed.verify(PUBLIC_KEY)) {
                log.info("Manifest signature check failed! Trying cached manifest.");
                OSUtils.deleteQuietly(temp);

                if (!Files.isRegularFile(manifest)) throw new BootstrapException("Manifest signature check failed and cached manifest is missing.");

                String cachedRawManifest = Files.readString(manifest, StandardCharsets.UTF_8);
                return new ManifestLoad(LauncherManifest.parse(cachedRawManifest), true);
            }

            move(temp, manifest);

            log.info("Manifest downloaded and cached: " + manifest);
            return new ManifestLoad(parsed, false);

        } catch (BootstrapException exception) {
            OSUtils.deleteQuietly(temp);
            throw exception;

        } catch (Exception exception) {
            OSUtils.deleteQuietly(temp);

            log.error("Unable to download or parse manifest. Trying cached manifest.", exception);

            if (!Files.isRegularFile(manifest)) {
                throw new BootstrapException("Network error and cached manifest is missing.", exception);
            }

            try {
                log.info("Using cached manifest: " + manifest);

                String cachedRawManifest = Files.readString(manifest, StandardCharsets.UTF_8);
                return new ManifestLoad(LauncherManifest.parse(cachedRawManifest), true);

            } catch (Exception cachedException) {
                throw new BootstrapException("Manifest error.", cachedException);
            }
        }
    }

    private static Path ensureLauncher(LauncherManifest manifest, TaskProgress progress, UtilityLog log) throws BootstrapException {
        try {
            Files.createDirectories(OSUtils.launcherDirectory());
        } catch (IOException exception) {
            throw new BootstrapException("File error.", exception);
        }

        Path target = launcherPath(manifest);
        if (isValid(target, manifest.size, manifest.checksumAlgorithm(), manifest.checksum())) {
            log.info("Launcher already valid: " + target);
            cleanupOldLaunchers(target, log);
            return target;
        }

        log.info("Launcher missing or invalid. Downloading: " + target);
        try {
            downloadFile(manifest.file, target, manifest.size, manifest.checksumAlgorithm(), manifest.checksum(), progress, 0.24, 0.68, "Downloading launcher");
            if (!isValid(target, manifest.size, manifest.checksumAlgorithm(), manifest.checksum())) throw new IOException("Downloaded launcher failed verification.");
            cleanupOldLaunchers(target, log);
            return target;
        } catch (Exception exception) {
            OSUtils.deleteQuietly(tempPath(target));
            throw new BootstrapException("Download error.", exception);
        }
    }

    private static Path ensureJava(int major, TaskProgress progress, UtilityLog log) throws BootstrapException {
        Path root = OSUtils.javaRuntimeDirectory(runtimeId(major));
        String runtimeId = root.getFileName().toString();
        Path tempRoot = root.resolveSibling(runtimeId + ".tmp");
        Path archive = OSUtils.javaDirectory().resolve(runtimeId + archiveExtension());

        cleanupRuntimeTemps(tempRoot, archive);

        Path java = findJava(root);
        if (Files.isRegularFile(java)) {
            OSUtils.makeExecutable(java);
            log.info("Java runtime already installed: " + java);
            return java;
        }

        try {
            Files.createDirectories(OSUtils.javaDirectory());
            String url = adoptiumUrl(major);
            log.info("Downloading Java runtime: " + url);
            downloadFile(url, archive, 0L, "", "", progress, 0.76, 0.90, "Downloading Java");

            report(progress, 0.93, "Installing Java...");
            OSUtils.deleteDirectory(tempRoot);
            extractRuntime(archive, tempRoot);

            java = findJava(tempRoot);
            if (!Files.isRegularFile(java)) throw new IOException("Java executable was not found after extraction.");
            OSUtils.makeExecutable(java);

            OSUtils.deleteDirectory(root);
            move(tempRoot, root);
            OSUtils.deleteQuietly(archive);

            java = findJava(root);
            if (!Files.isRegularFile(java)) throw new IOException("Java executable was not found after installation.");
            OSUtils.makeExecutable(java);
            log.info("Java runtime installed: " + java);
            return java;
        } catch (Exception exception) {
            cleanupRuntimeTemps(tempRoot, archive);
            throw new BootstrapException("Java error.", exception);
        }
    }

    private static void launch(Path java, Path launcher, UtilityLog log) throws BootstrapException {
        try {
            ProcessBuilder builder = new ProcessBuilder(java.toString(), "-jar", launcher.toString());
            builder.directory(OSUtils.launcherDirectory().toFile());
            builder.start();
            log.info("Launcher process started: " + launcher);
        } catch (Exception exception) {
            throw new BootstrapException("Launch error.", exception);
        }
    }

    private static void downloadFile(String url, Path target, long expectedSize, String algorithm, String expectedHash,
                                     TaskProgress progress, double from, double to, String label) throws Exception {
        Path temp = tempPath(target);
        try {
            Files.createDirectories(target.getParent());
            Files.deleteIfExists(temp);
            downloadRaw(url, temp, (value) -> {
                double total = from + (to - from) * value;
                report(progress, total, label + "... " + Math.round(total * 100.0) + "%");
            });

            if (expectedSize > 0 && Files.size(temp) != expectedSize) throw new IOException("Size mismatch for " + target);
            if (expectedHash != null && !expectedHash.isBlank() && !expectedHash.equalsIgnoreCase(hash(temp, algorithm))) {
                throw new IOException("Checksum mismatch for " + target);
            }
            move(temp, target);
        } finally {
            OSUtils.deleteQuietly(temp);
        }
    }

    private static void downloadRaw(String url, Path target, DownloadCallback progress) throws Exception {
        downloadRaw(url, target, progress, DOWNLOAD_TIMEOUT);
    }

    private static void downloadRaw(String url, Path target, DownloadCallback progress, Duration timeout) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", "LiteLauncher Bootstrap/" + System.getProperty("java.version", "java"))
                .GET()
                .build();

        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("HTTP " + response.statusCode() + " for " + response.uri());

        long length = response.headers().firstValueAsLong("Content-Length").orElse(0L);
        long done = 0L;
        long lastReport = 0L;

        try (InputStream input = response.body(); var output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                if (read == 0) continue;
                output.write(buffer, 0, read);
                done += read;

                long now = System.nanoTime();
                if (progress != null && now - lastReport > 75_000_000L) {
                    double value = length <= 0 ? 0.0 : Math.min(1.0, done / (double) length);
                    progress.update(value);
                    lastReport = now;
                }
            }
        }

        if (progress != null) progress.update(1.0);
    }

    private static boolean isValid(Path file, long expectedSize, String algorithm, String expectedHash) {
        try {
            if (!Files.isRegularFile(file)) return false;
            if (expectedSize > 0 && Files.size(file) != expectedSize) return false;
            return expectedHash == null || expectedHash.isBlank() || expectedHash.equalsIgnoreCase(hash(file, algorithm));
        } catch (Exception exception) {
            return false;
        }
    }

    private static String hash(Path file, String algorithm) throws Exception {
        String selected = algorithm == null || algorithm.isBlank() ? "SHA-1" : algorithm;
        MessageDigest digest = MessageDigest.getInstance(selected);
        try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (input.read(buffer) >= 0) {
                // DigestInputStream updates the digest.
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void extractRuntime(Path archive, Path target) throws Exception {
        Files.createDirectories(target);
        if (OSUtils.isWindows()) extractZip(archive, target);
        else extractTarGz(archive, target);
    }

    private static void extractZip(Path archive, Path target) throws Exception {
        Path root = target.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                String name = stripRoot(entry.getName());
                if (name.isBlank()) continue;

                Path out = root.resolve(name).toAbsolutePath().normalize();
                if (!out.startsWith(root)) continue;

                if (entry.isDirectory()) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void extractTarGz(Path archive, Path target) throws Exception {
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
                if (name.isBlank() || !out.startsWith(root)) {
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

    private static void applyTarMode(Path file, long mode) {
        if ((mode & 0111) != 0) OSUtils.makeExecutable(file);
    }

    private static Path findJava(Path root) {
        String preferred = OSUtils.isWindows() ? "javaw.exe" : "java";
        Path direct = root.resolve("bin").resolve(preferred);
        if (Files.isRegularFile(direct)) return direct;

        Path fallback = root.resolve("bin").resolve(OSUtils.isWindows() ? "java.exe" : "java");
        if (Files.isRegularFile(fallback)) return fallback;

        try (var stream = Files.find(root, 8, (path, attrs) -> {
            if (!attrs.isRegularFile() || path.getParent() == null || path.getFileName() == null) return false;
            String name = path.getFileName().toString();
            if (OSUtils.isWindows() && !(name.equalsIgnoreCase("javaw.exe") || name.equalsIgnoreCase("java.exe"))) return false;
            if (!OSUtils.isWindows() && !name.equals("java")) return false;
            return "bin".equals(path.getParent().getFileName().toString());
        })) {
            return stream.findFirst().orElse(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void cleanupStartupTemps(UtilityLog log) {
        try {
            cleanupTempFiles(OSUtils.bootstrapDirectory());
            cleanupTempFiles(OSUtils.launcherDirectory());
        } catch (Exception exception) {
            log.error("Unable to cleanup bootstrap temp files.", exception);
        }
    }

    private static void cleanupTempFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (var stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(TEMP_SUFFIX)).forEach(OSUtils::deleteQuietly);
        }
    }

    private static void cleanupRuntimeTemps(Path tempRoot, Path archive) {
        OSUtils.deleteQuietly(tempRoot);
        OSUtils.deleteQuietly(archive);
        OSUtils.deleteQuietly(tempPath(archive));
    }

    private static void cleanupOldLaunchers(Path keep, UtilityLog log) {
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


    private static Path launcherPath(LauncherManifest manifest) {
        return OSUtils.launcherDirectory().resolve(launcherFileName(manifest));
    }

    private static String runtimeId(int major) {
        return "jre-" + (major <= 0 ? currentJavaMajor() : major);
    }

    private static String launcherFileName(LauncherManifest manifest) {
        try {
            String path = URI.create(manifest.file).getPath();
            if (path != null) {
                int slash = path.lastIndexOf('/');
                String name = slash < 0 ? path : path.substring(slash + 1);
                name = sanitize(name);
                if (!name.isBlank()) return name;
            }
        } catch (Exception ignored) {
        }
        return "LiteLauncher-v" + sanitize(manifest.version) + ".jar";
    }

    private static String sanitize(String name) {
        if (name == null) return "";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Path tempPath(Path target) {
        return target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
    }

    private static void move(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String adoptiumUrl(int major) throws BootstrapException {
        return "https://api.adoptium.net/v3/binary/latest/" + (major <= 0 ? currentJavaMajor() : major) + "/ga/" + adoptiumOs() + "/" + adoptiumArch() + "/jre/hotspot/normal/eclipse?project=jdk";
    }

    private static String adoptiumOs() throws BootstrapException {
        if (OSUtils.isWindows()) return "windows";
        if (OSUtils.isMac()) return "mac";
        if (OSUtils.isLinux()) return "linux";
        throw new BootstrapException("Java error.");
    }

    private static String adoptiumArch() throws BootstrapException {
        String arch = OSUtils.arch();
        if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
        if (arch.contains("amd64") || arch.contains("x86_64") || arch.equals("x64")) return "x64";
        throw new BootstrapException("Java error.");
    }

    private static String archiveExtension() {
        return OSUtils.isWindows() ? ".zip" : ".tar.gz";
    }

    private static int currentJavaMajor() {
        String version = System.getProperty("java.version", "17");
        try {
            if (version.startsWith("1.")) return Integer.parseInt(version.substring(2, 3));
            int dot = version.indexOf('.');
            return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
        } catch (Exception ignored) {
            return 17;
        }
    }

    private static String stripRoot(String name) {
        if (name == null) return "";
        name = name.replace('\\', '/');
        int slash = name.indexOf('/');
        return slash < 0 ? "" : name.substring(slash + 1);
    }

    private static int readFully(InputStream input, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = input.read(buffer, offset, buffer.length - offset);
            if (read < 0) break;
            offset += read;
        }
        return offset;
    }

    private static boolean emptyBlock(byte[] block) {
        for (byte b : block) if (b != 0) return false;
        return true;
    }

    private static String tarString(byte[] data, int offset, int length) {
        int end = offset;
        while (end < offset + length && data[end] != 0) end++;
        return new String(data, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private static long tarSize(byte[] data, int offset, int length) {
        String text = tarString(data, offset, length).trim();
        if (text.isEmpty()) return 0L;
        return Long.parseLong(text, 8);
    }

    private static void copy(InputStream input, java.io.OutputStream output, long bytes) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long left = bytes;
        while (left > 0) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) throw new EOFException();
            output.write(buffer, 0, read);
            left -= read;
        }
    }

    private static void skip(InputStream input, long bytes) throws IOException {
        byte[] buffer = new byte[8192];
        long left = bytes;
        while (left > 0) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, left));
            if (read < 0) throw new EOFException();
            left -= read;
        }
    }

    private static void report(TaskProgress progress, double value, String details) {
        if (progress != null) progress.update(Math.max(0.0, Math.min(1.0, value)), details);
    }


    private static final class ManifestLoad {
        private final LauncherManifest manifest;
        private final boolean cached;

        private ManifestLoad(LauncherManifest manifest, boolean cached) {
            this.manifest = manifest;
            this.cached = cached;
        }
    }

    @FunctionalInterface
    private interface DownloadCallback {
        void update(double value);
    }
}
