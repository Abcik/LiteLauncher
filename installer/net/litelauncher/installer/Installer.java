package net.litelauncher.installer;

/**
 * Public transparency entrypoint.
 *
 * The official installer UI and embedded Bootstrap.jar are redacted. The
 * install path and shortcut logic remain available in InstallerBackend and
 * InstallerShortcuts for auditing.
 */
public final class Installer {

    private Installer() {
    }

    public static void main(String[] args) {
        System.out.println("LiteLauncher installer public transparency source.");
        System.out.println("Official installer UI, icons and embedded Bootstrap.jar are redacted.");
        System.out.println("Review InstallerBackend and InstallerShortcuts for install behavior.");
    }
}
