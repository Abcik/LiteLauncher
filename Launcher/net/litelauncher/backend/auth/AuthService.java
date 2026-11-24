package net.litelauncher.backend.auth;

import net.litelauncher.backend.platform.LauncherPaths;
import net.litelauncher.Language;
import net.litelauncher.Theme;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;

import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AuthService {

    private static final Duration LAUNCH_TOKEN_REFRESH_MARGIN = Duration.ofMinutes(5);
    /*
     * Minecraft tokens live ~24h and the game cannot renew its own token, so a new game gets one
     * with at least this much life left. Renewing only below this keeps login_with_xbox calls to
     * about one per day: that endpoint answers 429 when it is called on every launch.
     */
    private static final Duration GAME_TOKEN_MIN_LIFETIME = Duration.ofHours(6);
    /* Launch normally finds a fresh token; this wait only matters when renewal is still running. */
    private static final Duration LAUNCH_SESSION_WAIT = Duration.ofSeconds(15);

    private final OfflineProfileStore offlineProfiles = new OfflineProfileStore();
    private final MicrosoftSessionStore microsoftSessions = new MicrosoftSessionStore();
    private final MicrosoftProfileCacheStore microsoftProfileCache = new MicrosoftProfileCacheStore();
    private final MicrosoftAuthClient microsoftAuth;
    private final MicrosoftCallbackPageRenderer callbackPages;
    private final MicrosoftCallbackServer callbackServer;

    public AuthService(MicrosoftCallbackPageRenderer callbackPages) {
        this(callbackPages, new MicrosoftAuthClient());
    }

    AuthService(MicrosoftCallbackPageRenderer callbackPages, MicrosoftAuthClient microsoftAuth) {
        this.microsoftAuth = microsoftAuth;
        this.callbackPages = callbackPages == null ? MicrosoftCallbackPageRenderer.fallback() : callbackPages;
        this.callbackServer = new MicrosoftCallbackServer(this.callbackPages);
    }

    public List<Profile> loadProfiles() {
        ensureFiles();

        List<Profile> profiles = new ArrayList<>(offlineProfiles.read());
        for (MicrosoftSession session : microsoftSessions.read().values()) {
            MicrosoftProfileCache cache = microsoftProfileCache.read(session.profileId());
            profiles.add(new MicrosoftAuthResult(session, cache).profile());
        }
        return List.copyOf(profiles);
    }

    public void saveProfiles(List<Profile> profiles) {
        offlineProfiles.write(profiles);
        // An older launcher window's list must not delete accounts added by another window.
    }

    public boolean deleteMicrosoftProfile(Profile profile) {
        if (profile == null || !profile.microsoft()) return true;
        try (var account = MicrosoftAccountLock.tryAccount(profile.id())) {
            if (account == null) return false;
            microsoftSessions.remove(profile.id());
            microsoftProfileCache.delete(profile.id());
            return true;
        } catch (AuthException exception) {
            LauncherLog.error("Unable to delete Microsoft profile.", exception);
            return false;
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
            try (var ignored = MicrosoftAccountLock.account(result.session().profileId())) {
                microsoftSessions.save(result.session());
                microsoftProfileCache.save(result.session().profileId(), result.cache());
            }
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
        return updateMicrosoftProfile(profile, microsoftAuth::refresh);
    }

    public Profile uploadMicrosoftSkin(Profile profile, byte[] skinPng, boolean slim) throws AuthException {
        if (profile == null || !profile.microsoft()) return profile;
        return updateMicrosoftProfile(profile, (session, cache) -> microsoftAuth.uploadSkin(session, cache, skinPng, slim));
    }

    public Profile setMicrosoftCape(Profile profile, String capeId) throws AuthException {
        if (profile == null || !profile.microsoft()) return profile;
        return updateMicrosoftProfile(profile, (session, cache) -> microsoftAuth.setCape(session, cache, capeId));
    }

    public LaunchPreparation prepareLaunchAccount(Profile selectedProfile) throws AuthException {
        if (selectedProfile == null) throw new AuthException("Create or select a profile before launch.");
        return new LaunchPreparation(selectedProfile);
    }

    /**
     * Background renewal: called on launcher start and periodically, so a launch usually finds
     * a fresh token and does not touch the network at all. Skips quietly when the account is busy.
     */
    public void keepMicrosoftSessionFresh(Profile profile) {
        if (profile == null || !profile.microsoft()) return;
        try (var account = MicrosoftAccountLock.tryAccount(profile.id())) {
            if (account == null) return;
            MicrosoftSession session = microsoftSessions.read(profile.id());
            if (session != null) renewIfNeeded(profile, session, account);
        } catch (AuthException exception) {
            LauncherLog.error("Unable to renew Microsoft session in background.", exception);
        }
    }

    private boolean validForLaunch(MicrosoftSession session) {
        return validFor(session, LAUNCH_TOKEN_REFRESH_MARGIN);
    }

    private boolean freshForGame(MicrosoftSession session) {
        return validFor(session, GAME_TOKEN_MIN_LIFETIME);
    }

    private static boolean validFor(MicrosoftSession session, Duration lifetime) {
        return session != null
                && AuthUtils.hasText(session.minecraftAccessToken())
                && session.minecraftAccessTokenExpiresAt() != null
                && session.minecraftAccessTokenExpiresAt().isAfter(Instant.now().plus(lifetime));
    }

    /** Must be called under the account lock. Keeps a still usable token when renewal fails (429, network). */
    private MicrosoftSession renewIfNeeded(Profile profile, MicrosoftSession session, MicrosoftAccountLock account) throws AuthException {
        if (freshForGame(session) || account.inUse()) return session;
        try {
            return microsoftAuth.refreshMinecraftToken(session, microsoftSessions::save);
        } catch (AuthException exception) {
            if (exception.expiredSession() || !validForLaunch(session)) throw exception;
            LauncherLog.error("Unable to renew Microsoft session; the current token is still valid.", exception);
            return requireMicrosoftSession(profile);
        }
    }

    private Profile updateMicrosoftProfile(Profile profile, MicrosoftProfileOperation operation) throws AuthException {
        MicrosoftSession session;
        try (var account = MicrosoftAccountLock.account(profile.id())) {
            session = renewIfNeeded(profile, requireMicrosoftSession(profile), account);
        }
        // Appearance downloads must neither delay launch authentication nor write an older token back.
        MicrosoftProfileCache cache = microsoftProfileCache.read(session.profileId());
        for (int attempt = 0; ; attempt++) {
            try {
                MicrosoftProfileCache updated = operation.run(session, cache);
                try (var ignored = MicrosoftAccountLock.account(profile.id())) {
                    MicrosoftSession latest = requireMicrosoftSession(profile);
                    microsoftProfileCache.save(latest.profileId(), updated);
                    return updated.profile(latest.profileId());
                }
            } catch (AuthException exception) {
                if (!exception.accessTokenRejected()) throw exception;
                try (var account = MicrosoftAccountLock.account(profile.id())) {
                    MicrosoftSession latest = requireMicrosoftSession(profile);
                    boolean sameToken = Objects.equals(latest.minecraftAccessToken(), session.minecraftAccessToken());
                    if (sameToken) latest = rejectAccessToken(latest);
                    if (attempt > 0) throw exception;
                    if (!sameToken && validForLaunch(latest)) session = latest;
                    else {
                        if (account.inUse()) throw exception;
                        session = microsoftAuth.refreshMinecraftToken(latest, microsoftSessions::save);
                    }
                }
            }
        }
    }

    private MicrosoftSession rejectAccessToken(MicrosoftSession session) throws AuthException {
        MicrosoftSession rejected = new MicrosoftSession(session.clientId(), session.redirectUri(), session.profileId(),
                null, null, session.microsoftRefreshToken(), session.xuid(), Instant.now());
        microsoftSessions.save(rejected);
        return rejected;
    }

    private MicrosoftSession requireMicrosoftSession(Profile profile) throws AuthException {
        MicrosoftSession session = microsoftSessions.read(profile.id());
        if (session == null) throw AuthException.expiredSession("Microsoft session was not found.");
        return session;
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
            Files.createDirectories(LauncherPaths.authDirectory());
            microsoftProfileCache.ensureDirectory();
            if (!Files.exists(LauncherPaths.offlineSessionsFile())) offlineProfiles.write(List.of());
            microsoftSessions.initialize();
        } catch (Exception exception) {
            LauncherLog.error("Unable to prepare auth files.", exception);
        }
    }

    /** Refreshes while files are prepared; keeps the account locked until the game is registered. */
    public final class LaunchPreparation implements AutoCloseable {
        private final Profile profile;
        private final CompletableFuture<MicrosoftSession> ready = new CompletableFuture<>();
        private MicrosoftAccountLock accountLock;
        private boolean closed;

        private LaunchPreparation(Profile profile) {
            this.profile = profile;
            if (!profile.microsoft()) ready.complete(null);
            else Thread.ofVirtual().name("microsoft-launch-session").start(this::refresh);
        }

        private void refresh() {
            MicrosoftAccountLock acquired = null;
            try {
                acquired = MicrosoftAccountLock.account(profile.id());
                synchronized (this) {
                    if (closed) return;
                }
                MicrosoftSession current = requireMicrosoftSession(profile);
                // A token with enough life left is reused: no network, no delay, no 429.
                if (!freshForGame(current) && !acquired.inUse()) {
                    try {
                        current = microsoftAuth.refreshMinecraftToken(current, microsoftSessions::save);
                    } catch (AuthException exception) {
                        LauncherLog.error("Unable to refresh launch session; trying cached session or offline.", exception);
                        if (exception.expiredSession() || exception.accessTokenRejected()) {
                            rejectAccessToken(current);
                            current = null;
                        } else current = requireMicrosoftSession(profile);
                    }
                }
                synchronized (this) {
                    if (closed) return;
                    if (validForLaunch(current)) {
                        accountLock = acquired;
                        acquired = null;
                    } else current = null;
                    ready.complete(current);
                }
            } catch (Exception exception) {
                LauncherLog.error("Unable to prepare Microsoft launch session; starting offline.", exception);
                ready.complete(null);
            } finally {
                if (acquired != null) acquired.close();
            }
        }

        public LaunchAccount account() {
            try {
                MicrosoftSession current = ready.get(LAUNCH_SESSION_WAIT.toMillis(), TimeUnit.MILLISECONDS);
                synchronized (this) {
                    // Downloads may have taken longer than the token's remaining lifetime.
                    if (!closed && validForLaunch(current)) {
                        MicrosoftProfileCache cache = microsoftProfileCache.read(current.profileId());
                        LauncherLog.info("Microsoft launch session ready: profile=" + current.profileId()
                                + ", expiresAt=" + current.minecraftAccessTokenExpiresAt());
                        return LaunchAccount.online(cache == null ? profile : cache.profile(current.profileId()),
                                current.minecraftAccessToken(), current.xuid());
                    }
                }
            } catch (TimeoutException exception) {
                LauncherLog.info("Launch session not ready after file preparation and "
                        + LAUNCH_SESSION_WAIT.toSeconds() + " seconds; starting offline.");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Launch session preparation interrupted.");
            } catch (ExecutionException exception) {
                LauncherLog.error("Unable to prepare launch account; starting offline.", exception);
            }
            close();
            return LaunchAccount.offline(profile);
        }

        public synchronized void gameStarted(ProcessHandle process) {
            if (closed || accountLock == null) return;
            try {
                accountLock.gameStarted(process);
            } catch (AuthException exception) {
                LauncherLog.error("Unable to register running Minecraft account.", exception);
            }
        }

        @Override
        public synchronized void close() {
            closed = true;
            if (accountLock != null) {
                accountLock.close();
                accountLock = null;
            }
            // An in-flight refresh may still save rotated credentials, then releases its lock.
            // It must not hand its late result to a game already started offline.
        }
    }

    @FunctionalInterface
    private interface MicrosoftProfileOperation {
        MicrosoftProfileCache run(MicrosoftSession session, MicrosoftProfileCache cache) throws AuthException;
    }
}
