# Repository Update Notes

Recommended commit message:

```text
Update public transparency source to v0.3.132
```

Recommended commit description:

```text
Refresh launcher, bootstrap and installer public source after refactor.
Keep official pixel-perfect UI, glyph/font system, website/server code and production assets redacted.
```

When applying this archive over the existing Git repository, use a mirror copy command so deleted files are also removed.

On Windows PowerShell:

```powershell
robocopy "C:\Temp\LiteLauncher-public-source-v0.3.132" "C:\Projects\LiteLauncher" /MIR /XD .git
```

Then commit with:

```bash
git add -A
git commit -m "Update public transparency source to v0.3.132"
git push
```
