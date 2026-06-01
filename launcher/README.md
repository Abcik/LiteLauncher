# Launcher module

This module contains the public, reviewable LiteLauncher launcher core.

Included:

- account/profile handling;
- Microsoft OAuth, Xbox and Minecraft Services auth flow;
- encrypted Microsoft session storage;
- offline profile storage;
- Minecraft version manifest loading and inheritance resolving;
- library, asset, native and client download logic;
- Java runtime selection/download/extraction;
- game launch argument generation and process startup;
- i18n resource loading used by the public-safe callback page.

Redacted:

- official Swing pixel UI scenes;
- exact layout coordinates and window shape implementation;
- text animation system;
- bitmap font atlas, glyph data and rasterizer internals;
- official product artwork/assets.

The public `LiteLauncher` entrypoint is a placeholder. The official desktop
application entrypoint is not published here because it depends on the private
UI shell.
