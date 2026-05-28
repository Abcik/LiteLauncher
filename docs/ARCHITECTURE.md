# Public architecture overview

LiteLauncher is split into five practical components:

1. `installer` — places the bootstrap layer into the user's Minecraft directory and creates OS shortcuts/scripts.
2. `bootstrap` — downloads and validates the launcher manifest, launcher jar and Java runtime, then starts the main launcher.
3. `launcher` — manages profiles, auth, versions, downloads, Java runtimes and Minecraft launch arguments.
4. `server` — serves the static site, launcher manifest API and download endpoints.
5. `web` — official pixel website in the private repository; only a placeholder is included here.

## Runtime flow

User downloads installer → installer places bootstrap → shortcut starts bootstrap → bootstrap checks manifest and launcher jar → launcher handles accounts/version/downloads/runtime → launcher starts Minecraft.

## Trust-sensitive areas

- Auth/session storage: `launcher/net/litelauncher/backend/modules/auth/`
- Download verification: `launcher/net/litelauncher/backend/modules/download/`, `bootstrap/net/litelauncher/bootstrap/BootstrapBackend.java`
- Java runtime handling: `launcher/net/litelauncher/backend/modules/java/`, bootstrap runtime code
- Minecraft launch: `launcher/net/litelauncher/backend/modules/launch/`
- Distribution server: `server/net/litelauncher/server/http/`
