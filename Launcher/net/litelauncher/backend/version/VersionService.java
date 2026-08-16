package net.litelauncher.backend.version;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.modpack.ModpackService;

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

    private final ModpackService modpacks;
    private final HttpClient http = BackendUtils.http();

    public VersionService(ModpackService modpacks) {
        this.modpacks = modpacks;
    }

    public List<Version> loadVersions(boolean instancesStorageSystem) {
        Map<String, LocalVersion> localVersions = readLocalVersions(instancesStorageSystem);
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

        List<Version> normal = activeManifest != null
                ? mergeManifest(activeManifest, localVersions)
                : localOnly(localVersions);
        return withModpacks(normal, activeManifest);
    }

    public List<Version> loadCachedVersions(boolean instancesStorageSystem) {
        Map<String, LocalVersion> localVersions = readLocalVersions(instancesStorageSystem);
        JsonObject cachedManifest = readCachedManifest();
        List<Version> normal = cachedManifest != null
                ? mergeManifest(cachedManifest, localVersions)
                : localOnly(localVersions);
        return withModpacks(normal, cachedManifest);
    }

    public List<Version> refreshLocalVersions(List<Version> currentVersions, boolean instancesStorageSystem) {
        Map<String, LocalVersion> localVersions = readLocalVersions(instancesStorageSystem);
        Map<String, LocalVersion> allLocalVersions = new HashMap<>(localVersions);
        Map<String, Version> refreshed = new LinkedHashMap<>();

        for (Version version : currentVersions == null ? List.<Version>of() : currentVersions) {
            if (version.modpack()) continue;
            LocalVersion local = localVersions.remove(version.id());
            if (local == null && version.custom()) continue;
            refreshed.put(version.id(), withLocalState(version, local));
        }

        for (Version version : localOnly(localVersions)) refreshed.putIfAbsent(version.id(), version);
        List<Version> normal = orderInheritedCustomVersions(List.copyOf(refreshed.values()), allLocalVersions);
        return withModpacks(normal, readCachedManifest());
    }

    public void deleteInstalledVersion(Version version, boolean instancesStorageSystem) throws IOException {
        if (version == null || version.id().isBlank()) return;
        if (version.modpack()) {
            modpacks.delete(version.id());
            return;
        }
        if (instancesStorageSystem) {
            deleteInstance(version.id());
            return;
        }

        List<String> profileVersionIds = deleteVersionDirectories(version.id());
        for (String versionId : profileVersionIds) {
            removeProfileVersion(LauncherPaths.launcherProfileFile(), versionId);
            removeProfileVersion(LauncherPaths.launcherProfileMicrosoftStoreFile(), versionId);
        }
    }

    private void deleteInstance(String versionId) throws IOException {
        Path root = LauncherPaths.instancesDirectory().toAbsolutePath().normalize();
        Path target;
        try {
            target = LauncherPaths.instanceDirectory(versionId).toAbsolutePath().normalize();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid instance id.", exception);
        }
        if (!target.startsWith(root) || target.equals(root) || !Files.exists(target)) return;

        BackendUtils.deleteTree(target);
        LauncherLog.info("Instance deleted: " + versionId);
    }

    private List<String> deleteVersionDirectories(String versionId) throws IOException {
        List<String> deletedIds = new ArrayList<>();
        deletedIds.add(versionId);

        Path root = LauncherPaths.versionsDirectory();
        deleteVersionDirectory(root.resolve(versionId));
        if (!Files.isDirectory(root)) return deletedIds;

        try (Stream<Path> stream = Files.list(root)) {
            for (Path directory : stream.filter(Files::isDirectory).toList()) {
                if (!versionId.equals(readVersionDirectoryId(directory))) continue;
                deletedIds.add(directory.getFileName().toString());
                deleteVersionDirectory(directory);
            }
        }

        return deletedIds.stream().distinct().toList();
    }

    private String readVersionDirectoryId(Path directory) {
        String fallbackId = directory.getFileName().toString();
        Path jsonFile = directory.resolve(fallbackId + ".json");
        if (!Files.exists(jsonFile)) return fallbackId;

        try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            return JsonParser.object().from(reader).getString("id", fallbackId);
        } catch (Exception _) {
            return fallbackId;
        }
    }

    private void deleteVersionDirectory(Path directory) throws IOException {
        Path root = LauncherPaths.versionsDirectory().toAbsolutePath().normalize();
        Path target = directory.toAbsolutePath().normalize();
        if (!target.startsWith(root) || target.equals(root) || !Files.exists(target)) return;

        BackendUtils.deleteTree(target);
    }

    private void removeProfileVersion(Path file, String versionId) {
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.object().from(reader);
            if (removeProfileVersion(json, versionId)) writeProfileFile(file, json);
        } catch (Exception exception) {
            LauncherLog.error("Unable to update launcher profile file: " + file, exception);
        }
    }

    private boolean removeProfileVersion(JsonObject json, String versionId) {
        String selectedProfile = json.getString("selectedProfile", "");
        Object profilesValue = json.get("profiles");

        boolean changed = false;
        boolean selectedRemoved = versionId.equals(selectedProfile);
        if (profilesValue instanceof JsonObject profiles) {
            changed = profiles.remove(versionId) != null;
            List<String> keys = new ArrayList<>(profiles.keySet());
            for (String key : keys) {
                Object value = profiles.get(key);
                if (!(value instanceof JsonObject profile)) continue;
                if (versionId.equals(profile.getString("lastVersionId", ""))) {
                    profiles.remove(key);
                    selectedRemoved |= key.equals(selectedProfile);
                    changed = true;
                }
            }
        }

        if (selectedRemoved) {
            json.remove("selectedProfile");
            changed = true;
        }
        return changed;
    }

    private void writeProfileFile(Path file, JsonObject json) throws IOException {
        Files.createDirectories(file.getParent());
        BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(json).done());
    }

    private Version withLocalState(Version version, LocalVersion local) {
        if (version.pendingLoader()) {
            if (local == null) return version;
            long releaseTime = version.releaseTime() > 0L ? version.releaseTime() : local.releaseTime();
            return Version.modified(version.loader(), true, releaseTime);
        }
        if (version.custom()) {
            if (local == null) return version;
            return Version.custom(local.id(), local.type(), true, local.releaseTime(), local.inheritsFrom(), local.loaderKind());
        }

        boolean loaded = local != null;
        boolean modded = local != null && local.modded();
        long releaseTime = version.releaseTime() > 0L ? version.releaseTime() : (local == null ? 0L : local.releaseTime());
        return new Version(version.id(), version.title(), version.subtitle(), version.type(), modded, loaded, false,
                version.url(), releaseTime, version.loader());
    }

    private List<Version> withModpacks(List<Version> normalVersions, JsonObject manifest) {
        Map<String, Long> releaseTimes = minecraftReleaseTimes(manifest);
        List<Version> combined = new ArrayList<>(normalVersions == null ? List.of() : normalVersions);
        Set<String> ids = new HashSet<>();
        for (Version version : combined) ids.add(version.id());
        for (Version modpack : modpacks.loadVersions(releaseTimes)) {
            if (!ids.add(modpack.id())) {
                LauncherLog.info("Skipping modpack with conflicting version id: " + modpack.id());
                continue;
            }

            combined.add(modpackInsertionIndex(combined, modpack.releaseTime()), modpack);
        }
        return List.copyOf(combined);
    }

    private int modpackInsertionIndex(List<Version> versions, long modpackReleaseTime) {
        long effectiveReleaseTime = Long.MAX_VALUE;
        for (int index = 0; index < versions.size(); index++) {
            Version version = versions.get(index);
            long releaseTime = version.releaseTime();
            if (!version.custom() && releaseTime > 0L) effectiveReleaseTime = releaseTime;
            if (effectiveReleaseTime < modpackReleaseTime) return index;
        }
        return versions.size();
    }

    private Map<String, Long> minecraftReleaseTimes(JsonObject manifest) {
        Map<String, Long> result = new HashMap<>();
        if (manifest == null) return result;
        for (Object item : manifest.getArray("versions", new JsonArray())) {
            if (!(item instanceof JsonObject json)) continue;
            String id = json.getString("id", "");
            if (id.isBlank()) continue;
            result.put(id, BackendUtils.parseTime(json.getString("releaseTime", json.getString("time", ""))));
        }
        return result;
    }

    public String findOfficialVersionUrl(String id) {
        if (id == null || id.isBlank()) return "";
        JsonObject manifest = readCachedManifest();
        String url = versionUrl(manifest, id);
        if (!url.isBlank()) return url;

        try {
            manifest = readRemoteManifest();
            writeCachedManifest(manifest);
            return versionUrl(manifest, id);
        } catch (Exception exception) {
            LauncherLog.error("Unable to resolve official version URL: " + id, exception);
            return "";
        }
    }

    private String versionUrl(JsonObject manifest, String id) {
        if (manifest == null || id == null || id.isBlank()) return "";
        for (Object item : manifest.getArray("versions", new JsonArray())) {
            if (!(item instanceof JsonObject json)) continue;
            if (id.equals(json.getString("id", ""))) return json.getString("url", "");
        }
        return "";
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
        Path file = LauncherPaths.versionManifestFile();
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

        Path file = LauncherPaths.versionManifestFile();
        try {
            Files.createDirectories(file.getParent());
            BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(manifest).done());
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
            long releaseTime = BackendUtils.parseTime(json.getString("releaseTime", json.getString("time", "")));
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
                childrenByParent.computeIfAbsent(local.inheritsFrom(), _ -> new ArrayList<>()).add(version);
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
                versions.add(Version.custom(version.id(), version.type(), true, version.releaseTime(), version.inheritsFrom(), version.loaderKind()));
            } else {
                versions.add(Version.official(version.id(), version.type(), true, false, "", version.releaseTime()));
            }
        }
        return List.copyOf(versions);
    }

    private Map<String, LocalVersion> readLocalVersions(boolean instancesStorageSystem) {
        return instancesStorageSystem ? readInstanceVersions() : readStandardVersions();
    }

    private Map<String, LocalVersion> readStandardVersions() {
        Map<String, LocalVersion> versions = new HashMap<>();
        Path root = LauncherPaths.versionsDirectory();
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

    private Map<String, LocalVersion> readInstanceVersions() {
        Map<String, LocalVersion> versions = new HashMap<>();
        Path root = LauncherPaths.instancesDirectory();
        if (!Files.isDirectory(root)) return versions;

        try (Stream<Path> stream = Files.list(root)) {
            for (Path instance : stream.filter(Files::isDirectory).toList()) {
                String id = instance.getFileName().toString();
                Path versionDirectory = instance.resolve("versions").resolve(id);
                Path jsonFile = versionDirectory.resolve(id + ".json");
                if (!Files.isRegularFile(jsonFile)) continue;

                LocalVersion version = readValidInstanceVersion(versionDirectory, id);
                if (version != null) versions.put(id, version);
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to read instance versions.", exception);
        }
        return versions;
    }

    private LocalVersion readValidInstanceVersion(Path directory, String expectedId) {
        Path jsonFile = directory.resolve(expectedId + ".json");
        try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.object().from(reader);
            String id = json.getString("id", expectedId);
            if (!expectedId.equals(id)) {
                LauncherLog.info("Ignoring instance with mismatched version id: " + expectedId + " -> " + id);
                return null;
            }
            Version.Type type = parseType(json.getString("type", "custom"));
            String inheritsFrom = json.getString("inheritsFrom", "");
            String loaderKind = detectLoaderKind(id, json);
            boolean modded = !inheritsFrom.isBlank() || type == Version.Type.CUSTOM || !loaderKind.isBlank();
            long releaseTime = BackendUtils.parseTime(json.getString("releaseTime", json.getString("time", "")));
            return new LocalVersion(id, type, modded, releaseTime, inheritsFrom, loaderKind);
        } catch (Exception exception) {
            LauncherLog.error("Unable to read instance version: " + expectedId, exception);
            return null;
        }
    }

    private LocalVersion readLocalVersion(Path directory) {
        String fallbackId = directory.getFileName().toString();
        Path jsonFile = directory.resolve(fallbackId + ".json");
        if (!Files.exists(jsonFile)) return new LocalVersion(fallbackId, Version.Type.CUSTOM, true, 0L, "", "");

        try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.object().from(reader);
            String id = json.getString("id", fallbackId);
            Version.Type type = parseType(json.getString("type", "custom"));
            String inheritsFrom = json.getString("inheritsFrom", "");
            String loaderKind = detectLoaderKind(id, json);
            boolean modded = !inheritsFrom.isBlank() || type == Version.Type.CUSTOM || !loaderKind.isBlank();
            long releaseTime = BackendUtils.parseTime(json.getString("releaseTime", json.getString("time", "")));
            return new LocalVersion(id, type, modded, releaseTime, inheritsFrom, loaderKind);
        } catch (Exception exception) {
            LauncherLog.error("Unable to read local version: " + fallbackId, exception);
            return new LocalVersion(fallbackId, Version.Type.CUSTOM, true, 0L, "", "");
        }
    }

    private String detectLoaderKind(String id, JsonObject json) {
        boolean forge = false;
        for (Object item : json.getArray("libraries", new JsonArray())) {
            if (!(item instanceof JsonObject library)) continue;
            String name = library.getString("name", "").toLowerCase();
            if (name.startsWith("net.neoforged:")) return "NeoForge";
            if (name.startsWith("net.fabricmc:fabric-loader:")) return "Fabric";
            if (name.startsWith("org.quiltmc:quilt-loader:")) return "Quilt";
            if (name.startsWith("optifine:optifine:")) return "OptiFine";
            if (name.startsWith("net.minecraftforge:forge:")) forge = true;
        }
        if (forge) return "Forge";
        String lower = id == null ? "" : id.toLowerCase();
        if (lower.contains("optifine")) return "OptiFine";
        if (lower.contains("neoforge")) return "NeoForge";
        if (lower.contains("fabric")) return "Fabric";
        if (lower.contains("quilt")) return "Quilt";
        if (lower.contains("forge")) return "Forge";
        return "";
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


    private record LocalVersion(String id, Version.Type type, boolean modded, long releaseTime, String inheritsFrom, String loaderKind) {
        private LocalVersion {
            inheritsFrom = inheritsFrom == null ? "" : inheritsFrom;
            loaderKind = loaderKind == null ? "" : loaderKind;
        }
    }
}
