# LiteLauncher Bootstrap Public Source

This module contains the public-safe bootstrap/update layer.

Included:

- signed launcher manifest parsing and verification;
- manifest download/cache fallback flow;
- launcher jar download, checksum/size verification and atomic replacement;
- Java runtime detection/download/extraction helpers;
- old launcher cleanup and process startup logic.

Redacted:

- official lazy pixel progress window;
- official bitmap font/glyph assets;
- official visual assets.

The public entry point is a placeholder. Review `BootstrapBackend`, `LauncherManifest` and the platform helpers for the update logic.
