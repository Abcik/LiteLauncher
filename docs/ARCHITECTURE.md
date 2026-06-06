# Public Architecture Notes

LiteLauncher is split into three public modules in this repository.

## Installer

The installer prepares the LiteLauncher directory structure, writes the bootstrap payload in official builds and creates OS-specific launch scripts/shortcuts.

In this public source release, the official installer UI and embedded production `Bootstrap.jar` are removed. The path and shortcut logic remains available for review.

## Bootstrap

The bootstrap is the update layer between the OS shortcut and the main launcher. It loads a signed manifest, verifies launcher downloads, keeps a cached manifest fallback, ensures a Java runtime and starts the main launcher jar.

The official progress window is removed, but the update/verification logic remains available for review.

## Launcher

The launcher backend handles profile/account storage, Microsoft authentication, encrypted session persistence, Minecraft version resolving, downloads, Java runtime selection and game launch argument generation.

The official Swing pixel-perfect UI is removed. Placeholder entry points keep the public tree understandable without disclosing the product shell.
