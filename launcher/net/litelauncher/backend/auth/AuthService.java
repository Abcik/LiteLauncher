package net.litelauncher.backend.auth;

import net.litelauncher.Language;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.Theme;
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
            profiles.add(new MicrosoftAuthResult(entry.getValue()).profile());
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
            microsoftSessions.save(result.session());
            auth.page().complete(callbackPages.successHtml(auth.theme(), auth.language(), result.session().playerName(), result.session().skinPng(), result.session().slim()));
            return result.profile();
        } catch (AuthException exception) {
            auth.page().complete(callbackPages.errorHtml(auth.theme(), auth.language(), InformationMessages.WEB_AUTH_ERROR));
            throw exception;
        } finally {
            callbackServer.clear(auth);
        }
    }

    public Profile refreshMicrosoftProfile(Profile profile) throws AuthException {
        if (profile == null || !profile.microsoft()) return null;

        MicrosoftSession session = microsoftSessions.read(profile.id());
        if (session == null) throw AuthException.expiredSession("Microsoft session was not found.");

        MicrosoftAuthResult updated = microsoftAuth.refresh(session);
        microsoftSessions.save(updated.session());
        return updated.profile();
    }

    public LaunchAccount prepareLaunchAccount(Profile selectedProfile) throws AuthException {
        if (selectedProfile == null) throw new AuthException("Create or select a profile before launch.");
        if (!selectedProfile.microsoft()) return LaunchAccount.offline(selectedProfile);

        MicrosoftSession session = microsoftSessions.read(selectedProfile.id());
        if (session == null) throw AuthException.expiredSession("Microsoft session was not found.");

        try {
            MicrosoftAuthResult updated = microsoftAuth.refresh(session);
            microsoftSessions.save(updated.session());
            return updated.launchAccount();
        } catch (AuthException exception) {
            if (exception.expiredSession()) throw exception;
            return LaunchAccount.offline(selectedProfile);
        }
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
            if (!Files.exists(OSUtils.offlineSessionsFile())) offlineProfiles.write(List.of());
            if (!Files.exists(OSUtils.microsoftSessionsFile())) microsoftSessions.write(Map.of());
        } catch (Exception exception) {
            LauncherLog.error("Unable to prepare auth files.", exception);
        }
    }
}
