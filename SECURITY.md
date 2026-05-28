# Security policy

Please report security issues privately to the project owner before publishing details.

## Useful audit areas

- `launcher/net/litelauncher/backend/modules/auth/` — Microsoft/offline account handling.
- `launcher/net/litelauncher/backend/modules/download/` — validated downloads and temp-file handling.
- `launcher/net/litelauncher/backend/modules/java/` — Java runtime download and extraction.
- `bootstrap/net/litelauncher/bootstrap/` — launcher update logic.
- `server/net/litelauncher/server/http/` — manifest/download endpoints, static file serving, rate limiting and client IP handling.

## Do not publish

- real Microsoft session files;
- `offline-sessions.json` or `microsoft-sessions.json` from a user's machine;
- production configs;
- server logs/visit logs;
- private release artifacts;
- credentials, tokens, certificates or deployment keys.
