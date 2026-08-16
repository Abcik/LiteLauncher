package net.litelauncher.backend.download;

import net.litelauncher.backend.BootstrapLog;

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

    public static final String TEMP_SUFFIX = ".litelauncher-download";

    private static final int DOWNLOAD_ATTEMPTS = 3;
    private static final int BUFFER_SIZE = 64 * 1024;

    private final BootstrapLog log;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public DownloadService(BootstrapLog log) {
        this.log = log;
    }

    public void download(List<DownloadFile> files, DownloadProgress progress) throws DownloadException {
        List<DownloadFile> plan = unique(files);
        if (plan.isEmpty()) return;

        cleanupDownloadTemps(plan);

        ValidationSummary validation = validate(plan);
        List<DownloadFile> missing = validation.missing();
        long total = validation.totalBytes();
        long done = validation.doneBytes();

        info("Download check: total=" + plan.size()
                + ", valid=" + validation.validFiles()
                + ", missing=" + missing.size());
        report(progress, total, done, validation.validFiles(), plan.size(), "Checking files");
        if (missing.isEmpty()) return;

        int threads = downloadThreads();
        info("Starting download pool: threads=" + threads + ", files=" + missing.size());

        AtomicLong totalBytes = new AtomicLong(Math.max(total, done));
        AtomicLong downloadedBytes = new AtomicLong(done);
        AtomicLong completedFiles = new AtomicLong(validation.validFiles());
        AtomicLong lastReport = new AtomicLong();
        AtomicBoolean cancelled = new AtomicBoolean();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ExecutorCompletionService<Void> completion = new ExecutorCompletionService<>(executor);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (DownloadFile file : missing) {
                futures.add(completion.submit(() -> {
                    if (!cancelled.get())
                        downloadOne(file, totalBytes, downloadedBytes, completedFiles, plan.size(), lastReport, progress, cancelled);
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
                    throw new DownloadException("Unable to download files.", exception);
                } catch (ExecutionException exception) {
                    cancelled.set(true);
                    cancel(futures);
                    Throwable cause = unwrap(exception);
                    if (cause instanceof DownloadException downloadException) throw downloadException;
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
            info("Download pool stopped.");
        }
    }

    public boolean isValid(DownloadFile file) throws DownloadException {
        try {
            if (file == null || file.path() == null || !Files.isRegularFile(file.path())) return false;
            if (file.size() > 0 && Files.size(file.path()) != file.size()) return false;
            return file.checksum().isBlank() || file.checksum().equalsIgnoreCase(hash(file.path(), file.checksumAlgorithm()));
        } catch (DownloadException exception) {
            throw exception;
        } catch (Exception exception) {
            return false;
        }
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

    private ValidationSummary validate(List<DownloadFile> plan) throws DownloadException {
        List<DownloadFile> missing = new ArrayList<>();
        long totalBytes = 0L;
        long doneBytes = 0L;
        long validFiles = 0L;

        for (DownloadFile file : plan) {
            if (file.size() > 0) totalBytes += file.size();
            if (isValid(file)) {
                validFiles++;
                doneBytes += Math.max(0L, file.size());
            } else {
                if (file.url().isBlank()) throw new DownloadException("Unable to download files.");
                missing.add(file);
            }
        }

        return new ValidationSummary(missing, totalBytes, doneBytes, validFiles);
    }

    private void downloadOne(DownloadFile file, AtomicLong totalBytes, AtomicLong downloadedBytes, AtomicLong completedFiles,
                             long fileCount, AtomicLong lastReport, DownloadProgress progress, AtomicBoolean cancelled) throws DownloadException {
        Path temp = tempPath(file.path());
        AtomicBoolean contentLengthAdded = new AtomicBoolean();
        Throwable lastError = null;

        try {
            Files.createDirectories(file.path().getParent());

            for (int attempt = 1; attempt <= DOWNLOAD_ATTEMPTS; attempt++) {
                AtomicLong attemptBytes = new AtomicLong();
                try {
                    Files.deleteIfExists(temp);
                    info("Downloading attempt " + attempt + "/" + DOWNLOAD_ATTEMPTS + ": " + fileName(file) + " <- " + file.url());
                    downloadSingle(file, temp, totalBytes, downloadedBytes, completedFiles, fileCount, lastReport, progress, cancelled, attemptBytes, contentLengthAdded);
                    completedFiles.incrementAndGet();
                    report(progress, totalBytes.get(), downloadedBytes.get(), completedFiles.get(), fileCount, label(file));
                    info("Downloaded: " + fileName(file) + " -> " + file.path());
                    return;
                } catch (DownloadException exception) {
                    rollbackAttemptBytes(downloadedBytes, attemptBytes);
                    lastError = exception;
                    if (cancelled.get() || attempt >= DOWNLOAD_ATTEMPTS) throw exception;
                    error("Download attempt failed " + attempt + "/" + DOWNLOAD_ATTEMPTS + ": " + fileName(file) + " <- " + file.url(), exception);
                    waitBeforeRetry(attempt);
                } catch (Exception exception) {
                    rollbackAttemptBytes(downloadedBytes, attemptBytes);
                    lastError = exception;
                    if (cancelled.get() || attempt >= DOWNLOAD_ATTEMPTS) throw downloadException(exception);
                    error("Download attempt failed " + attempt + "/" + DOWNLOAD_ATTEMPTS + ": " + fileName(file) + " <- " + file.url(), exception);
                    waitBeforeRetry(attempt);
                } finally {
                    deleteQuietly(temp);
                }
            }
        } catch (DownloadException exception) {
            error("Download failed: " + fileName(file) + " <- " + file.url(), exception);
            throw exception;
        } catch (Exception exception) {
            error("Download failed: " + fileName(file) + " <- " + file.url(), exception);
            throw downloadException(exception);
        }

        throw downloadException(lastError);
    }

    private void downloadSingle(DownloadFile file, Path temp, AtomicLong totalBytes, AtomicLong downloadedBytes,
                                AtomicLong completedFiles, long fileCount, AtomicLong lastReport, DownloadProgress progress,
                                AtomicBoolean cancelled, AtomicLong attemptBytes, AtomicBoolean contentLengthAdded) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(file.url()))
                .timeout(Duration.ofMinutes(3))
                .header("User-Agent", "LiteLauncher/" + System.getProperty("java.version", "java"))
                .GET()
                .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        info("HTTP " + response.statusCode() + ": " + file.url() + " -> " + response.uri());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new DownloadException("Unable to download files.", new java.io.IOException("HTTP " + response.statusCode() + " for " + response.uri()));

        if (file.size() <= 0 && contentLengthAdded.compareAndSet(false, true)) {
            long length = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (length > 0) totalBytes.addAndGet(length);
        }

        try (InputStream input = response.body(); var output = Files.newOutputStream(temp)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (cancelled.get()) throw new DownloadException("Unable to download files.");
                if (read == 0) continue;
                output.write(buffer, 0, read);
                attemptBytes.addAndGet(read);
                reportThrottled(progress, totalBytes.get(), downloadedBytes.addAndGet(read), completedFiles.get(), fileCount, label(file), lastReport);
            }
        }

        if (!isValid(new DownloadFile(file.url(), temp, file.checksumAlgorithm(), file.checksum(), file.size(), file.label())))
            throw new DownloadException("Unable to download files.");
        try {
            Files.move(temp, file.path(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
            Files.move(temp, file.path(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void rollbackAttemptBytes(AtomicLong downloadedBytes, AtomicLong attemptBytes) {
        long bytes = attemptBytes.get();
        if (bytes > 0) downloadedBytes.addAndGet(-bytes);
    }

    private void waitBeforeRetry(int attempt) throws DownloadException {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DownloadException("Unable to download files.", exception);
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
        info("Download temp cleanup completed: directories=" + directories.size());
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

    public static Path tempPath(Path target) {
        return target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
    }

    private void cancel(List<? extends Future<?>> futures) {
        for (Future<?> future : futures) future.cancel(true);
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable result = throwable;
        while (result instanceof ExecutionException && result.getCause() != null) result = result.getCause();
        if (result instanceof RuntimeException runtime && runtime.getCause() != null) return runtime.getCause();
        return result;
    }

    private DownloadException downloadException(Throwable cause) {
        if (isConnectionProblem(cause)) return new DownloadException("Unable to connect to download server.", cause, true);
        return new DownloadException("Unable to download files.", cause);
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
        String action = label == null || label.isBlank() ? "Downloading files" : label;
        progress.update(value, action + "... " + Math.round(value * 100.0) + "%", filesDone + "/" + filesTotal);
    }

    private String fileName(DownloadFile file) {
        String name = file == null || file.path() == null || file.path().getFileName() == null ? "file" : file.path().getFileName().toString();
        String label = label(file);
        return label.isBlank() ? name : label + " (" + name + ")";
    }

    private String label(DownloadFile file) {
        if (file == null || file.label().isBlank()) return "Downloading files";
        return file.label().endsWith("...") ? file.label().substring(0, file.label().length() - 3) : file.label();
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
        }
    }

    public static String hash(Path file, String algorithm) throws DownloadException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm == null || algorithm.isBlank() ? "SHA-1" : algorithm);
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new DownloadException("Unable to hash file: " + file, exception);
        }
    }

    private void info(String message) {
        if (log != null) log.info(message);
    }

    private void error(String message, Throwable throwable) {
        if (log != null) log.error(message, throwable);
    }

    private record ValidationSummary(List<DownloadFile> missing, long totalBytes, long doneBytes, long validFiles) {
    }
}
