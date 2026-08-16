package net.litelauncher.backend.launch;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.version.Version;
import net.litelauncher.backend.version.VersionService;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VersionResolver {

    private final VersionService versionService;
    private final HttpClient http = BackendUtils.http();

    VersionResolver(VersionService versionService) {
        this.versionService = versionService;
    }

    ResolvedVersion resolve(Version selected, LaunchEnvironment environment) throws GameLaunchException {
        if (selected == null) throw new GameLaunchException("Select a Minecraft version before launch.", InformationMessages.SELECT_VERSION);
        if (environment == null || environment.launchVersionId() == null || environment.launchVersionId().isBlank()) {
            throw new GameLaunchException("Version launch id is empty.");
        }
        String selectedId = environment.launchVersionId();
        LauncherLog.info("Resolving version: " + selectedId + (environment.modpack() ? " / modpack=" + selected.id() : ""));
        List<JsonObject> chain = new ArrayList<>();
        resolveJson(selectedId, environment.modpack() ? "" : selected.url(), chain, new HashSet<>(), environment);
        if (chain.isEmpty()) throw new GameLaunchException("Version JSON was not found: " + selectedId);
        return merge(selectedId, chain, environment);
    }

    private void resolveJson(String id, String url, List<JsonObject> chain, Set<String> visited,
                             LaunchEnvironment environment) throws GameLaunchException {
        if (id == null || id.isBlank()) throw new GameLaunchException("Version id is empty.");
        if (!visited.add(id)) throw new GameLaunchException("Version inheritance loop: " + id);

        Path localFile = localVersionJsonPath(environment, id);
        GameLaunchException officialError = null;
        JsonObject json = readLocalJson(localFile);
        if (json != null) LauncherLog.info("Version JSON loaded from disk: " + localFile);

        if (json == null && environment.sharedVersionFallback()) {
            Path globalFile = versionJsonPath(LauncherPaths.versionsDirectory(), id);
            json = readLocalJson(globalFile);
            if (json != null) LauncherLog.info("Version JSON loaded from shared versions: " + globalFile);
        }

        if (json == null) {
            String officialUrl = BackendUtils.firstText(url, "");
            if (officialUrl.isBlank()) officialUrl = findVersionUrl(id);
            try {
                json = readOfficialJson(id, officialUrl, environment);
            } catch (GameLaunchException exception) {
                officialError = exception;
            }
        }
        if (json == null) {
            if (officialError != null) throw officialError;
            throw new GameLaunchException("Version JSON was not found: " + id);
        }

        String parent = json.getString("inheritsFrom");
        if (parent != null && !parent.isBlank()) {
            LauncherLog.info("Version inherits from: " + id + " -> " + parent);
            resolveJson(parent, "", chain, visited, environment);
        }
        chain.add(json);
    }

    private JsonObject readOfficialJson(String id, String url, LaunchEnvironment environment) throws GameLaunchException {
        if (url == null || url.isBlank()) return null;
        try {
            Path targetVersions = environment.instance() ? environment.versionsDirectory() : LauncherPaths.versionsDirectory();
            JsonObject json = downloadJson(id, url, targetVersions);
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
            return JsonParser.object().from(reader);
        } catch (Exception exception) {
            LauncherLog.error("Unable to read local version JSON: " + file, exception);
            return null;
        }
    }

    private JsonObject downloadJson(String id, String url, Path versionsDirectory) throws GameLaunchException {
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
            Path file = versionJsonPath(versionsDirectory, id);
            BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(json).done());
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
        return versionService.findOfficialVersionUrl(id);
    }

    private ResolvedVersion merge(String selectedId, List<JsonObject> chain, LaunchEnvironment environment) {
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
        int javaMajor = OSUtils.currentJavaMajor();
        JsonObject clientDownload = null;
        JsonObject assetIndex = null;
        String minecraftArguments = "";

        for (JsonObject json : chain) {
            String jsonId = json.getString("id", id);
            id = jsonId;
            long jsonReleaseTime = BackendUtils.parseTime(json.getString("releaseTime", json.getString("time", "")));

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
                String key = libraryKey(library);
                if (!key.isBlank()) libraries.put(key, library);
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
                List.copyOf(libraries.values()), jvmArguments, gameArguments, minecraftArguments, type, clientType, clientReleaseTime, environment);
    }

    private String libraryKey(JsonObject library) {
        String name = library == null ? "" : library.getString("name", "");
        if (name.isBlank()) return "";

        MavenName maven = MavenName.parse(name);
        if (!maven.valid()) return name;

        return maven.moduleKey() + "|" + rulesKey(library);
    }

    private String rulesKey(JsonObject library) {
        JsonArray rules = library == null ? null : library.getArray("rules");
        if (rules == null || rules.isEmpty()) return "rules:";
        return "rules:" + JsonWriter.string().value(rules).done();
    }

    private void append(JsonArray target, JsonArray source) {
        if (source == null) return;
        target.addAll(source);
    }

    private Path localVersionJsonPath(LaunchEnvironment environment, String id) throws GameLaunchException {
        return versionJsonPath(environment.versionsDirectory(), id);
    }

    private Path versionJsonPath(Path versionsDirectory, String id) throws GameLaunchException {
        String value = id == null ? "" : id;
        if (value.isBlank()) throw new GameLaunchException("Invalid version path.");
        try {
            return BackendUtils.safeResolve(versionsDirectory, value + "/" + value + ".json");
        } catch (IOException exception) {
            throw new GameLaunchException("Invalid version path.", exception);
        }
    }


}
