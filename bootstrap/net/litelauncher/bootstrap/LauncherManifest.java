package net.litelauncher.bootstrap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LauncherManifest {

    final String version;
    final String file;
    final String sha1;
    final long size;
    final int javaMajor;

    private LauncherManifest(String version, String file, String sha1, long size, int javaMajor) {
        this.version = version;
        this.file = file;
        this.sha1 = sha1;
        this.size = size;
        this.javaMajor = javaMajor;
    }

    static LauncherManifest parse(String json) throws BootstrapException {
        String version = string(json, "version");
        String file = string(json, "file");
        String sha1 = normalizeSha1(string(json, "sha1"));
        long size = number(json, "size", 0L);
        int javaMajor = (int) number(json, "javaMajor", 0L);

        if (version.isBlank()) throw new BootstrapException("Manifest error.");
        if (file.isBlank()) throw new BootstrapException("Manifest error.");
        if (!isSha1(sha1)) throw new BootstrapException("Manifest error.");
        if (size <= 0L) throw new BootstrapException("Manifest error.");
        if (javaMajor <= 0) javaMajor = currentJavaMajor();

        return new LauncherManifest(version, file, sha1, size, javaMajor);
    }

    String checksumAlgorithm() {
        return "SHA-1";
    }

    String checksum() {
        return sha1;
    }

    private static boolean isSha1(String value) {
        return value.length() == 40 && value.matches("[0-9a-fA-F]{40}");
    }

    private static String normalizeSha1(String value) {
        return value == null ? "" : value.trim().replace(" ", "").replace(":", "");
    }

    private static String string(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json == null ? "" : json);
        return matcher.find() ? unescape(matcher.group(1)).trim() : "";
    }

    private static long number(String json, String key, long fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json == null ? "" : json);
        if (!matcher.find()) return fallback;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                if (c == 'n') result.append('\n');
                else if (c == 'r') result.append('\r');
                else if (c == 't') result.append('\t');
                else result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static int currentJavaMajor() {
        String version = System.getProperty("java.version", "17");
        try {
            if (version.startsWith("1.")) return Integer.parseInt(version.substring(2, 3));
            int dot = version.indexOf('.');
            return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
        } catch (Exception ignored) {
            return 17;
        }
    }
}
