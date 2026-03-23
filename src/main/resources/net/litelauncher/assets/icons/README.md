# Icon assets for LiteLauncher

These PNG files are loaded from inside the JAR via classpath resources.
To replace any icon, keep the same filename and canvas size. Transparent background is recommended.

## Files and target sizes

- `logo-dark.png`, `logo-light.png` — 196x28 px
- `profile-dark.png`, `profile-light.png` — 24x24 px
- `theme-dark.png`, `theme-light.png` — 20x20 px
- `language-dark.png`, `language-light.png` — 20x20 px
- `settings-dark.png`, `settings-light.png` — 20x20 px
- `minimize-dark.png`, `minimize-light.png` — 16x16 px
- `close-dark.png`, `close-light.png` — 16x16 px
- `trash-dark.png`, `trash-light.png` — 16x16 px
- `chevron-down-dark.png`, `chevron-down-light.png` — 14x14 px

## Where they are used

- `logo-*` — left side of header
- `profile-*` — avatar inside the account chip
- `theme-*` — theme toggle button
- `language-*` — language button
- `settings-*` — settings button
- `minimize-*` — window minimize button
- `close-*` — header close button and popup close button
- `trash-*` — delete account button in the account popup
- `chevron-down-*` — reserved for dropdown arrows

## Notes

- `*-dark` is used when the launcher is in dark theme.
- `*-light` is used when the launcher is in light theme.
- If you want one icon for both themes, you can keep both files identical.
