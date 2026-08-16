package net.litelauncher.backend.auth;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AuthUtils {

    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return result;

        for (String part : rawQuery.split("&")) {
            int separator = part.indexOf('=');
            String key = separator < 0 ? part : part.substring(0, separator);
            String value = separator < 0 ? "" : part.substring(separator + 1);
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    static String formEncode(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!builder.isEmpty()) builder.append('&');
            builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    static String urlDecode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    static String text(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    static boolean hasText(String value) {
        return text(value) != null;
    }

    static String firstText(String... values) {
        for (String value : values) {
            String text = text(value);
            if (text != null) return text;
        }
        return null;
    }

    static Instant instant(String value) {
        try {
            return hasText(value) ? Instant.parse(value) : null;
        } catch (Exception _) {
            return null;
        }
    }

    static MinecraftCape findCape(List<MinecraftCape> capes, String id) {
        if (capes == null || id == null) return null;
        for (MinecraftCape cape : capes) if (cape != null && id.equals(cape.id())) return cape;
        return null;
    }

    static String errorDetails(String body, String fallback) {
        try {
            JsonObject json = JsonParser.object().from(body);
            return firstText(json.getString("error_description"), json.getString("errorMessage"),
                    json.getString("message"), json.getString("error"), fallback);
        } catch (Exception _) {
            String text = text(body);
            return text == null ? fallback : text.substring(0, Math.min(500, text.length()));
        }
    }


}
