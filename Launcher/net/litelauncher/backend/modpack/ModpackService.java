package net.litelauncher.backend.modpack;

import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.CancellationToken;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.backend.download.DownloadProgress;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.loader.LoaderInstaller;
import net.litelauncher.backend.loader.LoaderType;
import net.litelauncher.backend.loader.LoaderVersion;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.backend.version.Version;
import net.litelauncher.i18n.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ModpackService {

    private static final String METADATA_FILE = "litelauncher-modpack.json";
    private static final String INDEX_FILE = "modrinth.index.json";
    private static final int METADATA_SCHEMA = 1;
    private static final long MAX_INDEX_BYTES = 16L * 1024L * 1024L;
    private static final long MAX_OVERRIDE_ENTRY_BYTES = 5L * 1024L * 1024L * 1024L / 2L;
    private static final long MAX_OVERRIDE_TOTAL_BYTES = 10L * 1024L * 1024L * 1024L;
    private static final Set<String> DOWNLOAD_HOSTS = Set.of(
            "cdn.modrinth.com",
            "github.com",
            "raw.githubusercontent.com",
            "gitlab.com"
    );

    private final DownloadService downloads;
    private final LoaderInstaller loaderInstaller;

    public ModpackService(LoaderInstaller loaderInstaller, DownloadService downloads) {
        if (loaderInstaller == null || downloads == null) throw new IllegalArgumentException("Modpack dependencies are required.");
        this.loaderInstaller = loaderInstaller;
        this.downloads = downloads;
    }

    public List<Version> loadVersions(Map<String, Long> minecraftReleaseTimes) {
        List<Version> versions = new ArrayList<>();
        for (ModpackInstance instance : loadInstances()) {
            long releaseTime = minecraftReleaseTimes == null
                    ? 0L
                    : minecraftReleaseTimes.getOrDefault(instance.minecraftVersion(), 0L);
            versions.add(Version.modpack(instance.id(), instance.name(), instance.minecraftVersion(),
                    instance.installed(), releaseTime));
        }
        versions.sort(Comparator.comparingLong(Version::releaseTime).reversed().thenComparing(Version::id));
        return List.copyOf(versions);
    }

    public List<ModpackInstance> loadInstances() {
        Path root = LauncherPaths.modpacksDirectory();
        if (!Files.isDirectory(root)) return List.of();

        List<ModpackInstance> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(root)) {
            for (Path directory : stream.filter(Files::isDirectory).sorted().toList()) {
                if (directory.getFileName().toString().startsWith(".import-")) continue;
                ModpackInstance instance = readInstance(directory);
                if (instance != null) result.add(instance);
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to read modpack instances.", exception);
        }
        return List.copyOf(result);
    }

    public ModpackInstance find(String id) {
        if (id == null || id.isBlank()) return null;
        Path directory;
        try {
            directory = BackendUtils.safeResolve(LauncherPaths.modpacksDirectory(), id, false);
        } catch (IOException _) {
            return null;
        }
        return readInstance(directory);
    }

    public boolean importPack(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source) || !mrpackExtension(source)) {
            throw new IOException("Only .mrpack files are supported.");
        }

        Files.createDirectories(LauncherPaths.modpacksDirectory());
        try (ZipFile zip = new ZipFile(source.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry indexEntry = zip.getEntry(INDEX_FILE);
            if (indexEntry == null || indexEntry.isDirectory()) throw new IOException("modrinth.index.json is missing.");
            byte[] indexBytes = readLimited(zip.getInputStream(indexEntry), MAX_INDEX_BYTES);
            JsonObject index = JsonParser.object().from(new String(indexBytes, StandardCharsets.UTF_8));
            ParsedPack pack = parseIndex(index);
            String instanceId = instanceId(pack.name(), pack.versionId());
            Path target = BackendUtils.safeResolve(LauncherPaths.modpacksDirectory(), instanceId, false);
            if (Files.exists(target)) return false;

            Path temporary = BackendUtils.safeResolve(LauncherPaths.modpacksDirectory(), ".import-" + UUID.randomUUID(), false);
            try {
                ModpackInstance instance = new ModpackInstance(instanceId, pack.name(), pack.versionId(),
                        pack.minecraftVersion(), pack.loader(), pack.loaderVersion(), "", false, Set.of(), temporary);
                planFiles(instance, index);

                Set<String> overrideFiles = new HashSet<>();
                long overrideBytes = extractOverrideLayer(zip, "overrides/", temporary, overrideFiles, 0L);
                extractOverrideLayer(zip, "client-overrides/", temporary, overrideFiles, overrideBytes);
                instance = instance.withOverrideFiles(overrideFiles);

                Path manifest = temporary.resolve("manifest");
                Files.createDirectories(manifest);
                Files.write(manifest.resolve(INDEX_FILE), indexBytes);
                writeInstance(instance);
                moveDirectory(temporary, target);
                LauncherLog.info("Modpack imported: " + instanceId + " <- " + source.getFileName());
                return true;
            } finally {
                BackendUtils.deleteTreeQuietly(temporary);
            }
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Unable to import modpack.", exception);
        }
    }

    public ModpackInstance ensureReady(String instanceId, DownloadProgress progress,
                                       CancellationToken cancellation) throws Exception {
        ModpackInstance instance = find(instanceId);
        if (instance == null) throw new IOException("Modpack instance was not found: " + instanceId);
        checkCancelled(cancellation);

        Files.createDirectories(instance.directory());
        Files.createDirectories(instance.manifestDirectory());
        Files.createDirectories(instance.versionsDirectory());

        if (!loaderReady(instance)) {
            report(progress, 0.02, I18n.text("progress.preparingModpack"), instance.name());
            String launchVersionId = installLoader(instance, scaled(progress, 0.02, 0.42), cancellation);
            instance = instance.withLaunchVersion(launchVersionId);
            writeInstance(instance);
        }

        checkCancelled(cancellation);
        JsonObject index = readIndex(instance.indexFile());
        List<DownloadFile> files = planFiles(instance, index);
        downloads.download(files, scaled(progress, 0.42, 0.99), cancellation);

        if (!instance.installed()) {
            instance = instance.withInstalled(true);
            writeInstance(instance);
        }

        report(progress, 1.0, I18n.text("progress.preparingModpack"), instance.name());
        return instance;
    }

    private String installLoader(ModpackInstance instance, DownloadProgress progress, CancellationToken cancellation) throws Exception {
        String launchId = "litelauncher-" + instance.id();
        if (instance.loader() == ModpackLoader.VANILLA) {
            return loaderInstaller.installVanillaProfile(instance.minecraftVersion(), instance.versionsDirectory(),
                    launchId, progress, cancellation);
        }

        LoaderType type = switch (instance.loader()) {
            case FABRIC -> LoaderType.FABRIC;
            case FORGE -> LoaderType.FORGE;
            case NEOFORGE -> LoaderType.NEOFORGE;
            case QUILT -> LoaderType.QUILT;
            case VANILLA -> throw new IllegalStateException("Vanilla is handled separately.");
        };

        String loaderVersion = instance.loaderVersion();
        String artifactVersion = loaderVersion;
        String id;
        if (type == LoaderType.FABRIC) {
            id = "fabric-loader-" + loaderVersion + "-" + instance.minecraftVersion();
        } else if (type == LoaderType.QUILT) {
            id = "quilt-loader-" + loaderVersion + "-" + instance.minecraftVersion();
        } else if (type == LoaderType.FORGE) {
            artifactVersion = loaderVersion.startsWith(instance.minecraftVersion() + "-")
                    ? loaderVersion : instance.minecraftVersion() + "-" + loaderVersion;
            String display = artifactVersion.substring((instance.minecraftVersion() + "-").length());
            id = instance.minecraftVersion() + "-forge-" + display;
        } else if ("1.20.1".equals(instance.minecraftVersion())) {
            artifactVersion = loaderVersion.startsWith(instance.minecraftVersion() + "-")
                    ? loaderVersion : instance.minecraftVersion() + "-" + loaderVersion;
            String display = artifactVersion.substring((instance.minecraftVersion() + "-").length());
            id = instance.minecraftVersion() + "-forge-" + display;
        } else {
            id = "neoforge-" + loaderVersion;
        }

        LoaderVersion loader = new LoaderVersion(type, instance.minecraftVersion(), loaderVersion, id, artifactVersion, "");
        String override = type == LoaderType.FABRIC || type == LoaderType.QUILT ? launchId : "";
        return loaderInstaller.install(loader, instance.versionsDirectory(),
                instance.manifestDirectory().resolve("loader-work"), override, progress, cancellation);
    }

    public void delete(String instanceId) throws IOException {
        if (instanceId == null || instanceId.isBlank()) return;
        Path root = LauncherPaths.modpacksDirectory().toAbsolutePath().normalize();
        Path target = BackendUtils.safeResolve(root, instanceId, false);
        if (!Files.exists(target)) return;
        BackendUtils.deleteTreeQuietly(target);
        LauncherLog.info("Modpack deleted: " + instanceId);
    }

    private ParsedPack parseIndex(JsonObject index) throws IOException {
        if (index.getInt("formatVersion", -1) != 1) throw new IOException("Unsupported .mrpack format version.");
        if (!"minecraft".equals(index.getString("game", ""))) throw new IOException("Unsupported modpack game.");

        String name = required(index.getString("name"), "Modpack name is missing.");
        String versionId = required(index.getString("versionId"), "Modpack versionId is missing.");
        JsonObject dependencies = index.getObject("dependencies");
        if (dependencies == null) throw new IOException("Modpack dependencies are missing.");
        String minecraft = required(dependencies.getString("minecraft"), "Minecraft dependency is missing.");

        ModpackLoader loader = ModpackLoader.VANILLA;
        String loaderVersion = "";
        for (Map.Entry<String, Object> dependency : dependencies.entrySet()) {
            if ("minecraft".equals(dependency.getKey())) continue;
            ModpackLoader candidate = ModpackLoader.fromDependencyId(dependency.getKey());
            if (candidate == null) throw new IOException("Unsupported mod loader: " + dependency.getKey());
            if (loader != ModpackLoader.VANILLA) throw new IOException("Multiple mod loaders are not supported.");
            if (!(dependency.getValue() instanceof String value)) {
                throw new IOException("Mod loader version is invalid: " + dependency.getKey());
            }
            loader = candidate;
            loaderVersion = required(value, "Mod loader version is missing.");
        }

        return new ParsedPack(name, versionId, minecraft, loader, loaderVersion);
    }

    private List<DownloadFile> planFiles(ModpackInstance instance, JsonObject index) throws IOException {
        List<DownloadFile> result = new ArrayList<>();
        Set<Path> targets = new HashSet<>();
        JsonArray files = index.getArray("files", new JsonArray());
        for (Object item : files) {
            if (!(item instanceof JsonObject file)) throw new IOException("Invalid modpack file entry.");

            JsonObject env = file.getObject("env");
            String client = env == null ? "required" : env.getString("client", "required");
            if (!"required".equalsIgnoreCase(client)
                    && !"optional".equalsIgnoreCase(client)
                    && !"unsupported".equalsIgnoreCase(client)) {
                throw new IOException("Invalid client environment value: " + client);
            }
            if ("unsupported".equalsIgnoreCase(client)) continue;

            String relative = required(file.getString("path"), "Modpack file path is missing.");
            Path target = instancePath(instance.directory(), relative);
            if (!targets.add(target)) throw new IOException("Duplicate modpack file path: " + relative);

            JsonObject hashes = file.getObject("hashes", new JsonObject());
            String sha1 = required(hashes.getString("sha1"), "Modpack file SHA-1 is missing.");
            if (!sha1.matches("(?i)[0-9a-f]{40}")) throw new IOException("Invalid modpack file SHA-1: " + relative);

            JsonArray urls = file.getArray("downloads", new JsonArray());
            String url = firstValidUrl(urls);
            if (url.isBlank()) throw new IOException("Modpack file download URL is invalid: " + relative);

            long size = file.getLong("fileSize", 0L);
            if (size < 0L) throw new IOException("Invalid modpack file size: " + relative);
            if (instance.overrideFiles().contains(relativeKey(instance.directory(), target))) continue;
            result.add(new DownloadFile(url, target, sha1, size,
                    I18n.text("progress.downloadingModpackFiles")));
        }
        return List.copyOf(result);
    }

    private boolean validDownloadUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && DOWNLOAD_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    private long extractOverrideLayer(ZipFile zip, String prefix, Path instanceRoot,
                                      Set<String> overrideFiles, long totalBytes) throws IOException {
        Set<Path> layerTargets = new HashSet<>();
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = normalizeZipPath(entry.getName());
            if (!name.startsWith(prefix) || name.equals(prefix)) continue;

            Path target = instancePath(instanceRoot, name.substring(prefix.length()));
            if (entry.isDirectory()) {
                Files.createDirectories(target);
                continue;
            }
            if (!layerTargets.add(target)) throw new IOException("Duplicate override path: " + entry.getName());

            if (entry.getSize() > MAX_OVERRIDE_ENTRY_BYTES) throw new IOException("Override file is too large.");
            Files.createDirectories(target.getParent());
            try (InputStream input = zip.getInputStream(entry); OutputStream output = Files.newOutputStream(target)) {
                totalBytes += copyLimited(input, output, MAX_OVERRIDE_ENTRY_BYTES);
            }
            if (totalBytes > MAX_OVERRIDE_TOTAL_BYTES) throw new IOException("Modpack overrides are too large.");
            overrideFiles.add(relativeKey(instanceRoot, target));
        }
        return totalBytes;
    }

    private boolean loaderReady(ModpackInstance instance) {
        if (instance.launchVersionId().isBlank()) return false;
        Path file = instance.versionsDirectory().resolve(instance.launchVersionId())
                .resolve(instance.launchVersionId() + ".json");
        if (!Files.isRegularFile(file)) return false;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.object().from(reader);
            return instance.launchVersionId().equals(json.getString("id", ""));
        } catch (Exception _) {
            return false;
        }
    }

    private ModpackInstance readInstance(Path directory) {
        Path file = directory.resolve(METADATA_FILE);
        if (!Files.isRegularFile(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.object().from(reader);
            if (json.getInt("schemaVersion", -1) != METADATA_SCHEMA) return null;
            String id = json.getString("id", directory.getFileName().toString());
            if (!directory.getFileName().toString().equals(id)) return null;
            ModpackLoader loader;
            try {
                loader = ModpackLoader.valueOf(json.getString("loader", "VANILLA"));
            } catch (IllegalArgumentException _) {
                return null;
            }
            String name = json.getString("name", id);
            String versionId = json.getString("versionId", "");
            String minecraftVersion = json.getString("minecraftVersion", "");
            String loaderVersion = json.getString("loaderVersion", "");
            if (id.isBlank() || name.isBlank() || versionId.isBlank() || minecraftVersion.isBlank()) return null;
            if (loader != ModpackLoader.VANILLA && loaderVersion.isBlank()) return null;
            if (!Files.isRegularFile(directory.resolve("manifest").resolve(INDEX_FILE))) return null;

            Set<String> overrideFiles = new HashSet<>();
            for (Object item : json.getArray("overrideFiles", new JsonArray())) {
                if (!(item instanceof String value) || value.isBlank()) return null;
                Path target = instancePath(directory, value);
                overrideFiles.add(relativeKey(directory, target));
            }

            return new ModpackInstance(id, name, versionId, minecraftVersion, loader, loaderVersion,
                    json.getString("launchVersionId", ""), json.getBoolean("installed", false),
                    overrideFiles, directory);
        } catch (Exception exception) {
            LauncherLog.error("Unable to read modpack instance: " + directory.getFileName(), exception);
            return null;
        }
    }

    private void writeInstance(ModpackInstance instance) throws IOException {
        JsonObject json = new JsonObject();
        json.put("schemaVersion", METADATA_SCHEMA);
        json.put("id", instance.id());
        json.put("name", instance.name());
        json.put("versionId", instance.versionId());
        json.put("minecraftVersion", instance.minecraftVersion());
        json.put("loader", instance.loader().name());
        json.put("loaderVersion", instance.loaderVersion());
        json.put("launchVersionId", instance.launchVersionId());
        json.put("installed", instance.installed());
        JsonArray overrideFiles = new JsonArray();
        instance.overrideFiles().stream().sorted().forEach(overrideFiles::add);
        json.put("overrideFiles", overrideFiles);
        writeJsonAtomic(instance.directory().resolve(METADATA_FILE), json);
    }

    private JsonObject readIndex(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("Modpack index is missing.");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.object().from(reader);
        } catch (Exception exception) {
            throw new IOException("Unable to read modpack index.", exception);
        }
    }

    private Path instancePath(Path root, String relative) throws IOException {
        String value = normalizeZipPath(relative);
        if (value.isBlank() || value.startsWith("/") || value.startsWith("\\") || drivePath(value)
                || hasParentSegment(value)) {
            throw new IOException("Invalid modpack path.");
        }

        Path out = BackendUtils.safeResolve(root, value, false);
        Path base = root.toAbsolutePath().normalize();
        String firstName = base.relativize(out).getName(0).toString().toLowerCase(Locale.ROOT);
        if (METADATA_FILE.equals(firstName)
                || "manifest".equals(firstName)
                || "versions".equals(firstName)
                || firstName.startsWith(".litelauncher")) {
            throw new IOException("Modpack path targets launcher metadata: " + relative);
        }
        return out;
    }

    private String relativeKey(Path root, Path target) {
        String key = root.toAbsolutePath().normalize().relativize(target.toAbsolutePath().normalize())
                .toString().replace('\\', '/');
        return OSUtils.os().windows() ? key.toLowerCase(Locale.ROOT) : key;
    }

    private boolean hasParentSegment(String path) {
        for (String segment : path.split("/", -1)) {
            if ("..".equals(segment)) return true;
        }
        return false;
    }

    private void moveDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        BackendUtils.move(source, target);
    }

    private void writeJsonAtomic(Path file, JsonObject json) throws IOException {
        BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(json).done());
    }

    private byte[] readLimited(InputStream input, long limit) throws IOException {
        try (input; var output = new java.io.ByteArrayOutputStream()) {
            copyLimited(input, output, limit);
            return output.toByteArray();
        }
    }

    private long copyLimited(InputStream input, OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0L;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) continue;
            total += read;
            if (total > limit) throw new IOException("Archive entry is too large.");
            output.write(buffer, 0, read);
        }
        return total;
    }

    private String firstValidUrl(JsonArray urls) {
        for (Object item : urls) {
            if (item instanceof String value && validDownloadUrl(value)) return value;
        }
        return "";
    }

    private String instanceId(String name, String versionId) {
        String id = trimIdEdges(slug(name) + "-" + slug(versionId));
        if (id.isBlank()) id = "modpack";
        if (id.length() > 120) id = trimIdEdges(id.substring(0, 120));
        return id.isBlank() ? "modpack" : id;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD);
        StringBuilder result = new StringBuilder();
        boolean separator = false;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.getType(codePoint) == Character.NON_SPACING_MARK) continue;
            if (Character.isLetterOrDigit(codePoint)) {
                if (separator && !result.isEmpty()) result.append('-');
                result.appendCodePoint(Character.toLowerCase(codePoint));
                separator = false;
            } else if (codePoint == '.' || codePoint == '_') {
                if (separator && !result.isEmpty()) result.append('-');
                result.appendCodePoint(codePoint);
                separator = false;
            } else {
                separator = !result.isEmpty();
            }
        }
        return trimIdEdges(result.toString());
    }

    private String trimIdEdges(String value) {
        String result = value == null ? "" : value;
        int start = 0;
        int end = result.length();
        while (start < end && idSeparator(result.charAt(start))) start++;
        while (end > start && idSeparator(result.charAt(end - 1))) end--;
        return result.substring(start, end);
    }

    private boolean idSeparator(char value) {
        return value == '-' || value == '.' || value == '_';
    }

    private String normalizeZipPath(String value) {
        String path = value == null ? "" : value.replace('\\', '/');
        while (path.startsWith("./")) path = path.substring(2);
        return path;
    }

    private boolean drivePath(String path) {
        return path.length() >= 3 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':'
                && (path.charAt(2) == '/' || path.charAt(2) == '\\');
    }

    private boolean mrpackExtension(Path source) {
        String name = source.getFileName() == null ? "" : source.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".mrpack");
    }

    private String required(String value, String message) throws IOException {
        if (value == null || value.isBlank()) throw new IOException(message);
        return value;
    }

    private DownloadProgress scaled(DownloadProgress progress, double from, double to) {
        return (value, action, details) -> {
            if (progress != null) progress.update(from + (to - from) * value, action, details);
        };
    }

    private void report(DownloadProgress progress, double value, String action, String details) {
        if (progress != null) progress.update(value, action, details);
    }

    private void checkCancelled(CancellationToken cancellation) {
        if (cancellation != null) cancellation.throwIfCancelled();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Modpack installation cancelled.");
    }

    private record ParsedPack(String name, String versionId, String minecraftVersion,
                              ModpackLoader loader, String loaderVersion) {
    }
}
