package net.litelauncher.backend.modules.launch;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.modules.version.Version;
import net.litelauncher.backend.modules.version.VersionService;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VersionResolver {

    private final VersionService versionService;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    VersionResolver(VersionService versionService) {
        this.versionService = versionService;
    }

    ResolvedVersion resolve(Version selected) throws GameLaunchException {
        if (selected == null) throw new GameLaunchException("Select a Minecraft version before launch.", InformationMessages.SELECT_VERSION);
        LauncherLog.info("Resolving version: " + selected.id());
        List<JsonObject> chain = new ArrayList<>();
        resolveJson(selected.id(), selected.url(), chain, new HashSet<>());
        if (chain.isEmpty()) throw new GameLaunchException("Version JSON was not found: " + selected.id());
        return merge(selected.id(), chain);
    }

    private void resolveJson(String id, String url, List<JsonObject> chain, Set<String> visited) throws GameLaunchException {
        if (id == null || id.isBlank()) throw new GameLaunchException("Version id is empty.");
        if (!visited.add(id)) throw new GameLaunchException("Version inheritance loop: " + id);

        Path localFile = versionJsonPath(id);
        String officialUrl = firstText(url, findVersionUrl(id));
        GameLaunchException officialError = null;
        JsonObject json = null;
        try {
            json = readOfficialJson(id, officialUrl);
        } catch (GameLaunchException exception) {
            officialError = exception;
        }
        if (json == null) {
            json = readLocalJson(localFile);
            if (json != null) LauncherLog.info("Version JSON loaded from disk: " + localFile);
        }
        if (json == null) {
            if (officialError != null) throw officialError;
            throw new GameLaunchException("Version JSON was not found: " + id);
        }

        String parent = json.getString("inheritsFrom");
        if (parent != null && !parent.isBlank()) {
            LauncherLog.info("Version inherits from: " + id + " -> " + parent);
            resolveJson(parent, findVersionUrl(parent), chain, visited);
        }
        chain.add(json);
    }

    private JsonObject readOfficialJson(String id, String url) throws GameLaunchException {
        if (url == null || url.isBlank()) return null;
        try {
            JsonObject json = downloadJson(id, url);
            LauncherLog.info("Version JSON loaded from official source: " + id);
            return json;
        } catch (GameLaunchException exception) {
            LauncherLog.error("Unable to refresh official version JSON, using local copy if possible: " + id, exception);
            throw exception;
        }
    }

    private JsonObject readLocalJson(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.object().from(reader);
            cleanupLegacyPatchFields(file, json);
            return json;
        } catch (Exception exception) {
            LauncherLog.error("Unable to read local version JSON: " + file, exception);
            return null;
        }
    }

    private void cleanupLegacyPatchFields(Path file, JsonObject json) {
        boolean changed = false;
        for (String key : List.of("patchedID", "patchedSHA1", "patchedSHA", "patchJsonId")) {
            if (!json.containsKey(key)) continue;
            json.remove(key);
            changed = true;
        }
        if (!changed) return;

        try {
            Files.writeString(file, JsonWriter.indent("  ").string().value(json).done(), StandardCharsets.UTF_8);
            LauncherLog.info("Removed legacy LiteLauncher patch fields from version JSON: " + file);
        } catch (Exception exception) {
            LauncherLog.error("Unable to clean legacy LiteLauncher patch fields: " + file, exception);
        }
    }

    private JsonObject downloadJson(String id, String url) throws GameLaunchException {
        if (url == null || url.isBlank()) return null;
        try {
            LauncherLog.info("Downloading version JSON: " + id + " <- " + url);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "LiteLauncher/" + System.getProperty("java.version", "java"))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LauncherLog.info("Version JSON HTTP " + response.statusCode() + ": " + url);
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("HTTP " + response.statusCode());
            JsonObject json = JsonParser.object().from(response.body());
            Path file = versionJsonPath(id);
            Files.createDirectories(file.getParent());
            Files.writeString(file, JsonWriter.indent("  ").string().value(json).done(), StandardCharsets.UTF_8);
            LauncherLog.info("Version JSON saved: " + file);
            return json;
        } catch (GameLaunchException exception) {
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Unable to download version JSON: " + id + " <- " + url, exception);
            throw new GameLaunchException("Unable to download version JSON: " + id, exception, InformationMessages.DOWNLOAD_ERROR);
        }
    }

    private String findVersionUrl(String id) {
        for (Version version : versionService.loadCachedVersions()) if (id.equals(version.id())) return version.url();
        for (Version version : versionService.loadVersions()) if (id.equals(version.id())) return version.url();
        return "";
    }

    private ResolvedVersion merge(String selectedId, List<JsonObject> chain) {
        Map<String, JsonObject> libraries = new LinkedHashMap<>();
        JsonArray jvmArguments = new JsonArray();
        JsonArray gameArguments = new JsonArray();
        String id = selectedId;
        String jarId = "";
        String clientDownloadId = "";
        boolean jarExplicit = false;
        String mainClass = "";
        String assets = "legacy";
        String type = "release";
        String clientType = "release";
        long clientReleaseTime = 0L;
        int javaMajor = currentJavaMajor();
        JsonObject clientDownload = null;
        JsonObject assetIndex = null;
        String minecraftArguments = "";

        for (JsonObject json : chain) {
            String jsonId = json.getString("id", id);
            id = jsonId;
            long jsonReleaseTime = parseTime(json.getString("releaseTime", json.getString("time", "")));

            if (json.containsKey("jar")) {
                jarId = json.getString("jar", jarId);
                jarExplicit = true;
            }

            type = json.getString("type", type);
            mainClass = json.getString("mainClass", mainClass);
            assets = json.getString("assets", assets);
            if (json.getObject("assetIndex") != null) assetIndex = json.getObject("assetIndex");

            JsonObject client = json.getObject("downloads", new JsonObject()).getObject("client");
            if (client != null) {
                clientDownload = client;
                clientDownloadId = jsonId;
                clientType = json.getString("type", clientType);
                if (jsonReleaseTime > 0) clientReleaseTime = jsonReleaseTime;
                if (!jarExplicit) jarId = jsonId;
            }

            JsonObject javaVersion = json.getObject("javaVersion");
            if (javaVersion != null) javaMajor = Math.max(1, javaVersion.getInt("majorVersion", javaMajor));

            for (Object item : json.getArray("libraries", new JsonArray())) {
                if (!(item instanceof JsonObject library)) continue;
                String name = library.getString("name", "");
                if (!name.isBlank()) libraries.put(name, library);
            }

            JsonObject arguments = json.getObject("arguments");
            if (arguments != null) {
                append(jvmArguments, arguments.getArray("jvm"));
                append(gameArguments, arguments.getArray("game"));
            }

            String oldArguments = json.getString("minecraftArguments");
            if (oldArguments != null && !oldArguments.isBlank()) minecraftArguments = oldArguments;
        }

        if (jarId.isBlank()) jarId = !clientDownloadId.isBlank() ? clientDownloadId : selectedId;
        LauncherLog.info("Version merge result: selected=" + selectedId + ", id=" + id + ", jar=" + jarId + ", clientDownload=" + clientDownloadId + ", mainClass=" + mainClass + ", assets=" + assets + ", java=" + javaMajor + ", libraries=" + libraries.size() + ", clientType=" + clientType + ", clientReleaseTime=" + clientReleaseTime);
        return new ResolvedVersion(selectedId, id, jarId, clientDownloadId, jarExplicit, mainClass, assets, javaMajor, clientDownload, assetIndex,
                List.copyOf(libraries.values()), jvmArguments, gameArguments, minecraftArguments, type, clientType, clientReleaseTime);
    }

    private void append(JsonArray target, JsonArray source) {
        if (source == null) return;
        for (Object item : source) target.add(item);
    }

    private Path versionJsonPath(String id) throws GameLaunchException {
        String value = id == null ? "" : id;
        if (value.isBlank()) throw new GameLaunchException("Invalid version path.");
        Path root = OSUtils.versionsDirectory().toAbsolutePath().normalize();
        Path out = root.resolve(value).resolve(value + ".json").toAbsolutePath().normalize();
        if (!out.startsWith(root) || out.equals(root)) throw new GameLaunchException("Invalid version path.");
        return out;
    }

    private int currentJavaMajor() {
        String version = System.getProperty("java.version", "17");
        try {
            if (version.startsWith("1.")) return Integer.parseInt(version.substring(2, 3));
            int dot = version.indexOf('.');
            String major = dot < 0 ? version : version.substring(0, dot);
            return Math.max(1, Integer.parseInt(major));
        } catch (Exception ignored) {
            return 17;
        }
    }

    private long parseTime(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String firstText(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }
}
