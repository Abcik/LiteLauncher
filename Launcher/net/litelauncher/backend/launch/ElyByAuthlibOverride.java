package net.litelauncher.backend.launch;

import net.litelauncher.backend.platform.LauncherPaths;
import net.litelauncher.backend.download.DownloadFile;
import net.litelauncher.i18n.I18n;

import java.nio.file.Path;

record ElyByAuthlibOverride(String sourceVersion, String fileName, String url, String sha1, long size) {

    DownloadFile download() throws GameLaunchException {
        return new DownloadFile(url, path(), sha1, size, I18n.text("progress.downloadingLibraries"));
    }

    Path path() throws GameLaunchException {
        if (sourceVersion == null || !sourceVersion.matches("[A-Za-z0-9._+-]+")) {
            throw new GameLaunchException("Invalid Ely.by Authlib source version.");
        }
        if (fileName == null || !fileName.matches("authlib-[A-Za-z0-9._+-]+\\.jar")) {
            throw new GameLaunchException("Invalid Ely.by Authlib file name.");
        }

        String patchedVersion = fileName.substring("authlib-".length(), fileName.length() - ".jar".length());
        if (!patchedVersion.startsWith(sourceVersion + "-ely.")) {
            throw new GameLaunchException("Ely.by Authlib file does not match source version.");
        }

        Path root = LauncherPaths.librariesDirectory().toAbsolutePath().normalize();
        Path path = root.resolve("by/ely/authlib")
                .resolve(patchedVersion)
                .resolve(fileName)
                .toAbsolutePath().normalize();
        if (!path.startsWith(root) || path.equals(root)) throw new GameLaunchException("Invalid Ely.by Authlib path.");
        return path;
    }

    String patchedVersion() {
        if (fileName == null || !fileName.startsWith("authlib-") || !fileName.endsWith(".jar")) return "";
        return fileName.substring("authlib-".length(), fileName.length() - ".jar".length());
    }
}
