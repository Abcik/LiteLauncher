package net.litelauncher.backend;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

public final class BackendUtils {

    private static final HttpClient HTTP = createHttpClient(null);
    private static final HttpClient HTTP_1 = createHttpClient(HttpClient.Version.HTTP_1_1);

    private BackendUtils() {
    }

    public static HttpClient http() {
        return HTTP;
    }

    public static HttpClient http1() {
        return HTTP_1;
    }

    public static Path safeResolve(Path root, String relative) throws IOException {
        return safeResolve(root, relative, false);
    }

    public static Path safeResolve(Path root, String relative, boolean allowRoot) throws IOException {
        if (root == null || relative == null) throw new IOException("Invalid path.");
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relative.replace('\\', '/')).normalize();
        if (!resolved.startsWith(normalizedRoot) || (!allowRoot && resolved.equals(normalizedRoot))) {
            throw new IOException("Path escapes target directory: " + relative);
        }
        return resolved;
    }

    public static void writeAtomic(Path file, String value) throws IOException {
        writeAtomic(file, (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    public static void writeAtomic(Path file, byte[] value) throws IOException {
        Path parent = file == null ? null : file.toAbsolutePath().normalize().getParent();
        if (file == null || parent == null) throw new IOException("Invalid file path.");
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            Files.write(temp, value == null ? new byte[0] : value);
            moveReplace(temp, file);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException _) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException _) {
            Files.move(source, target);
        }
    }

    public static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    public static void deleteTreeQuietly(Path root) {
        try {
            deleteTree(root);
        } catch (IOException _) {
        }
    }

    public static void deleteQuietly(Path file) {
        try {
            if (file != null) Files.deleteIfExists(file);
        } catch (IOException _) {
        }
    }

    public static long parseTime(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception _) {
            return 0L;
        }
    }

    public static String firstText(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private static HttpClient createHttpClient(HttpClient.Version version) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.ALWAYS);
        if (version != null) builder.version(version);
        return builder.build();
    }
}
