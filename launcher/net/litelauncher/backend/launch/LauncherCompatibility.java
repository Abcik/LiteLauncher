package net.litelauncher.backend.launch;

import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LauncherCompatibility {

    public static void ensureProfileFiles() {
        ensureJsonFile(OSUtils.launcherProfileFile());
        ensureJsonFile(OSUtils.launcherProfileMicrosoftStoreFile());
    }

    private static void ensureJsonFile(Path file) {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, "{\"profiles\": {}}", StandardCharsets.UTF_8);
                LauncherLog.info("Created compatibility profile file: " + file);
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to create compatibility profile file: " + file, exception);
        }
    }
}
