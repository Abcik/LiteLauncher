package net.litelauncher;

public enum Language {

    ENGLISH("english", "English"),
    SPANISH("spanish", "Español"),
    RUSSIAN("russian", "Русский"),
    PORTUGUESE("portuguese", "Português"),
    GERMAN("german", "Deutsch"),
    FRENCH("french", "Français"),
    TURKISH("turkish", "Türkçe"),
    POLISH("polish", "Polski"),
    ITALIAN("italian", "Italiano"),
    UKRAINIAN("ukrainian", "Українська");

    private final String key;
    private final String title;

    Language(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public String key() {
        return key;
    }

    public String title() {
        return title;
    }
}
