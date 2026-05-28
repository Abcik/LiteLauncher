package net.litelauncher.backend.modules.launch;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import java.util.Locale;
import java.util.Map;

final class RuleEvaluator {

    private RuleEvaluator() {
    }

    static boolean allowed(JsonObject json, Map<String, Boolean> features) {
        JsonArray rules = json == null ? null : json.getArray("rules");
        return allowed(rules, features, rules == null || rules.isEmpty());
    }

    static boolean allowed(JsonArray rules, Map<String, Boolean> features, boolean defaultValue) {
        if (rules == null || rules.isEmpty()) return defaultValue;

        boolean allowed = false;
        for (Object item : rules) {
            if (!(item instanceof JsonObject rule) || !matches(rule, features)) continue;
            allowed = "allow".equalsIgnoreCase(rule.getString("action", "allow"));
        }
        return allowed;
    }

    private static boolean matches(JsonObject rule, Map<String, Boolean> features) {
        JsonObject os = rule.getObject("os");
        if (os != null && !matchesOs(os)) return false;

        JsonObject ruleFeatures = rule.getObject("features");
        if (ruleFeatures != null) {
            for (Map.Entry<String, Object> entry : ruleFeatures.entrySet()) {
                boolean expected = Boolean.TRUE.equals(entry.getValue());
                boolean actual = features != null && Boolean.TRUE.equals(features.get(entry.getKey()));
                if (expected != actual) return false;
            }
        }
        return true;
    }

    private static boolean matchesOs(JsonObject os) {
        String name = os.getString("name");
        String arch = os.getString("arch");
        String version = os.getString("version");
        if (name != null && !name.equals(osName())) return false;
        if (arch != null && !System.getProperty("os.arch", "").toLowerCase(Locale.ROOT).matches(arch)) return false;
        return version == null || System.getProperty("os.version", "").matches(version);
    }

    static String osName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }
}
