package net.litelauncher.ui.util;

public final class BuildTextFormatter {
    private BuildTextFormatter() {
    }

    public static Parts parts(String build) {
        return split(build);
    }

    public static String normalize(String build) {
        Parts parts = split(build);
        return parts.version() + (parts.type().isEmpty() ? "" : " " + parts.type());
    }

    private static Parts split(String build) {
        String cleaned = build == null ? "" : build.replace('|', ' ').replaceAll("\\s+", " ").trim();
        int firstSpace = cleaned.indexOf(' ');
        if (firstSpace <= 0) {
            return new Parts(cleaned, "");
        }
        return new Parts(cleaned.substring(0, firstSpace).trim(), cleaned.substring(firstSpace + 1).trim());
    }

    public record Parts(String version, String type) {
    }
}
