package net.litelauncher.backend.java;

import net.litelauncher.backend.platform.OperatingSystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class JavaRuntimeManifestService {

    static final String MANIFEST_URL = "https://litelauncher.net/api/v1/launcher/java_manifest.json";

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ATTEMPTS = 3;
    private static final int CONNECT_TIMEOUT_MILLIS = 20_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    private int normalizeMajor(int major) {
        return major == 16 ? 17 : major;
    }

    String runtimeId(int major) {
        return "jre-" + normalizeMajor(major);
    }

    JavaRuntimePackage resolve(int requestedMajor) throws IOException {
        return parse(requestedMajor, loadManifest());
    }

    private JavaRuntimePackage parse(int requestedMajor, String manifest) throws IOException {
        int major = normalizeMajor(requestedMajor);
        String os = operatingSystem();
        String arch = architecture();

        Map<String, Object> root = object(new JsonParser(manifest).parse(), "Java manifest root");
        if (integer(root.get("schemaVersion"), "schemaVersion") != SCHEMA_VERSION)
            throw new IOException("Unsupported Java manifest schema version.");

        Map<String, Object> runtimes = object(root.get("runtimes"), "runtimes");
        Map<String, Object> version = optionalObject(runtimes.get(Integer.toString(major)));
        Map<String, Object> systems = version == null ? null : optionalObject(version.get(os));
        Map<String, Object> runtime = systems == null ? null : optionalObject(systems.get(arch));
        if (runtime == null) throw new IOException("Java " + major + " is unavailable for " + os + "/" + arch + ".");

        String name = text(runtime.get("name"), "name");
        String url = text(runtime.get("url"), "url");
        String sha1 = text(runtime.get("sha1"), "sha1");
        long size = integer(runtime.get("size"), "size");

        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0)
            throw new IOException("Invalid Java package name in manifest.");
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".zip") && !lowerName.endsWith(".tar.gz"))
            throw new IOException("Unsupported Java archive format in manifest.");
        if (sha1.length() != 40 || !hex(sha1))
            throw new IOException("Invalid Java package SHA-1 in manifest.");
        if (size <= 0L) throw new IOException("Invalid Java package size in manifest.");

        return new JavaRuntimePackage(major, name, url, sha1, size);
    }

    private String loadManifest() throws IOException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return requestManifest();
            } catch (IOException exception) {
                lastError = exception;
            }
        }
        throw new IOException("Unable to load Java runtime manifest.", lastError);
    }

    private String requestManifest() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(MANIFEST_URL).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "LiteLauncher");

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300)
                throw new IOException("Java manifest request failed with HTTP " + status + ".");
            try (InputStream input = connection.getInputStream()) {
                return readText(input);
            }
        } finally {
            connection.disconnect();
        }
    }

    private String operatingSystem() throws IOException {
        String value = OperatingSystem.current().javaManifestName();
        if (!isBlank(value)) return value;
        throw new IOException("Unsupported operating system for Java runtime.");
    }

    private String architecture() throws IOException {
        String value = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (value.contains("aarch64") || value.contains("arm64")) return "aarch64";
        if (value.contains("amd64") || value.contains("x86_64") || value.equals("x64")) return "x64";
        throw new IOException("Unsupported architecture for Java runtime: " + value + ".");
    }

    private static String readText(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) if (read > 0) output.write(buffer, 0, read);
        return output.toString(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> object(Object value, String name) throws IOException {
        Map<String, Object> result = optionalObject(value);
        if (result == null) throw new IOException("Invalid Java manifest field: " + name + ".");
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionalObject(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    private static String text(Object value, String name) throws IOException {
        if (value instanceof String text) {
            if (!isBlank(text)) return text;
        }
        throw new IOException("Invalid Java manifest field: " + name + ".");
    }

    private static long integer(Object value, String name) throws IOException {
        if (value instanceof Long) return (Long) value;
        throw new IOException("Invalid Java manifest field: " + name + ".");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean hex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) return false;
        }
        return true;
    }

    private static final class JsonParser {

        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parse() throws IOException {
            skipWhitespace();
            Object value = value();
            skipWhitespace();
            if (index != text.length()) throw error("Unexpected trailing data");
            return value;
        }

        private Object value() throws IOException {
            skipWhitespace();
            if (index >= text.length()) throw error("Unexpected end of JSON");
            char c = text.charAt(index);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (c == '-' || (c >= '0' && c <= '9')) return number();
            if (literal("true")) return Boolean.TRUE;
            if (literal("false")) return Boolean.FALSE;
            if (literal("null")) return null;
            throw error("Unexpected JSON value");
        }

        private Map<String, Object> object() throws IOException {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) return result;
            while (true) {
                skipWhitespace();
                if (index >= text.length() || text.charAt(index) != '"') throw error("Expected object key");
                String key = string();
                skipWhitespace();
                expect(':');
                result.put(key, value());
                skipWhitespace();
                if (consume('}')) return result;
                expect(',');
            }
        }

        private List<Object> array() throws IOException {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) return result;
            while (true) {
                result.add(value());
                skipWhitespace();
                if (consume(']')) return result;
                expect(',');
            }
        }

        private String string() throws IOException {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') return result.toString();
                if (c != '\\') {
                    if (c < 0x20) throw error("Invalid string character");
                    result.append(c);
                    continue;
                }
                if (index >= text.length()) throw error("Invalid string escape");
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u': result.append(unicode()); break;
                    default: throw error("Invalid string escape");
                }
            }
            throw error("Unterminated string");
        }

        private char unicode() throws IOException {
            if (index + 4 > text.length()) throw error("Invalid unicode escape");
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(text.charAt(index++), 16);
                if (digit < 0) throw error("Invalid unicode escape");
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        private Number number() throws IOException {
            int start = index;
            if (consume('-') && index >= text.length()) throw error("Invalid number");
            // A leading zero is complete unless a fraction or exponent follows.
            if (!consume('0')) digits();

            boolean decimal = false;
            if (consume('.')) {
                decimal = true;
                digits();
            }
            if (consume('e') || consume('E')) {
                decimal = true;
                if (!consume('+')) consume('-');
                digits();
            }
            String value = text.substring(start, index);
            try {
                if (decimal) return Double.valueOf(value);
                return Long.valueOf(value);
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void digits() throws IOException {
            int start = index;
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c < '0' || c > '9') break;
                index++;
            }
            if (start == index) throw error("Expected digit");
        }

        private boolean literal(String value) {
            if (!text.regionMatches(index, value, 0, value.length())) return false;
            index += value.length();
            return true;
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (!consume(expected)) throw error("Expected '" + expected + "'");
        }

        private boolean consume(char expected) {
            if (index >= text.length() || text.charAt(index) != expected) return false;
            index++;
            return true;
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') return;
                index++;
            }
        }

        private IOException error(String message) {
            return new IOException(message + " at JSON offset " + index + ".");
        }
    }
}
