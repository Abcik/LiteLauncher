# LiteLauncher Installer Public Source

This module contains the public-safe installer logic for LiteLauncher.

Included:

- platform path resolution;
- bootstrap placement flow;
- OS script and shortcut creation logic;
- install progress contract and logging utility.

Redacted:

- official pixel-perfect Swing installer UI;
- official installer icons/assets;
- embedded production `Bootstrap.jar` payload.

The public entry point is a placeholder. The useful audit surface is in `InstallerBackend`, `InstallerFiles` and `InstallerShortcuts`.
