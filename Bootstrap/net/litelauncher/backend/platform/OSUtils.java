package net.litelauncher.backend.platform;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class OSUtils {

    public static OperatingSystem os() {
        return OperatingSystem.current();
    }

    public static void disableOsScaling() {
        if (os().windows()) System.setProperty("sun.java2d.uiScale", "1");
    }

    public static Path minecraftDirectory() {
        return LauncherPaths.minecraftDirectory();
    }

    public static Path logsDirectory() {
        return LauncherPaths.logsDirectory();
    }

    public static Path liteLauncherDirectory() {
        return LauncherPaths.liteLauncherDirectory();
    }

    public static Path javaDirectory() {
        return LauncherPaths.javaDirectory();
    }

    public static Path javaRuntimeDirectory(String runtimeId) {
        return LauncherPaths.javaRuntimeDirectory(runtimeId);
    }

    public static Path bootstrapDirectory() {
        return LauncherPaths.bootstrapDirectory();
    }

    public static Path launcherDirectory() {
        return LauncherPaths.launcherDirectory();
    }

    public static Path bootstrapManifestFile() {
        return LauncherPaths.bootstrapManifestFile();
    }

    public static int currentJavaMajor() {
        String version = System.getProperty("java.version", "17");
        try {
            if (version.startsWith("1.")) return Integer.parseInt(version.substring(2, 3));
            int dot = version.indexOf('.');
            String major = dot < 0 ? version : version.substring(0, dot);
            return Math.max(1, Integer.parseInt(major));
        } catch (Exception ignored) {
            return 17;
        }
    }

    public static void openFile(Path file) throws IOException {
        if (file == null) throw new IOException("File path is empty.");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file.toFile());
            return;
        }

        openWithSystemCommand(file.toString());
    }

    public static void deleteDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) return;
        try (var stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    public static void deleteQuietly(Path path) {
        try {
            if (path == null) return;
            if (Files.isDirectory(path)) deleteDirectory(path);
            else Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private static void openWithSystemCommand(String target) throws IOException {
        OperatingSystem current = os();
        ProcessBuilder builder;
        if (current.macos()) builder = new ProcessBuilder("open", target);
        else if (current.windows()) builder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", target);
        else builder = new ProcessBuilder("xdg-open", target);
        builder.start();
    }

    private OSUtils() {
    }
}
