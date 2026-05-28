package net.litelauncher.backend.modules.version;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class VersionService {

    private static final URI VERSION_MANIFEST = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public List<Version> loadVersions() {
        Map<String, LocalVersion> localVersions = readLocalVersions();
        JsonObject cachedManifest = readCachedManifest();
        JsonObject activeManifest = cachedManifest;

        try {
            JsonObject remoteManifest = readRemoteManifest();
            if (cachedManifest == null || latestChanged(cachedManifest, remoteManifest)) {
                writeCachedManifest(remoteManifest);
                activeManifest = remoteManifest;
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to update version manifest.", exception);
        }

        if (activeManifest != null) return mergeManifest(activeManifest, localVersions);
        return localOnly(localVersions);
    }

    public List<Version> loadCachedVersions() {
        Map<String, LocalVersion> localVersions = readLocalVersions();
        JsonObject cachedManifest = readCachedManifest();
        if (cachedManifest != null) return mergeManifest(cachedManifest, localVersions);
        return localOnly(localVersions);
    }

    public List<Version> loadLocalVersions() {
        return localOnly(readLocalVersions());
    }

    public List<Version> refreshLocalVersions(List<Version> currentVersions) {
        Map<String, LocalVersion> localVersions = readLocalVersions();
        Map<String, LocalVersion> allLocalVersions = new HashMap<>(localVersions);
        Map<String, Version> refreshed = new LinkedHashMap<>();

        for (Version version : currentVersions == null ? List.<Version>of() : currentVersions) {
            LocalVersion local = localVersions.remove(version.id());
            if (local == null && version.custom()) continue;
            refreshed.put(version.id(), withLocalState(version, local));
        }

        for (Version version : localOnly(localVersions)) refreshed.putIfAbsent(version.id(), version);
        return orderInheritedCustomVersions(List.copyOf(refreshed.values()), allLocalVersions);
    }

    private Version withLocalState(Version version, LocalVersion local) {
        if (version.custom()) {
            return Version.custom(local.id(), local.type(), true, local.releaseTime(), local.inheritsFrom());
        }

        boolean loaded = local != null;
        boolean modded = local != null && local.modded();
        long releaseTime = version.releaseTime() > 0L ? version.releaseTime() : (local == null ? 0L : local.releaseTime());
        return new Version(version.id(), version.title(), version.subtitle(), version.type(), modded, loaded, false, version.url(), releaseTime);
    }

    private JsonObject readRemoteManifest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(VERSION_MANIFEST)
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Bad manifest response: " + response.statusCode());
        }
        return JsonParser.object().from(response.body());
    }

    private JsonObject readCachedManifest() {
        Path file = OSUtils.versionManifestFile();
        if (!Files.exists(file)) return null;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.object().from(reader);
        } catch (Exception exception) {
            LauncherLog.error("Unable to read cached version manifest.", exception);
            return null;
        }
    }

    private void writeCachedManifest(JsonObject manifest) {
        if (manifest == null) return;

        Path file = OSUtils.versionManifestFile();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, JsonWriter.indent("  ").string().value(manifest).done(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LauncherLog.error("Unable to write cached version manifest.", exception);
        }
    }

    private boolean latestChanged(JsonObject cachedManifest, JsonObject remoteManifest) {
        if (cachedManifest == null || remoteManifest == null) return true;
        return !latestValue(cachedManifest, "release").equals(latestValue(remoteManifest, "release"))
                || !latestValue(cachedManifest, "snapshot").equals(latestValue(remoteManifest, "snapshot"));
    }

    private String latestValue(JsonObject manifest, String key) {
        if (manifest == null || key == null) return "";

        Object latest = manifest.get("latest");
        if (!(latest instanceof JsonObject json)) return "";
        return json.getString(key, "");
    }

    private List<Version> mergeManifest(JsonObject manifest, Map<String, LocalVersion> localVersions) {
        Map<String, Version> versions = new LinkedHashMap<>();
        JsonArray manifestVersions = manifest.getArray("versions", new JsonArray());

        for (Object item : manifestVersions) {
            if (!(item instanceof JsonObject json)) continue;

            String id = json.getString("id");
            if (id == null || id.isBlank()) continue;

            LocalVersion local = localVersions.remove(id);
            Version.Type type = parseType(json.getString("type"));
            long releaseTime = parseTime(json.getString("releaseTime", json.getString("time", "")));
            String url = json.getString("url", "");

            versions.put(id, Version.official(id, type, local != null, local != null && local.modded(), url, releaseTime));
        }

        for (Version version : localOnly(localVersions)) versions.putIfAbsent(version.id(), version);
        return orderInheritedCustomVersions(List.copyOf(versions.values()), localVersions);
    }

    private List<Version> orderInheritedCustomVersions(List<Version> versions, Map<String, LocalVersion> localVersions) {
        Map<String, List<Version>> childrenByParent = new LinkedHashMap<>();
        Set<String> inheritedIds = new HashSet<>();

        for (Version version : versions) {
            LocalVersion local = localVersions.get(version.id());
            if (version.custom() && local != null && !local.inheritsFrom().isBlank()) {
                childrenByParent.computeIfAbsent(local.inheritsFrom(), key -> new ArrayList<>()).add(version);
                inheritedIds.add(version.id());
            }
        }

        if (inheritedIds.isEmpty()) return versions;

        List<Version> ordered = new ArrayList<>();
        for (Version version : versions) {
            if (inheritedIds.contains(version.id())) continue;

            ordered.add(version);
            List<Version> inherited = childrenByParent.remove(version.id());
            if (inherited != null) ordered.addAll(inherited);
        }

        for (List<Version> inherited : childrenByParent.values()) ordered.addAll(inherited);
        return List.copyOf(ordered);
    }

    private List<Version> localOnly(Map<String, LocalVersion> localVersions) {
        List<LocalVersion> local = new ArrayList<>(localVersions.values());
        local.sort(Comparator
                .comparingLong(LocalVersion::releaseTime).reversed()
                .thenComparing(LocalVersion::id));

        List<Version> versions = new ArrayList<>();
        for (LocalVersion version : local) {
            if (version.modded() || version.type() == Version.Type.CUSTOM) {
                versions.add(Version.custom(version.id(), version.type(), true, version.releaseTime(), version.inheritsFrom()));
            } else {
                versions.add(Version.official(version.id(), version.type(), true, false, "", version.releaseTime()));
            }
        }
        return List.copyOf(versions);
    }

    private Map<String, LocalVersion> readLocalVersions() {
        Map<String, LocalVersion> versions = new HashMap<>();
        Path root = OSUtils.versionsDirectory();
        if (!Files.isDirectory(root)) return versions;

        try (Stream<Path> stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(path -> {
                LocalVersion version = readLocalVersion(path);
                versions.put(version.id(), version);
            });
        } catch (Exception exception) {
            LauncherLog.error("Unable to read local versions.", exception);
        }
        return versions;
    }

    private LocalVersion readLocalVersion(Path directory) {
        String fallbackId = directory.getFileName().toString();
        Path jsonFile = directory.resolve(fallbackId + ".json");
        if (!Files.exists(jsonFile)) return new LocalVersion(fallbackId, Version.Type.CUSTOM, true, 0L, "");

        try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.object().from(reader);
            String id = json.getString("id", fallbackId);
            Version.Type type = parseType(json.getString("type", "custom"));
            String inheritsFrom = json.getString("inheritsFrom", "");
            boolean modded = !inheritsFrom.isBlank() || type == Version.Type.CUSTOM;
            long releaseTime = parseTime(json.getString("releaseTime", json.getString("time", "")));
            return new LocalVersion(id, type, modded, releaseTime, inheritsFrom);
        } catch (Exception exception) {
            LauncherLog.error("Unable to read local version: " + fallbackId, exception);
            return new LocalVersion(fallbackId, Version.Type.CUSTOM, true, 0L, "");
        }
    }

    private Version.Type parseType(String type) {
        if (type == null) return Version.Type.CUSTOM;
        return switch (type.toLowerCase()) {
            case "release" -> Version.Type.RELEASE;
            case "snapshot" -> Version.Type.SNAPSHOT;
            case "old_beta" -> Version.Type.BETA;
            case "old_alpha" -> Version.Type.ALPHA;
            default -> Version.Type.CUSTOM;
        };
    }

    private long parseTime(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private record LocalVersion(String id, Version.Type type, boolean modded, long releaseTime, String inheritsFrom) {
        private LocalVersion {
            inheritsFrom = inheritsFrom == null ? "" : inheritsFrom;
        }
    }
}
