package net.litelauncher.bootstrap;

import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.UtilityLog;

/**
 * Public-safe bootstrap entry point.
 *
 * The official lazy pixel progress window is redacted. BootstrapBackend, manifest
 * validation, download verification and Java runtime extraction remain available for audit.
 */
public final class Bootstrap {

    private Bootstrap() {
    }

    public static void main(String[] args) {
        UtilityLog log = new UtilityLog(OSUtils.logsDirectory().resolve("litelauncher_bootstrap.log"));
        log.start("Bootstrap public shell started");
        try {
            BootstrapBackend.updateAndLaunch((progress, details) -> {
                int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * 100.0);
                System.out.println(percent + "% - " + details);
            }, log);
        } catch (Exception exception) {
            log.error("Bootstrap failed.", exception);
            String message = exception instanceof BootstrapException && exception.getMessage() != null && !exception.getMessage().isBlank()
                    ? exception.getMessage()
                    : "Unable to start LiteLauncher.";
            System.err.println(message);
            System.err.println("Log: " + OSUtils.logsDirectory().resolve("litelauncher_bootstrap.log"));
            System.exit(1);
        }
    }
}
