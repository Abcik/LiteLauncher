package net.litelauncher.bootstrap;

import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.UtilityLog;

/**
 * Public transparency entrypoint.
 *
 * The official pixel progress UI is redacted. This console entrypoint keeps
 * the update/verification flow runnable for private inspection if the rest
 * of the official distribution artifacts are provided.
 */
public final class Bootstrap {

    private Bootstrap() {
    }

    public static void main(String[] args) throws Exception {
        UtilityLog log = new UtilityLog(OSUtils.logsDirectory().resolve("litelauncher_bootstrap.log"));
        log.start("Bootstrap public transparency entrypoint started");
        BootstrapBackend.updateAndLaunch((value, details) -> {
            int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * 100.0);
            System.out.println(percent + "% " + details);
        }, log);
    }
}
