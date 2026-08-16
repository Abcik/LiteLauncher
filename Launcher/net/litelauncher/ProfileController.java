package net.litelauncher;

import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.auth.AuthException;
import net.litelauncher.backend.auth.ElyBySkin;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.frontend.modules.auth.SkinAvatar;
import net.litelauncher.frontend.modules.skin.DefaultPlayerSkin;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

final class ProfileController {

    private final LauncherStore store;
    private final LauncherState state;
    private final LauncherServices services;
    private final Set<String> refreshingProfiles = new HashSet<>();
    private final Set<String> refreshingOfflineSkins = new HashSet<>();

    private List<Profile> profiles;
    private BufferedImage selectedAvatar;
    private String selectedAvatarId;
    private String selectedAvatarSkin;
    private BufferedImage selectedAvatarSource;
    private boolean selectedAvatarSlim;

    ProfileController(LauncherStore store, LauncherState state, LauncherServices services) {
        this.store = store;
        this.state = state;
        this.services = services;
        profiles = services.authService().loadProfiles();
        normalizeSelection();
    }

    void start() {
        refreshProfile(selected());
        services.elyBySkinService().prune(profiles);
        Thread.ofVirtual().name("ely-authlib-refresh").start(services.elyByAuthlibCatalog()::refresh);
    }

    List<Profile> all() {
        return profiles;
    }

    Profile selected() {
        return byId(state.selectedProfileId);
    }

    BufferedImage selectedAvatar() {
        Profile profile = selected();
        if (profile == null) {
            clearAvatar();
            return null;
        }

        String skinPng = profile.microsoft() ? profile.skinPng() : null;
        BufferedImage source = null;
        boolean slim = profile.slim();
        if (!profile.microsoft()) {
            DefaultPlayerSkin.Skin skin = offlineSkin(profile);
            source = skin.image();
            slim = skin.slim();
        }

        if (!Objects.equals(selectedAvatarId, profile.id())
                || !Objects.equals(selectedAvatarSkin, skinPng)
                || selectedAvatarSource != source
                || selectedAvatarSlim != slim) {
            selectedAvatar = profile.microsoft()
                    ? SkinAvatar.create(skinPng, slim)
                    : SkinAvatar.create(source, slim);
            selectedAvatarId = profile.id();
            selectedAvatarSkin = skinPng;
            selectedAvatarSource = source;
            selectedAvatarSlim = slim;
        }
        return selectedAvatar;
    }

    DefaultPlayerSkin.Skin offlineSkin(Profile profile) {
        DefaultPlayerSkin.Skin fallback = DefaultPlayerSkin.forProfile(profile);
        ElyBySkin skin = services.elyBySkinService().cached(profile);
        return skin == null ? fallback : new DefaultPlayerSkin.Skin(skin.image(), skin.slim());
    }

    void refreshOfflineSkin(Profile profile) {
        if (store.runOnEdt(() -> refreshOfflineSkin(profile))) return;
        Profile current = byId(profile == null ? null : profile.id());
        if (current == null || current.microsoft() || !current.elyBy() || !refreshingOfflineSkins.add(current.id())) return;

        Thread.ofVirtual().name("ely-skin-refresh").start(() -> {
            services.elyBySkinService().refresh(current);
            SwingUtilities.invokeLater(() -> {
                refreshingOfflineSkins.remove(current.id());
                invalidateAvatar(current.id());
                store.emit(LauncherStore.Event.PROFILE_APPEARANCE_CHANGED);
            });
        });
    }

    boolean isSelected(Profile profile) {
        return profile != null && Objects.equals(state.selectedProfileId, profile.id());
    }

