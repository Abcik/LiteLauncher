package net.litelauncher.ui;

import java.awt.Desktop;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Public-safe utility logger retained for bootstrap/backend auditing.
 * The official pixel UI logger/window integration is intentionally redacted.
 */
public final class UtilityLog {
    private final Path file;

    public UtilityLog(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public void start(String message) {
        write("INFO", message);
    }

    public void info(String message) {
        write("INFO", message);
    }

    public void error(String message, Throwable throwable) {
        write("ERROR", message + System.lineSeparator() + stackTrace(throwable));
    }

    public void open() {
        try {
            if (Desktop.isDesktopSupported() && Files.isRegularFile(file)) Desktop.getDesktop().open(file.toFile());
        } catch (Exception ignored) {
            // Opening the log is best-effort only.
        }
    }

    private void write(String level, String message) {
        try {
            Files.createDirectories(file.getParent());
            String line = "[" + Instant.now() + "] " + level + " - " + message + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            System.err.println("LiteLauncher log failure: " + exception.getMessage());
        }
    }

    private static String stackTrace(Throwable throwable) {
        if (throwable == null) return "";
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
