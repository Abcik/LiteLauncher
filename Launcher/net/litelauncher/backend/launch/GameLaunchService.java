package net.litelauncher.backend.launch;

import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.CancellationToken;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.auth.AuthException;
import net.litelauncher.backend.auth.AuthService;
import net.litelauncher.backend.auth.LaunchAccount;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.download.DownloadException;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.backend.download.DownloadService;
import net.litelauncher.backend.java.JavaRuntimeService;
import net.litelauncher.backend.modpack.ModpackInstance;
import net.litelauncher.backend.loader.LoaderInstaller;
import net.litelauncher.backend.modpack.ModpackService;
import net.litelauncher.backend.version.Version;
import net.litelauncher.backend.version.VersionService;
import net.litelauncher.i18n.I18n;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.CancellationException;

public final class GameLaunchService {

    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

    private final AuthService authService;
    private final VersionResolver versions;
    private final ModpackService modpacks;
    private final ElyByAuthlibCatalog elyByAuthlibCatalog;
    private final LoaderInstaller loaderInstaller;
    private final DownloadService downloads;
    private final JavaRuntimeService javaRuntime;
    private final GameArgumentBuilder arguments = new GameArgumentBuilder();
    private final ClientJarPatcher clientPatcher = new ClientJarPatcher();
    private final LegacyOptionsFixer legacyOptions = new LegacyOptionsFixer();
    private final NativeLibraryExtractor nativeExtractor = new NativeLibraryExtractor();
    private final LaunchArtifacts artifacts = new LaunchArtifacts();

    public GameLaunchService(AuthService authService, VersionService versionService, ModpackService modpacks,
                             ElyByAuthlibCatalog elyByAuthlibCatalog, LoaderInstaller loaderInstaller, DownloadService downloads) {
        if (authService == null || versionService == null || modpacks == null || elyByAuthlibCatalog == null
                || loaderInstaller == null || downloads == null) throw new IllegalArgumentException("Launch dependencies are required.");
        this.authService = authService;
        this.versions = new VersionResolver(versionService);
        this.modpacks = modpacks;
        this.elyByAuthlibCatalog = elyByAuthlibCatalog;
        this.loaderInstaller = loaderInstaller;
        this.downloads = downloads;
        this.javaRuntime = new JavaRuntimeService(downloads);
    }

