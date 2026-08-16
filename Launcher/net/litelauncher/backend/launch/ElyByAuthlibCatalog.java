package net.litelauncher.backend.launch;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ElyByAuthlibCatalog {

    private static final URI MANIFEST_URL = URI.create("https://litelauncher.net/api/v1/launcher/ely_manifest.json");
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;

    private final HttpClient http = BackendUtils.http1();

    private volatile Map<String, ElyByAuthlibOverride> overrides = loadInitial();
    private volatile boolean refreshComplete;

    public synchronized void refresh() {
        if (refreshComplete) return;
        try {
            String json = downloadManifest();
            Map<String, ElyByAuthlibOverride> parsed = parse(json);
            overrides = parsed;

            try {
                writeCache(json);
            } catch (Exception exception) {
                LauncherLog.error("Unable to cache Ely.by Authlib manifest; downloaded data will be used for this session.", exception);
            }

            LauncherLog.info("Ely.by Authlib manifest updated: entries=" + parsed.size());
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            LauncherLog.info("Ely.by Authlib manifest update was interrupted.");
        } catch (Exception exception) {
            LauncherLog.error("Unable to update Ely.by Authlib manifest; existing manifest data will be used.", exception);
        } finally {
            refreshComplete = true;
        }
    }

    ElyByAuthlibPlan plan(ResolvedVersion version) {
        String authlibVersion = authlibVersion(version);
        if (authlibVersion.isBlank()) return ElyByAuthlibPlan.unavailable();

        ElyByAuthlibOverride override = overrides.get(authlibVersion);
        if (override == null && !refreshComplete) {
            refresh();
            override = overrides.get(authlibVersion);
        }
        if (override == null) {
            LauncherLog.info("Ely.by Authlib override is unavailable for com.mojang:authlib:" + authlibVersion);
            return ElyByAuthlibPlan.unavailable();
        }
        return ElyByAuthlibPlan.planned(override);
    }

    private String downloadManifest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(MANIFEST_URL)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "LiteLauncher/" + LauncherState.LAUNCHER_VERSION)
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] body = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        if (body == null || body.length == 0 || body.length > MAX_MANIFEST_BYTES) {
            throw new IllegalStateException("Invalid manifest size: " + (body == null ? 0 : body.length));
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private String authlibVersion(ResolvedVersion version) {
        if (version == null) {
            LauncherLog.info("Ely.by Authlib override is unavailable: Minecraft version is unresolved.");
            return "";
        }

        Set<String> versions = new LinkedHashSet<>();
        for (JsonObject library : version.libraries()) {
            if (!RuleEvaluator.allowed(library, Map.of())) continue;
            MavenName name = MavenName.parse(library.getString("name", ""));
            if (name.valid() && "com.mojang".equals(name.group()) && "authlib".equals(name.artifact())) {
                versions.add(name.version());
            }
        }

        if (versions.isEmpty()) {
            LauncherLog.info("Ely.by Authlib override is unavailable: version has no active com.mojang:authlib library.");
            return "";
        }
        if (versions.size() > 1) {
            LauncherLog.info("Ely.by Authlib override is unavailable: multiple active Authlib versions were found: " + versions);
            return "";
        }
        return versions.iterator().next();
    }

    private Map<String, ElyByAuthlibOverride> loadInitial() {
        Map<String, ElyByAuthlibOverride> cached = readCache();
        if (!cached.isEmpty()) {
            LauncherLog.info("Ely.by Authlib manifest loaded from cache: entries=" + cached.size());
        } else {
            LauncherLog.info("Ely.by Authlib manifest cache is unavailable; waiting for server update.");
        }
        return cached;
    }

    private Map<String, ElyByAuthlibOverride> readCache() {
        Path file = LauncherPaths.elyByAuthlibManifestFile();
        if (!Files.isRegularFile(file)) return Map.of();
        try {
            long size = Files.size(file);
            if (size <= 0L || size > MAX_MANIFEST_BYTES) {
                throw new IllegalStateException("Invalid cached manifest size: " + size);
            }
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            LauncherLog.error("Unable to read cached Ely.by Authlib manifest.", exception);
            BackendUtils.deleteQuietly(file);
            return Map.of();
        }
    }

    private Map<String, ElyByAuthlibOverride> parse(String text) throws Exception {
        JsonObject root = JsonParser.object().from(text);
        int schemaVersion = root.getInt("schemaVersion", 0);
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported Ely.by Authlib manifest schema: " + schemaVersion);
        }

        JsonObject libraries = root.getObject("libraries");
        if (libraries == null) {
            throw new IllegalStateException("Ely.by Authlib manifest has no libraries object.");
        }

        Map<String, ElyByAuthlibOverride> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : libraries.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject json)) continue;

            String sourceVersion = entry.getKey() == null ? "" : entry.getKey().trim();
            String fileName = json.getString("name", "").trim();
            String url = json.getString("url", "").trim();
            String sha1 = json.getString("sha1", "").trim().toLowerCase(Locale.ROOT);
            long size = json.getLong("size", 0L);

            if (!validSourceVersion(sourceVersion)
                    || !validFileName(sourceVersion, fileName)
                    || !validUrl(url)
                    || !sha1.matches("[0-9a-f]{40}")
                    || size <= 0L) continue;

            parsed.put(sourceVersion, new ElyByAuthlibOverride(sourceVersion, fileName, url, sha1, size));
        }

        if (parsed.isEmpty()) throw new IllegalStateException("Ely.by Authlib manifest has no valid entries.");
        return Map.copyOf(parsed);
    }

    private boolean validSourceVersion(String value) {
        return value != null && value.matches("[A-Za-z0-9._+-]+");
    }

    private boolean validFileName(String sourceVersion, String fileName) {
        if (fileName == null || !fileName.matches("authlib-[A-Za-z0-9._+-]+\\.jar")) return false;
        String patchedVersion = fileName.substring("authlib-".length(), fileName.length() - ".jar".length());
        return patchedVersion.startsWith(sourceVersion + "-ely.");
    }

    private boolean validUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null;
        } catch (Exception _) {
            return false;
        }
    }

    private void writeCache(String json) throws Exception {
        BackendUtils.writeAtomic(LauncherPaths.elyByAuthlibManifestFile(), json);
    }

}
