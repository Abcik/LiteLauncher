package net.litelauncher.backend.loader;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.CancellationToken;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.backend.download.DownloadProgress;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.i18n.I18n;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class LoaderInstaller {

    private static final String FABRIC_PROFILE = "https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json";
    private static final String QUILT_PROFILE = "https://meta.quiltmc.org/v3/versions/loader/%s/%s/profile/json";
    private static final String FORGE_MAVEN = "https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar";
    private static final String NEOFORGE_MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar";
    private static final String LEGACY_NEOFORGE_MAVEN = "https://maven.neoforged.net/releases/net/neoforged/forge/%s/forge-%s-installer.jar";
    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final DownloadService downloads;
    private final HttpClient http = BackendUtils.http();

    public LoaderInstaller(DownloadService downloads) {
        if (downloads == null) throw new IllegalArgumentException("DownloadService is required.");
        this.downloads = downloads;
    }

    public String install(LoaderVersion loader, Path versionsDirectory, Path workDirectory,
                          String idOverride, DownloadProgress progress, CancellationToken cancellation) throws Exception {
        if (loader == null) throw new IOException("Loader version is missing.");
        checkCancelled(cancellation);
        Files.createDirectories(versionsDirectory);
        Files.createDirectories(workDirectory);

        return switch (loader.type()) {
            case FABRIC -> installRemoteProfile(FABRIC_PROFILE, loader, versionsDirectory, idOverride, progress, cancellation);
            case QUILT -> installRemoteProfile(QUILT_PROFILE, loader, versionsDirectory, idOverride, progress, cancellation);
            case FORGE, NEOFORGE -> installWithInstaller(loader, versionsDirectory, workDirectory, progress, cancellation);
            case OPTIFINE -> installOptiFine(loader, versionsDirectory, workDirectory, progress, cancellation);
        };
    }

    public String installVanillaProfile(String minecraftVersion, Path versionsDirectory, String id,
                                        DownloadProgress progress, CancellationToken cancellation) throws Exception {
        checkCancelled(cancellation);
        String launchId = safe(id).isBlank() ? minecraftVersion : id;
        JsonObject profile = new JsonObject();
        profile.put("id", launchId);
        if (!launchId.equals(minecraftVersion)) profile.put("inheritsFrom", minecraftVersion);
        profile.put("type", "release");
        String now = Instant.now().toString();
        profile.put("time", now);
        profile.put("releaseTime", now);
        report(progress, 0.5, I18n.text("progress.installingLoader"), launchId);
        installProfile(versionsDirectory, profile, launchId, cancellation);
        report(progress, 1.0, I18n.text("progress.installingLoader"), launchId);
        return launchId;
    }

    private String installRemoteProfile(String template, LoaderVersion loader, Path versionsDirectory,
                                        String idOverride, DownloadProgress progress, CancellationToken cancellation) throws Exception {
        report(progress, 0.05, I18n.text("progress.downloadingLoader"), loader.type().title());
        String url = template.formatted(segment(loader.minecraftVersion()), segment(loader.version()));
        JsonObject profile = JsonParser.object().from(get(url, cancellation));
        String id = safe(idOverride).isBlank() ? profile.getString("id", loader.id()) : idOverride;
        if (id.isBlank()) id = loader.id();
        profile.put("id", id);
        report(progress, 0.85, I18n.text("progress.installingLoader"), loader.type().title());
        installProfile(versionsDirectory, profile, id, cancellation);
        report(progress, 1.0, I18n.text("progress.installingLoader"), id);
        LauncherLog.info("Loader profile installed: " + loader.type() + " / " + id);
        return id;
    }

    private String installWithInstaller(LoaderVersion loader, Path versionsDirectory, Path workDirectory,
                                        DownloadProgress progress, CancellationToken cancellation) throws Exception {
        String coordinate = loader.artifactVersion();
        if (coordinate.isBlank()) throw new IOException("Loader artifact version is missing.");
        String url = installerUrl(loader, coordinate);
        Path installer = installerCache(loader, coordinate);
        ensureInstaller(url, installer, scaled(progress, 0.0, 0.55), cancellation);

        Path staging = workDirectory.resolve("installer-staging");
        deleteTree(staging);
        Files.createDirectories(staging);
        Files.writeString(staging.resolve("launcher_profiles.json"), "{\"profiles\":{}}", StandardCharsets.UTF_8);

        Path log = workDirectory.resolve("loader-installer.log");
        Files.createDirectories(log.getParent());
        Process process = null;
        try {
            checkCancelled(cancellation);
            report(progress, 0.60, I18n.text("progress.installingLoader"), loader.type().title());
            process = new ProcessBuilder(javaExecutable().toString(), "-jar", installer.toString(),
                    "--installClient", staging.toAbsolutePath().toString())
                    .directory(staging.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
                    .start();

            while (!process.waitFor(200, TimeUnit.MILLISECONDS)) checkCancelled(cancellation);
            if (process.exitValue() != 0) throw new IOException("Loader installer exited with code " + process.exitValue());

            checkCancelled(cancellation);
            mergeDirectory(staging.resolve("libraries"), LauncherPaths.librariesDirectory(), cancellation);
            InstalledVersion installed = findInstalledVersion(loader, staging.resolve("versions"));
            Path target = BackendUtils.safeResolve(versionsDirectory, installed.id());
            deleteTree(target);
            moveOrCopyDirectory(installed.directory(), target, cancellation);
            normalizeVersionJson(target, installed.id());
            report(progress, 1.0, I18n.text("progress.installingLoader"), installed.id());
            LauncherLog.info("Loader installed: " + loader.type() + " / " + installed.id());
            return installed.id();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            if (process != null && process.isAlive()) process.destroyForcibly();
            throw new CancellationException("Loader installation cancelled.");
        } catch (CancellationException exception) {
            if (process != null && process.isAlive()) process.destroyForcibly();
            throw exception;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            deleteTree(staging);
        }
    }

    private String installOptiFine(LoaderVersion loader, Path versionsDirectory, Path workDirectory,
                                   DownloadProgress progress, CancellationToken cancellation) throws Exception {
        if (loader.downloadUrl().isBlank()) throw new IOException("OptiFine download URL is missing.");
        report(progress, 0.02, I18n.text("progress.downloadingLoader"), loader.type().title());

        Path installer = installerCache(loader, loader.artifactVersion().isBlank() ? loader.version() : loader.artifactVersion());
        ensureInstaller(loader.downloadUrl(), installer, scaled(progress, 0.02, 0.30), cancellation);

        VanillaClient vanilla = ensureVanillaClient(loader.minecraftVersion(), versionsDirectory,
                scaled(progress, 0.30, 0.55), cancellation);

        String optifineArtifactVersion = loader.minecraftVersion() + "_" + loader.version();
        Path library = LauncherPaths.librariesDirectory().resolve("optifine/OptiFine")
                .resolve(optifineArtifactVersion)
                .resolve("OptiFine-" + optifineArtifactVersion + ".jar");
        Files.createDirectories(library.getParent());

        report(progress, 0.58, I18n.text("progress.installingLoader"), loader.type().title());
        if (zipContains(installer, "optifine/Patcher.class")) {
            runOptiFinePatcher(installer, vanilla.jar(), library, workDirectory, cancellation);
        } else {
            copyAtomic(installer, library);
        }
        deleteZipEntry(library, "/META-INF/mods.toml");

        List<String> launchWrappers = installOptiFineLaunchWrappers(installer);
        JsonObject profile = optiFineProfile(loader, launchWrappers);
        installProfile(versionsDirectory, profile, loader.id(), cancellation);
        report(progress, 1.0, I18n.text("progress.installingLoader"), loader.id());
        LauncherLog.info("OptiFine installed: " + loader.id());
        return loader.id();
    }

    private VanillaClient ensureVanillaClient(String minecraftVersion, Path versionsDirectory,
                                               DownloadProgress progress, CancellationToken cancellation) throws Exception {
        Path directory = BackendUtils.safeResolve(versionsDirectory, minecraftVersion);
        Path jsonFile = directory.resolve(minecraftVersion + ".json");
        JsonObject json = null;
        if (Files.isRegularFile(jsonFile)) {
            try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
                json = JsonParser.object().from(reader);
            }
        }
        if (json == null || json.getObject("downloads", new JsonObject()).getObject("client") == null) {
            report(progress, 0.05, I18n.text("progress.checkingFiles"), minecraftVersion);
            JsonObject manifest = JsonParser.object().from(get(VERSION_MANIFEST, cancellation));
            String versionUrl = "";
            for (Object item : manifest.getArray("versions", new JsonArray())) {
                if (!(item instanceof JsonObject object)) continue;
                if (minecraftVersion.equals(object.getString("id", ""))) {
                    versionUrl = object.getString("url", "");
                    break;
                }
            }
            if (versionUrl.isBlank()) throw new IOException("Minecraft version metadata was not found: " + minecraftVersion);
            json = JsonParser.object().from(get(versionUrl, cancellation));
            Files.createDirectories(directory);
            writeJsonAtomic(jsonFile, json);
        }

        JsonObject client = json.getObject("downloads", new JsonObject()).getObject("client");
        if (client == null) throw new IOException("Minecraft client download is missing: " + minecraftVersion);
        Path jar = directory.resolve(minecraftVersion + ".jar");
        DownloadFile file = new DownloadFile(client.getString("url", ""), jar,
                client.getString("sha1", ""), client.getLong("size", 0L), I18n.text("progress.downloadingClient"));
        downloads.download(List.of(file), scaled(progress, 0.15, 1.0), cancellation);
        return new VanillaClient(jar);
    }

    private void runOptiFinePatcher(Path installer, Path minecraftJar, Path output, Path workDirectory,
                                    CancellationToken cancellation) throws Exception {
        Files.createDirectories(output.getParent());
        Files.deleteIfExists(output);
        Path log = workDirectory.resolve("optifine-patcher.log");
        Process process = null;
        try {
            process = new ProcessBuilder(javaExecutable().toString(), "-cp", installer.toString(), "optifine.Patcher",
                    minecraftJar.toString(), installer.toString(), output.toString())
                    .directory(workDirectory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
                    .start();
            while (!process.waitFor(200, TimeUnit.MILLISECONDS)) checkCancelled(cancellation);
            if (process.exitValue() != 0 || !validJar(output))
                throw new IOException("OptiFine patcher exited with code " + process.exitValue());
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            if (process != null && process.isAlive()) process.destroyForcibly();
            throw new CancellationException("OptiFine installation cancelled.");
        } catch (CancellationException exception) {
            if (process != null && process.isAlive()) process.destroyForcibly();
            throw exception;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    private List<String> installOptiFineLaunchWrappers(Path installer) throws Exception {
        List<String> libraries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(installer.toFile())) {
            ZipEntry embedded20 = zip.getEntry("launchwrapper-2.0.jar");
            if (embedded20 != null) {
                Path target = LauncherPaths.librariesDirectory().resolve("optifine/launchwrapper/2.0/launchwrapper-2.0.jar");
                extractZipEntry(zip, embedded20, target);
                libraries.add("optifine:launchwrapper:2.0");
            }

            ZipEntry versionFile = zip.getEntry("launchwrapper-of.txt");
            if (versionFile != null) {
                String version;
                try (var input = zip.getInputStream(versionFile)) {
                    version = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
                }
                if (!version.isBlank()) {
                    ZipEntry embedded = zip.getEntry("launchwrapper-of-" + version + ".jar");
                    if (embedded != null) {
                        Path target = LauncherPaths.librariesDirectory().resolve("optifine/launchwrapper-of")
                                .resolve(version).resolve("launchwrapper-of-" + version + ".jar");
                        extractZipEntry(zip, embedded, target);
                        libraries.add("optifine:launchwrapper-of:" + version);
                    }
                }
            }
        }
        if (libraries.isEmpty()) libraries.add("net.minecraft:launchwrapper:1.12");
        return List.copyOf(libraries);
    }

    private JsonObject optiFineProfile(LoaderVersion loader, List<String> launchWrappers) {
        JsonObject profile = new JsonObject();
        profile.put("id", loader.id());
        profile.put("inheritsFrom", loader.minecraftVersion());
        profile.put("type", "release");
        profile.put("mainClass", "net.minecraft.launchwrapper.Launch");
        String now = Instant.now().toString();
        profile.put("time", now);
        profile.put("releaseTime", now);

        JsonArray libraries = new JsonArray();
        JsonObject optifine = new JsonObject();
        optifine.put("name", "optifine:OptiFine:" + loader.minecraftVersion() + "_" + loader.version());
        libraries.add(optifine);
        for (String launchWrapper : launchWrappers) {
            JsonObject wrapper = new JsonObject();
            wrapper.put("name", launchWrapper);
            libraries.add(wrapper);
        }
        profile.put("libraries", libraries);

        JsonObject arguments = new JsonObject();
        JsonArray game = new JsonArray();
        game.add("--tweakClass");
        game.add("optifine.OptiFineTweaker");
        arguments.put("game", game);
        profile.put("arguments", arguments);
        return profile;
    }

    private void installProfile(Path versionsDirectory, JsonObject profile, String fallbackId,
                                CancellationToken cancellation) throws Exception {
        checkCancelled(cancellation);
        String id = profile.getString("id", fallbackId);
        if (id == null || id.isBlank()) id = fallbackId;
        if (id == null || id.isBlank()) throw new IOException("Loader profile id is empty.");
        profile.put("id", id);
        Path directory = BackendUtils.safeResolve(versionsDirectory, id);
        Files.createDirectories(directory);
        writeJsonAtomic(directory.resolve(id + ".json"), profile);
    }

    private InstalledVersion findInstalledVersion(LoaderVersion loader, Path versions) throws Exception {
        if (!Files.isDirectory(versions)) throw new IOException("Loader installer did not create a version.");
        InstalledVersion best = null;
        int bestScore = Integer.MIN_VALUE;
        try (Stream<Path> stream = Files.list(versions)) {
            for (Path directory : stream.filter(Files::isDirectory).sorted().toList()) {
                String fallback = directory.getFileName().toString();
                Path preferred = directory.resolve(fallback + ".json");
                Path jsonFile = Files.isRegularFile(preferred) ? preferred : firstJson(directory);
                if (jsonFile == null) continue;
                JsonObject json;
                try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
                    json = JsonParser.object().from(reader);
                }
                String id = json.getString("id", fallback);
                if (id == null || id.isBlank()) id = fallback;
                int score = installedVersionScore(loader, id, json);
                if (best == null || score > bestScore) {
                    best = new InstalledVersion(id, directory);
                    bestScore = score;
                }
            }
        }
        if (best == null) throw new IOException("Loader installer did not create a readable version.");
        return best;
    }

    private int installedVersionScore(LoaderVersion loader, String id, JsonObject json) {
        String value = id.toLowerCase(Locale.ROOT);
        String loaderVersion = loader.version().toLowerCase(Locale.ROOT);
        int score = 0;
        if (!id.equals(loader.minecraftVersion())) score += 2;
        if (!loaderVersion.isBlank() && value.contains(loaderVersion)) score += 4;
        if (json.getString("inheritsFrom", "").equals(loader.minecraftVersion())) score += 3;
        if (loader.type() == LoaderType.NEOFORGE && (value.contains("neoforge") || json.toString().contains("net.neoforged"))) score += 8;
        if (loader.type() == LoaderType.FORGE && value.contains("forge") && !value.contains("neoforge")) score += 8;
        if (id.equals(loader.id())) score += 16;
        return score;
    }

    private String installerUrl(LoaderVersion loader, String coordinate) {
        if (loader.type() == LoaderType.FORGE) return FORGE_MAVEN.formatted(coordinate, coordinate);
        if (loader.type() == LoaderType.NEOFORGE && "1.20.1".equals(loader.minecraftVersion()))
            return LEGACY_NEOFORGE_MAVEN.formatted(coordinate, coordinate);
        return NEOFORGE_MAVEN.formatted(coordinate, coordinate);
    }

    private Path installerCache(LoaderVersion loader, String coordinate) throws IOException {
        String name = loader.type().name().toLowerCase(Locale.ROOT) + "-" + safeFileName(coordinate) + ".jar";
        Path root = LauncherPaths.launcherDataDirectory().resolve("loader-installers").toAbsolutePath().normalize();
        Files.createDirectories(root);
        return BackendUtils.safeResolve(root, name);
    }

    private void ensureInstaller(String url, Path installer, DownloadProgress progress,
                                 CancellationToken cancellation) throws Exception {
        if (!validJar(installer)) {
            Files.deleteIfExists(installer);
            downloads.download(List.of(new DownloadFile(url, installer, "", 0L,
                    I18n.text("progress.downloadingLoader"))), progress, cancellation);
        }
        if (!validJar(installer)) {
            Files.deleteIfExists(installer);
            throw new IOException("Downloaded loader installer is not a valid JAR: " + installer.getFileName());
        }
    }

    private boolean validJar(Path file) {
        if (!Files.isRegularFile(file)) return false;
        try {
            if (Files.size(file) <= 0L) return false;
            try (ZipFile zip = new ZipFile(file.toFile())) {
                return zip.entries().hasMoreElements();
            }
        } catch (Exception _) {
            return false;
        }
    }

    private String get(String url, CancellationToken cancellation) throws Exception {
        checkCancelled(cancellation);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("User-Agent", "LiteLauncher/" + System.getProperty("java.version", "java"))
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkCancelled(cancellation);
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Loader metadata returned HTTP " + response.statusCode() + " for " + url);
        return response.body();
    }

    private void normalizeVersionJson(Path directory, String id) throws IOException {
        Path expected = directory.resolve(id + ".json");
        if (Files.isRegularFile(expected)) return;
        Path source = firstJson(directory);
        if (source == null) throw new IOException("Installed loader version JSON is missing.");
        BackendUtils.moveReplace(source, expected);
    }

    private Path firstJson(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .findFirst().orElse(null);
        }
    }

    private void mergeDirectory(Path source, Path target, CancellationToken cancellation) throws Exception {
        if (!Files.isDirectory(source)) return;
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.sorted().toList()) {
                checkCancelled(cancellation);
                Path relative = source.relativize(path);
                Path out = BackendUtils.safeResolve(target, relative.toString(), true);
                if (Files.isDirectory(path)) Files.createDirectories(out);
                else if (Files.isRegularFile(path)) {
                    if (Files.isRegularFile(out) && Files.size(out) == Files.size(path)) continue;
                    copyAtomic(path, out);
                }
            }
        }
    }

    private void moveOrCopyDirectory(Path source, Path target, CancellationToken cancellation) throws Exception {
        Files.createDirectories(target.getParent());
        try {
            BackendUtils.move(source, target);
            return;
        } catch (Exception _) {
        }
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.sorted().toList()) {
                checkCancelled(cancellation);
                Path out = BackendUtils.safeResolve(target, source.relativize(path).toString(), true);
                if (Files.isDirectory(path)) Files.createDirectories(out);
                else if (Files.isRegularFile(path)) copyAtomic(path, out);
            }
        }
    }

    private void extractZipEntry(ZipFile zip, ZipEntry entry, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".litelauncher-copy");
        try (var input = zip.getInputStream(entry)) {
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        moveAtomic(temp, target);
    }

    private boolean zipContains(Path file, String entry) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            return zip.getEntry(entry) != null;
        } catch (Exception _) {
            return false;
        }
    }

    private void deleteZipEntry(Path jar, String entry) {
        try {
            URI uri = URI.create("jar:" + jar.toUri());
            try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                Files.deleteIfExists(fs.getPath(entry));
            }
        } catch (Exception _) {
            LauncherLog.info("OptiFine library has no removable " + entry + " entry.");
        }
    }

    private void copyAtomic(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".litelauncher-copy");
        Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
        moveAtomic(temp, target);
    }

    private void moveAtomic(Path source, Path target) throws IOException {
        try {
            BackendUtils.moveReplace(source, target);
        } finally {
            BackendUtils.deleteQuietly(source);
        }
    }

    private void writeJsonAtomic(Path file, JsonObject json) throws IOException {
        BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(json).done());
    }

    private void deleteTree(Path root) {
        BackendUtils.deleteTreeQuietly(root);
    }

    private Path javaExecutable() {
        String executable = OSUtils.os().windows() ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home", ""), "bin", executable);
    }

    private String segment(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String safeFileName(String value) {
        return safe(value).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private DownloadProgress scaled(DownloadProgress progress, double from, double to) {
        return (value, action, details) -> report(progress, from + (to - from) * value, action, details);
    }

    private void report(DownloadProgress progress, double value, String action, String details) {
        if (progress != null) progress.update(value, action, details);
    }

    private void checkCancelled(CancellationToken cancellation) {
        if (cancellation != null) cancellation.throwIfCancelled();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Loader installation cancelled.");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record InstalledVersion(String id, Path directory) {}
    private record VanillaClient(Path jar) {}
}
