package net.litelauncher;

import net.litelauncher.backend.CancellationToken;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.auth.AuthException;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.launch.LaunchResult;
import net.litelauncher.backend.launch.LaunchSettings;
import net.litelauncher.backend.version.Version;
import net.litelauncher.i18n.I18n;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

final class LaunchController {

    private static final int CONTROL_COOLDOWN_MS = 250;

    private final LauncherStore store;
    private final LauncherState state;
    private final LauncherServices services;
    private final ProfileController profiles;
    private final VersionController versions;

    private boolean busy;
    private CancellationToken cancellation;
    private Thread launchThread;
    private Timer controlCooldownTimer;
    private long controlLockedUntil;
    private int runningGames;
    private double progress;
    private String actionText = "";
    private String detailsText = "";

    LaunchController(LauncherStore store, LauncherState state, LauncherServices services,
                     ProfileController profiles, VersionController versions) {
        this.store = store;
        this.state = state;
        this.services = services;
        this.profiles = profiles;
        this.versions = versions;
    }

    boolean busy() { return busy; }
    double progress() { return progress; }
    boolean gameRunning() { return runningGames > 0; }
    String actionText() { return actionText; }
    String detailsText() { return detailsText; }
    boolean controlLocked() { return System.currentTimeMillis() < controlLockedUntil; }

    void launch(Consumer<String> onError) {
        if (busy || controlLocked()) return;

        lockControl();
        busy = true;
        CancellationToken current = new CancellationToken();
        cancellation = current;
        I18n.setCurrentLanguage(state.language);
        setProgress(current, 0.0, I18n.text("progress.preparingLaunch"), "");

        Profile profile = profiles.selected();
        Version version = versions.selected();
        LaunchSettings settings = snapshot();
        launchThread = Thread.ofVirtual().name("game-launch").start(() -> {
            try {
                profiles.refreshOfflineSkinBeforeLaunch(profile);
                LaunchResult result = services.gameLaunchService().launch(profile, version, settings,
                        (value, action, details) -> setProgress(current, value, action, details),
                        process -> addRunningGame(process, version, current), current);
                SwingUtilities.invokeLater(() -> complete(result, settings.closeAfterLaunch(), current));
            } catch (CancellationException _) {
                LauncherLog.info("Launch cancelled.");
                SwingUtilities.invokeLater(() -> cancelCompleted(current));
            } catch (AuthException exception) {
                LauncherLog.error("Launch failed.", exception);
                SwingUtilities.invokeLater(() -> fail(profile, exception, onError, current));
            } catch (Exception exception) {
                LauncherLog.error("Launch failed.", exception);
                SwingUtilities.invokeLater(() -> fail(null, exception, onError, current));
            }
        });
    }

    void cancel() {
        if (store.runOnEdt(this::cancel)) return;
        if (!busy || controlLocked()) return;

        lockControl();
        CancellationToken current = cancellation;
        Thread thread = launchThread;
        if (current != null) current.cancel();
        if (thread != null) thread.interrupt();
        clear(current);
    }

    void refreshPresence() {
        services.discordRpcService().showLauncher();
    }

    void shutdown() {
        if (cancellation != null) cancellation.cancel();
        if (launchThread != null) launchThread.interrupt();
        services.discordRpcService().shutdown();
    }

    private void addRunningGame(Process process, Version version, CancellationToken current) {
        services.discordRpcService().showGame(process, version);
        SwingUtilities.invokeLater(() -> {
            clear(current);
            runningGames++;
            store.emit(LauncherStore.Event.GAME_STATUS_CHANGED);
        });
        process.onExit().thenRun(() -> SwingUtilities.invokeLater(() -> {
            services.discordRpcService().hideGame(process);
            if (runningGames > 0) runningGames--;
            store.emit(LauncherStore.Event.GAME_STATUS_CHANGED);
        }));
    }

    private LaunchSettings snapshot() {
        state.normalize();
        return new LaunchSettings(
                state.language,
                state.memoryAmount,
                state.screenWidth,
                state.screenHeight,
                state.fullscreen,
                state.jvmArguments,
                state.closeAfterLaunch,
                state.instancesStorageSystem
        );
    }

    private void complete(LaunchResult result, boolean closeAfterLaunch, CancellationToken current) {
        if (result != null && result.updatedProfile() != null) profiles.applyUpdate(result.updatedProfile());
        if (closeAfterLaunch) closeLauncherWindow();
        else clear(current);
    }

    private void fail(Profile profile, Exception exception, Consumer<String> onError, CancellationToken current) {
        if (current != null && current.cancelled()) {
            clear(current);
            return;
        }
        if (current != cancellation) return;

        clear(current);
        if (exception instanceof AuthException authException && authException.expiredSession()) {
            profiles.removeExpiredMicrosoftProfile(profile);
        }
        if (onError != null) onError.accept(InformationMessages.launch(exception));
    }

    private void cancelCompleted(CancellationToken current) {
        if (current == cancellation) clear(current);
    }

    private void clear(CancellationToken current) {
        if (current != null && current != cancellation) return;
        busy = false;
        cancellation = null;
        launchThread = null;
        actionText = "";
        detailsText = "";
        progress = 0.0;
        store.emit(LauncherStore.Event.LAUNCH_PROGRESS_CHANGED);
    }

    private void lockControl() {
        controlLockedUntil = System.currentTimeMillis() + CONTROL_COOLDOWN_MS;
        if (controlCooldownTimer != null) controlCooldownTimer.stop();
        controlCooldownTimer = new Timer(CONTROL_COOLDOWN_MS, _ -> store.emit(LauncherStore.Event.LAUNCH_PROGRESS_CHANGED));
        controlCooldownTimer.setRepeats(false);
        controlCooldownTimer.start();
        store.emit(LauncherStore.Event.LAUNCH_PROGRESS_CHANGED);
    }

    private void setProgress(CancellationToken current, double value, String action, String details) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setProgress(current, value, action, details));
            return;
        }
        if (current != cancellation) return;
        progress = Math.clamp(value, 0.0, 1.0);
        actionText = action == null ? "" : action;
        detailsText = details == null ? "" : details;
        store.emit(LauncherStore.Event.LAUNCH_PROGRESS_CHANGED);
    }

    private void closeLauncherWindow() {
        Window window = LiteLauncher.window;
        if (window == null) {
            System.exit(0);
            return;
        }
        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }
}