    public LaunchResult launch(Profile profile, Version version, LaunchSettings settings, LaunchProgress progress, Consumer<Process> onProcessStarted, CancellationToken cancellation) throws GameLaunchException, AuthException {
        checkCancelled(cancellation);
        LauncherLog.info("Launch started");
        LauncherLog.info("Selected profile: " + (profile == null ? "null" : profile.username() + " / " + profile.id() + " / microsoft=" + profile.microsoft()));
        LauncherLog.info("Selected version: " + (version == null ? "null" : version.id() + " / " + version.url()));
        if (profile == null) throw new GameLaunchException("Create or select a profile before launch.", InformationMessages.SELECT_PROFILE);
        if (version == null) throw new GameLaunchException("Select a Minecraft version before launch.", InformationMessages.SELECT_VERSION);

        checkCancelled(cancellation);
        progress.update(0.01, I18n.text(settings.language(), "progress.preparingLaunch"), "account");
        LaunchAccount account = authService.prepareLaunchAccount(profile);
        LauncherLog.info("Launch account prepared: username=" + account.username() + ", uuid=" + account.uuid() + ", userType=" + account.userType());

        LaunchEnvironment environment;
        double assetCheckProgress;
        double planningProgress;
        if (version.modpack()) {
            checkCancelled(cancellation);
            progress.update(0.03, I18n.text(settings.language(), "progress.preparingModpack"), version.title());
            try {
                ModpackInstance instance = modpacks.ensureReady(version.id(),
                        scaledDownloads(progress, 0.03, 0.30), cancellation);
                environment = LaunchEnvironment.modpack(instance);
            } catch (CancellationException exception) {
                throw exception;
            } catch (DownloadException exception) {
                throw downloadException(exception);
            } catch (Exception exception) {
                if (cancellation != null && cancellation.cancelled()) {
                    throw new CancellationException("Modpack preparation cancelled.");
                }
                throw new GameLaunchException("Unable to prepare modpack.", exception, InformationMessages.DOWNLOAD_ERROR);
            }
            assetCheckProgress = 0.33;
            planningProgress = 0.36;
        } else if (version.pendingLoader()) {
            checkCancelled(cancellation);
            progress.update(0.03, I18n.text(settings.language(), "progress.preparingLaunch"), version.title());
            String launchId;
            Path targetVersions;
            try {
                targetVersions = settings.instancesStorageSystem()
                        ? LauncherPaths.instanceVersionsDirectory(version.id())
                        : LauncherPaths.versionsDirectory();
                Path installedJson = targetVersions.resolve(version.id()).resolve(version.id() + ".json");
                if (isValidVersionJson(installedJson, version.id())) {
                    launchId = version.id();
                } else {
                    Path work = LauncherPaths.launcherDataDirectory().resolve("loader-work").resolve(safePath(version.id()));
                    launchId = loaderInstaller.install(version.loader(), targetVersions, work, "",
                            scaledDownloads(progress, 0.03, 0.30), cancellation);
                }
            } catch (CancellationException exception) {
                throw exception;
            } catch (DownloadException exception) {
                throw downloadException(exception);
            } catch (Exception exception) {
                if (cancellation != null && cancellation.cancelled()) throw new CancellationException("Loader installation cancelled.");
                throw new GameLaunchException("Unable to install mod loader.", exception, InformationMessages.DOWNLOAD_ERROR);
            }
            environment = settings.instancesStorageSystem()
                    ? LaunchEnvironment.instance(version.id(), launchId)
                    : LaunchEnvironment.minecraft(launchId);
            assetCheckProgress = 0.33;
            planningProgress = 0.36;
        } else {
            environment = settings.instancesStorageSystem()
                    ? LaunchEnvironment.instance(version.id(), version.id())
                    : LaunchEnvironment.minecraft(version.id());
            assetCheckProgress = 0.05;
            planningProgress = 0.08;
        }
        try {
            Files.createDirectories(environment.gameDirectory());
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to create game directory.", exception);
        }

        checkCancelled(cancellation);
        progress.update((version.modpack() || version.pendingLoader()) ? 0.31 : 0.03, I18n.text(settings.language(), "progress.preparingLaunch"), "version");
        ResolvedVersion resolved = versions.resolve(version, environment);
        if (resolved.mainClass() == null || resolved.mainClass().isBlank())
            throw new GameLaunchException("Version has no mainClass: " + resolved.id());
        LauncherLog.info("Version resolved: id=" + resolved.id() + ", jar=" + resolved.jarId() + ", clientDownload=" + resolved.clientDownloadId() + ", java=" + resolved.javaMajor() + ", libraries=" + resolved.libraries().size());

        ClientJarPlan clientJar = planClientJar(resolved);
        boolean shouldPatch = clientPatcher.needsPatch(resolved);
        ElyByAuthlibPlan elyByAuthlib = shouldUseElyBy(profile)
                ? elyByAuthlibCatalog.plan(resolved)
                : ElyByAuthlibPlan.unavailable();
        LauncherLog.info("Client jar planned: id=" + clientJar.id() + ", original=" + clientJar.originalPath() + ", patch=" + shouldPatch);

        checkCancelled(cancellation);
        progress.update(assetCheckProgress, I18n.text(settings.language(), "progress.checkingFiles"), "asset index");
        DownloadFile assetIndexFile = assetIndexDownload(resolved);
        try {
            if (assetIndexFile != null && !downloads.isPresent(assetIndexFile, cancellation)) {
                downloads.download(List.of(assetIndexFile), scaledDownloads(progress, assetCheckProgress, planningProgress), cancellation);
            }
        } catch (DownloadException exception) {
            throw downloadException(exception);
        }
        checkCancelled(cancellation);
        JsonObject assetIndex = assetIndexFile != null && Files.isRegularFile(assetIndexFile.path()) ? readJson(assetIndexFile.path()) : null;

        checkCancelled(cancellation);
        progress.update(planningProgress, I18n.text(settings.language(), "progress.checkingFiles"), "planning");
        List<DownloadFile> files = new ArrayList<>();
        addClient(files, resolved, clientJar);
        files.addAll(artifacts.libraryDownloads(resolved));
        if (assetIndexFile != null) {
            files.add(assetIndexFile);
            addAssets(files, assetIndex);
        }

        LauncherLog.info("Files queued for validation/download: " + files.size());
        try {
            downloads.download(files, scaledDownloads(progress, planningProgress, 0.84), cancellation);
        } catch (DownloadException exception) {
            throw downloadException(exception);
        }

        checkCancelled(cancellation);
        progress.update(0.84, I18n.text(settings.language(), "progress.preparingLaunch"), "assets");
        mapLegacyAssets(assetIndex, environment.gameDirectory(), cancellation);

        elyByAuthlib = prepareElyByAuthlib(elyByAuthlib, progress, cancellation);

        if (shouldPatch) {
            checkCancelled(cancellation);
            progress.update(0.87, I18n.text(settings.language(), "progress.patchingOldClient"), resolved.id());
            clientJar = clientJar.withRuntime(clientPatcher.ensurePatched(clientJar.originalPath(), clientSha1(resolved, clientJar), cancellation));
            LauncherLog.info("Client jar runtime: " + clientJar.runtimePath() + ", patched=" + clientJar.patched());
        }

        checkCancelled(cancellation);
        progress.update(0.89, I18n.text(settings.language(), "progress.preparingNatives"), resolved.id());
        Path natives = nativeExtractor.extract(environment.nativesDirectory(), artifacts.nativeLibraries(resolved), cancellation);
        LauncherLog.info("Natives ready at: " + natives);

        checkCancelled(cancellation);
        Path java = javaRuntime.ensureJava(resolved.javaMajor(), scaledLaunch(progress, 0.91, 0.98), cancellation);
        LauncherLog.info("Java executable: " + java);

        checkCancelled(cancellation);
        progress.update(0.98, I18n.text(settings.language(), "progress.preparingLaunch"), "command");
        legacyOptions.fixIfNeeded(resolved, environment.gameDirectory());
        LaunchClasspath classpath = artifacts.classpath(resolved, clientJar, elyByAuthlib);
        List<String> command = arguments.build(java, resolved, assetIndex, account, settings, environment.gameDirectory(),
                natives, classpath, elyByAuthlib.active());
        LauncherLog.info("Command prepared: args=" + command.size() + ", libraries=" + classpath.libraryCount() + ", client=" + clientJar.runtimeFileName() + ", elyBy=" + elyByAuthlib.active() + ", classpathLength=" + classpath.length());

        checkCancelled(cancellation);
        progress.update(0.99, I18n.text(settings.language(), "progress.startingGame"), resolved.id());
        startMinecraft(command, resolved.id(), environment.gameDirectory(), onProcessStarted, cancellation);
        progress.update(1.0, I18n.text(settings.language(), "progress.gameStarted"), resolved.id());
        LauncherLog.info("Process started.");
        return new LaunchResult(account.updatedProfile());
    }

