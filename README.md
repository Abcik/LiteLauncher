
<img width="880" height="660" alt="launcher_preview" src="https://github.com/user-attachments/assets/beca2b0b-7049-41af-89f4-78091eaa06c7" />

# LiteLauncher Public Source

LiteLauncher is a lightweight Minecraft launcher focused on a clean install flow, transparent updates, Microsoft/offline profiles, Minecraft version resolving, Java runtime handling and game launch preparation.

This repository is a **public transparency source release**. It is intended to let users and developers audit the security-sensitive and infrastructure-sensitive parts of LiteLauncher without exposing the official pixel-perfect product shell.

> The official LiteLauncher UI, exact pixel layout, text animation system, bitmap font/glyph data, website presentation layer, official artwork and production release pipeline are intentionally not included here.

## What is included

- `bootstrap/` — update/launch layer, manifest parsing, launcher download verification, Java runtime download/extraction logic.
- `installer/` — install paths, bootstrap placement, OS shortcut/script creation logic.
- `launcher/` — backend launcher code: account/profile handling, Microsoft auth flow, encrypted session storage, Minecraft version resolving, downloads, Java runtime service, launch argument generation and game process startup.
- `docs/` — public architecture, redaction and security notes.

## What is intentionally redacted

- official Swing pixel UI scenes and exact layout coordinates;
- text animation logic;
- bitmap font atlas, glyph data and rasterization details;
- official website pixel app;
- official artwork/assets that define the product look;
- built binary payloads such as the embedded official `Bootstrap.jar`;
- production configs, logs, credentials, deployment files and future business/ad logic.

See [`REDACTION.md`](REDACTION.md) for the full redaction map.

## Why this repository exists

The goal is transparency where it matters most:

- how accounts are stored;
- how Microsoft sign-in works;
- how downloads are verified;
- how the launcher chooses Java;
- how Minecraft versions/libraries/assets are resolved;
- how the game process is started;
- how the bootstrap consumes update manifests and verifies launcher downloads

The goal is **not** to publish a clone-ready copy of the official LiteLauncher visual identity.

## Build status

This repository is not guaranteed to build into the exact official LiteLauncher application because the official UI layer and official assets are redacted.

Some modules include public-safe console/placeholders so the core logic remains easier to inspect and adapt for private testing.

## Security

Please see [`SECURITY.md`](SECURITY.md). Do not publish real session files, logs, production configs or release artifacts.

## License

See [`LICENSE.md`](LICENSE.md) and [`TRADEMARK.md`](TRADEMARK.md). In short: code is visible for transparency and auditing, but official branding/assets/product identity are not granted for reuse.
