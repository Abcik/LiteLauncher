package net.litelauncher.ui;

import net.litelauncher.backend.platform.OSUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class UtilityLog {

    private final Path file;

    public UtilityLog(Path file) {
        this.file = file;
    }

    public void start(String message) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "LiteLauncher Bootstrap log - " + Instant.now() + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            info(message);
            info("OS: " + System.getProperty("os.name", "unknown") + " / " + System.getProperty("os.arch", "unknown"));
            info("Java: " + System.getProperty("java.version", "unknown"));
            info("Minecraft directory: " + OSUtils.minecraftDirectory());
        } catch (IOException ignored) {
        }
    }

    public void info(String message) {
        write("INFO", message, null);
    }

    public void error(String message, Throwable throwable) {
        write("ERROR", message, throwable);
    }

    public void open() {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.writeString(file, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            OSUtils.openFile(file);
        } catch (Exception exception) {
            error("Unable to open log file: " + file, exception);
        }
    }

    private synchronized void write(String level, String message, Throwable throwable) {
        StringBuilder text = new StringBuilder();
        text.append('[').append(Instant.now()).append("] ").append(level).append("  ")
                .append(message == null || message.isBlank() ? "-" : message)
                .append(System.lineSeparator());
        if (throwable != null) text.append(stackTrace(throwable));

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (IOException ignored) {
        }
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer + System.lineSeparator();
    }
}
