# LiteLauncher

**LiteLauncher** is a lightweight open-source desktop launcher focused on speed, clarity, and a clean user experience.

Created by **Abcik**. Contact: **abcik@ukr.net**

**!!! WORK STILL IN PROGRESS !!!**


## Why LiteLauncher

- lightweight interface with minimal visual noise
- fast, focused desktop flow built around quick access to play
- transparent open-source codebase
- no offline or cracked account flow in the public project
- designed around standard Microsoft account support after platform approval

## Project status

LiteLauncher is being prepared for public release and further development.

Microsoft account integration and launcher-side authorization are planned after platform approval. The public repository already reflects the intended product direction and user interface.

## GitHub Pages

This repository includes a ready-to-publish GitHub Pages website in `docs/`.

After pushing the repository to GitHub:

1. Open **Settings → Pages**.
2. Set the source to **Deploy from a branch**.
3. Choose the **main** branch and the **/docs** folder.
4. Save.

GitHub Pages can publish static files directly from a repository, including the `docs` folder, so a separate hosting setup is not required. It can also be connected to a custom domain later if needed.

## Project layout

```text
src/main/java        Java source files
docs/                GitHub Pages website
src/main/resources   icons and bundled resources
```

## Run locally

### Maven

```bash
mvn compile exec:java
```

### Plain JDK

```bash
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
java -cp out:src/main/resources net.litelauncher.app.LiteLauncherApplication
```

## Public site pages

- `/` — project overview
- `/privacy.html` — privacy statement
- `/terms.html` — terms of use

## Branding notice

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**
## License

Released under the MIT License.

