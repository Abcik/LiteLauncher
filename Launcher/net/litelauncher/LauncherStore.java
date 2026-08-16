package net.litelauncher;

import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.auth.AuthException;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.loader.LoaderOption;
import net.litelauncher.backend.loader.LoaderVersion;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.backend.version.Version;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.modules.skin.DefaultPlayerSkin;
import net.litelauncher.i18n.I18n;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class LauncherStore {

    public enum Event {
        PROFILES_CHANGED,
        PROFILE_APPEARANCE_CHANGED,
        SELECTED_PROFILE_CHANGED,
        VERSIONS_CHANGED,
        SELECTED_VERSION_CHANGED,
        LANGUAGE_CHANGED,
        THEME_CHANGED,
        THEME_COLORS_CHANGED,
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
    private final ProfileController profiles;
    private final VersionController versions;
    private final LaunchController launch;

    private LauncherStore() {
        I18n.setCurrentLanguage(state.language);
        Palette.setCustomThemesEnabled(state.customThemes);

        profiles = new ProfileController(this, state, services);
        versions = new VersionController(this, state, services);
        launch = new LaunchController(this, state, services, profiles, versions);

        state.save();
        versions.start();
        profiles.start();
    }

    public static LauncherStore get() {
        return INSTANCE;
    }

    public LauncherState state() {
        return state;
    }

    public void subscribe(Listener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public List<Profile> profiles() { return profiles.all(); }
    public Profile selectedProfile() { return profiles.selected(); }
    public BufferedImage selectedProfileAvatar() { return profiles.selectedAvatar(); }
    public DefaultPlayerSkin.Skin offlineSkin(Profile profile) { return profiles.offlineSkin(profile); }
    public void refreshOfflineSkin(Profile profile) { profiles.refreshOfflineSkin(profile); }
    public boolean isSelectedProfile(Profile profile) { return profiles.isSelected(profile); }
    public void addProfile(Profile profile) { profiles.add(profile); }
    public void setOfflineElyBy(Profile profile, boolean enabled) { profiles.setOfflineElyBy(profile, enabled); }
    public void openMicrosoftCallbackServer() throws AuthException { profiles.openMicrosoftCallbackServer(); }
    public void closeMicrosoftCallbackServer() { profiles.closeMicrosoftCallbackServer(); }
    public void signInWithMicrosoft(Consumer<String> onError) { profiles.signInWithMicrosoft(onError); }

    public void uploadMicrosoftSkin(Profile profile, byte[] skinPng, boolean slim, Consumer<String> onError, Runnable onComplete) {
        profiles.uploadMicrosoftSkin(profile, skinPng, slim, onError, onComplete);
    }

    public void setMicrosoftCape(Profile profile, String capeId, Consumer<String> onError, Runnable onComplete) {
        profiles.setMicrosoftCape(profile, capeId, onError, onComplete);
    }

    public void selectProfile(int index) { profiles.select(index); }
    public void selectProfile(Profile profile) { profiles.select(profile); }
    public void deleteProfile(int index) { profiles.delete(index); }

    public List<Version> versions() { return versions.visible(); }
    public Version selectedVersion() { return versions.selected(); }
    public boolean isSelectedVersion(Version version) { return versions.isSelected(version); }
    public void refreshLocalVersions() { versions.refreshLocal(); }
    public void importModpack(Path file, Consumer<String> onError) { versions.importModpack(file, onError); }
    public void selectVersion(int index) { versions.select(index); }
    public void selectVersion(Version version) { versions.select(version); }
    public void deleteVersion(int index) { versions.delete(index); }
    public void deleteVersion(Version version) { versions.delete(version); }
    public void resolveLoaderOptions(Version version, Consumer<List<LoaderOption>> onComplete) { versions.resolveLoaderOptions(version, onComplete); }
    public void chooseLoaderVersion(LoaderVersion loader, Version baseVersion) { versions.chooseLoaderVersion(loader, baseVersion); }

    public boolean launchBusy() { return launch.busy(); }
    public double launchProgress() { return launch.progress(); }
    public boolean gameRunning() { return launch.gameRunning(); }
    public String launchActionText() { return launch.actionText(); }
    public String launchDetailsText() { return launch.detailsText(); }
    public void launchSelectedGame(Consumer<String> onError) { launch.launch(onError); }
    public void cancelLaunch() { launch.cancel(); }
    public boolean launchControlLocked() { return launch.controlLocked(); }
    public void refreshLauncherPresence() { launch.refreshPresence(); }
    public void shutdownBeforeExit() { launch.shutdown(); }

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

    public void notifyThemeColorsChanged() {
        if (runOnEdt(this::notifyThemeColorsChanged)) return;
        emit(Event.THEME_COLORS_CHANGED);
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
        int safeMemory = Math.clamp(memoryAmount, 512, OSUtils.maxMemoryMb());
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

    public boolean instancesStorageSystem() {
        return state.instancesStorageSystem;
    }

    public void setInstancesStorageSystem(boolean instancesStorageSystem) {
        if (runOnEdt(() -> setInstancesStorageSystem(instancesStorageSystem))) return;
        if (state.instancesStorageSystem == instancesStorageSystem) return;
        state.instancesStorageSystem = instancesStorageSystem;
        saveStateAndEmit(Event.SETTINGS_CHANGED);
    }

    public boolean customThemes() {
        return state.customThemes;
    }

    public void setCustomThemes(boolean customThemes) {
        if (runOnEdt(() -> setCustomThemes(customThemes))) return;
        if (state.customThemes == customThemes) return;
        state.customThemes = customThemes;
        Palette.setCustomThemesEnabled(customThemes);
        state.save();
        emit(Event.SETTINGS_CHANGED);
        emit(Event.THEME_COLORS_CHANGED);
    }

    public void deleteCustomThemeColors() {
        if (runOnEdt(this::deleteCustomThemeColors)) return;
        Palette.deleteCustomThemeColors();
        emit(Event.THEME_COLORS_CHANGED);
    }

    public boolean releaseFilter() { return versions.releaseFilter(); }
    public void setReleaseFilter(boolean value) { versions.setReleaseFilter(value); }
    public boolean snapshotFilter() { return versions.snapshotFilter(); }
    public void setSnapshotFilter(boolean value) { versions.setSnapshotFilter(value); }
    public boolean modpackFilter() { return versions.modpackFilter(); }
    public void setModpackFilter(boolean value) { versions.setModpackFilter(value); }
    public boolean legacyFilter() { return versions.legacyFilter(); }
    public void setLegacyFilter(boolean value) { versions.setLegacyFilter(value); }

    boolean runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) return false;
        SwingUtilities.invokeLater(action);
        return true;
    }

    void saveStateAndEmit(Event event) {
        state.normalize();
        state.save();
        emit(event);
    }

    void emit(Event event) {
        if (SwingUtilities.isEventDispatchThread()) emitNow(event);
        else SwingUtilities.invokeLater(() -> emitNow(event));
    }

    private void emitNow(Event event) {
        for (Listener listener : List.copyOf(listeners)) listener.onStoreEvent(event);
    }
}
