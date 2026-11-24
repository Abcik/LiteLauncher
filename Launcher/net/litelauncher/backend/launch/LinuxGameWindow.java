package net.litelauncher.backend.launch;

import com.grack.nanojson.JsonObject;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.LauncherPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gives the Minecraft window a dock icon on Linux.
 *
 * Docks (GNOME, KDE, ...) find a window's icon through a .desktop file whose StartupWMClass matches
 * the window's WM_CLASS. Minecraft has no such file, and its WM_CLASS depends on the version
 * (window title, or "java"). So every game window gets one fixed class, and a hidden .desktop entry
 * with that class points to the Minecraft icon taken from the game's own assets.
 */
final class LinuxGameWindow {

    static final String WINDOW_CLASS = "litelauncher-minecraft";

    /* Largest first: newer asset indexes have up to 256x256, old ones only 32x32. */
    private static final String[] ICONS = {
            "icons/icon_256x256.png", "icons/icon_128x128.png", "icons/icon_48x48.png", "icons/icon_32x32.png"
    };

    private LinuxGameWindow() {
    }

    /** Sets the same WM_CLASS for every window library Minecraft has used. */
    static List<String> withWindowClass(List<String> command, Map<String, String> environment) {
        environment.put("RESOURCE_NAME", WINDOW_CLASS); // GLFW (1.13 - 1.21): WM_CLASS instance
        environment.put("SDL_APP_ID", WINDOW_CLASS);    // SDL3 (26.x): WM_CLASS class / Wayland app_id
        List<String> result = new ArrayList<>(command);
        result.add(1, "-DLWJGL_WM_CLASS=" + WINDOW_CLASS); // LWJGL 2 (up to 1.12): WM_CLASS class
        return result;
    }

    /** Creates or refreshes the hidden desktop entry. Never fails the launch. */
    static void ensureDesktopEntry(JsonObject assetIndex, Path assetObjects) {
        try {
            Path icon = LauncherPaths.launcherDataDirectory().resolve("linux").resolve("minecraft-icon.png");
            updateIcon(assetIndex, assetObjects, icon);
            if (!Files.isRegularFile(icon)) return;

            Path entry = applicationsDirectory().resolve(WINDOW_CLASS + ".desktop");
            String text = desktopEntry(icon);
            if (Files.isRegularFile(entry) && text.equals(Files.readString(entry, StandardCharsets.UTF_8))) return;
            Files.createDirectories(entry.getParent());
            BackendUtils.writeAtomic(entry, text);
            LauncherLog.info("Minecraft desktop entry written: " + entry);
        } catch (Exception exception) {
            LauncherLog.error("Unable to prepare Minecraft dock icon.", exception);
        }
    }

    /** Copies the best icon of this version, unless a larger one is already in place. */
    private static void updateIcon(JsonObject assetIndex, Path assetObjects, Path target) throws Exception {
        JsonObject objects = assetIndex == null ? null : assetIndex.getObject("objects");
        if (objects == null) return;
        for (String name : ICONS) {
            JsonObject object = objects.getObject(name);
            String hash = object == null ? "" : object.getString("hash", "");
            if (hash.length() < 2) continue;
            Path source = assetObjects.resolve(hash.substring(0, 2)).resolve(hash);
            if (!Files.isRegularFile(source)) continue;
            if (width(source) <= width(target)) return;
            Files.createDirectories(target.getParent());
            BackendUtils.writeAtomic(target, Files.readAllBytes(source));
            LauncherLog.info("Minecraft dock icon updated from " + name);
            return;
        }
    }

    static String desktopEntry(Path icon) {
        Path launcher = LauncherPaths.liteLauncherDirectory().resolve("bootstrap").resolve("launch-litelauncher.sh");
        return "[Desktop Entry]\n"
                + "Version=1.0\n"
                + "Type=Application\n"
                + "Name=Minecraft\n"
                + "Comment=Minecraft started from LiteLauncher\n"
                // Exec is required by the spec; opening it from the dock starts the launcher.
                + "Exec=" + execArgument(launcher) + "\n"
                + "Icon=" + oneLine(icon.toAbsolutePath().toString()) + "\n"
                + "Terminal=false\n"
                + "Categories=Game;\n"
                // Not an app of its own: only lends its icon to running game windows.
                + "NoDisplay=true\n"
                + "StartupWMClass=" + WINDOW_CLASS + "\n";
    }

    private static int width(Path image) {
        try {
            if (!Files.isRegularFile(image)) return 0;
            BufferedImage read = ImageIO.read(image.toFile());
            return read == null ? 0 : read.getWidth();
        } catch (Exception exception) {
            return 0;
        }
    }

    private static Path applicationsDirectory() {
        String dataHome = System.getenv("XDG_DATA_HOME");
        Path base = dataHome != null && !dataHome.isBlank()
                ? Path.of(dataHome)
                : Path.of(System.getProperty("user.home", "."), ".local", "share");
        return base.resolve("applications");
    }

    private static String execArgument(Path path) {
        return "\"" + oneLine(path.toAbsolutePath().toString())
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
                .replace("`", "\\`") + "\"";
    }

    private static String oneLine(String value) {
        return value.replace("\n", "").replace("\r", "");
    }
}
