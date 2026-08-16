package net.litelauncher;

import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.loader.LoaderOption;
import net.litelauncher.backend.loader.LoaderVersion;
import net.litelauncher.backend.version.Version;
import net.litelauncher.i18n.I18n;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

final class VersionController {

    private final LauncherStore store;
    private final LauncherState state;
    private final LauncherServices services;

    private List<Version> versions;
    private List<Version> visibleVersions = List.of();
    private boolean refreshing;
    private boolean importingModpack;

    VersionController(LauncherStore store, LauncherState state, LauncherServices services) {
        this.store = store;
        this.state = state;
        this.services = services;
        versions = services.versionService().loadCachedVersions(state.instancesStorageSystem);
        refreshVisible();
        normalizeSelection();
    }

    void start() {
        refreshRemote();
    }

    List<Version> visible() {
        return visibleVersions;
    }

    Version selected() {
        return byId(state.selectedVersionId);
    }

    boolean isSelected(Version version) {
        return version != null && Objects.equals(state.selectedVersionId, version.id());
    }

    void refreshRemote() {
        if (refreshing) return;
        refreshing = true;
        boolean instances = state.instancesStorageSystem;
        Thread.ofVirtual().name("versions-refresh").start(() -> {
            try {
                List<Version> loaded = services.versionService().loadVersions(instances);
                SwingUtilities.invokeLater(() -> {
                    refreshing = false;
                    if (state.instancesStorageSystem != instances) {
                        refreshLocal();
                        return;
                    }
                    apply(mergePendingLoaders(loaded, versions));
                });
            } catch (Exception exception) {
                LauncherLog.error("Unable to refresh versions.", exception);
                SwingUtilities.invokeLater(() -> refreshing = false);
            }
        });
    }

    void refreshLocal() {
        if (refreshing) return;
        refreshing = true;
        boolean instances = state.instancesStorageSystem;
        List<Version> current = List.copyOf(versions);
        Thread.ofVirtual().name("local-versions-refresh").start(() -> {
            try {
                List<Version> loaded = services.versionService().refreshLocalVersions(current, instances);
                SwingUtilities.invokeLater(() -> {
                    refreshing = false;
                    if (state.instancesStorageSystem != instances) {
                        refreshLocal();
                        return;
                    }
                    applyIfChanged(loaded);
                });
            } catch (Exception exception) {
                LauncherLog.error("Unable to refresh local versions.", exception);
                SwingUtilities.invokeLater(() -> refreshing = false);
            }
        });
    }

    void importModpack(Path file, Consumer<String> onError) {
        if (store.runOnEdt(() -> importModpack(file, onError))) return;
        if (file == null || importingModpack) return;

        importingModpack = true;
        boolean instances = state.instancesStorageSystem;
        List<Version> current = List.copyOf(versions);
        Thread.ofVirtual().name("modpack-import").start(() -> {
            try {
                boolean imported = services.modpackService().importPack(file);
                List<Version> loaded = imported
                        ? services.versionService().refreshLocalVersions(current, instances)
                        : current;
                SwingUtilities.invokeLater(() -> {
                    importingModpack = false;
                    if (imported) apply(loaded);
                });
            } catch (Exception exception) {
                LauncherLog.error("Unable to import modpack: " + file, exception);
                SwingUtilities.invokeLater(() -> {
                    importingModpack = false;
                    if (onError != null) onError.accept(I18n.text("modpacks.importError"));
                });
            }
        });
    }

    void select(int index) {
        if (store.runOnEdt(() -> select(index))) return;
        if (index >= 0 && index < visibleVersions.size()) select(visibleVersions.get(index));
    }

    void select(Version version) {
        if (store.runOnEdt(() -> select(version))) return;
        if (version == null || Objects.equals(state.selectedVersionId, version.id())) return;
        state.selectedVersionId = version.id();
        store.saveStateAndEmit(LauncherStore.Event.SELECTED_VERSION_CHANGED);
    }

    void delete(int index) {
        if (store.runOnEdt(() -> delete(index))) return;
        if (index >= 0 && index < visibleVersions.size()) delete(visibleVersions.get(index));
    }

    void delete(Version removed) {
        if (store.runOnEdt(() -> delete(removed))) return;
        if (removed == null || byId(removed.id()) == null) return;
        if ((!removed.loaded() && !removed.custom() && !removed.modpack()) || refreshing || importingModpack) return;

        boolean instances = state.instancesStorageSystem;
        refreshing = true;
        List<Version> current = List.copyOf(versions);
        Thread.ofVirtual().name("version-delete").start(() -> {
            try {
                services.versionService().deleteInstalledVersion(removed, instances);
                List<Version> loaded = services.versionService().refreshLocalVersions(current, instances);
                SwingUtilities.invokeLater(() -> {
                    refreshing = false;
                    apply(loaded);
                });
            } catch (Exception exception) {
                LauncherLog.error("Unable to delete version: " + removed.id(), exception);
                SwingUtilities.invokeLater(() -> {
                    refreshing = false;
                    refreshLocal();
                });
            }
        });
    }

