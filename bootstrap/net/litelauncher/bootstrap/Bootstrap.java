package net.litelauncher.bootstrap;

import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.UtilityLog;

/**
 * Public transparency entry point.
 *
 * The official lazy pixel progress window is intentionally redacted. The update
 * backend is preserved and can still be audited through BootstrapBackend and the
 * related manifest/download/runtime services.
 */
public final class Bootstrap {

    private Bootstrap() {
    }

    public static void main(String[] args) {
        UtilityLog log = new UtilityLog(OSUtils.logsDirectory().resolve("litelauncher_bootstrap.log"));
        log.start("Bootstrap started from public source placeholder");
        System.out.println("LiteLauncher bootstrap UI is redacted in the public source release.");
        System.out.println("Audit BootstrapBackend and related services for update/verification logic.");
    }
}
