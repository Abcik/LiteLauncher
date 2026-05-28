package net.litelauncher;

public enum Language {

    ENGLISH("English"),
    SPANISH("Español"),
    RUSSIAN("Русский"),
    PORTUGUESE("Português"),
    GERMAN("Deutsch"),
    FRENCH("Français"),
    TURKISH("Türkçe"),
    POLISH("Polski"),
    ITALIAN("Italiano"),
    UKRAINIAN("Українська");

    private final String title;

    Language(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}