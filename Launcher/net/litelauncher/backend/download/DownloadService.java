package net.litelauncher.backend.download;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.CancellationToken;
import net.litelauncher.backend.LauncherLog;
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
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class DownloadService {

    private static final String TEMP_SUFFIX = ".litelauncher-download";
    private static final int DOWNLOAD_ATTEMPTS = 3;

    private final HttpClient http = BackendUtils.http();

    public void download(List<DownloadFile> files, DownloadProgress progress, CancellationToken cancellation) throws DownloadException {
        checkCancelled(cancellation);
        List<DownloadFile> plan = unique(files);
        if (plan.isEmpty()) return;

        cleanupDownloadTemps(plan);
        try {
            FileCheck check = checkFiles(plan, progress, cancellation);
            List<DownloadFile> missing = check.missing();
            long total = check.totalBytes();
            long done = check.doneBytes();

            LauncherLog.info("Download check: total=" + plan.size()
                    + ", valid=" + check.presentFiles()
                    + ", missing=" + missing.size());
            report(progress, total, done, check.presentFiles(), plan.size(), I18n.text("progress.checkingFiles"));
            if (missing.isEmpty()) return;

            int workerCount = Math.min(downloadThreads(), missing.size());
            LauncherLog.info("Starting download workers: threads=" + workerCount + ", files=" + missing.size());

            AtomicLong totalBytes = new AtomicLong(Math.max(total, done));
            AtomicLong downloadedBytes = new AtomicLong(done);
            AtomicLong completedFiles = new AtomicLong(check.presentFiles());
            AtomicLong lastReport = new AtomicLong();
            AtomicBoolean cancelled = new AtomicBoolean();
            AtomicInteger nextFile = new AtomicInteger();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<Thread> workers = new ArrayList<>(workerCount);
            for (int index = 0; index < workerCount; index++) {
                workers.add(Thread.ofVirtual().name("download-worker-" + (index + 1)).unstarted(() -> {
                    while (!cancelled.get()) {
                        int fileIndex = nextFile.getAndIncrement();
                        if (fileIndex >= missing.size()) return;
                        try {
                            downloadOne(missing.get(fileIndex), totalBytes, downloadedBytes, completedFiles, plan.size(),
                                    lastReport, progress, cancelled, cancellation);
                        } catch (Throwable throwable) {
                            failure.compareAndSet(null, throwable);
                            cancelled.set(true);
                            interruptOthers(workers);
                            return;
                        }
                    }
                }));
            }
            workers.forEach(Thread::start);
            joinWorkers(workers, cancelled, cancellation);

            Throwable cause = failure.get();
            if (cause instanceof CancellationException cancellationException) throw cancellationException;
            if (cause instanceof DownloadException downloadException) throw downloadException;
            if (cause != null) throw downloadException(cause);
        } finally {
            cleanupDownloadTemps(plan);
        }
    }

    public boolean isPresent(DownloadFile file, CancellationToken cancellation) {
        try {
            checkCancelled(cancellation);
            if (file == null || file.path() == null || !Files.isRegularFile(file.path())) return false;
            return file.size() <= 0 || Files.size(file.path()) == file.size();
        } catch (CancellationException exception) {
            throw exception;
        } catch (Exception _) {
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
        return List.copyOf(result.values());
    }

    private FileCheck checkFiles(List<DownloadFile> plan, DownloadProgress progress, CancellationToken cancellation) throws DownloadException {
        long totalBytes = 0L;
        for (DownloadFile file : plan) if (file.size() > 0) totalBytes += file.size();

        List<DownloadFile> missing = new ArrayList<>();
        AtomicLong lastReport = new AtomicLong();
        long doneBytes = 0L;
        long presentFiles = 0L;

        for (DownloadFile file : plan) {
            checkCancelled(cancellation);
            if (isPresent(file, cancellation)) {
                presentFiles++;
                doneBytes += Math.max(0L, file.size());
            } else {
                if (file.url().isBlank()) throw new DownloadException("Unable to download files.");
                missing.add(file);
            }
            reportThrottled(progress, totalBytes, doneBytes, presentFiles, plan.size(), I18n.text("progress.checkingFiles"), lastReport);
        }

        return new FileCheck(missing, totalBytes, doneBytes, presentFiles);
    }

    private void downloadOne(DownloadFile file, AtomicLong totalBytes, AtomicLong downloadedBytes, AtomicLong completedFiles,
                             long fileCount, AtomicLong lastReport, DownloadProgress progress, AtomicBoolean cancelled, CancellationToken cancellation) throws DownloadException {
        Path temp = tempPath(file.path());
        AtomicBoolean contentLengthAdded = new AtomicBoolean();
        Throwable lastError = null;

        try {
            Files.createDirectories(file.path().getParent());

            for (int attempt = 1; attempt <= DOWNLOAD_ATTEMPTS; attempt++) {
                checkCancelled(cancellation);
                AtomicLong attemptBytes = new AtomicLong();
                try {
                    Files.deleteIfExists(temp);
                    LauncherLog.info("Downloading attempt " + attempt + "/" + DOWNLOAD_ATTEMPTS + ": " + fileName(file) + " <- " + file.url());
                    downloadSingle(file, temp, totalBytes, downloadedBytes, completedFiles, fileCount, lastReport, progress, cancelled, cancellation, attemptBytes, contentLengthAdded);
                    completedFiles.incrementAndGet();
                    report(progress, totalBytes.get(), downloadedBytes.get(), completedFiles.get(), fileCount, label(file));
                    LauncherLog.info("Downloaded: " + fileName(file) + " -> " + file.path());
                    return;
                } catch (CancellationException exception) {
                    rollbackAttemptBytes(downloadedBytes, attemptBytes);
                    throw exception;
                } catch (DownloadException exception) {
                    rollbackAttemptBytes(downloadedBytes, attemptBytes);
                    lastError = exception;
                    if (cancelled.get() || attempt >= DOWNLOAD_ATTEMPTS) throw exception;
                    LauncherLog.error("Download attempt failed " + attempt + "/" + DOWNLOAD_ATTEMPTS + ": " + fileName(file) + " <- " + file.url(), exception);
                    waitBeforeRetry(attempt, cancellation);
                } catch (Exception exception) {
                    rollbackAttemptBytes(downloadedBytes, attemptBytes);
                    lastError = exception;
                    if (isCancelled(cancellation)) {
                        Thread.currentThread().interrupt();
                        throw new CancellationException("Download cancelled.");
                    }
                    if (cancelled.get() || attempt >= DOWNLOAD_ATTEMPTS) throw downloadException(exception);
                    LauncherLog.error("Download attempt failed " + attempt + "/" + DOWNLOAD_ATTEMPTS + ": " + fileName(file) + " <- " + file.url(), exception);
                    waitBeforeRetry(attempt, cancellation);
                } finally {
                    BackendUtils.deleteQuietly(temp);
                }
            }
        } catch (CancellationException exception) {
            LauncherLog.info("Download cancelled: " + fileName(file));
            throw exception;
        } catch (DownloadException exception) {
            LauncherLog.error("Download failed: " + fileName(file) + " <- " + file.url(), exception);
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Download failed: " + fileName(file) + " <- " + file.url(), exception);
            throw downloadException(exception);
        }

        throw downloadException(lastError);
    }

    private void downloadSingle(DownloadFile file, Path temp, AtomicLong totalBytes, AtomicLong downloadedBytes,
                                AtomicLong completedFiles, long fileCount, AtomicLong lastReport, DownloadProgress progress,
                                AtomicBoolean cancelled, CancellationToken cancellation, AtomicLong attemptBytes, AtomicBoolean contentLengthAdded) throws Exception {
        checkCancelled(cancellation);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(file.url()))
                .timeout(Duration.ofMinutes(3));
        if (!hasHeader(file.headers(), "User-Agent"))
            requestBuilder.header("User-Agent", "LiteLauncher/" + System.getProperty("java.version", "java"));
        file.headers().forEach(requestBuilder::header);
        HttpRequest request = requestBuilder.GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        LauncherLog.info("HTTP " + response.statusCode() + ": " + file.url() + " -> " + response.uri());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new DownloadException("Unable to download files.", new java.io.IOException("HTTP " + response.statusCode() + " for " + response.uri()));

        if (file.size() <= 0 && contentLengthAdded.compareAndSet(false, true)) {
            long length = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (length > 0) totalBytes.addAndGet(length);
        }

        try (InputStream input = response.body(); var output = Files.newOutputStream(temp)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                checkCancelled(cancellation);
                if (cancelled.get()) throw new DownloadException("Unable to download files.");
                if (read == 0) continue;
                output.write(buffer, 0, read);
                attemptBytes.addAndGet(read);
                reportThrottled(progress, totalBytes.get(), downloadedBytes.addAndGet(read), completedFiles.get(), fileCount, label(file), lastReport);
            }
        }

        verifyDownloaded(file, temp, cancellation);
        BackendUtils.moveReplace(temp, file.path());
    }


    private void verifyDownloaded(DownloadFile file, Path temp, CancellationToken cancellation) throws DownloadException {
        try {
            checkCancelled(cancellation);
            if (!Files.isRegularFile(temp)) throw new DownloadException("Unable to download files.");
            if (file.size() > 0 && Files.size(temp) != file.size())
                throw new DownloadException("Downloaded file has an unexpected size: " + fileName(file));
            if (!file.sha1().isBlank() && !file.sha1().equalsIgnoreCase(sha1(temp, cancellation)))
                throw new DownloadException("Downloaded file has an invalid SHA-1: " + fileName(file));
        } catch (CancellationException | DownloadException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DownloadException("Unable to verify downloaded file: " + fileName(file), exception);
        }
    }

    private boolean hasHeader(Map<String, String> headers, String name) {
        return headers != null && name != null && headers.keySet().stream().anyMatch(name::equalsIgnoreCase);
    }

    private void rollbackAttemptBytes(AtomicLong downloadedBytes, AtomicLong attemptBytes) {
        long bytes = attemptBytes.get();
        if (bytes > 0) downloadedBytes.addAndGet(-bytes);
    }

    private void waitBeforeRetry(int attempt, CancellationToken cancellation) throws DownloadException {
        checkCancelled(cancellation);
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (isCancelled(cancellation)) throw new CancellationException("Download cancelled.");
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
        Map<Path, Set<String>> targetsByDirectory = new LinkedHashMap<>();
        for (DownloadFile file : files) {
            if (file == null || file.path() == null || file.path().getParent() == null || file.path().getFileName() == null) continue;
            targetsByDirectory.computeIfAbsent(file.path().getParent(), _ -> new HashSet<>())
                    .add(file.path().getFileName().toString());
        }

        for (Map.Entry<Path, Set<String>> entry : targetsByDirectory.entrySet()) {
            Path directory = entry.getKey();
            if (!Files.isDirectory(directory)) continue;
            Set<String> targets = entry.getValue();
            try (var stream = Files.list(directory)) {
                stream.filter(path -> {
                    String name = path.getFileName() == null ? "" : path.getFileName().toString();
                    for (String target : targets) {
                        if (name.equals(target + TEMP_SUFFIX) || name.equals(target + ".download")) return true;
                    }
                    return false;
                }).forEach(BackendUtils::deleteQuietly);
            } catch (Exception _) {
            }
        }
        LauncherLog.info("Download temp cleanup completed: directories=" + targetsByDirectory.size());
    }

    private Path tempPath(Path target) {
        return target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
    }

    private void joinWorkers(List<Thread> workers, AtomicBoolean cancelled, CancellationToken cancellation) throws DownloadException {
        boolean interrupted = false;
        for (Thread worker : workers) {
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException _) {
                    interrupted = true;
                    cancelled.set(true);
                    workers.forEach(Thread::interrupt);
                }
            }
        }
        if (!interrupted) return;
        Thread.currentThread().interrupt();
        if (isCancelled(cancellation)) throw new CancellationException("Download cancelled.");
        throw new DownloadException("Unable to download files.", new InterruptedException("Download interrupted."));
    }

    private void interruptOthers(List<Thread> workers) {
        Thread current = Thread.currentThread();
        workers.stream().filter(worker -> worker != current).forEach(Thread::interrupt);
    }

    private boolean isCancelled(CancellationToken cancellation) {
        return cancellation != null && cancellation.cancelled();
    }

    private void checkCancelled(CancellationToken cancellation) {
        if (cancellation != null) cancellation.throwIfCancelled();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Download cancelled.");
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
        value = Math.clamp(value, 0.0, 1.0);
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

    public static String sha1(Path file, CancellationToken cancellation) throws DownloadException {
        try {
            if (cancellation != null) cancellation.throwIfCancelled();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (cancellation != null) cancellation.throwIfCancelled();
                    if (Thread.currentThread().isInterrupted()) throw new CancellationException("Hashing cancelled.");
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (CancellationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DownloadException("Unable to hash file: " + file, exception);
        }
    }


    private record FileCheck(List<DownloadFile> missing, long totalBytes, long doneBytes, long presentFiles) {
    }
}
