package net.litelauncher.backend.launch;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.i18n.I18n;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LaunchArtifacts {

    private static final String LIBRARIES_URL = "https://libraries.minecraft.net/";

    List<DownloadFile> libraryDownloads(ResolvedVersion version) throws GameLaunchException {
        List<DownloadFile> files = new ArrayList<>();
        for (JsonObject library : version.libraries()) {
            if (!RuleEvaluator.allowed(library, Map.of())) continue;

            JsonObject artifact = library.getObject("downloads", new JsonObject()).getObject("artifact");
            JsonObject nativeArtifact = nativeArtifact(library);

            if (artifact != null) {
                files.add(downloadFile(artifact, library.getString("url", LIBRARIES_URL), library.getString("name", ""), I18n.text(I18n.currentLanguage(), "progress.downloadingLibraries")));
            } else if (nativeArtifact == null) {
                addMavenLibrary(files, library);
            } else {
                LauncherLog.info("Native-only library has no normal artifact: " + library.getString("name", ""));
            }

            if (nativeArtifact != null) {
                files.add(downloadFile(nativeArtifact, library.getString("url", LIBRARIES_URL), library.getString("name", ""), I18n.text(I18n.currentLanguage(), "progress.downloadingNatives")));
            }
        }
        return files;
    }

    List<NativeLibraryExtractor.NativeLibrary> nativeLibraries(ResolvedVersion version) throws GameLaunchException {
        List<NativeLibraryExtractor.NativeLibrary> result = new ArrayList<>();
        for (JsonObject library : version.libraries()) {
            if (!RuleEvaluator.allowed(library, Map.of())) continue;
            JsonObject artifact = nativeArtifact(library);
            if (artifact != null) result.add(new NativeLibraryExtractor.NativeLibrary(libraryPath(artifact.getString("path", "")), excludes(library)));
        }
        return result;
    }

    LaunchClasspath classpath(ResolvedVersion version, ClientJarPlan client, ElyByAuthlibPlan elyByAuthlib) throws GameLaunchException {
        List<LaunchClasspath.Entry> entries = new ArrayList<>();
        boolean elyByAuthlibAdded = false;
        for (JsonObject library : version.libraries()) {
            if (!RuleEvaluator.allowed(library, Map.of())) continue;

            JsonObject artifact = library.getObject("downloads", new JsonObject()).getObject("artifact");
            if (artifact == null && nativeArtifact(library) != null) continue;

            MavenName name = MavenName.parse(library.getString("name", ""));
            if (elyByAuthlib != null && elyByAuthlib.active() && isSourceAuthlib(name, elyByAuthlib.sourceVersion())) {
                if (!elyByAuthlibAdded) {
                    Path file = elyByAuthlib.runtimePath();
                    if (!Files.isRegularFile(file)) throw new GameLaunchException("Ely.by Authlib is missing: " + file.getFileName());
                    entries.add(new LaunchClasspath.Entry(file, LaunchClasspath.Role.LIBRARY));
                    elyByAuthlibAdded = true;
                }
                continue;
            }

            String path = artifact == null
                    ? mavenPath(library.getString("name", ""))
                    : artifact.getString("path", mavenPath(library.getString("name", "")));
            if (path.isBlank()) continue;

            Path file = libraryPath(path);
            if (!Files.isRegularFile(file)) throw new GameLaunchException("Library is missing: " + file.getFileName());
            entries.add(new LaunchClasspath.Entry(file, LaunchClasspath.Role.LIBRARY));
        }

        Path clientJar = client.runtimePath();
        if (!Files.isRegularFile(clientJar)) throw new GameLaunchException("Client jar is missing: " + clientJar.getFileName());
        entries.add(new LaunchClasspath.Entry(clientJar, LaunchClasspath.Role.CLIENT_RUNTIME));
        return new LaunchClasspath(entries);
    }

    private boolean isSourceAuthlib(MavenName name, String version) {
        return name != null && name.valid()
                && "com.mojang".equals(name.group())
                && "authlib".equals(name.artifact())
                && name.version().equals(version);
    }

    private void addMavenLibrary(List<DownloadFile> files, JsonObject library) throws GameLaunchException {
        String path = mavenPath(library.getString("name", ""));
        if (!path.isBlank()) files.add(new DownloadFile(joinUrl(library.getString("url", LIBRARIES_URL), path), libraryPath(path), "", 0L, I18n.text(I18n.currentLanguage(), "progress.downloadingLibraries")));
    }

    private DownloadFile downloadFile(JsonObject artifact, String baseUrl, String name, String label) throws GameLaunchException {
        String path = libraryPathText(artifact.getString("path", mavenPath(name)));
        String url = BackendUtils.firstText(artifact.getString("url"), joinUrl(baseUrl, path));
        return new DownloadFile(url, libraryPath(path), artifact.getString("sha1", ""), artifact.getLong("size", 0L), label);
    }

    private JsonObject nativeArtifact(JsonObject library) {
        String classifier = nativeClassifier(library);
        if (classifier.isBlank()) return null;

        String path = mavenPath(library.getString("name", "") + ":" + classifier);
        if (path.isBlank()) return null;

        JsonObject artifact = library.getObject("downloads", new JsonObject()).getObject("classifiers", new JsonObject()).getObject(classifier);
        if (artifact != null) {
            if (artifact.getString("path", "").isBlank()) artifact.put("path", path);
            if (artifact.getString("url", "").isBlank()) artifact.put("url", joinUrl(library.getString("url", LIBRARIES_URL), path));
            return artifact;
        }

        JsonObject fallback = new JsonObject();
        fallback.put("path", path);
        fallback.put("url", joinUrl(library.getString("url", LIBRARIES_URL), path));
        return fallback;
    }

    private String nativeClassifier(JsonObject library) {
        JsonObject natives = library.getObject("natives");
        if (natives == null) return "";
        return natives.getString(RuleEvaluator.osName(), "").replace("${arch}", OSUtils.is64Bit() ? "64" : "32");
    }

    private Set<String> excludes(JsonObject library) {
        Set<String> excludes = new HashSet<>();
        JsonArray array = library.getObject("extract", new JsonObject()).getArray("exclude", new JsonArray());
        for (Object item : array) if (item instanceof String value) excludes.add(value);
        excludes.add("META-INF/");
        return excludes;
    }

    private Path libraryPath(String path) throws GameLaunchException {
        return safeResolve(LauncherPaths.librariesDirectory(), libraryPathText(path), "Invalid library path.");
    }

    private Path safeResolve(Path root, String relative, String error) throws GameLaunchException {
        try {
            return BackendUtils.safeResolve(root, relative);
        } catch (Exception exception) {
            throw new GameLaunchException(error, exception);
        }
    }

    private String libraryPathText(String path) {
        String value = path == null ? "" : path.replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        if (value.startsWith("libraries/")) value = value.substring("libraries/".length());
        return value;
    }

    private String mavenPath(String name) {
        MavenName maven = MavenName.parse(name);
        return maven.valid() ? maven.path() : "";
    }

    private String joinUrl(String base, String path) {
        String url = base == null || base.isBlank() ? LIBRARIES_URL : base;
        return (url.endsWith("/") ? url : url + "/") + path;
    }


}
