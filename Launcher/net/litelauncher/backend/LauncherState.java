package net.litelauncher.backend;

import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.Language;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.Theme;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LauncherState {

    public static final String TITLE = "LiteLauncher";
    public static final String LAUNCHER_VERSION = "v0.8.1";

    public static final int LOGICAL_WIDTH = 440;
    public static final int LOGICAL_HEIGHT = 330;

    public int scale = OSUtils.launcherScale(LOGICAL_WIDTH, LOGICAL_HEIGHT);
    public Language language = Language.ENGLISH;
    public Theme theme = Theme.LIGHT;

    public int memoryAmount = OSUtils.automaticMemoryMb();
    public boolean autoMemory = true;

    public int screenWidth = 854;
    public int screenHeight = 480;
    public boolean fullscreen;
    public String jvmArguments = "";
    public boolean closeAfterLaunch;
    public boolean instancesStorageSystem;
    public boolean customThemes;

    public boolean releaseFilter = true;
    public boolean modpackFilter = true;
    public boolean snapshotFilter = false;
    public boolean legacyFilter = false;

    public String selectedProfileId;
    public String selectedVersionId;

    public static LauncherState load() {
        LauncherState state = new LauncherState();
        Path file = LauncherPaths.launcherStateFile();

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                state.read(JsonParser.object().from(reader));
            } catch (Exception exception) {
                LauncherLog.error("Unable to load launcher state.", exception);
            }
        }

        state.normalize();
        state.save();
        return state;
    }

    public void save() {
        Path file = LauncherPaths.launcherStateFile();
        try {
            BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(toJson()).done());
        } catch (Exception exception) {
            LauncherLog.error("Unable to save launcher state.", exception);
        }
    }

    private void read(JsonObject json) {
        scale = Math.max(1, json.getInt("scale", scale));
        language = enumValue(Language.class, json.getString("language"), language);
        theme = enumValue(Theme.class, json.getString("theme"), theme);

        memoryAmount = json.getInt("memoryAmount", memoryAmount);
        autoMemory = json.getBoolean("autoMemory", autoMemory);

        screenWidth = Math.max(1, json.getInt("screenWidth", screenWidth));
        screenHeight = Math.max(1, json.getInt("screenHeight", screenHeight));
        fullscreen = json.getBoolean("fullscreen", fullscreen);
        jvmArguments = json.getString("jvmArguments", jvmArguments);
        closeAfterLaunch = json.getBoolean("closeAfterLaunch", closeAfterLaunch);
        instancesStorageSystem = json.getBoolean("instancesStorageSystem", instancesStorageSystem);
        customThemes = json.getBoolean("customThemes", customThemes);

        releaseFilter = json.getBoolean("releaseFilter", releaseFilter);
        modpackFilter = json.getBoolean("modpackFilter", modpackFilter);
        snapshotFilter = json.getBoolean("snapshotFilter", snapshotFilter);
        legacyFilter = json.getBoolean("legacyFilter", legacyFilter);

        selectedProfileId = json.getString("selectedProfileId", selectedProfileId);
        selectedVersionId = json.getString("selectedVersionId", selectedVersionId);
    }

    private JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("scale", scale);
        json.put("language", language.name());
        json.put("theme", theme.name());

        json.put("memoryAmount", memoryAmount);
        json.put("autoMemory", autoMemory);
        json.put("screenWidth", screenWidth);
        json.put("screenHeight", screenHeight);
        json.put("fullscreen", fullscreen);
        json.put("jvmArguments", jvmArguments);
        json.put("closeAfterLaunch", closeAfterLaunch);
        json.put("instancesStorageSystem", instancesStorageSystem);
        json.put("customThemes", customThemes);

        json.put("releaseFilter", releaseFilter);
        json.put("modpackFilter", modpackFilter);
        json.put("snapshotFilter", snapshotFilter);
        json.put("legacyFilter", legacyFilter);

        json.put("selectedProfileId", selectedProfileId);
        json.put("selectedVersionId", selectedVersionId);
        return json;
    }

    public void normalize() {
        scale = Math.clamp(scale, 1, OSUtils.maxLauncherScale(LOGICAL_HEIGHT));
        memoryAmount = Math.clamp(memoryAmount, 512, OSUtils.maxMemoryMb());
        if (autoMemory) memoryAmount = OSUtils.automaticMemoryMb();
        if (screenWidth < 1) screenWidth = 854;
        if (screenHeight < 1) screenHeight = 480;
        if (jvmArguments == null) jvmArguments = "";
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String name, T fallback) {
        if (name == null || name.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException _) {
            return fallback;
        }
    }
}
