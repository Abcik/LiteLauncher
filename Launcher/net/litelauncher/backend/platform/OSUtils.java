package net.litelauncher.backend.platform;

import com.sun.management.OperatingSystemMXBean;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Path;

public final class OSUtils {

    private static final int MEMORY_STEP_MB = 256;
    private static final int MIN_AUTO_MEMORY_MB = 512;
    private static final int MAX_AUTO_MEMORY_MB = 4096;
    private static final int DEFAULT_MEMORY_MB = 2048;

    public static OperatingSystem os() {
        return OperatingSystem.current();
    }

    public static void disableOsScaling() {
        if (os().windows()) System.setProperty("sun.java2d.uiScale", "1");
    }

    public static void setApplicationName(String name) {
        if (os().macos()) System.setProperty("apple.awt.application.name", name);
    }

    public static int totalMemoryMb() {
        try {
            OperatingSystemMXBean system = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long megabytes = system.getTotalMemorySize() / 1024L / 1024L;
            if (megabytes <= 0) return 0;
            if (megabytes > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            return (int) megabytes;
        } catch (Throwable _) {
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
        return Math.clamp(rounded, MIN_AUTO_MEMORY_MB, MAX_AUTO_MEMORY_MB);
    }

    public static int launcherScale(int logicalWidth, int logicalHeight) {
        int width = screenWidth();
        if (width <= 0 || logicalWidth <= 0) return 1;

        int scale = (width / 2) / logicalWidth;
        return Math.clamp(scale, 1, maxLauncherScale(logicalHeight));
    }

    public static int maxLauncherScale(int logicalHeight) {
        int height = screenHeight();
        if (height <= 0 || logicalHeight <= 0) return 1;

        return Math.max(1, height / logicalHeight);
    }

    public static int screenWidth() {
        try {
            if (GraphicsEnvironment.isHeadless()) return 0;
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            return size == null ? 0 : size.width;
        } catch (Throwable _) {
            return 0;
        }
    }

    public static int screenHeight() {
        try {
            if (GraphicsEnvironment.isHeadless()) return 0;
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            return size == null ? 0 : size.height;
        } catch (Throwable _) {
            return 0;
        }
    }

    public static boolean is64Bit() {
        return System.getProperty("os.arch", "").contains("64");
    }

    public static int currentJavaMajor() {
        return Runtime.version().feature();
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

    public static String machineId() {
        return MachineId.read();
    }

    private static void openWithSystemCommand(String target) throws IOException {
        OperatingSystem current = os();
        ProcessBuilder builder;
        if (current.macos()) builder = new ProcessBuilder("open", target);
        else if (current.windows()) builder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", target);
        else builder = new ProcessBuilder("xdg-open", target);
        builder.start();
    }

}
