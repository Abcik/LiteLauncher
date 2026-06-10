# Redaction Map

This repository is intentionally not a full copy of the official LiteLauncher product.

## Preserved

- Installer backend/path/shortcut logic.
- Bootstrap update, signed manifest verification, download verification and Java runtime logic.
- Launcher account/auth/session storage logic.
- Minecraft version resolving, download preparation, Java runtime selection and game launch logic.
- Public-safe placeholder entry points.
- Documentation needed to understand the public source tree.

## Removed or replaced

### Visual product shell

Removed:

- official Swing pixel UI scenes;
- exact layout coordinates;
- button/scroll/slider/input visual implementation;
- custom window shape/pixel-perfect drawing details;
- text animation system;
- bitmap font/glyph rasterization implementation and atlas.

Replaced with:

- simple placeholder `main` classes that explain where the audit surface lives.

### Assets

Removed:

- official logos;
- illustrations;
- taskbar/window icons;
- bitmap font atlas;
- installer/bootstrap visual assets;
- embedded production `Bootstrap.jar`.

## Rationale

The public repository should let users verify that LiteLauncher handles accounts, downloads, updates and Minecraft startup transparently, while protecting the original visual identity and product presentation from clone-ready reuse.
