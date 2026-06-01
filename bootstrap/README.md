# Bootstrap module

This module contains the public LiteLauncher update/launch layer.

Included:

- launcher manifest download and parsing;
- cached manifest fallback;
- launcher jar size/SHA-1 verification;
- atomic launcher jar replacement;
- older launcher cleanup;
- Adoptium JRE download/extraction logic;
- safe archive extraction checks;
- process startup for the main launcher.

Redacted:

- official pixel progress window;
- bitmap font/glyph resources;
- official visual assets.

The public `Bootstrap` entrypoint is console-based so the update flow remains
easy to inspect without publishing the official UI.
