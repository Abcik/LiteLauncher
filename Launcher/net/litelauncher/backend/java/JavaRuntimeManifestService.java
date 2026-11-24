package net.litelauncher.backend.java;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.OperatingSystem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

final class JavaRuntimeManifestService {

    static final String MANIFEST_URL = "https://litelauncher.net/api/v1/launcher/java_manifest.json";

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ATTEMPTS = 3;

    private int normalizeMajor(int major) {
        return major == 16 ? 17 : major;
    }

    /** Native runtimes keep their old folder names; an emulated one gets its own folder (jre-8-x64). */
    String runtimeId(int major, String arch) {
        String id = "jre-" + normalizeMajor(major);
        return arch.equals(hostArchitecture()) ? id : id + "-" + arch;
    }

    JavaRuntimePackage resolve(int requestedMajor, String arch) throws IOException {
        return parse(requestedMajor, arch, loadManifest());
    }

    boolean available(int requestedMajor, String arch) throws IOException {
        try {
            parse(requestedMajor, arch, loadManifest());
            return true;
        } catch (UnavailableRuntimeException exception) {
            return false;
        }
    }

    /** "aarch64", "x64", or "" for anything else. */
    static String hostArchitecture() {
        String value = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (value.contains("aarch64") || value.contains("arm64")) return "aarch64";
        if (value.contains("amd64") || value.contains("x86_64") || value.equals("x64")) return "x64";
        return "";
    }

    private JavaRuntimePackage parse(int requestedMajor, String arch, String manifest) throws IOException {
        int major = normalizeMajor(requestedMajor);
        String os = operatingSystem();
        if (blank(arch)) throw new IOException("Unsupported architecture for Java runtime: " + System.getProperty("os.arch", "") + ".");

        JsonObject root;
        try {
            root = JsonParser.object().from(manifest);
        } catch (Exception exception) {
            throw new IOException("Invalid Java runtime manifest.", exception);
        }
        if (root.getInt("schemaVersion", 0) != SCHEMA_VERSION)
            throw new IOException("Unsupported Java manifest schema version.");

        JsonObject runtime = root.getObject("runtimes", new JsonObject())
                .getObject(Integer.toString(major), new JsonObject())
                .getObject(os, new JsonObject())
                .getObject(arch);
        if (runtime == null) throw new UnavailableRuntimeException("Java " + major + " is unavailable for " + os + "/" + arch + ".");

        String name = requiredText(runtime, "name");
        String url = requiredText(runtime, "url");
        String sha1 = requiredText(runtime, "sha1");
        long size = runtime.getLong("size", 0L);

        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0)
            throw new IOException("Invalid Java package name in manifest.");
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".zip") && !lowerName.endsWith(".tar.gz"))
            throw new IOException("Unsupported Java archive format in manifest.");
        if (sha1.length() != 40 || !hex(sha1))
            throw new IOException("Invalid Java package SHA-1 in manifest.");
        if (size <= 0L) throw new IOException("Invalid Java package size in manifest.");

        return new JavaRuntimePackage(runtimeId(major, arch), name, url, sha1, size);
    }

    private String loadManifest() throws IOException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(MANIFEST_URL))
                        .timeout(Duration.ofSeconds(30))
                        .header("Accept", "application/json")
                        .header("User-Agent", "LiteLauncher")
                        .GET()
                        .build();
                HttpResponse<String> response = BackendUtils.http().send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) return response.body();
                lastError = new IOException("Java manifest request failed with HTTP " + response.statusCode() + ".");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Java manifest request was interrupted.", exception);
            } catch (IOException exception) {
                lastError = exception;
            }
        }
        throw new IOException("Unable to load Java runtime manifest.", lastError);
    }

    private String operatingSystem() throws IOException {
        String value = OperatingSystem.current().javaManifestName();
        if (!blank(value)) return value;
        throw new IOException("Unsupported operating system for Java runtime.");
    }

    private static final class UnavailableRuntimeException extends IOException {
        private UnavailableRuntimeException(String message) {
            super(message);
        }
    }

    private static String requiredText(JsonObject json, String key) throws IOException {
        String value = json.getString(key);
        if (!blank(value)) return value;
        throw new IOException("Invalid Java manifest field: " + key + ".");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean hex(String value) {
        for (int i = 0; i < value.length(); i++) if (Character.digit(value.charAt(i), 16) < 0) return false;
        return true;
    }
}
