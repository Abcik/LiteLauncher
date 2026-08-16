package net.litelauncher.backend.launch;

import net.litelauncher.backend.platform.LauncherPaths;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.auth.LaunchAccount;
import net.litelauncher.backend.platform.OSUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GameArgumentBuilder {

    private static final String BOOTSTRAP_LAUNCHER = "cpw.mods.bootstraplauncher.BootstrapLauncher";
    private static final String IGNORE_LIST_PROPERTY = "-DignoreList=";

    List<String> build(Path java, ResolvedVersion version, JsonObject assetIndex, LaunchAccount account, LaunchSettings settings,
                       Path gameDirectory, Path natives, LaunchClasspath classpath, boolean elyByActive) {
        Map<String, String> values = values(version, assetIndex, account, settings, gameDirectory, natives, classpath);
        Map<String, Boolean> features = Map.of(
                "has_custom_resolution", !settings.fullscreen() && settings.screenWidth() > 0 && settings.screenHeight() > 0,
                "is_demo_user", false,
                "has_quick_plays_support", false,
                "is_quick_play_singleplayer", false,
                "is_quick_play_multiplayer", false,
                "is_quick_play_realms", false
        );

        List<String> command = new ArrayList<>();
        command.add(java.toString());
        if (OSUtils.os().macos()) command.add("-Xdock:name=Minecraft " + minecraftVersion(version));
        command.add("-Xmx" + settings.memoryAmount() + "M");
        command.add("-Xms512M");
        for (String argument : split(settings.jvmArguments())) {
            if (!isMemoryArgument(argument)) command.add(replace(argument, values));
        }

        List<String> jvm = parseJsonArguments(version.jvmArguments(), values, features);
        if (jvm.isEmpty()) addDefaultJvm(command, values);
        else command.addAll(jvm);
        ensureJvm(command, values);
        ensureBootstrapLauncherCompatibility(command, version, classpath);
        addOfflineMultiplayerFix(command, version, account, elyByActive);

        command.add(version.mainClass());
        List<String> game = new ArrayList<>();
        if (!version.minecraftArguments().isBlank()) game.addAll(replaceAll(split(version.minecraftArguments()), values));
        game.addAll(parseJsonArguments(version.gameArguments(), values, features));
        command.addAll(game);
        ensureGame(command, settings);
        return command;
    }

    private String minecraftVersion(ResolvedVersion version) {
        return BackendUtils.firstText(version.clientDownloadId(), version.jarId(), version.selectedId());
    }

    private Map<String, String> values(ResolvedVersion version, JsonObject assetIndex, LaunchAccount account, LaunchSettings settings,
                                       Path gameDirectory, Path natives, LaunchClasspath classpath) {
        Map<String, String> values = new HashMap<>();
        values.put("auth_player_name", account.username());
        values.put("version_name", version.id());
        values.put("game_directory", gameDirectory.toString());
        values.put("assets_root", LauncherPaths.assetsDirectory().toString());
        values.put("assets_index_name", version.assets());
        values.put("game_assets", gameAssets(assetIndex, gameDirectory).toString());
        values.put("auth_uuid", account.uuid());
        values.put("auth_access_token", account.accessToken());
        values.put("auth_session", account.legacySession());
        values.put("clientid", "");
        values.put("auth_xuid", account.xuid());
        values.put("user_type", account.userType());
        values.put("version_type", version.type());
        values.put("natives_directory", natives.toString());
        values.put("launcher_name", LauncherState.TITLE);
        values.put("launcher_version", LauncherState.LAUNCHER_VERSION);
        values.put("classpath", classpath.text());
        values.put("classpath_separator", java.io.File.pathSeparator);
        values.put("library_directory", LauncherPaths.librariesDirectory().toString());
        values.put("user_properties", "{}");
        values.put("resolution_width", Integer.toString(settings.screenWidth()));
        values.put("resolution_height", Integer.toString(settings.screenHeight()));
        return values;
    }

    private Path gameAssets(JsonObject index, Path gameDirectory) {
        if (index != null && index.getBoolean("map_to_resources", false)) return gameDirectory.resolve("resources");
        if (index != null && index.getBoolean("virtual", false)) return LauncherPaths.assetsDirectory().resolve("virtual").resolve("legacy");
        return LauncherPaths.assetsDirectory();
    }

    private List<String> parseJsonArguments(JsonArray array, Map<String, String> values, Map<String, Boolean> features) {
        List<String> result = new ArrayList<>();
        if (array == null) return result;
        for (Object item : array) addArgumentItem(result, item, values, features);
        return result;
    }

    private void addArgumentItem(List<String> result, Object item, Map<String, String> values, Map<String, Boolean> features) {
        if (item instanceof String text) {
            result.add(replace(text, values));
            return;
        }
        if (!(item instanceof JsonObject object)) return;
        if (!RuleEvaluator.allowed(object.getArray("rules"), features, object.getArray("rules") == null)) return;

        Object value = object.get("value");
        if (value instanceof String text) result.add(replace(text, values));
        else if (value instanceof JsonArray array) {
            for (Object child : array) if (child instanceof String text) result.add(replace(text, values));
        }
    }

    private void addDefaultJvm(List<String> command, Map<String, String> values) {
        command.add("-Djava.library.path=" + values.get("natives_directory"));
        command.add("-Dminecraft.launcher.brand=" + values.get("launcher_name"));
        command.add("-Dminecraft.launcher.version=" + values.get("launcher_version"));
        command.add("-cp");
        command.add(values.get("classpath"));
    }

    private void ensureJvm(List<String> command, Map<String, String> values) {
        if (command.stream().noneMatch(argument -> argument.startsWith("-Djava.library.path"))) {
            command.add("-Djava.library.path=" + values.get("natives_directory"));
        }
        if (!containsClassPath(command)) {
            command.add("-cp");
            command.add(values.get("classpath"));
        }
    }

    private void ensureBootstrapLauncherCompatibility(List<String> command, ResolvedVersion version, LaunchClasspath classpath) {
        if (!usesBootstrapLauncher(version)) return;

        List<String> added = new ArrayList<>();
        for (String fileName : classpath.clientFileNames()) {
            if (appendIgnoreList(command, fileName)) added.add(fileName);
        }
        if (!added.isEmpty()) LauncherLog.info("BootstrapLauncher ignoreList extended: " + added);
    }

    private boolean usesBootstrapLauncher(ResolvedVersion version) {
        if (version == null) return false;
        if (BOOTSTRAP_LAUNCHER.equals(version.mainClass())) return true;

        for (JsonObject library : version.libraries()) {
            String name = library.getString("name", "");
            if (name.startsWith("cpw.mods:bootstraplauncher:")) return true;
        }
        return false;
    }

    private boolean appendIgnoreList(List<String> command, String value) {
        if (value == null || value.isBlank()) return false;

        for (int i = 0; i < command.size(); i++) {
            String argument = command.get(i);
            if (argument == null || !argument.startsWith(IGNORE_LIST_PROPERTY)) continue;

            String current = argument.substring(IGNORE_LIST_PROPERTY.length());
            if (ignoreListCovers(current, value)) return false;
            command.set(i, IGNORE_LIST_PROPERTY + (current.isBlank() ? value : current + "," + value));
            return true;
        }

        command.add(IGNORE_LIST_PROPERTY + value);
        return true;
    }

    private boolean ignoreListCovers(String list, String fileName) {
        if (list == null || fileName == null || fileName.isBlank()) return false;
        for (String item : list.split(",")) {
            String prefix = item.trim();
            if (!prefix.isBlank() && fileName.startsWith(prefix)) return true;
        }
        return false;
    }

    private void addOfflineMultiplayerFix(List<String> command, ResolvedVersion version, LaunchAccount account, boolean elyByActive) {
        if (elyByActive || account == null || !"legacy".equals(account.userType())) return;
        if (!isOfflineMultiplayerFixVersion(version)) return;

        addOfflineApiHost(command, "minecraft.api.auth.host");
        addOfflineApiHost(command, "minecraft.api.account.host");
        addOfflineApiHost(command, "minecraft.api.session.host");
        addOfflineApiHost(command, "minecraft.api.services.host");
    }

    private boolean isOfflineMultiplayerFixVersion(ResolvedVersion version) {
        if (version == null) return false;
        return is1164or1165(version.selectedId())
                || is1164or1165(version.id())
                || is1164or1165(version.jarId())
                || is1164or1165(version.clientDownloadId());
    }

    private boolean is1164or1165(String value) {
        if (value == null) return false;
        return value.contains("1.16.4") || value.contains("1.16.5");
    }

    private void addOfflineApiHost(List<String> command, String key) {
        String prefix = "-D" + key + "=";
        for (String argument : command) {
            if (argument != null && argument.startsWith(prefix)) return;
        }
        command.add(prefix + "https://nope.invalid");
    }

    private boolean containsClassPath(List<String> arguments) {
        for (String argument : arguments) {
            if ("-cp".equals(argument) || "-classpath".equals(argument) || argument.startsWith("-cp=")) return true;
        }
        return false;
    }

    private void ensureGame(List<String> command, LaunchSettings settings) {
        if (settings.fullscreen() && !command.contains("--fullscreen")) command.add("--fullscreen");
        if (!settings.fullscreen() && !command.contains("--width")) {
            command.add("--width");
            command.add(Integer.toString(settings.screenWidth()));
            command.add("--height");
            command.add(Integer.toString(settings.screenHeight()));
        }
    }

    private List<String> replaceAll(List<String> arguments, Map<String, String> values) {
        List<String> result = new ArrayList<>();
        for (String argument : arguments) result.add(replace(argument, values));
        return result;
    }

    private String replace(String value, Map<String, String> values) {
        String result = value == null ? "" : value;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private boolean isMemoryArgument(String argument) {
        return argument != null && (argument.startsWith("-Xmx") || argument.startsWith("-Xms"));
    }

    private List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isBlank()) return parts;

        StringBuilder current = new StringBuilder();
        boolean quote = false;
        char quoteChar = 0;
        boolean escape = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (quote) {
                if (c == quoteChar) quote = false;
                else current.append(c);
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = true;
                quoteChar = c;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) parts.add(current.toString());
        return parts;
    }
}
