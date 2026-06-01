package net.litelauncher.frontend;

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