    private ElyByAuthlibPlan prepareElyByAuthlib(ElyByAuthlibPlan plan, LaunchProgress progress, CancellationToken cancellation) {
        if (plan == null || plan.override() == null) return ElyByAuthlibPlan.unavailable();

        try {
            checkCancelled(cancellation);
            progress.update(0.85, I18n.text("progress.checkingFiles"), "Ely.by Authlib");
            DownloadFile file = plan.download();
            if (!downloads.isPresent(file, cancellation)) {
                downloads.download(List.of(file), scaledDownloads(progress, 0.85, 0.86), cancellation);
            }
            LauncherLog.info("Ely.by Authlib enabled: com.mojang:authlib:" + plan.sourceVersion()
                    + " -> by.ely:authlib:" + plan.override().patchedVersion());
            return plan.activate(file.path());
        } catch (CancellationException exception) {
            throw exception;
        } catch (Exception exception) {
            LauncherLog.error("Unable to prepare Ely.by Authlib; Minecraft will start without Ely.by skin support.", exception);
            return ElyByAuthlibPlan.unavailable();
        }
    }

    private boolean shouldUseElyBy(Profile profile) {
        return profile != null && !profile.microsoft() && profile.elyBy();
    }

    private void startMinecraft(List<String> command, String versionId, Path gameDirectory, Consumer<Process> onProcessStarted, CancellationToken cancellation) throws GameLaunchException {
        try {
            checkCancelled(cancellation);
            Process process = new ProcessBuilder(command)
                    .directory(gameDirectory.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            LauncherLog.info("Minecraft process started for " + versionId);
            if (onProcessStarted != null) onProcessStarted.accept(process);
            watchMinecraft(process, versionId);
        } catch (CancellationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to start Minecraft.", exception);
        }
    }

    private void watchMinecraft(Process process, String versionId) {
        Thread.ofVirtual().name("game-watcher").start(() -> {
            try {
                int code = process.waitFor();
                LauncherLog.info("Minecraft process exited with code " + code + " for " + versionId);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                LauncherLog.error("Unable to watch Minecraft process.", exception);
            }
        });
    }


    private GameLaunchException downloadException(DownloadException exception) {
        String message = exception.connectionProblem()
                ? "Unable to connect to download server."
                : "Unable to download files.";
        return new GameLaunchException(message, exception, InformationMessages.DOWNLOAD_ERROR);
    }

    private void checkCancelled(CancellationToken cancellation) {
        if (cancellation != null) cancellation.throwIfCancelled();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Launch cancelled.");
    }

    private net.litelauncher.backend.download.DownloadProgress scaledDownloads(LaunchProgress progress, double from, double to) {
        return (value, action, details) -> progress.update(from + (to - from) * value, action, details);
    }

    private LaunchProgress scaledLaunch(LaunchProgress progress, double from, double to) {
        return (value, action, details) -> progress.update(from + (to - from) * value, action, details);
    }

    private ClientJarPlan planClientJar(ResolvedVersion version) throws GameLaunchException {
        if (version.jarExplicit()) {
            String id = BackendUtils.firstText(version.jarId(), version.clientDownloadId(), version.selectedId());
            return ClientJarPlan.original(id, versionJar(version, id));
        }

        Path selectedJar = versionJar(version, version.selectedId());
        if (usableClientJar(selectedJar)) return ClientJarPlan.original(version.selectedId(), selectedJar);
        if (Files.isRegularFile(selectedJar)) {
            LauncherLog.info("Ignoring empty client jar placeholder: " + selectedJar);
        }

        String id = BackendUtils.firstText(version.clientDownloadId(), version.jarId(), version.selectedId());
        return ClientJarPlan.original(id, versionJar(version, id));
    }

    private static boolean usableClientJar(Path path) {
        if (!Files.isRegularFile(path)) return false;
        try {
            return Files.size(path) > 0L;
        } catch (Exception _) {
            return false;
        }
    }

    private void addClient(List<DownloadFile> files, ResolvedVersion version, ClientJarPlan client) {
        JsonObject download = version.clientDownload();
        if (download == null || client == null || !client.id().equals(version.clientDownloadId())) return;
        files.add(new DownloadFile(download.getString("url", ""), client.originalPath(), download.getString("sha1", ""), download.getLong("size", 0L), I18n.text("progress.downloadingClient")));
    }

    private String clientSha1(ResolvedVersion version, ClientJarPlan client) {
        JsonObject download = version.clientDownload();
        if (download == null || client == null || !client.id().equals(version.clientDownloadId())) return "";
        return download.getString("sha1", "");
    }


    private DownloadFile assetIndexDownload(ResolvedVersion version) throws GameLaunchException {
        JsonObject index = version.assetIndex();
        if (index == null) return null;
        String id = BackendUtils.firstText(index.getString("id"), version.assets(), "legacy");
        return new DownloadFile(index.getString("url", ""), assetIndexPath(id), index.getString("sha1", ""), index.getLong("size", 0L), I18n.text("progress.downloadingAssetIndex"));
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
            files.add(new DownloadFile(RESOURCES_URL + hash.substring(0, 2) + "/" + hash, path, hash, asset.getLong("size", 0L), I18n.text("progress.downloadingAssets")));
        }
    }