    void add(Profile profile) {
        if (store.runOnEdt(() -> add(profile))) return;
        if (profile == null) return;

        List<Profile> updated = new ArrayList<>(profiles);
        int existing = indexOf(profile.id());
        if (existing >= 0) updated.set(existing, profile);
        else updated.add(profile);
        profiles = List.copyOf(updated);

        String oldSelected = state.selectedProfileId;
        state.selectedProfileId = profile.id();
        persistProfiles();

        store.saveStateAndEmit(LauncherStore.Event.PROFILES_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedProfileId)) store.emit(LauncherStore.Event.SELECTED_PROFILE_CHANGED);
        refreshProfile(profile);
    }

    void setOfflineElyBy(Profile profile, boolean enabled) {
        if (store.runOnEdt(() -> setOfflineElyBy(profile, enabled))) return;
        Profile current = byId(profile == null ? null : profile.id());
        if (current == null || current.microsoft() || current.elyBy() == enabled) return;

        Profile updated = Profile.offline(current.username(), enabled);
        applyUpdate(updated);
        invalidateAvatar(updated.id());
        store.emit(LauncherStore.Event.PROFILE_APPEARANCE_CHANGED);
        if (enabled) refreshOfflineSkin(updated);
    }

    void openMicrosoftCallbackServer() throws AuthException {
        services.authService().openMicrosoftCallbackServer();
    }

    void closeMicrosoftCallbackServer() {
        services.authService().closeMicrosoftCallbackServer();
    }

    void signInWithMicrosoft(Consumer<String> onError) {
        Thread.ofVirtual().name("microsoft-sign-in").start(() -> {
            try {
                Profile profile = services.authService().signInWithMicrosoft(state.theme, state.language);
                SwingUtilities.invokeLater(() -> add(profile));
            } catch (Exception exception) {
                LauncherLog.error("Microsoft sign-in failed.", exception);
                SwingUtilities.invokeLater(() -> {
                    if (onError != null) onError.accept(InformationMessages.text(InformationMessages.SIGN_IN_ERROR));
                });
            }
        });
    }

    void uploadMicrosoftSkin(Profile profile, byte[] skinPng, boolean slim, Consumer<String> onError, Runnable onComplete) {
        updateMicrosoftProfile(profile, InformationMessages.SKIN_ERROR, "Unable to upload Minecraft skin.", onError, onComplete,
                () -> services.authService().uploadMicrosoftSkin(profile, skinPng, slim));
    }

    void setMicrosoftCape(Profile profile, String capeId, Consumer<String> onError, Runnable onComplete) {
        updateMicrosoftProfile(profile, InformationMessages.CAPE_ERROR, "Unable to update Minecraft cape.", onError, onComplete,
                () -> services.authService().setMicrosoftCape(profile, capeId));
    }

    void select(int index) {
        if (store.runOnEdt(() -> select(index))) return;
        if (index >= 0 && index < profiles.size()) select(profiles.get(index));
    }

    void select(Profile profile) {
        if (store.runOnEdt(() -> select(profile))) return;
        if (profile == null || Objects.equals(state.selectedProfileId, profile.id())) return;
        state.selectedProfileId = profile.id();
        store.saveStateAndEmit(LauncherStore.Event.SELECTED_PROFILE_CHANGED);
        refreshProfile(profile);
    }

    void delete(int index) {
        if (store.runOnEdt(() -> delete(index))) return;
        if (index < 0 || index >= profiles.size()) return;

        String oldSelected = state.selectedProfileId;
        Profile removed = profiles.get(index);
        List<Profile> updated = new ArrayList<>(profiles);
        updated.remove(index);
        profiles = List.copyOf(updated);
        refreshingProfiles.remove(removed.id());
        refreshingOfflineSkins.remove(removed.id());

        if (Objects.equals(oldSelected, removed.id())) {
            state.selectedProfileId = profiles.isEmpty() ? null : profiles.get(Math.min(index, profiles.size() - 1)).id();
        }

        persistProfiles();
        store.saveStateAndEmit(LauncherStore.Event.PROFILES_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedProfileId)) {
            store.emit(LauncherStore.Event.SELECTED_PROFILE_CHANGED);
            refreshProfile(selected());
        }
    }

    void refreshOfflineSkinBeforeLaunch(Profile profile) {
        if (profile == null || profile.microsoft() || !profile.elyBy()) return;
        services.elyBySkinService().refresh(profile);
        SwingUtilities.invokeLater(() -> {
            invalidateAvatar(profile.id());
            store.emit(LauncherStore.Event.PROFILE_APPEARANCE_CHANGED);
        });
    }

    void applyUpdate(Profile profile) {
        if (profile == null) return;
        refreshingProfiles.remove(profile.id());
        int index = indexOf(profile.id());
        if (index < 0 || profiles.get(index).equals(profile)) return;

        List<Profile> updated = new ArrayList<>(profiles);
        updated.set(index, profile);
        profiles = List.copyOf(updated);
        services.authService().saveProfiles(profiles);
        invalidateAvatar(profile.id());

        store.emit(LauncherStore.Event.PROFILES_CHANGED);
        if (Objects.equals(state.selectedProfileId, profile.id())) store.emit(LauncherStore.Event.SELECTED_PROFILE_CHANGED);
    }

    void removeExpiredMicrosoftProfile(Profile profile) {
        int index = indexOf(profile == null ? null : profile.id());
        if (index < 0) return;

        String oldSelected = state.selectedProfileId;
        List<Profile> updated = new ArrayList<>(profiles);
        updated.remove(index);
        profiles = List.copyOf(updated);
        normalizeSelection();
        persistProfiles();

        store.saveStateAndEmit(LauncherStore.Event.PROFILES_CHANGED);
        if (!Objects.equals(oldSelected, state.selectedProfileId)) {
            store.emit(LauncherStore.Event.SELECTED_PROFILE_CHANGED);
            refreshProfile(selected());
        }
    }

    private void updateMicrosoftProfile(Profile profile, String errorKey, String logMessage, Consumer<String> onError,
                                        Runnable onComplete, MicrosoftProfileUpdate operation) {
        if (profile == null || !profile.microsoft()) {
            if (onComplete != null) SwingUtilities.invokeLater(onComplete);
            return;
        }
        Thread.ofVirtual().name("microsoft-profile-update").start(() -> {
            try {
                Profile updated = operation.run();
                SwingUtilities.invokeLater(() -> {
                    applyUpdate(updated);
                    if (onComplete != null) onComplete.run();
                });
            } catch (AuthException exception) {
                LauncherLog.error(logMessage, exception);
                SwingUtilities.invokeLater(() -> {
                    if (exception.expiredSession()) removeExpiredMicrosoftProfile(profile);
                    if (onError != null) onError.accept(InformationMessages.text(exception.expiredSession()
                            ? InformationMessages.SIGN_IN_ERROR : errorKey));
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception exception) {
                LauncherLog.error(logMessage, exception);
                SwingUtilities.invokeLater(() -> {
                    if (onError != null) onError.accept(InformationMessages.text(errorKey));
                    if (onComplete != null) onComplete.run();
                });
            }
        });
    }

    private void refreshProfile(Profile profile) {
        refreshMicrosoftProfile(profile);
        refreshOfflineSkin(profile);
    }

    private void refreshMicrosoftProfile(Profile profile) {
        if (profile == null || !profile.microsoft() || !refreshingProfiles.add(profile.id())) return;
        Thread.ofVirtual().name("microsoft-profile-refresh").start(() -> {
            try {
                Profile updated = services.authService().refreshMicrosoftProfile(profile);
                SwingUtilities.invokeLater(() -> applyUpdate(updated));
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

    private void persistProfiles() {
        services.authService().saveProfiles(profiles);
        services.elyBySkinService().prune(profiles);
    }

    private void normalizeSelection() {
        if (byId(state.selectedProfileId) == null) state.selectedProfileId = profiles.isEmpty() ? null : profiles.getFirst().id();
    }


    private int indexOf(String id) {
        if (id == null) return -1;
        for (int index = 0; index < profiles.size(); index++) if (id.equals(profiles.get(index).id())) return index;
        return -1;
    }

    private Profile byId(String id) {
        int index = indexOf(id);
        return index < 0 ? null : profiles.get(index);
    }

    private void invalidateAvatar(String profileId) {
        if (Objects.equals(selectedAvatarId, profileId)) clearAvatar();
    }

    private void clearAvatar() {
        selectedAvatar = null;
        selectedAvatarId = null;
        selectedAvatarSkin = null;
        selectedAvatarSource = null;
        selectedAvatarSlim = false;
    }

    @FunctionalInterface
    private interface MicrosoftProfileUpdate {
        Profile run() throws AuthException;
    }
}
