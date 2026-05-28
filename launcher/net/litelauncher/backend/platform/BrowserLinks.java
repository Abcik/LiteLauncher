package net.litelauncher.backend.platform;

import net.litelauncher.backend.LauncherLog;

import java.net.URI;

public final class BrowserLinks {

    public static final String BUY_MINECRAFT = "https://www.minecraft.net/store/minecraft-java-bedrock-edition-pc";
    public static final String WEBSITE = "https://litelauncher.net";
    public static final String DISCORD = "https://discord.com/invite/DJ2YYnS8YV";
    public static final String GITHUB = "https://github.com/abcik/LiteLauncher";

    private BrowserLinks() {
    }

    public static void open(String url) {
        try {
            OSUtils.openUri(URI.create(url));
        } catch (Exception exception) {
            LauncherLog.error("Unable to open link: " + url, exception);
        }
    }
}
