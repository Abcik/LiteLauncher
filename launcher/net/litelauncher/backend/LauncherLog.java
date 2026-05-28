package net.litelauncher.backend;

import net.litelauncher.backend.platform.OSUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class LauncherLog {

    private static final Object LOCK = new Object();

    private LauncherLog() {
    }

    public static Path file() {
        return OSUtils.logsDirectory().resolve("litelauncher_latest.log");
    }

    public static void start(String title) {
        synchronized (LOCK) {
            try {
                Files.createDirectories(file().getParent());
                Files.deleteIfExists(file());
                Files.writeString(file(), "LiteLauncher log - " + Instant.now() + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                info(title);
                info("OS: " + System.getProperty("os.name", "unknown") + " / " + System.getProperty("os.arch", "unknown"));
                info("Java: " + System.getProperty("java.version", "unknown"));
                info("Minecraft directory: " + OSUtils.minecraftDirectory());
            } catch (Exception ignored) {
            }
        }
    }

    public static void info(String message) {
        write("INFO", message, null);
    }

    public static void error(String message, Throwable throwable) {
        write("ERROR", message, throwable);
    }

    private static void write(String level, String message, Throwable throwable) {
        synchronized (LOCK) {
            try {
                Files.createDirectories(file().getParent());
                StringBuilder text = new StringBuilder();
                text.append('[').append(Instant.now()).append("] ").append(level).append("  ")
                        .append(message == null || message.isBlank() ? "-" : message)
                        .append(System.lineSeparator());
                if (throwable != null) text.append(stackTrace(throwable));
                Files.writeString(file(), text.toString(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            } catch (Exception ignored) {
            }
        }
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer + System.lineSeparator();
    }
}
