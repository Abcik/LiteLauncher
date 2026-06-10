# Security Policy

## Public audit scope

The most important public audit areas are:

- Microsoft authentication flow;
- offline profile handling;
- encrypted Microsoft session storage;
- bootstrap manifest verification and cache fallback;
- launcher download verification;
- Java runtime download/extraction;
- Minecraft library/asset/native download handling;
- game launch argument generation.

## Sensitive files that must never be published

Do not publish:

- `microsoft-sessions.json`;
- `offline-sessions.json`;
- Microsoft profile cache JSON files;
- launcher logs that may include local paths or error details;
- private signing keys, tokens, secrets or deployment files.

## Reporting issues

Please open a GitHub issue for non-sensitive bugs.

For sensitive security issues, do not paste real tokens, session files or private logs into a public issue. Share only the minimum reproduction details needed to understand the problem.
