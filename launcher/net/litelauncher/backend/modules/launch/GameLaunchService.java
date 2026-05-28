package net.litelauncher.backend.modules.launch;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.modules.auth.AuthException;
import net.litelauncher.backend.modules.auth.AuthService;
import net.litelauncher.backend.modules.auth.LaunchAccount;
import net.litelauncher.backend.modules.auth.Profile;
import net.litelauncher.backend.modules.download.DownloadFile;
import net.litelauncher.backend.modules.download.DownloadService;
import net.litelauncher.backend.modules.java.JavaRuntimeService;
import net.litelauncher.backend.modules.version.Version;
import net.litelauncher.backend.modules.version.VersionService;
import net.litelauncher.backend.platform.OSUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GameLaunchService {

    private static final String LIBRARIES_URL = "https://libraries.minecraft.net/";
    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

    private final AuthService authService;
    private final VersionResolver versions;
    private final DownloadService downloads = new DownloadService();
    private final JavaRuntimeService javaRuntime = new JavaRuntimeService(downloads);
    private final GameArgumentBuilder arguments = new GameArgumentBuilder();
    private final ClientJarPatcher clientPatcher = new ClientJarPatcher();
    private final LegacyOptionsFixer legacyOptions = new LegacyOptionsFixer();

    public GameLaunchService(AuthService authService, VersionService versionService) {
        this.authService = authService;
        this.versions = new VersionResolver(versionService);
    }

    public LaunchResult launch(Profile profile, Version version, LauncherState state, LaunchProgress progress) throws GameLaunchException, AuthException {
        LauncherLog.info("Launch started");
        LauncherCompatibility.ensureProfileFiles();
        LauncherLog.info("Selected profile: " + (profile == null ? "null" : profile.username() + " / " + profile.id() + " / microsoft=" + profile.microsoft()));
        LauncherLog.info("Selected version: " + (version == null ? "null" : version.id() + " / " + version.url()));
        if (profile == null) throw new GameLaunchException("Create or select a profile before launch.", InformationMessages.SELECT_PROFILE);
        if (version == null) throw new GameLaunchException("Select a Minecraft version before launch.", InformationMessages.SELECT_VERSION);

        progress.update(0.01, "Preparing launch...", version.id());
        LaunchAccount account = authService.prepareLaunchAccount(profile);
        LauncherLog.info("Launch account prepared: username=" + account.username() + ", uuid=" + account.uuid() + ", userType=" + account.userType());

        ResolvedVersion resolved = versions.resolve(version);
        if (resolved.mainClass() == null || resolved.mainClass().isBlank()) throw new GameLaunchException("Version has no mainClass: " + resolved.id());
        LauncherLog.info("Version resolved: id=" + resolved.id() + ", jar=" + resolved.jarId() + ", clientDownload=" + resolved.clientDownloadId() + ", java=" + resolved.javaMajor() + ", libraries=" + resolved.libraries().size());

        ClientJar originalClient = launchClientJar(resolved);
        boolean shouldPatch = clientPatcher.needsPatch(resolved);
        Path clientJar = originalClient.path();
        LauncherLog.info("Client jar: id=" + originalClient.id() + ", path=" + originalClient.path() + ", patch=" + shouldPatch);

        DownloadFile assetIndexFile = assetIndexDownload(resolved);
        if (assetIndexFile != null && !downloads.isValid(assetIndexFile)) downloads.download(List.of(assetIndexFile), scaledDownloads(progress, 0.02, 0.08));
        JsonObject assetIndex = assetIndexFile != null && Files.isRegularFile(assetIndexFile.path()) ? readJson(assetIndexFile.path()) : null;

        List<DownloadFile> files = new ArrayList<>();
        addClient(files, resolved, originalClient);
        addLibraries(files, resolved);
        if (assetIndexFile != null) {
            files.add(assetIndexFile);
            addAssets(files, assetIndex);
        }

        LauncherLog.info("Files queued for validation/download: " + files.size());
        downloads.download(files, scaledDownloads(progress, 0.08, 0.84));
        mapLegacyAssets(assetIndex);

        if (shouldPatch) {
            progress.update(0.86, "Patching old client...", resolved.id());
            clientJar = clientPatcher.ensurePatched(originalClient.path(), clientSha1(resolved, originalClient));
        }

        progress.update(0.88, "Preparing natives...", resolved.id());
        Path natives = extractNatives(resolved);
        LauncherLog.info("Natives ready at: " + natives);

        Path java = javaRuntime.ensureJava(resolved.javaMajor(), scaledLaunch(progress, 0.90, 0.98));
        LauncherLog.info("Java executable: " + java);

        legacyOptions.fixIfNeeded(resolved);
        String classpath = classpath(resolved, clientJar);
        List<String> command = arguments.build(java, resolved, assetIndex, account, state, natives, classpath);
        LauncherLog.info("Command prepared: args=" + command.size() + ", classpathLength=" + classpath.length());

        progress.update(0.99, "Starting game...", resolved.id());
        startProcess(command, resolved.id());
        progress.update(1.0, "Game started", resolved.id());
        LauncherLog.info("Process started.");
        return new LaunchResult(account.updatedProfile());
    }

    public void shutdown() {
        downloads.shutdown();
    }

    private net.litelauncher.backend.modules.download.DownloadProgress scaledDownloads(LaunchProgress progress, double from, double to) {
        return (value, action, details) -> progress.update(from + (to - from) * value, action, details);
    }

    private LaunchProgress scaledLaunch(LaunchProgress progress, double from, double to) {
        return (value, action, details) -> progress.update(from + (to - from) * value, action, details);
    }

    private ClientJar launchClientJar(ResolvedVersion version) throws GameLaunchException {
        if (version.jarExplicit()) {
            String id = firstText(version.jarId(), version.clientDownloadId(), version.selectedId());
            return new ClientJar(id, versionJar(id));
        }

        Path selectedJar = versionJar(version.selectedId());
        if (Files.isRegularFile(selectedJar)) return new ClientJar(version.selectedId(), selectedJar);

        String id = firstText(version.clientDownloadId(), version.jarId(), version.selectedId());
        return new ClientJar(id, versionJar(id));
    }

    private void addClient(List<DownloadFile> files, ResolvedVersion version, ClientJar client) {
        JsonObject download = version.clientDownload();
        if (download == null || client == null || !client.id().equals(version.clientDownloadId())) return;
        files.add(new DownloadFile(download.getString("url", ""), client.path(), download.getString("sha1", ""), download.getLong("size", 0L), "Downloading client"));
    }

    private String clientSha1(ResolvedVersion version, ClientJar client) {
        JsonObject download = version.clientDownload();
        if (download == null || client == null || !client.id().equals(version.clientDownloadId())) return "";
        return download.getString("sha1", "");
    }

    private void addLibraries(List<DownloadFile> files, ResolvedVersion version) throws GameLaunchException {
        for (JsonObject library : version.libraries()) {
            if (!RuleEvaluator.allowed(library, Map.of())) continue;
            JsonObject artifact = library.getObject("downloads", new JsonObject()).getObject("artifact");
            JsonObject nativeArtifact = nativeArtifact(library);

            if (artifact != null) files.add(downloadFile(artifact, library.getString("url", LIBRARIES_URL), library.getString("name", ""), "Downloading libraries"));
            else if (nativeArtifact == null) addMavenLibrary(files, library);
            else LauncherLog.info("Native-only library has no normal artifact: " + library.getString("name", ""));

            if (nativeArtifact != null) files.add(downloadFile(nativeArtifact, library.getString("url", LIBRARIES_URL), library.getString("name", ""), "Downloading natives"));
        }
    }

    private DownloadFile assetIndexDownload(ResolvedVersion version) throws GameLaunchException {
        JsonObject index = version.assetIndex();
        if (index == null) return null;
        String id = firstText(index.getString("id"), version.assets(), "legacy");
        return new DownloadFile(index.getString("url", ""), assetIndexPath(id), index.getString("sha1", ""), index.getLong("size", 0L), "Downloading asset index");
    }

    private void addAssets(List<DownloadFile> files, JsonObject index) throws GameLaunchException {
        if (index == null) return;
        JsonObject objects = index.getObject("objects");
        if (objects == null) return;

        for (Object item : objects.values()) {
            if (!(item instanceof JsonObject asset)) continue;
            String hash = asset.getString("hash", "");
            if (hash.length() < 2) continue;
            Path path = assetObjectPath(hash);
            files.add(new DownloadFile(RESOURCES_URL + hash.substring(0, 2) + "/" + hash, path, hash, asset.getLong("size", 0L), "Downloading assets"));
        }
    }

    private void addMavenLibrary(List<DownloadFile> files, JsonObject library) throws GameLaunchException {
        String path = mavenPath(library.getString("name", ""));
        if (!path.isBlank()) files.add(new DownloadFile(joinUrl(library.getString("url", LIBRARIES_URL), path), libraryPath(path), "", 0L, "Downloading libraries"));
    }

    private DownloadFile downloadFile(JsonObject artifact, String baseUrl, String name, String label) throws GameLaunchException {
        String path = libraryPathText(artifact.getString("path", mavenPath(name)));
        String url = firstText(artifact.getString("url"), joinUrl(baseUrl, path));
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
        return natives.getString(RuleEvaluator.osName(), "").replace("${arch}", is64Bit() ? "64" : "32");
    }

    private Path extractNatives(ResolvedVersion version) throws GameLaunchException {
        Path natives = OSUtils.nativesDirectory(version.id());
        try {
            Files.createDirectories(natives);
            for (JsonObject library : version.libraries()) {
                if (!RuleEvaluator.allowed(library, Map.of())) continue;
                JsonObject artifact = nativeArtifact(library);
                if (artifact != null) extractNativeJar(libraryPath(artifact.getString("path", "")), natives, excludes(library));
            }
            return natives;
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to extract native libraries.", exception);
        }
    }

    private Set<String> excludes(JsonObject library) {
        Set<String> excludes = new HashSet<>();
        JsonArray array = library.getObject("extract", new JsonObject()).getArray("exclude", new JsonArray());
        for (Object item : array) if (item instanceof String value) excludes.add(value);
        excludes.add("META-INF/");
        return excludes;
    }

    private void extractNativeJar(Path jar, Path target, Set<String> excludes) throws Exception {
        if (!Files.isRegularFile(jar)) throw new GameLaunchException("Native library is missing: " + jar.getFileName());
        Path root = target.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(jar))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.isDirectory() || excluded(entry.getName(), excludes)) continue;
                Path out = safeResolve(root, entry.getName(), "Invalid native library entry path.");
                if (Files.isRegularFile(out)) continue;
                Files.createDirectories(out.getParent());
                Files.copy(zip, out);
            }
        }
    }

    private boolean excluded(String name, Set<String> excludes) {
        for (String exclude : excludes) if (name.startsWith(exclude)) return true;
        return false;
    }

    private String classpath(ResolvedVersion version, Path clientJar) throws GameLaunchException {
        List<String> paths = new ArrayList<>();
        for (JsonObject library : version.libraries()) {
            if (!RuleEvaluator.allowed(library, Map.of())) continue;
            JsonObject artifact = library.getObject("downloads", new JsonObject()).getObject("artifact");
            if (artifact == null && nativeArtifact(library) != null) continue;
            String path = artifact == null ? mavenPath(library.getString("name", "")) : artifact.getString("path", mavenPath(library.getString("name", "")));
            if (path.isBlank()) continue;
            Path file = libraryPath(path);
            if (!Files.isRegularFile(file)) throw new GameLaunchException("Library is missing: " + file.getFileName());
            paths.add(file.toString());
        }
        if (!Files.isRegularFile(clientJar)) throw new GameLaunchException("Client jar is missing: " + clientJar.getFileName());
        paths.add(clientJar.toString());
        return String.join(java.io.File.pathSeparator, paths);
    }

    private void mapLegacyAssets(JsonObject index) throws GameLaunchException {
        if (index == null || (!index.getBoolean("virtual", false) && !index.getBoolean("map_to_resources", false))) return;
        JsonObject objects = index.getObject("objects");
        if (objects == null) return;

        Path target = index.getBoolean("map_to_resources", false)
                ? OSUtils.minecraftDirectory().resolve("resources")
                : OSUtils.assetsDirectory().resolve("virtual").resolve("legacy");

        try {
            for (Map.Entry<String, Object> entry : objects.entrySet()) {
                if (!(entry.getValue() instanceof JsonObject asset)) continue;
                String hash = asset.getString("hash", "");
                if (hash.length() < 2) continue;

                Path source = assetObjectPath(hash);
                Path out = safeResolve(target, entry.getKey(), "Invalid legacy asset path.");
                if (!Files.isRegularFile(source)) continue;
                if (Files.isRegularFile(out) && Files.size(out) == Files.size(source)) continue;
                Files.createDirectories(out.getParent());
                Files.copy(source, out, StandardCopyOption.REPLACE_EXISTING);
            }
            LauncherLog.info("Legacy assets mapped to: " + target);
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to map legacy assets.", exception);
        }
    }

    private JsonObject readJson(Path file) throws GameLaunchException {
        try (InputStream input = Files.newInputStream(file)) {
            return JsonParser.object().from(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to read JSON file: " + file.getFileName(), exception);
        }
    }

    private void startProcess(List<String> command, String versionId) throws GameLaunchException {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(OSUtils.minecraftDirectory().toFile())
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            LauncherLog.info("Minecraft process started for " + versionId);
            watchProcess(process, versionId);
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to start Minecraft.", exception);
        }
    }

    private void watchProcess(Process process, String versionId) {
        Thread thread = new Thread(() -> {
            try {
                int code = process.waitFor();
                LauncherLog.info("Minecraft process exited with code " + code + " for " + versionId);
            } catch (Exception exception) {
                LauncherLog.error("Unable to watch Minecraft process.", exception);
            }
        }, "LiteLauncher Game Watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private Path versionJar(String id) throws GameLaunchException {
        return safeResolveVersionFile(id, ".jar");
    }

    private Path assetIndexPath(String id) throws GameLaunchException {
        return safeResolve(OSUtils.assetIndexesDirectory(), id + ".json", "Invalid asset index path.");
    }

    private Path assetObjectPath(String hash) throws GameLaunchException {
        String value = hash == null ? "" : hash.replace('\\', '/');
        if (value.length() < 2) throw new GameLaunchException("Invalid asset object path.");
        return safeResolve(OSUtils.assetObjectsDirectory(), value.substring(0, 2) + "/" + value, "Invalid asset object path.");
    }

    private Path safeResolveVersionFile(String id, String suffix) throws GameLaunchException {
        String value = id == null ? "" : id;
        if (value.isBlank()) throw new GameLaunchException("Invalid version path.");
        return safeResolve(OSUtils.versionsDirectory(), value + "/" + value + suffix, "Invalid version path.");
    }

    private Path libraryPath(String path) throws GameLaunchException {
        return safeResolve(OSUtils.librariesDirectory(), libraryPathText(path), "Invalid library path.");
    }

    private Path safeResolve(Path root, String relative, String error) throws GameLaunchException {
        Path base = root.toAbsolutePath().normalize();
        Path out = base.resolve(relative == null ? "" : relative).toAbsolutePath().normalize();
        if (!out.startsWith(base) || out.equals(base)) throw new GameLaunchException(error);
        return out;
    }

    private String libraryPathText(String path) {
        String value = path == null ? "" : path.replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        if (value.startsWith("libraries/")) value = value.substring("libraries/".length());
        return value;
    }

    private String mavenPath(String name) {
        String[] parts = name == null ? new String[0] : name.split(":");
        if (parts.length < 3) return "";
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        return parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + classifier + ".jar";
    }

    private String joinUrl(String base, String path) {
        String url = base == null || base.isBlank() ? LIBRARIES_URL : base;
        return (url.endsWith("/") ? url : url + "/") + path;
    }

    private String firstText(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private boolean is64Bit() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.contains("64") || arch.contains("aarch64") || arch.contains("arm64");
    }

    private record ClientJar(String id, Path path) {
    }
}
