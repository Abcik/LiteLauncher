package net.litelauncher;

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
