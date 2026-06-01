package net.litelauncher;

import net.litelauncher.backend.LauncherState;

/**
 * Public transparency entrypoint.
 *
 * The official desktop UI is intentionally not included in this repository.
 * Review the backend modules for auth, downloads, version resolving,
 * Java runtime handling and game launch logic.
 */
public final class LiteLauncher {

    /** Public placeholder for code paths that reference the official Swing window. */
    public static java.awt.Window window;

    private LiteLauncher() {
    }

    public static void main(String[] args) {
        System.out.println(LauncherState.TITLE + " public transparency source " + LauncherState.LAUNCHER_VERSION);
        System.out.println("Official pixel-perfect UI and product assets are redacted from this repository.");
        System.out.println("Start reviewing from net.litelauncher.backend.modules.* and LauncherStore.");
    }
}
