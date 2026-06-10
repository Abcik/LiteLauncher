# LiteLauncher Launcher Public Source

This module contains the public-safe LiteLauncher backend/core logic.

Included:

- offline profile storage;
- Microsoft OAuth/Xbox/Minecraft auth flow;
- encrypted Microsoft session storage;
- Microsoft skin upload, cape selection and profile appearance caching;
- Minecraft version manifest loading and local version discovery;
- version inheritance resolving;
- library, asset, native and client download preparation;
- Java runtime selection/download helpers;
- game launch argument generation and process startup;
- launcher state persistence and normalization;
- i18n data used by the backend and public-safe placeholders.

Redacted:

- official Swing pixel-perfect scenes;
- exact layout coordinates;
- text animation system;
- bitmap font atlas, glyph data and rasterization details;
- official logos, illustrations, taskbar/window icons and other product-defining assets.

The public `LiteLauncher` entry point is a placeholder. The audit surface is in `LauncherStore` and `net.litelauncher.backend.*`.
