package net.litelauncher;

import net.litelauncher.backend.auth.AuthException;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.launch.GameLaunchException;
import net.litelauncher.backend.launch.LaunchResult;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.version.Version;
import net.litelauncher.i18n.I18n;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class LauncherStore {

    public enum Event {
        PROFILES_CHANGED,
        SELECTED_PROFILE_CHANGED,
        VERSIONS_CHANGED,
        SELECTED_VERSION_CHANGED,
        LANGUAGE_CHANGED,
        THEME_CHANGED,
        SCALE_CHANGED,
        SETTINGS_CHANGED,
        FILTERS_CHANGED,
        LAUNCH_PROGRESS_CHANGED,
        GAME_STATUS_CHANGED
    }

    @FunctionalInterface
    public interface Listener {
        void onStoreEvent(Event event);
    }

    private static final LauncherStore INSTANCE = new LauncherStore();

    private final LauncherState state = LauncherState.load();
    private final LauncherServices services = new LauncherServices();
    private final List<Listener> listeners = new ArrayList<>();

    private final Set<String> refreshingProfiles = new HashSet<>();

    private List<Profile> profiles;
    private List<Version> versions;
    private boolean refreshingVersions;
    private boolean launchBusy;
    private int runningGames;
    private double launchProgress;
    private String launchActionText = "";
    private String launchDetailsText = "";
    private Timer hideLaunchProgressTimer;

    private LauncherStore() {
        I18n.setCurrentLanguage(state.language);
        profiles = services.authService().loadProfiles();
        versions = services.versionService().loadCachedVersions();
        normalizeProfileSelection();
        normalizeVersionSelection();
        state.save();
        refreshVersions();
        refreshMicrosoftProfile(selectedProfile());
    }

    public static LauncherStore get() {
        return INSTANCE;
    }

    public LauncherState state() {
        return state;
    }

    public void subscribe(Listener listener) {
        if (listener == null || listeners.contains(listener)) return;
        listeners.add(listener);
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public List<Profile> profiles() {
        return profiles;
    }

    public Profile selectedProfile() {
        return profileById(state.selectedProfileId);
    }

    public BufferedImage selectedProfileAvatar() {
        return profileAvatar(selectedProfile());
    }

    public BufferedImage profileAvatar(Profile profile) {
        return null; // Avatar rendering belongs to the redacted official UI layer.
    }

    public boolean isSelectedProfile(Profile profile) {
        return profile != null && Objects.equals(state.selectedProfileId, profile.id());
    }

    public void setProfiles(List<Profile> profiles) {
        if (runOnEdt(() -> setProfiles(profiles))) return;

        String oldSelected = state.selectedProfileId;
        this.profiles = profiles == null ? List.of() : List.copyOf(profiles);
        trimRefreshingProfiles();
        normalizeProfileSelection();
        services.authService().saveProfiles(this.profiles);

        saveStateAndEmit(Event.PROFILES_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedProfileId)) emit(Event.SELECTED_PROFILE_CHANGED);
    }

    public void addProfile(Profile profile) {
        if (runOnEdt(() -> addProfile(profile))) return;
        if (profile == null) return;

        List<Profile> profiles = new ArrayList<>(this.profiles);
        int existing = profileIndex(profile.id());
        if (existing >= 0) profiles.set(existing, profile);
        else profiles.add(profile);
        this.profiles = List.copyOf(profiles);

        String oldSelected = state.selectedProfileId;
        state.selectedProfileId = profile.id();
        services.authService().saveProfiles(this.profiles);

        saveStateAndEmit(Event.PROFILES_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedProfileId)) emit(Event.SELECTED_PROFILE_CHANGED);
    }

    public void openMicrosoftCallbackServer() throws AuthException {
        services.authService().openMicrosoftCallbackServer();
    }

    public void closeMicrosoftCallbackServer() {
        services.authService().closeMicrosoftCallbackServer();
    }

    public void signInWithMicrosoft(Consumer<String> onError) {
        services.backendExecutor().run(() -> {
            try {
                Profile profile = services.authService().signInWithMicrosoft(theme(), language());
                SwingUtilities.invokeLater(() -> addProfile(profile));
            } catch (Exception exception) {
                LauncherLog.error("Microsoft sign-in failed.", exception);
                SwingUtilities.invokeLater(() -> {
                    if (onError != null) onError.accept(InformationMessages.SIGN_IN_ERROR);
                });
            }
        });
    }

    public void selectProfile(int index) {
        if (runOnEdt(() -> selectProfile(index))) return;
        if (index < 0 || index >= profiles.size()) return;
        selectProfile(profiles.get(index));
    }

    public void selectProfile(Profile profile) {
        if (runOnEdt(() -> selectProfile(profile))) return;
        if (profile == null || Objects.equals(state.selectedProfileId, profile.id())) return;
        state.selectedProfileId = profile.id();
        saveStateAndEmit(Event.SELECTED_PROFILE_CHANGED);
        refreshMicrosoftProfile(profile);
    }

    public void deleteProfile(int index) {
        if (runOnEdt(() -> deleteProfile(index))) return;
        if (index < 0 || index >= profiles.size()) return;

        String oldSelected = state.selectedProfileId;
        Profile removed = profiles.get(index);
        List<Profile> profiles = new ArrayList<>(this.profiles);
        profiles.remove(index);
        this.profiles = List.copyOf(profiles);
        refreshingProfiles.remove(removed.id());

        if (Objects.equals(oldSelected, removed.id())) {
            state.selectedProfileId = this.profiles.isEmpty()
                    ? null
                    : this.profiles.get(Math.min(index, this.profiles.size() - 1)).id();
        }

        services.authService().saveProfiles(this.profiles);
        saveStateAndEmit(Event.PROFILES_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedProfileId)) {
            emit(Event.SELECTED_PROFILE_CHANGED);
            refreshMicrosoftProfile(selectedProfile());
        }
    }

    private void refreshMicrosoftProfile(Profile profile) {
        if (profile == null || !profile.microsoft() || refreshingProfiles.contains(profile.id())) return;
        refreshingProfiles.add(profile.id());

        services.backendExecutor().run(() -> {
            try {
                Profile updated = services.authService().refreshMicrosoftProfile(profile);
                SwingUtilities.invokeLater(() -> applyProfileUpdate(updated));
            } catch (AuthException exception) {
                LauncherLog.error("Unable to refresh Microsoft profile.", exception);
                SwingUtilities.invokeLater(() -> {
                    refreshingProfiles.remove(profile.id());
                    if (exception.expiredSession()) removeExpiredMicrosoftProfile(profile);
                });
            } catch (Exception exception) {
                LauncherLog.error("Unable to refresh Microsoft profile.", exception);
                SwingUtilities.invokeLater(() -> refreshingProfiles.remove(profile.id()));
            }
        });
    }

    private void removeExpiredMicrosoftProfile(Profile profile) {
        int index = profileIndex(profile == null ? null : profile.id());
        if (index < 0) return;

        String oldSelected = state.selectedProfileId;
        List<Profile> profiles = new ArrayList<>(this.profiles);
        profiles.remove(index);
        this.profiles = List.copyOf(profiles);
        normalizeProfileSelection();
        services.authService().saveProfiles(this.profiles);

        saveStateAndEmit(Event.PROFILES_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedProfileId)) {
            emit(Event.SELECTED_PROFILE_CHANGED);
            refreshMicrosoftProfile(selectedProfile());
        }
    }

    private void applyProfileUpdate(Profile profile) {
        if (profile == null) return;
        refreshingProfiles.remove(profile.id());

        int index = profileIndex(profile.id());
        if (index < 0) return;

        boolean changed = !profiles.get(index).equals(profile);
        if (changed) {
            List<Profile> profiles = new ArrayList<>(this.profiles);
            profiles.set(index, profile);
            this.profiles = List.copyOf(profiles);
            services.authService().saveProfiles(this.profiles);
        }

        emit(Event.PROFILES_CHANGED);
        if (changed && Objects.equals(state.selectedProfileId, profile.id())) emit(Event.SELECTED_PROFILE_CHANGED);
    }

    public List<Version> versions() {
        List<Version> visible = new ArrayList<>();
        for (Version version : versions) if (isVisible(version)) visible.add(version);
        return List.copyOf(visible);
    }

    public Version selectedVersion() {
        return versionById(state.selectedVersionId);
    }

    public boolean isSelectedVersion(Version version) {
        return version != null && Objects.equals(state.selectedVersionId, version.id());
    }

    public void setVersions(List<Version> versions) {
        if (runOnEdt(() -> setVersions(versions))) return;
        applyVersions(versions == null ? List.of() : List.copyOf(versions));
    }

    public void refreshVersions() {
        if (refreshingVersions) return;
        refreshingVersions = true;
        services.backendExecutor().run(() -> {
            try {
                List<Version> loadedVersions = services.versionService().loadVersions();
                SwingUtilities.invokeLater(() -> {
                    refreshingVersions = false;
                    applyVersions(loadedVersions);
                });
            } catch (Exception exception) {
                LauncherLog.error("Unable to refresh versions.", exception);
                SwingUtilities.invokeLater(() -> refreshingVersions = false);
            }
        });
    }

    public void refreshLocalVersions() {
        if (refreshingVersions) return;
        refreshingVersions = true;
        List<Version> currentVersions = List.copyOf(versions);
        services.backendExecutor().run(() -> {
            try {
                List<Version> loadedVersions = services.versionService().refreshLocalVersions(currentVersions);
                SwingUtilities.invokeLater(() -> {
                    refreshingVersions = false;
                    applyVersionsIfChanged(loadedVersions);
                });
            } catch (Exception exception) {
                LauncherLog.error("Unable to refresh local versions.", exception);
                SwingUtilities.invokeLater(() -> refreshingVersions = false);
            }
        });
    }

    public void selectVersion(int index) {
        if (runOnEdt(() -> selectVersion(index))) return;
        List<Version> visible = versions();
        if (index < 0 || index >= visible.size()) return;
        selectVersion(visible.get(index));
    }

    public void selectVersion(Version version) {
        if (runOnEdt(() -> selectVersion(version))) return;
        if (version == null || Objects.equals(state.selectedVersionId, version.id())) return;
        state.selectedVersionId = version.id();
        saveStateAndEmit(Event.SELECTED_VERSION_CHANGED);
    }

    public boolean launchBusy() {
        return launchBusy;
    }

    public double launchProgress() {
        return launchProgress;
    }

    public boolean gameRunning() {
        return runningGames > 0;
    }

    public String launchActionText() {
        return launchActionText;
    }

    public String launchDetailsText() {
        return launchDetailsText;
    }

    public void launchSelectedGame(Consumer<String> onError) {
        if (launchBusy) return;
        launchBusy = true;
        I18n.setCurrentLanguage(state.language);
        setLaunchProgress(0.0, I18n.text("progress.preparingLaunch"), "");

        Profile profile = selectedProfile();
        Version version = selectedVersion();
        LauncherState snapshot = launchStateSnapshot();
        services.backendExecutor().run(() -> {
            try {
                LaunchResult result = services.gameLaunchService().launch(profile, version, snapshot, this::setLaunchProgress, this::addRunningGame);
                SwingUtilities.invokeLater(() -> completeLaunch(result, snapshot.closeAfterLaunch));
            } catch (AuthException exception) {
                LauncherLog.error("Launch failed.", exception);
                SwingUtilities.invokeLater(() -> failLaunch(profile, exception, onError));
            } catch (Exception exception) {
                LauncherLog.error("Launch failed.", exception);
                SwingUtilities.invokeLater(() -> failLaunch(null, exception, onError));
            }
        });
    }

    private void addRunningGame(Process process) {
        SwingUtilities.invokeLater(() -> {
            runningGames++;
            emit(Event.GAME_STATUS_CHANGED);
        });
        process.onExit().thenRun(() -> SwingUtilities.invokeLater(() -> {
            if (runningGames > 0) runningGames--;
            emit(Event.GAME_STATUS_CHANGED);
        }));
    }

    private LauncherState launchStateSnapshot() {
        LauncherState copy = new LauncherState();
        copy.scale = state.scale;
        copy.language = state.language;
        copy.theme = state.theme;
        copy.memoryAmount = state.memoryAmount;
        copy.autoMemory = state.autoMemory;
        copy.screenWidth = state.screenWidth;
        copy.screenHeight = state.screenHeight;
        copy.fullscreen = state.fullscreen;
        copy.jvmArguments = state.jvmArguments;
        copy.closeAfterLaunch = state.closeAfterLaunch;
        copy.releaseFilter = state.releaseFilter;
        copy.moddedFilter = state.moddedFilter;
        copy.snapshotFilter = state.snapshotFilter;
        copy.legacyFilter = state.legacyFilter;
        copy.selectedProfileId = state.selectedProfileId;
        copy.selectedVersionId = state.selectedVersionId;
        copy.normalize();
        return copy;
    }

    private void completeLaunch(LaunchResult result, boolean closeAfterLaunch) {
        if (result != null && result.updatedProfile() != null) applyProfileUpdate(result.updatedProfile());
        setLaunchProgress(1.0, I18n.text("progress.gameStarted"), "");
        if (closeAfterLaunch) {
            closeLauncherWindow();
            return;
        }
        hideLaunchProgressLater();
    }

    private void failLaunch(Profile profile, Exception exception, Consumer<String> onError) {
        launchBusy = false;
        launchActionText = "";
        launchDetailsText = "";
        if (hideLaunchProgressTimer != null) hideLaunchProgressTimer.stop();
        emit(Event.LAUNCH_PROGRESS_CHANGED);

        if (exception instanceof AuthException authException && authException.expiredSession()) removeExpiredMicrosoftProfile(profile);
        if (onError != null) onError.accept(InformationMessages.launch(exception));
    }

    private void setLaunchProgress(double progress, String actionText, String detailsText) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setLaunchProgress(progress, actionText, detailsText));
            return;
        }
        launchProgress = Math.max(0.0, Math.min(1.0, progress));
        launchActionText = actionText == null ? "" : actionText;
        launchDetailsText = detailsText == null ? "" : detailsText;
        emit(Event.LAUNCH_PROGRESS_CHANGED);
    }

    private void hideLaunchProgressLater() {
        if (hideLaunchProgressTimer != null) hideLaunchProgressTimer.stop();
        hideLaunchProgressTimer = new Timer(2000, event -> {
            launchBusy = false;
            launchActionText = "";
            launchDetailsText = "";
            launchProgress = 0.0;
            emit(Event.LAUNCH_PROGRESS_CHANGED);
        });
        hideLaunchProgressTimer.setRepeats(false);
        hideLaunchProgressTimer.start();
    }

    private void closeLauncherWindow() {
        Window window = LiteLauncher.window;
        if (window == null) {
            System.exit(0);
            return;
        }
        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }

    public Language language() {
        return state.language;
    }

    public void setLanguage(Language language) {
        if (runOnEdt(() -> setLanguage(language))) return;
        if (language == null || state.language == language) return;
        state.language = language;
        I18n.setCurrentLanguage(language);
        saveStateAndEmit(Event.LANGUAGE_CHANGED);
    }

    public Theme theme() {
        return state.theme;
    }

    public void setTheme(Theme theme) {
        if (runOnEdt(() -> setTheme(theme))) return;
        if (theme == null || state.theme == theme) return;
        state.theme = theme;
        saveStateAndEmit(Event.THEME_CHANGED);
    }

    public int scale() {
        return state.scale;
    }

    public void setScale(int scale) {
        if (runOnEdt(() -> setScale(scale))) return;
        int safeScale = Math.max(1, scale);
        if (state.scale == safeScale) return;
        state.scale = safeScale;
        saveStateAndEmit(Event.SCALE_CHANGED);
    }

    public int memoryAmount() {
        return state.memoryAmount;
    }

    public void setMemoryAmount(int memoryAmount) {
        if (runOnEdt(() -> setMemoryAmount(memoryAmount))) return;
        int safeMemory = Math.max(512, Math.min(OSUtils.maxMemoryMb(), memoryAmount));
        if (state.memoryAmount == safeMemory) return;
        state.memoryAmount = safeMemory;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public boolean autoMemory() {
        return state.autoMemory;
    }

    public void setAutoMemory(boolean autoMemory) {
        if (runOnEdt(() -> setAutoMemory(autoMemory))) return;

        int memory = autoMemory ? OSUtils.automaticMemoryMb() : state.memoryAmount;
        if (state.autoMemory == autoMemory && state.memoryAmount == memory) return;

        state.autoMemory = autoMemory;
        state.memoryAmount = memory;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public int screenWidth() {
        return state.screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        if (runOnEdt(() -> setScreenWidth(screenWidth))) return;
        int safeWidth = Math.max(1, screenWidth);
        if (state.screenWidth == safeWidth) return;
        state.screenWidth = safeWidth;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public int screenHeight() {
        return state.screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        if (runOnEdt(() -> setScreenHeight(screenHeight))) return;
        int safeHeight = Math.max(1, screenHeight);
        if (state.screenHeight == safeHeight) return;
        state.screenHeight = safeHeight;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public boolean fullscreen() {
        return state.fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        if (runOnEdt(() -> setFullscreen(fullscreen))) return;
        if (state.fullscreen == fullscreen) return;
        state.fullscreen = fullscreen;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public String jvmArguments() {
        return state.jvmArguments;
    }

    public void setJvmArguments(String jvmArguments) {
        if (runOnEdt(() -> setJvmArguments(jvmArguments))) return;
        String safeArguments = jvmArguments == null ? "" : jvmArguments;
        if (Objects.equals(state.jvmArguments, safeArguments)) return;
        state.jvmArguments = safeArguments;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public boolean closeAfterLaunch() {
        return state.closeAfterLaunch;
    }

    public void setCloseAfterLaunch(boolean closeAfterLaunch) {
        if (runOnEdt(() -> setCloseAfterLaunch(closeAfterLaunch))) return;
        if (state.closeAfterLaunch == closeAfterLaunch) return;
        state.closeAfterLaunch = closeAfterLaunch;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public boolean releaseFilter() {
        return state.releaseFilter;
    }

    public void setReleaseFilter(boolean releaseFilter) {
        if (runOnEdt(() -> setReleaseFilter(releaseFilter))) return;
        if (state.releaseFilter == releaseFilter) return;
        state.releaseFilter = releaseFilter;
        saveStateAndEmit(Event.FILTERS_CHANGED);
    }

    public boolean snapshotFilter() {
        return state.snapshotFilter;
    }

    public void setSnapshotFilter(boolean snapshotFilter) {
        if (runOnEdt(() -> setSnapshotFilter(snapshotFilter))) return;
        if (state.snapshotFilter == snapshotFilter) return;
        state.snapshotFilter = snapshotFilter;
        saveStateAndEmit(Event.FILTERS_CHANGED);
    }

    public boolean moddedFilter() {
        return state.moddedFilter;
    }

    public void setModdedFilter(boolean moddedFilter) {
        if (runOnEdt(() -> setModdedFilter(moddedFilter))) return;
        if (state.moddedFilter == moddedFilter) return;
        state.moddedFilter = moddedFilter;
        saveStateAndEmit(Event.FILTERS_CHANGED);
    }

    public boolean legacyFilter() {
        return state.legacyFilter;
    }

    public void setLegacyFilter(boolean legacyFilter) {
        if (runOnEdt(() -> setLegacyFilter(legacyFilter))) return;
        if (state.legacyFilter == legacyFilter) return;
        state.legacyFilter = legacyFilter;
        saveStateAndEmit(Event.FILTERS_CHANGED);
    }

    private void applyVersions(List<Version> versions) {
        String oldSelected = state.selectedVersionId;
        this.versions = versions == null ? List.of() : List.copyOf(versions);
        normalizeVersionSelection();

        saveStateAndEmit(Event.VERSIONS_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedVersionId)) emit(Event.SELECTED_VERSION_CHANGED);
    }

    private void applyVersionsIfChanged(List<Version> versions) {
        List<Version> nextVersions = versions == null ? List.of() : List.copyOf(versions);
        if (this.versions.equals(nextVersions)) return;
        applyVersions(nextVersions);
    }

    private void normalizeProfileSelection() {
        if (profileById(state.selectedProfileId) == null) {
            state.selectedProfileId = profiles.isEmpty() ? null : profiles.getFirst().id();
        }
    }

    private void normalizeVersionSelection() {
        if (versionById(state.selectedVersionId) == null) {
            state.selectedVersionId = versions.isEmpty() ? null : versions.getFirst().id();
        }
    }

    private void trimRefreshingProfiles() {
        Set<String> keep = profiles.stream().map(Profile::id).collect(java.util.stream.Collectors.toSet());
        refreshingProfiles.removeIf(id -> !keep.contains(id));
    }

    private int profileIndex(String id) {
        if (id == null) return -1;
        for (int index = 0; index < profiles.size(); index++) {
            if (id.equals(profiles.get(index).id())) return index;
        }
        return -1;
    }

    private Profile profileById(String id) {
        int index = profileIndex(id);
        return index < 0 ? null : profiles.get(index);
    }

    private Version versionById(String id) {
        if (id == null) return null;
        for (Version version : versions) if (id.equals(version.id())) return version;
        return null;
    }

    private boolean isVisible(Version version) {
        if (version == null) return false;
        if (state.moddedFilter && version.modded()) return true;
        if (!state.legacyFilter && version.legacy()) return false;
        if (!state.snapshotFilter && version.snapshot()) return false;
        if (!state.releaseFilter && version.releaseLike()) return false;
        return true;
    }

    private boolean runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) return false;
        SwingUtilities.invokeLater(action);
        return true;
    }

    private void saveStateAndEmit(Event event) {
        state.normalize();
        state.save();
        emit(event);
    }

    private void emit(Event event) {
        if (SwingUtilities.isEventDispatchThread()) emitNow(event);
        else SwingUtilities.invokeLater(() -> emitNow(event));
    }

    private void emitNow(Event event) {
        for (Listener listener : List.copyOf(listeners)) listener.onStoreEvent(event);
    }
}
