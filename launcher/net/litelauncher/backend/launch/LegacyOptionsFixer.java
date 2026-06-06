package net.litelauncher.backend.launch;

import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class LegacyOptionsFixer {

    private static final long OPTIONS_SPLIT_TIME = Instant.parse("2013-07-01T00:00:00Z").toEpochMilli();

    void fixIfNeeded(ResolvedVersion version) throws GameLaunchException {
        if (!needsFix(version)) return;

        Path file = OSUtils.minecraftDirectory().resolve("options.txt");
        if (!Files.isRegularFile(file)) return;

        try {
            List<String> original = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<String> fixed = new ArrayList<>();
            boolean changed = false;
            boolean hasLanguage = false;

            for (String line : original) {
                if (line.startsWith("lang:")) {
                    hasLanguage = true;
                    String value = line.substring("lang:".length()).trim();
                    String fixedLanguage = fixLanguage(value);
                    String fixedLine = "lang:" + fixedLanguage;
                    fixed.add(fixedLine);
                    changed |= !fixedLine.equals(line);
                } else {
                    fixed.add(line);
                }
            }

            if (!hasLanguage) {
                fixed.add("lang:en_US");
                changed = true;
            }

            if (!changed) return;
            backup(file);
            Files.write(file, fixed, StandardCharsets.UTF_8);
            LauncherLog.info("Legacy options fixed for " + version.id() + ": " + file);
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to fix legacy options.txt.", exception);
        }
    }

    private boolean needsFix(ResolvedVersion version) {
        if (version == null) return false;
        if ("pre-1.6".equals(version.assets())) return true;
        long time = version.clientReleaseTime();
        return time > 0 && time < OPTIONS_SPLIT_TIME;
    }

    private String fixLanguage(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) return "en_US";

        String[] parts = text.replace('-', '_').split("_");
        if (parts.length >= 2 && parts[0].length() == 2 && parts[1].length() == 2) {
            return parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT);
        }
        return text;
    }

    private void backup(Path file) throws IOException {
        Path directory = OSUtils.launcherDataDirectory().resolve("options-backups");
        Files.createDirectories(directory);
        Path backup = directory.resolve("options-legacy-backup.txt");
        if (!Files.isRegularFile(backup)) Files.copy(file, backup);
    }
}
