package net.litelauncher.i18n;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.Language;
import net.litelauncher.backend.LauncherLog;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class I18n {

    private static final String RESOURCE = "i18n/translations.json";
    private static final Map<String, Map<String, String>> TRANSLATIONS = loadTranslations();
    private static volatile Language currentLanguage = Language.ENGLISH;

    public static void setCurrentLanguage(Language language) {
        currentLanguage = language == null ? Language.ENGLISH : language;
    }

    public static Language currentLanguage() {
        return currentLanguage;
    }

    public static String text(String key) {
        return text(currentLanguage, key);
    }

    public static String text(Language language, String key) {
        if (key == null || key.isBlank()) return "";
        Language safeLanguage = language == null ? Language.ENGLISH : language;

        String value = find(safeLanguage.key(), key);
        if (value != null) return value;

        value = find(Language.ENGLISH.key(), key);
        return value == null ? key : value;
    }

    public static String format(String key, Object... values) {
        return format(currentLanguage, key, values);
    }

    public static String format(Language language, String key, Object... values) {
        String text = text(language, key);
        if (values == null) return text;
        for (int i = 0; i + 1 < values.length; i += 2) {
            String name = String.valueOf(values[i]);
            String value = String.valueOf(values[i + 1]);
            text = text.replace("{" + name + "}", value);
        }
        return text;
    }

    private static String find(String languageKey, String textKey) {
        Map<String, String> language = TRANSLATIONS.get(languageKey);
        return language == null ? null : language.get(textKey);
    }

    private static Map<String, Map<String, String>> loadTranslations() {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        try (InputStream stream = I18n.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) return result;
            JsonObject root = JsonParser.object().from(stream);
            for (Map.Entry<String, Object> languageEntry : root.entrySet()) {
                if (!(languageEntry.getValue() instanceof JsonObject jsonLanguage)) continue;
                Map<String, String> language = new LinkedHashMap<>();
                for (Map.Entry<String, Object> textEntry : jsonLanguage.entrySet()) {
                    Object value = textEntry.getValue();
                    if (value != null) language.put(textEntry.getKey(), String.valueOf(value));
                }
                result.put(languageEntry.getKey(), Map.copyOf(language));
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to load translations.", exception);
        }
        return Map.copyOf(result);
    }
}
