# Installer module

This module contains the public LiteLauncher installation logic.

Included:

- Minecraft/LiteLauncher path resolution;
- bootstrap directory creation;
- Java/runtime directory creation;
- OS shortcut and launch script generation for Windows, macOS and Linux;
- atomic resource copy helpers.

Redacted:

- official installer pixel UI;
- official shortcut icons;
- embedded official `Bootstrap.jar` binary payload.

The public `Installer` entrypoint is a placeholder. Review `InstallerBackend`
and `InstallerShortcuts` for the real install behavior.
