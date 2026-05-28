package net.litelauncher;

import net.litelauncher.backend.LauncherLog;

import java.awt.Window;

/**
 * Public-safe launcher entry point.
 *
 * The official LiteLauncher window, pixel-perfect scenes, glyph renderer, animations and
 * exact layout are intentionally not included in this public transparency release.
 * Backend/auth/download/version/launch code remains available for audit.
 */
public final class LiteLauncher {

    public static Window window;

    private LiteLauncher() {
    }

    public static void main(String[] args) {
        LauncherLog.start("Launcher public shell started");
        System.out.println("LiteLauncher public source shell");
        System.out.println("Official pixel UI is redacted. Inspect backend modules for auth, downloads, Java runtime and Minecraft launch logic.");
    }
}
