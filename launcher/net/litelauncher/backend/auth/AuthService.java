package net.litelauncher.backend.auth;

import net.litelauncher.Language;
import net.litelauncher.Theme;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;

import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public final class AuthService {

    private final OfflineProfileStore offlineProfiles = new OfflineProfileStore();
    private final MicrosoftSessionStore microsoftSessions = new MicrosoftSessionStore();
    private final MicrosoftProfileCacheStore microsoftProfileCache = new MicrosoftProfileCacheStore();
    private final MicrosoftAuthClient microsoftAuth = new MicrosoftAuthClient();
    private final MicrosoftCallbackPageRenderer callbackPages;
    private final MicrosoftCallbackServer callbackServer;

    public AuthService(MicrosoftCallbackPageRenderer callbackPages) {
        this.callbackPages = callbackPages == null ? MicrosoftCallbackPageRenderer.fallback() : callbackPages;
        this.callbackServer = new MicrosoftCallbackServer(this.callbackPages);
    }

    public List<Profile> loadProfiles() {
        ensureFiles();

        List<Profile> profiles = new ArrayList<>(offlineProfiles.read());
        for (Map.Entry<String, MicrosoftSession> entry : microsoftSessions.read().entrySet()) {
            MicrosoftSession session = entry.getValue();
            MicrosoftProfileCache cache = microsoftProfileCache.read(session.profileId());
            profiles.add(new MicrosoftAuthResult(session, cache).profile());
        }
        return List.copyOf(profiles);
    }

    public void saveProfiles(List<Profile> profiles) {
        offlineProfiles.write(profiles);
        try {
            Set<String> keep = profiles == null ? Set.of() : profiles.stream()
                    .filter(profile -> profile != null && profile.microsoft())
                    .map(Profile::id)
                    .collect(Collectors.toSet());
            microsoftSessions.prune(keep);
            microsoftProfileCache.prune(keep);
        } catch (Exception exception) {
            LauncherLog.error("Unable to prune Microsoft sessions.", exception);
        }
    }

    public void openMicrosoftCallbackServer() throws AuthException {
        callbackServer.open();
    }

    public void closeMicrosoftCallbackServer() {
        callbackServer.close();
    }

    public Profile signInWithMicrosoft(Theme theme, Language language) throws AuthException {
        PendingMicrosoftAuth auth = callbackServer.start(theme, language);
        try {
            openBrowser(auth.authorizationUrl());
            String code = waitForCallback(auth);
            MicrosoftAuthResult result = microsoftAuth.authenticate(code, auth.codeVerifier(), auth.redirectUri());
            saveMicrosoftResult(result);
            Profile profile = result.profile();
            auth.page().complete(callbackPages.successHtml(auth.theme(), auth.language(), profile.username(), profile.skinPng(), profile.slim()));
            return profile;
        } catch (AuthException exception) {
            auth.page().complete(callbackPages.errorHtml(auth.theme(), auth.language(), InformationMessages.WEB_AUTH_ERROR));
            throw exception;
        } finally {
            callbackServer.clear(auth);
        }
    }

    public Profile refreshMicrosoftProfile(Profile profile) throws AuthException {
        if (profile == null || !profile.microsoft()) return null;
        return updateMicrosoftProfile(profile, microsoftAuth::refresh).profile();
    }

    public Profile uploadMicrosoftSkin(Profile profile, byte[] skinPng, boolean slim) throws AuthException {
        if (profile == null || !profile.microsoft()) return profile;
        return updateMicrosoftProfile(profile, (session, cache) -> microsoftAuth.uploadSkin(session, cache, skinPng, slim)).profile();
    }

    public Profile setMicrosoftCape(Profile profile, String capeId) throws AuthException {
        if (profile == null || !profile.microsoft()) return profile;
        return updateMicrosoftProfile(profile, (session, cache) -> microsoftAuth.setCape(session, cache, capeId)).profile();
    }

    public LaunchAccount prepareLaunchAccount(Profile selectedProfile) throws AuthException {
        if (selectedProfile == null) throw new AuthException("Create or select a profile before launch.");
        if (!selectedProfile.microsoft()) return LaunchAccount.offline(selectedProfile);

        try {
            MicrosoftAuthResult updated = updateMicrosoftProfile(selectedProfile, microsoftAuth::refresh);
            return updated.launchAccount();
        } catch (AuthException exception) {
            if (exception.expiredSession()) throw exception;
            return LaunchAccount.offline(selectedProfile);
        }
    }

    private MicrosoftAuthResult updateMicrosoftProfile(Profile profile, MicrosoftProfileOperation operation) throws AuthException {
        MicrosoftSession session = requireMicrosoftSession(profile);
        MicrosoftProfileCache cache = microsoftProfileCache.read(session.profileId());
        MicrosoftAuthResult updated = operation.run(session, cache);
        saveMicrosoftResult(updated);
        return updated;
    }

    private MicrosoftSession requireMicrosoftSession(Profile profile) throws AuthException {
        if (profile == null || !profile.microsoft()) throw AuthException.expiredSession("Microsoft session was not found.");
        MicrosoftSession session = microsoftSessions.read(profile.id());
        if (session == null) {
            microsoftProfileCache.delete(profile.id());
            throw AuthException.expiredSession("Microsoft session was not found.");
        }
        return session;
    }

    private void saveMicrosoftResult(MicrosoftAuthResult result) throws AuthException {
        microsoftSessions.save(result.session());
        microsoftProfileCache.save(result.session().profileId(), result.cache());
    }

    private String waitForCallback(PendingMicrosoftAuth auth) throws AuthException {
        try {
            return auth.code().get(5, TimeUnit.MINUTES);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Waiting for Microsoft sign-in was interrupted.", exception);
        } catch (TimeoutException exception) {
            throw new AuthException("Microsoft sign-in timed out.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof AuthException authException) throw authException;
            throw new AuthException("Unable to complete Microsoft sign-in.", cause);
        }
    }

    private void openBrowser(URI uri) throws AuthException {
        try {
            OSUtils.openUri(uri);
        } catch (Exception exception) {
            throw new AuthException("Unable to open Microsoft sign-in page.", exception);
        }
    }

    private void ensureFiles() {
        try {
            Files.createDirectories(OSUtils.authDirectory());
            microsoftProfileCache.ensureDirectory();
            if (!Files.exists(OSUtils.offlineSessionsFile())) offlineProfiles.write(List.of());
            if (!Files.exists(OSUtils.microsoftSessionsFile())) microsoftSessions.write(Map.of());
        } catch (Exception exception) {
            LauncherLog.error("Unable to prepare auth files.", exception);
        }
    }

    @FunctionalInterface
    private interface MicrosoftProfileOperation {
        MicrosoftAuthResult run(MicrosoftSession session, MicrosoftProfileCache cache) throws AuthException;
    }
}
