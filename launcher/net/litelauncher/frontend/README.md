# Redacted launcher frontend

The official Swing pixel UI is not included in this repository.

Redacted examples:

- scene layout and pixel-perfect coordinates;
- custom window shape/chrome details;
- text animation logic;
- glyph layout/rasterization;
- pixel font atlas and generated glyph data;
- proprietary artwork and official UI images.

Kept minimal public placeholders:

- `Theme` enum, because backend state and auth callback code reference it;
- simple Microsoft callback HTML renderer, because the Microsoft auth flow should remain auditable;
- `SkinAvatar` placeholder, because `LauncherStore` exposes avatar generation.
