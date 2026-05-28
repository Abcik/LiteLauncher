package net.litelauncher.backend.platform;

import com.sun.management.OperatingSystemMXBean;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.lang.management.ManagementFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OSUtils {

    private static final int MEMORY_STEP_MB = 256;
    private static final int MIN_AUTO_MEMORY_MB = 512;
    private static final int MAX_AUTO_MEMORY_MB = 4096;
    private static final int DEFAULT_MEMORY_MB = 2048;

    public static Path minecraftDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) return Path.of(appData, ".minecraft");
            return Path.of(home, "AppData", "Roaming", ".minecraft");
        }

        if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "minecraft");
        return Path.of(home, ".minecraft");
    }

    public static int totalMemoryMb() {
        try {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long megabytes = os.getTotalMemorySize() / 1024L / 1024L;
            if (megabytes <= 0) return 0;
            if (megabytes > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            return (int) megabytes;
        } catch (Throwable exception) {
            return 0;
        }
    }

    public static int maxMemoryMb() {
        int memory = totalMemoryMb();
        return memory > 0 ? Math.max(MIN_AUTO_MEMORY_MB, memory) : MAX_AUTO_MEMORY_MB;
    }

    public static int automaticMemoryMb() {
        int memory = totalMemoryMb();
        if (memory <= 0) return DEFAULT_MEMORY_MB;

        int quarter = memory / 4;
        int rounded = ((quarter + MEMORY_STEP_MB / 2) / MEMORY_STEP_MB) * MEMORY_STEP_MB;
        return clamp(rounded, MIN_AUTO_MEMORY_MB, MAX_AUTO_MEMORY_MB);
    }

    public static int launcherScale(int logicalWidth) {
        return launcherScale(logicalWidth, logicalWidth);
    }

    public static int launcherScale(int logicalWidth, int logicalHeight) {
        int screenWidth = screenWidth();
        if (screenWidth <= 0 || logicalWidth <= 0) return 1;

        int scale = (screenWidth / 2) / logicalWidth;
        return clamp(scale, 1, maxLauncherScale(logicalHeight));
    }

    public static int maxLauncherScale(int logicalHeight) {
        int screenHeight = screenHeight();
        if (screenHeight <= 0 || logicalHeight <= 0) return 1;

        return Math.max(1, screenHeight / logicalHeight);
    }

    public static int screenWidth() {
        try {
            if (GraphicsEnvironment.isHeadless()) return 0;
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            return size == null ? 0 : size.width;
        } catch (Throwable exception) {
            return 0;
        }
    }

    public static int screenHeight() {
        try {
            if (GraphicsEnvironment.isHeadless()) return 0;
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            return size == null ? 0 : size.height;
        } catch (Throwable exception) {
            return 0;
        }
    }

    public static void openUri(URI uri) throws IOException {
        if (uri == null) throw new IOException("URI is empty.");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
            return;
        }

        openWithSystemCommand(uri.toString());
    }

    public static void openFile(Path file) throws IOException {
        if (file == null) throw new IOException("File path is empty.");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file.toFile());
            return;
        }

        openWithSystemCommand(file.toString());
    }

    private static void openWithSystemCommand(String target) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder builder;

        if (os.contains("mac")) builder = new ProcessBuilder("open", target);
        else if (os.contains("win")) builder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", target);
        else builder = new ProcessBuilder("xdg-open", target);

        builder.start();
    }

    public static Path versionsDirectory() {
        return minecraftDirectory().resolve("versions");
    }

    public static Path librariesDirectory() {
        return minecraftDirectory().resolve("libraries");
    }

    public static Path assetsDirectory() {
        return minecraftDirectory().resolve("assets");
    }

    public static Path assetIndexesDirectory() {
        return assetsDirectory().resolve("indexes");
    }

    public static Path assetObjectsDirectory() {
        return assetsDirectory().resolve("objects");
    }

    public static Path liteLauncherDirectory() {
        return minecraftDirectory().resolve("litelauncher");
    }

    public static Path javaDirectory() {
        return liteLauncherDirectory().resolve("java");
    }

    public static Path javaRuntimeDirectory(String runtimeId) {
        return javaDirectory().resolve(runtimeId == null || runtimeId.isBlank() ? "jre-8" : runtimeId);
    }

    public static Path nativesDirectory(String versionId) {
        String safeId = versionId == null || versionId.isBlank() ? "default" : versionId.replaceAll("[^A-Za-z0-9._-]", "_");
        return versionsDirectory().resolve(safeId).resolve("natives");
    }

    public static Path logsDirectory() {
        return minecraftDirectory().resolve("logs");
    }

    public static Path launcherProfileFile() {
        return minecraftDirectory().resolve("launcher_profiles.json");
    }

    public static Path launcherProfileMicrosoftStoreFile() {
        return minecraftDirectory().resolve("launcher_profiles_microsoft_store.json");
    }

    public static Path launcherDataDirectory() {
        return liteLauncherDirectory().resolve("data");
    }

    public static Path authDirectory() {
        return launcherDataDirectory().resolve("auth");
    }

    public static Path launcherStateFile() {
        return launcherDataDirectory().resolve("launcher-state.json");
    }

    public static Path versionManifestFile() {
        return versionsDirectory().resolve("version_manifest_v2.json");
    }

    public static Path offlineSessionsFile() {
        return authDirectory().resolve("offline-sessions.json");
    }

    public static Path microsoftSessionsFile() {
        return authDirectory().resolve("microsoft-sessions.json");
    }

    public static Path getMinecraftDirectory() {
        return minecraftDirectory();
    }

    public static String machineId() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return firstText(
                    windowsMachineGuid("HKLM\\SOFTWARE\\Microsoft\\Cryptography"),
                    windowsMachineGuid("HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Cryptography")
            );
        }
        if (os.contains("mac")) return macHardwareUuid();
        return firstText(readText(Path.of("/etc/machine-id")), readText(Path.of("/var/lib/dbus/machine-id")));
    }

    private static String windowsMachineGuid(String key) {
        String output = runCommand("reg", "query", key, "/v", "MachineGuid");
        if (output == null) return null;

        for (String line : output.split("\\R")) {
            if (!line.contains("MachineGuid")) continue;
            String[] parts = line.trim().split("\\s+", 3);
            if (parts.length == 3) return parts[2].trim();
        }
        return null;
    }

    private static String macHardwareUuid() {
        String output = firstText(
                runCommand("/usr/sbin/ioreg", "-rd1", "-c", "IOPlatformExpertDevice"),
                runCommand("ioreg", "-rd1", "-c", "IOPlatformExpertDevice")
        );
        if (output == null) return null;

        for (String line : output.split("\\R")) {
            int marker = line.indexOf("IOPlatformUUID");
            if (marker < 0) continue;

            int first = line.indexOf('"', marker);
            int second = first < 0 ? -1 : line.indexOf('"', first + 1);
            int third = second < 0 ? -1 : line.indexOf('"', second + 1);
            if (second >= 0 && third > second) return line.substring(second + 1, third);
        }
        return null;
    }

    private static String readText(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            return text(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return null;
        }
    }

    private static String runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            int code = process.waitFor();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return code == 0 ? text(output) : null;
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static String firstText(String first, String second) {
        return text(first) != null ? text(first) : text(second);
    }

    private static String text(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

}
