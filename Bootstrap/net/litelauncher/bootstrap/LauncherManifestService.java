package net.litelauncher.bootstrap;

import net.litelauncher.backend.BootstrapLog;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.platform.OSUtils;

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
import java.time.Duration;

final class LauncherManifestService {

    private static final String PUBLIC_KEY = "MCowBQYDK2VwAyEA3Paqfi8Xb+fEZBAxGy1LjMH4YI3SlPW3rKT6kZH7APA=";
    private static final String VERSION_MANIFEST_URL = "https://litelauncher.net/api/v1/launcher/version_manifest.json";
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final Duration MANIFEST_TIMEOUT = Duration.ofSeconds(5);

    private final BootstrapLog log;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(MANIFEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    LauncherManifestService(BootstrapLog log) {
        this.log = log;
    }

    ManifestLoad loadManifest() throws BootstrapException {
        Path manifest = OSUtils.bootstrapManifestFile();
        Path temp = DownloadService.tempPath(manifest);

        try {
            Files.createDirectories(manifest.getParent());
            Files.deleteIfExists(temp);

            downloadRaw(VERSION_MANIFEST_URL, temp, MANIFEST_TIMEOUT);

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

    private void downloadRaw(String url, Path target, Duration timeout) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", "LiteLauncher Bootstrap/" + System.getProperty("java.version", "java"))
                .GET()
                .build();

        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        log.info("HTTP " + response.statusCode() + ": " + url + " -> " + response.uri());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new java.io.IOException("HTTP " + response.statusCode() + " for " + response.uri());

        try (InputStream input = response.body(); var output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                if (read == 0) continue;
                output.write(buffer, 0, read);
            }
        }
    }

    private void move(Path source, Path target) throws java.io.IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.io.IOException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
