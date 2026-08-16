package net.litelauncher.frontend;

import net.litelauncher.backend.BackendUtils;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.Theme;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.LauncherPaths;

import java.awt.Color;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

final class ThemeColorOverrides {

    private static final ThemeColorOverrides INSTANCE = new ThemeColorOverrides();

    private final EnumMap<Theme, EnumMap<Palette, Color>> overrides = new EnumMap<>(Theme.class);
    private boolean enabled;
    private boolean loaded;
    private long revision;

    private ThemeColorOverrides() {
        for (Theme theme : Theme.values()) overrides.put(theme, new EnumMap<>(Palette.class));
    }

    static ThemeColorOverrides get() {
        return INSTANCE;
    }

    synchronized Color color(Palette palette, Theme theme, Color fallback) {
        if (!enabled) return fallback;
        Color override = overrides.get(safeTheme(theme)).get(palette);
        return override == null ? fallback : override;
    }

    synchronized String customHex(Palette palette, Theme theme) {
        ensureLoadedForEditing();
        Color override = overrides.get(safeTheme(theme)).get(palette);
        return override == null ? null : hex(override);
    }

    synchronized Color customizationColor(Palette palette, Theme theme, Color fallback) {
        ensureLoadedForEditing();
        Color override = overrides.get(safeTheme(theme)).get(palette);
        return override == null ? fallback : override;
    }

    synchronized boolean setCustomHex(Palette palette, Theme theme, String hex) {
        if (palette == null) return false;
        ensureLoadedForEditing();

        Theme safeTheme = safeTheme(theme);
        EnumMap<Palette, Color> themeOverrides = overrides.get(safeTheme);

        Color next = parseHex(hex);
        Color fallback = palette.defaultColor(safeTheme);
        if (next != null && sameRgb(next, fallback)) next = null;
        else if (next != null) next = withAlpha(next, fallback.getAlpha());

        Color previous = themeOverrides.get(palette);
        if (sameNullableRgb(previous, next)) return false;

        if (next == null) themeOverrides.remove(palette);
        else themeOverrides.put(palette, next);

        revision++;
        save();
        return true;
    }

    synchronized void importFile(Path source) throws Exception {
        if (source == null || !Files.isRegularFile(source)) throw new IllegalArgumentException("Theme file does not exist.");

        EnumMap<Theme, EnumMap<Palette, Color>> imported = emptyOverrides();
        EnumSet<Theme> seenThemes = EnumSet.noneOf(Theme.class);

        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.object().from(reader);
            for (Map.Entry<String, Object> themeEntry : root.entrySet()) {
                Theme theme = parseThemeName(themeEntry.getKey());
                if (theme == null || !seenThemes.add(theme)) throw new IllegalArgumentException("Invalid theme section: " + themeEntry.getKey());
                if (!(themeEntry.getValue() instanceof JsonObject themeJson)) throw new IllegalArgumentException("Theme section must be an object.");

                EnumMap<Palette, Color> themeOverrides = imported.get(theme);
                for (Map.Entry<String, Object> colorEntry : themeJson.entrySet()) {
                    Palette palette;
                    try {
                        palette = Palette.valueOf(colorEntry.getKey());
                    } catch (IllegalArgumentException exception) {
                        throw new IllegalArgumentException("Unknown theme color: " + colorEntry.getKey(), exception);
                    }

                    if (!(colorEntry.getValue() instanceof String value)) throw new IllegalArgumentException("Theme color must be a HEX string.");
                    Color color = parseHex(value);
                    if (color == null) throw new IllegalArgumentException("Invalid HEX color: " + value);

                    Color fallback = palette.defaultColor(theme);
                    if (!sameRgb(color, fallback)) themeOverrides.put(palette, withAlpha(color, fallback.getAlpha()));
                }
            }
        }

