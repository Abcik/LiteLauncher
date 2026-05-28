package net.litelauncher.installer;

import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.UtilityLog;

/**
 * Public-safe installer entry point.
 *
 * The official pixel-perfect installer window is redacted. InstallerBackend and
 * InstallerShortcuts are kept so install paths, shortcut creation and file operations
 * can be audited.
 */
public final class Installer {

    private Installer() {
    }

    public static void main(String[] args) {
        UtilityLog log = new UtilityLog(OSUtils.logsDirectory().resolve("litelauncher_installer.log"));
        log.start("Installer public shell started");
        try {
            InstallerBackend.install((progress, details) -> {
                int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * 100.0);
                System.out.println(percent + "% - " + details);
            }, log);
            System.out.println("LiteLauncher installation finished.");
        } catch (Exception exception) {
            log.error("Installation failed.", exception);
            System.err.println("Installation failed: " + exception.getMessage());
            System.err.println("Log: " + OSUtils.logsDirectory().resolve("litelauncher_installer.log"));
            System.exit(1);
        }
    }
}
