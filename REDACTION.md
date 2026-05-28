# Redaction map

This public archive was generated from the private LiteLauncher project with product-identity and pixel-perfect implementation details removed.

## Kept

### Security-sensitive / trust-sensitive code

- Microsoft OAuth/Xbox/Minecraft auth client code.
- Local callback server flow.
- Offline profile handling.
- Microsoft session encryption/decryption store.
- Profile conversion to launch accounts.
- Download verification by size/SHA-1 where used by the original code.
- Java runtime resolving/downloading/extraction logic.
- Minecraft version resolving, inheritance merging and launch argument generation.
- Server manifest/download/static handlers, rate limiter and client IP handling.

### Operational code

- Installer path logic.
- OS shortcut/script generation.
- Bootstrap manifest loading/caching/update flow.
- Server runtime and HTTP handlers.

## Redacted

### Launcher UI

Removed/replaced:

- `launcher/net/litelauncher/frontend/scenes/**`
- `launcher/net/litelauncher/frontend/modules/animation/**`
- `launcher/net/litelauncher/frontend/modules/text/**`
- `launcher/net/litelauncher/frontend/modules/render/**`
- `launcher/net/litelauncher/frontend/modules/button/**`
- `launcher/net/litelauncher/frontend/modules/field/**`
- `launcher/net/litelauncher/frontend/modules/overlay/**`
- `launcher/net/litelauncher/frontend/modules/scroll/**`
- official `Palette`, `PixelPainter`, scene layout and window shape implementation
- `launcher/assets/**`

Minimal placeholders remain for backend compatibility:

- `net/litelauncher/frontend/Theme.java`
- `net/litelauncher/frontend/modules/auth/MicrosoftCallbackPage.java`
- `net/litelauncher/frontend/modules/auth/SkinAvatar.java`

### Installer/bootstrap UI

Removed/replaced:

- custom pixel progress windows;
- pixel text/glyph rasterizer;
- official UI painter classes;
- pixel font atlas/logo assets.

Kept:

- `TaskProgress`
- `UtilityLog`
- backend install/update logic.

### Website

The official static website pixel app was replaced with a minimal placeholder page.

Removed/replaced:

- bitmap font JS data;
- stage/orientation layout system;
- pixel content registry;
- custom renderers;
- official website images/theme assets.

### Binaries and production material

Removed:

- embedded official `Bootstrap.jar` payload;
- production configs/logs/secrets/deployment files, if any were present;
- release artifacts.

## Account storage was not redacted

Account/profile logic is intentionally kept because it is one of the main reasons to publish a transparency source release. Review these files first:

- `launcher/net/litelauncher/backend/modules/auth/AuthService.java`
- `launcher/net/litelauncher/backend/modules/auth/MicrosoftAuthClient.java`
- `launcher/net/litelauncher/backend/modules/auth/MicrosoftCallbackServer.java`
- `launcher/net/litelauncher/backend/modules/auth/MicrosoftSessionStore.java`
- `launcher/net/litelauncher/backend/modules/auth/OfflineProfileStore.java`
- `launcher/net/litelauncher/backend/platform/OSUtils.java`
