package net.litelauncher.installer;

import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.TaskProgress;
import net.litelauncher.ui.UtilityLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class InstallerBackend {

    private static final String BOOTSTRAP_RESOURCE = "/payload/Bootstrap.jar";
    private static final String WINDOWS_ICON_RESOURCE = "/icons/windows/litelauncher.ico";
    private static final String MAC_ICON_RESOURCE = "/icons/macos/litelauncher.icns";
    private static final String LINUX_ICON_RESOURCE = "/icons/linux/litelauncher.png";

    public static void install(TaskProgress progress, UtilityLog log) throws Exception {
        report(progress, 0.05, "Preparing files... 5%");
        Files.createDirectories(OSUtils.bootstrapDirectory());
        Files.createDirectories(OSUtils.launcherDirectory());
        Files.createDirectories(OSUtils.javaDirectory());
        Files.createDirectories(OSUtils.iconsDirectory());
        Files.createDirectories(OSUtils.desktopDirectory());
        log.info("Install directory: " + OSUtils.bootstrapDirectory());

        report(progress, 0.25, "Writing bootstrap... 25%");
        copyBootstrap(log);

        report(progress, 0.45, "Writing icons... 45%");
        copyIcons(log);

        report(progress, 0.65, "Writing launcher... 65%");
        InstallerShortcuts.createLaunchScripts(log);

        report(progress, 0.85, "Creating shortcut... 85%");
        InstallerShortcuts.createDesktopShortcut(log);

        report(progress, 1.0, "Done. 100%");
        log.info("Installation completed successfully.");
    }

    private static void copyBootstrap(UtilityLog log) throws IOException {
        Path target = OSUtils.bootstrapJar();
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            copyResource(BOOTSTRAP_RESOURCE, temp);
            move(temp, target);
            log.info("Bootstrap installed: " + target);
        } finally {
            OSUtils.deleteQuietly(temp);
        }
    }

    private static void copyIcons(UtilityLog log) throws IOException {
        copyResource(WINDOWS_ICON_RESOURCE, OSUtils.iconsDirectory().resolve("launcher.ico"));
        copyResource(MAC_ICON_RESOURCE, OSUtils.iconsDirectory().resolve("launcher.icns"));
        copyResource(LINUX_ICON_RESOURCE, OSUtils.iconsDirectory().resolve("launcher.png"));
        log.info("Shortcut icons installed: " + OSUtils.iconsDirectory());
    }

    private static void copyResource(String resource, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream input = InstallerBackend.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing resource: " + resource);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void writeText(Path file, String text, boolean windowsLineEndings) throws IOException {
        Files.createDirectories(file.getParent());
        String data = windowsLineEndings ? text.replace("\n", "\r\n") : text;
        Files.writeString(file, data, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void move(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void report(TaskProgress progress, double value, String details) {
        if (progress != null) progress.update(Math.max(0.0, Math.min(1.0, value)), details);
    }
}
