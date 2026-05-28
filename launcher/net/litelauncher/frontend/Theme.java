package net.litelauncher.frontend;

/**
 * Minimal public enum kept because backend state/auth code references the selected UI theme.
 * The official theme palette, pixel renderer and exact UI implementation are intentionally redacted.
 */
public enum Theme {

    LIGHT("light"), DARK("dark");

    private final String identifier;

    Theme(String identifier) {
        this.identifier = identifier;
    }

    public String identifier() {
        return identifier;
    }
}