    private void mapLegacyAssets(JsonObject index, Path gameDirectory, CancellationToken cancellation) throws GameLaunchException {
        checkCancelled(cancellation);
        if (index == null || (!index.getBoolean("virtual", false) && !index.getBoolean("map_to_resources", false))) return;
        JsonObject objects = index.getObject("objects");
        if (objects == null) return;

        Path target = index.getBoolean("map_to_resources", false)
                ? gameDirectory.resolve("resources")
                : LauncherPaths.assetsDirectory().resolve("virtual").resolve("legacy");

        try {
            for (Map.Entry<String, Object> entry : objects.entrySet()) {
                checkCancelled(cancellation);
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
        } catch (CancellationException exception) {
            throw exception;
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

    private boolean isValidVersionJson(Path file, String expectedId) {
        if (!Files.isRegularFile(file)) return false;
        try (InputStream input = Files.newInputStream(file)) {
            JsonObject json = JsonParser.object().from(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            String id = json.getString("id", "");
            return expectedId != null && expectedId.equals(id);
        } catch (Exception exception) {
            LauncherLog.info("Ignoring invalid installed version JSON: " + file + " (" + exception.getMessage() + ")");
            return false;
        }
    }

    private Path versionJar(ResolvedVersion version, String id) throws GameLaunchException {
        Path local = safeResolveVersionJar(version.environment().versionsDirectory(), id);
        if (Files.isRegularFile(local)) return local;
        if (version.environment().instance()) return local;
        if (version.environment().modpack() && id.equals(version.selectedId())) return local;
        return safeResolveVersionJar(LauncherPaths.versionsDirectory(), id);
    }

    private Path assetIndexPath(String id) throws GameLaunchException {
        return safeResolve(LauncherPaths.assetIndexesDirectory(), id + ".json", "Invalid asset index path.");
    }

    private Path assetObjectPath(String hash) throws GameLaunchException {
        String value = hash == null ? "" : hash.replace('\\', '/');
        if (value.length() < 2) throw new GameLaunchException("Invalid asset object path.");
        return safeResolve(LauncherPaths.assetObjectsDirectory(), value.substring(0, 2) + "/" + value, "Invalid asset object path.");
    }

    private Path safeResolveVersionJar(Path versionsDirectory, String id) throws GameLaunchException {
        String value = id == null ? "" : id;
        if (value.isBlank()) throw new GameLaunchException("Invalid version path.");
        return safeResolve(versionsDirectory, value + "/" + value + ".jar", "Invalid version path.");
    }


    private Path safeResolve(Path root, String relative, String error) throws GameLaunchException {
        try {
            return BackendUtils.safeResolve(root, relative);
        } catch (Exception exception) {
            throw new GameLaunchException(error, exception);
        }
    }


    private String safePath(String value) {
        String safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "loader" : safe;
    }

}