        writeOverrides(imported);
        replaceOverrides(imported);
        loaded = true;
        revision++;
    }

    synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;
        if (enabled) reloadFromDisk();
        revision++;
    }

    synchronized void deleteFileAndClear() {
        boolean hadValues = !isEmpty(overrides);
        boolean deleted = false;
        try {
            deleted = Files.deleteIfExists(LauncherPaths.themeColorsFile());
        } catch (Exception exception) {
            LauncherLog.error("Unable to delete custom theme colors.", exception);
        }

        clearOverrides();
        loaded = true;
        if (hadValues || deleted) revision++;
    }

    synchronized long revision() {
        return revision;
    }

    private void ensureLoadedForEditing() {
        if (!loaded) reloadFromDisk();
    }

    private void reloadFromDisk() {
        clearOverrides();
        loaded = true;

        Path file = LauncherPaths.themeColorsFile();
        if (!Files.isRegularFile(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.object().from(reader);
            for (Theme theme : Theme.values()) {
                JsonObject themeJson = themeObject(root, theme);
                if (themeJson == null) continue;

                EnumMap<Palette, Color> themeOverrides = overrides.get(theme);
                for (Palette palette : Palette.values()) {
                    Color color = parseHex(themeJson.getString(palette.name()));
                    Color fallback = palette.defaultColor(theme);
                    if (color == null || sameRgb(color, fallback)) continue;
                    themeOverrides.put(palette, withAlpha(color, fallback.getAlpha()));
                }
            }
        } catch (Exception exception) {
            clearOverrides();
            LauncherLog.error("Unable to load custom theme colors.", exception);
        }
    }

    private void clearOverrides() {
        for (EnumMap<Palette, Color> themeOverrides : overrides.values()) themeOverrides.clear();
    }

    private void replaceOverrides(EnumMap<Theme, EnumMap<Palette, Color>> values) {
        for (Theme theme : Theme.values()) {
            EnumMap<Palette, Color> target = overrides.get(theme);
            target.clear();
            target.putAll(values.get(theme));
        }
    }

    private void save() {
        try {
            writeOverrides(overrides);
        } catch (Exception exception) {
            LauncherLog.error("Unable to save custom theme colors.", exception);
        }
    }

    private void writeOverrides(EnumMap<Theme, EnumMap<Palette, Color>> values) throws Exception {
        Path file = LauncherPaths.themeColorsFile();
        if (isEmpty(values)) {
            Files.deleteIfExists(file);
            return;
        }

        JsonObject root = new JsonObject();
        for (Theme theme : Theme.values()) {
            Map<Palette, Color> themeOverrides = values.get(theme);
            if (themeOverrides == null || themeOverrides.isEmpty()) continue;

            JsonObject themeJson = new JsonObject();
            for (Palette palette : Palette.values()) {
                Color color = themeOverrides.get(palette);
                if (color != null) themeJson.put(palette.name(), "#" + hex(color));
            }
            if (!themeJson.isEmpty()) root.put(themeName(theme), themeJson);
        }

        BackendUtils.writeAtomic(file, JsonWriter.indent("  ").string().value(root).done());
    }

    private static boolean isEmpty(EnumMap<Theme, EnumMap<Palette, Color>> values) {
        return values.values().stream().allMatch(Map::isEmpty);
    }

    private static EnumMap<Theme, EnumMap<Palette, Color>> emptyOverrides() {
        EnumMap<Theme, EnumMap<Palette, Color>> values = new EnumMap<>(Theme.class);
        for (Theme theme : Theme.values()) values.put(theme, new EnumMap<>(Palette.class));
        return values;
    }

    private static Theme parseThemeName(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        for (Theme theme : Theme.values()) {
            if (themeName(theme).equalsIgnoreCase(normalized)
                    || theme.name().equalsIgnoreCase(normalized)
                    || theme.identifier().equalsIgnoreCase(normalized)) return theme;
        }
        return null;
    }

    private static JsonObject themeObject(JsonObject root, Theme theme) {
        JsonObject object = root.getObject(themeName(theme));
        if (object != null) return object;
        object = root.getObject(theme.name());
        if (object != null) return object;
        return root.getObject(theme.identifier());
    }

    private static String themeName(Theme theme) {
        String name = theme.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static Theme safeTheme(Theme theme) {
        return theme == null ? Theme.LIGHT : theme;
    }

    private static Color parseHex(String value) {
        if (value == null) return null;
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) return null;
        for (int index = 0; index < hex.length(); index++) if (!isHex(hex.charAt(index))) return null;

        try {
            return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static boolean isHex(char character) {
        char upper = Character.toUpperCase(character);
        return (upper >= '0' && upper <= '9') || (upper >= 'A' && upper <= 'F');
    }

    private static String hex(Color color) {
        return String.format(Locale.ROOT, "%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static boolean sameRgb(Color first, Color second) {
        return first != null && second != null
                && first.getRed() == second.getRed()
                && first.getGreen() == second.getGreen()
                && first.getBlue() == second.getBlue();
    }

    private static boolean sameNullableRgb(Color first, Color second) {
        if (first == null || second == null) return first == second;
        return sameRgb(first, second);
    }
}
