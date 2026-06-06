package net.litelauncher;

import net.litelauncher.backend.LauncherState;

import java.awt.Window;

/**
 * Public transparency entry point.
 *
 * The official LiteLauncher Swing pixel-perfect application shell is intentionally
 * redacted. Backend services, account/session storage, version resolution,
 * downloads, Java runtime handling and game launch preparation remain available
 * for review in this repository.
 */
public final class LiteLauncher {

    /** Public placeholder for backend code that references the official window. */
    public static Window window;

    private LiteLauncher() {
    }

    public static void main(String[] args) {
        System.out.println(LauncherState.TITLE + " " + LauncherState.LAUNCHER_VERSION);
        System.out.println("Official pixel-perfect UI is redacted in this public source release.");
        System.out.println("Audit LauncherStore and net.litelauncher.backend.* for launcher logic.");
    }
}