    void resolveLoaderOptions(Version version, Consumer<List<LoaderOption>> onComplete) {
        if (version == null || !version.modificationInstallersAvailable() || onComplete == null) {
            if (onComplete != null) onComplete.accept(List.of());
            return;
        }
        Thread.ofVirtual().name("loader-options").start(() -> {
            List<LoaderOption> options = services.loaderCatalog().resolve(version.id());
            SwingUtilities.invokeLater(() -> onComplete.accept(options));
        });
    }

    void chooseLoaderVersion(LoaderVersion loader, Version baseVersion) {
        if (store.runOnEdt(() -> chooseLoaderVersion(loader, baseVersion))) return;
        if (loader == null || baseVersion == null) return;

        Version existing = byId(loader.id());
        if (existing != null) {
            select(existing);
            return;
        }

        Version pending = Version.modified(loader, false, baseVersion.releaseTime());
        List<Version> updated = new ArrayList<>(versions);
        int baseIndex = -1;
        for (int index = 0; index < updated.size(); index++) {
            if (baseVersion.id().equals(updated.get(index).id())) {
                baseIndex = index;
                break;
            }
        }
        int insertAt = baseIndex < 0 ? updated.size() : baseIndex + 1;
        while (insertAt < updated.size()) {
            Version next = updated.get(insertAt);
            if (next.loader() == null || !baseVersion.id().equals(next.loader().minecraftVersion())) break;
            insertAt++;
        }
        updated.add(insertAt, pending);
        apply(updated);
        select(pending);
    }

    boolean releaseFilter() { return state.releaseFilter; }
    boolean snapshotFilter() { return state.snapshotFilter; }
    boolean modpackFilter() { return state.modpackFilter; }
    boolean legacyFilter() { return state.legacyFilter; }

    void setReleaseFilter(boolean value) {
        if (store.runOnEdt(() -> setReleaseFilter(value)) || state.releaseFilter == value) return;
        state.releaseFilter = value;
        filterChanged();
    }

    void setSnapshotFilter(boolean value) {
        if (store.runOnEdt(() -> setSnapshotFilter(value)) || state.snapshotFilter == value) return;
        state.snapshotFilter = value;
        filterChanged();
    }

    void setModpackFilter(boolean value) {
        if (store.runOnEdt(() -> setModpackFilter(value)) || state.modpackFilter == value) return;
        state.modpackFilter = value;
        filterChanged();
    }

    void setLegacyFilter(boolean value) {
        if (store.runOnEdt(() -> setLegacyFilter(value)) || state.legacyFilter == value) return;
        state.legacyFilter = value;
        filterChanged();
    }

    private void filterChanged() {
        refreshVisible();
        store.saveStateAndEmit(LauncherStore.Event.FILTERS_CHANGED);
    }

    private List<Version> mergePendingLoaders(List<Version> loaded, List<Version> current) {
        List<Version> result = new ArrayList<>(loaded == null ? List.of() : loaded);
        Set<String> ids = new HashSet<>();
        for (Version version : result) ids.add(version.id());
        for (Version version : current == null ? List.<Version>of() : current) {
            if (!version.pendingLoader() || !ids.add(version.id())) continue;
            int baseIndex = -1;
            for (int index = 0; index < result.size(); index++) {
                if (version.loader().minecraftVersion().equals(result.get(index).id())) {
                    baseIndex = index;
                    break;
                }
            }
            result.add(baseIndex < 0 ? result.size() : baseIndex + 1, version);
        }
        return List.copyOf(result);
    }

    private void apply(List<Version> versions) {
        String oldSelected = state.selectedVersionId;
        this.versions = versions == null ? List.of() : List.copyOf(versions);
        refreshVisible();
        normalizeSelection();
        store.saveStateAndEmit(LauncherStore.Event.VERSIONS_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedVersionId)) store.emit(LauncherStore.Event.SELECTED_VERSION_CHANGED);
    }

    private void applyIfChanged(List<Version> next) {
        List<Version> normalized = next == null ? List.of() : List.copyOf(next);
        if (!versions.equals(normalized)) apply(normalized);
    }

    private void normalizeSelection() {
        if (byId(state.selectedVersionId) == null) state.selectedVersionId = defaultVersionId();
    }

    private String defaultVersionId() {
        for (Version version : versions) if (version.type() == Version.Type.RELEASE && !version.modded()) return version.id();
        for (Version version : versions) if (version.type() == Version.Type.RELEASE) return version.id();
        return versions.isEmpty() ? null : versions.getFirst().id();
    }

    private void refreshVisible() {
        List<Version> visible = new ArrayList<>();
        for (Version version : versions) if (isVisible(version)) visible.add(version);
        visibleVersions = List.copyOf(visible);
    }

    private Version byId(String id) {
        if (id == null) return null;
        for (Version version : versions) if (id.equals(version.id())) return version;
        return null;
    }

    private boolean isVisible(Version version) {
        if (version == null) return false;
        if (version.modpack()) return state.modpackFilter;
        if (version.legacy()) return state.legacyFilter;
        if (version.snapshot()) return state.snapshotFilter;
        if (version.releaseLike()) return state.releaseFilter;
        return true;
    }
}
