package net.litelauncher.backend.modules.download;

import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.modules.launch.GameLaunchException;
import net.litelauncher.i18n.I18n;

import javax.net.ssl.SSLException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class DownloadService {

    private static final String TEMP_SUFFIX = ".litelauncher-download";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(16))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public void download(List<DownloadFile> files, DownloadProgress progress) throws GameLaunchException {
        List<DownloadFile> plan = unique(files);
        if (plan.isEmpty()) return;

        cleanupDownloadTemps(plan);

        List<DownloadFile> missing = new ArrayList<>();
        long total = 0L;
        long done = 0L;

        for (DownloadFile file : plan) {
            if (file.size() > 0) total += file.size();
            if (isValid(file)) done += Math.max(0L, file.size());
            else if (file.url().isBlank()) throw new GameLaunchException("Unable to download files.", InformationMessages.DOWNLOAD_ERROR);
            else missing.add(file);
        }

        LauncherLog.info("Download check: total=" + plan.size() + ", valid=" + (plan.size() - missing.size()) + ", missing=" + missing.size());
        report(progress, total, done, plan.size() - missing.size(), plan.size(), I18n.text("progress.checkingFiles"));
        if (missing.isEmpty()) return;

        int threads = downloadThreads();
        LauncherLog.info("Starting download pool: threads=" + threads + ", files=" + missing.size());

        AtomicLong totalBytes = new AtomicLong(Math.max(total, done));
        AtomicLong downloadedBytes = new AtomicLong(done);
        AtomicLong completedFiles = new AtomicLong(plan.size() - missing.size());
        AtomicLong lastReport = new AtomicLong();
        AtomicBoolean cancelled = new AtomicBoolean();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ExecutorCompletionService<Void> completion = new ExecutorCompletionService<>(executor);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (DownloadFile file : missing) {
                futures.add(completion.submit(() -> {
                    if (!cancelled.get()) downloadOne(file, totalBytes, downloadedBytes, completedFiles, plan.size(), lastReport, progress, cancelled);
                    return null;
                }));
            }

            for (int i = 0; i < futures.size(); i++) {
                try {
                    completion.take().get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    cancelled.set(true);
                    cancel(futures);
                    throw new GameLaunchException("Unable to download files.", exception, InformationMessages.DOWNLOAD_ERROR);
                } catch (ExecutionException exception) {
                    cancelled.set(true);
                    cancel(futures);
                    Throwable cause = unwrap(exception);
                    if (cause instanceof GameLaunchException launchException) throw launchException;
                    throw downloadException(cause);
                }
            }
        } finally {
            cancelled.set(true);
            executor.shutdownNow();
            try {
                executor.awaitTermination(8, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            cleanupDownloadTemps(plan);
            LauncherLog.info("Download pool stopped.");
        }
    }

    public boolean isValid(DownloadFile file) throws GameLaunchException {
        try {
            if (file == null || file.path() == null || !Files.isRegularFile(file.path())) return false;
            if (file.size() > 0 && Files.size(file.path()) != file.size()) return false;
            return file.sha1().isBlank() || file.sha1().equalsIgnoreCase(sha1(file.path()));
        } catch (GameLaunchException exception) {
            throw exception;
        } catch (Exception exception) {
            return false;
        }
    }

    public void shutdown() {
        // The download executor is created per download session and closed immediately after it.
    }

    private List<DownloadFile> unique(List<DownloadFile> files) {
        Map<Path, DownloadFile> result = new LinkedHashMap<>();
        if (files == null) return List.of();
        for (DownloadFile file : files) {
            if (file == null || file.path() == null) continue;
            result.putIfAbsent(file.path().normalize(), file);
        }
        return new ArrayList<>(result.values());
    }

    private void downloadOne(DownloadFile file, AtomicLong totalBytes, AtomicLong downloadedBytes, AtomicLong completedFiles,
                             long fileCount, AtomicLong lastReport, DownloadProgress progress, AtomicBoolean cancelled) throws GameLaunchException {
        Path temp = tempPath(file.path());
        try {
            Files.createDirectories(file.path().getParent());
            Files.deleteIfExists(temp);
            LauncherLog.info("Downloading: " + fileName(file) + " <- " + file.url());
            downloadSingle(file, temp, totalBytes, downloadedBytes, completedFiles, fileCount, lastReport, progress, cancelled);
            completedFiles.incrementAndGet();
            report(progress, totalBytes.get(), downloadedBytes.get(), completedFiles.get(), fileCount, label(file));
            LauncherLog.info("Downloaded: " + fileName(file) + " -> " + file.path());
        } catch (GameLaunchException exception) {
            LauncherLog.error("Download failed: " + fileName(file) + " <- " + file.url(), exception);
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Download failed: " + fileName(file) + " <- " + file.url(), exception);
            throw downloadException(exception);
        } finally {
            deleteQuietly(temp);
        }
    }

    private void downloadSingle(DownloadFile file, Path temp, AtomicLong totalBytes, AtomicLong downloadedBytes,
                                AtomicLong completedFiles, long fileCount, AtomicLong lastReport, DownloadProgress progress,
                                AtomicBoolean cancelled) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(file.url()))
                .timeout(Duration.ofMinutes(3))
                .header("User-Agent", "LiteLauncher/" + System.getProperty("java.version", "java"))
                .GET()
                .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        LauncherLog.info("HTTP " + response.statusCode() + ": " + file.url() + " -> " + response.uri());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new GameLaunchException("Unable to download files.", new java.io.IOException("HTTP " + response.statusCode() + " for " + response.uri()), InformationMessages.DOWNLOAD_ERROR);

        if (file.size() <= 0) {
            long length = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (length > 0) totalBytes.addAndGet(length);
        }

        try (InputStream input = response.body(); var output = Files.newOutputStream(temp)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (cancelled.get()) throw new GameLaunchException("Unable to download files.", InformationMessages.DOWNLOAD_ERROR);
                if (read == 0) continue;
                output.write(buffer, 0, read);
                reportThrottled(progress, totalBytes.get(), downloadedBytes.addAndGet(read), completedFiles.get(), fileCount, label(file), lastReport);
            }
        }

        if (!isValid(new DownloadFile(file.url(), temp, file.sha1(), file.size(), file.label()))) throw new GameLaunchException("Unable to download files.", InformationMessages.DOWNLOAD_ERROR);
        try {
            Files.move(temp, file.path(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
            Files.move(temp, file.path(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private int downloadThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 2) return 4;
        if (cores <= 4) return 8;
        return 16;
    }

    private void cleanupDownloadTemps(List<DownloadFile> files) {
        Set<Path> directories = new HashSet<>();
        for (DownloadFile file : files) {
            if (file == null || file.path() == null || file.path().getParent() == null) continue;
            directories.add(file.path().getParent());
            deleteQuietly(tempPath(file.path()));
            deleteOldTempFor(file.path());
        }
        LauncherLog.info("Download temp cleanup completed: directories=" + directories.size());
    }

    private void deleteOldTempFor(Path target) {
        Path directory = target.getParent();
        Path name = target.getFileName();
        if (directory == null || name == null || !Files.isDirectory(directory)) return;
        String prefix = name.toString();
        try (var stream = Files.list(directory)) {
            stream.filter(path -> {
                String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
                return fileName.startsWith(prefix) && (fileName.endsWith(".download") || fileName.endsWith(TEMP_SUFFIX));
            }).forEach(this::deleteQuietly);
        } catch (Exception ignored) {
        }
    }

    private Path tempPath(Path target) {
        return target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
    }

    private void cancel(List<Future<Void>> futures) {
        for (Future<Void> future : futures) future.cancel(true);
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable result = throwable;
        while (result instanceof ExecutionException && result.getCause() != null) result = result.getCause();
        if (result instanceof RuntimeException runtime && runtime.getCause() != null) return runtime.getCause();
        return result;
    }

    private GameLaunchException downloadException(Throwable cause) {
        if (isConnectionProblem(cause)) return new GameLaunchException("Unable to connect to download server.", cause, InformationMessages.DOWNLOAD_ERROR);
        return new GameLaunchException("Unable to download files.", cause, InformationMessages.DOWNLOAD_ERROR);
    }

    private boolean isConnectionProblem(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof HttpTimeoutException
                    || current instanceof InterruptedIOException
                    || current instanceof SSLException) return true;
        }
        return false;
    }

    private void reportThrottled(DownloadProgress progress, long total, long done, long filesDone, long filesTotal, String label, AtomicLong lastReport) {
        long now = System.nanoTime();
        long last = lastReport.get();
        if (now - last < 75_000_000L || !lastReport.compareAndSet(last, now)) return;
        report(progress, total, done, filesDone, filesTotal, label);
    }

    private void report(DownloadProgress progress, long total, long done, long filesDone, long filesTotal, String label) {
        if (progress == null) return;
        double value = total <= 0 ? (filesTotal <= 0 ? 1.0 : filesDone / (double) filesTotal) : done / (double) total;
        value = Math.max(0.0, Math.min(1.0, value));
        progress.update(value, (label == null || label.isBlank() ? I18n.text("progress.downloadingFiles") : label) + "... " + Math.round(value * 100.0) + "%", filesDone + "/" + filesTotal);
    }

    private String fileName(DownloadFile file) {
        String name = file == null || file.path() == null || file.path().getFileName() == null ? "file" : file.path().getFileName().toString();
        String label = label(file);
        return label.isBlank() ? name : label + " (" + name + ")";
    }

    private String label(DownloadFile file) {
        if (file == null || file.label().isBlank()) return I18n.text("progress.downloadingFiles");
        return file.label().endsWith("...") ? file.label().substring(0, file.label().length() - 3) : file.label();
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
        }
    }

    public static String sha1(Path file) throws GameLaunchException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
                byte[] buffer = new byte[64 * 1024];
                while (input.read(buffer) >= 0) {
                    // DigestInputStream updates the digest.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to hash file: " + file, exception);
        }
    }
}
