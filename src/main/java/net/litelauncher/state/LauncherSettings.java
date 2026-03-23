package net.litelauncher.state;

public final class LauncherSettings {
    private static final int MIN_GAME_WIDTH = 640;
    private static final int MAX_GAME_WIDTH = 7680;
    private static final int MIN_GAME_HEIGHT = 360;
    private static final int MAX_GAME_HEIGHT = 4320;

    private int memoryMb = 4096;
    private boolean snapshotsEnabled;
    private boolean modificationsEnabled;
    private String language = "English";
    private int gameWidth = 1280;
    private int gameHeight = 720;
    private boolean fullscreen;
    private String launchArguments = "";
    private boolean closeLauncherAfterLaunch;

    public int getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(int memoryMb) {
        this.memoryMb = Math.max(1, memoryMb);
    }

    public boolean isSnapshotsEnabled() {
        return snapshotsEnabled;
    }

    public void setSnapshotsEnabled(boolean snapshotsEnabled) {
        this.snapshotsEnabled = snapshotsEnabled;
    }

    public boolean isModificationsEnabled() {
        return modificationsEnabled;
    }

    public void setModificationsEnabled(boolean modificationsEnabled) {
        this.modificationsEnabled = modificationsEnabled;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language == null ? "English" : language;
    }

    public int getGameWidth() {
        return gameWidth;
    }

    public void setGameWidth(int gameWidth) {
        this.gameWidth = clamp(gameWidth, MIN_GAME_WIDTH, MAX_GAME_WIDTH);
    }

    public int getGameHeight() {
        return gameHeight;
    }

    public void setGameHeight(int gameHeight) {
        this.gameHeight = clamp(gameHeight, MIN_GAME_HEIGHT, MAX_GAME_HEIGHT);
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public String getLaunchArguments() {
        return launchArguments;
    }

    public void setLaunchArguments(String launchArguments) {
        this.launchArguments = launchArguments == null ? "" : launchArguments.trim();
    }

    public boolean isCloseLauncherAfterLaunch() {
        return closeLauncherAfterLaunch;
    }

    public void setCloseLauncherAfterLaunch(boolean closeLauncherAfterLaunch) {
        this.closeLauncherAfterLaunch = closeLauncherAfterLaunch;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
