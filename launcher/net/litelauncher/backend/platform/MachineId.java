package net.litelauncher.backend.platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MachineId {

    private MachineId() {
    }

    public static String read() {
        OperatingSystem os = OperatingSystem.current();
        if (os.windows()) {
            return firstText(
                    windowsMachineGuid("HKLM\\SOFTWARE\\Microsoft\\Cryptography"),
                    windowsMachineGuid("HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Cryptography")
            );
        }
        if (os.macos()) return macHardwareUuid();
        return firstText(readText(Path.of("/etc/machine-id")), readText(Path.of("/var/lib/dbus/machine-id")));
    }

    private static String windowsMachineGuid(String key) {
        String output = runCommand("reg", "query", key, "/v", "MachineGuid");
        if (output == null) return null;

        for (String line : output.split("\\R")) {
            if (!line.contains("MachineGuid")) continue;
            String[] parts = line.trim().split("\\s+", 3);
            if (parts.length == 3) return parts[2].trim();
        }
        return null;
    }

    private static String macHardwareUuid() {
        String output = firstText(
                runCommand("/usr/sbin/ioreg", "-rd1", "-c", "IOPlatformExpertDevice"),
                runCommand("ioreg", "-rd1", "-c", "IOPlatformExpertDevice")
        );
        if (output == null) return null;

        for (String line : output.split("\\R")) {
            int marker = line.indexOf("IOPlatformUUID");
            if (marker < 0) continue;

            int first = line.indexOf('"', marker);
            int second = first < 0 ? -1 : line.indexOf('"', first + 1);
            int third = second < 0 ? -1 : line.indexOf('"', second + 1);
            if (second >= 0 && third > second) return line.substring(second + 1, third);
        }
        return null;
    }

    private static String readText(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            return text(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return null;
        }
    }

    private static String runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            int code = process.waitFor();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return code == 0 ? text(output) : null;
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static String firstText(String first, String second) {
        return text(first) != null ? text(first) : text(second);
    }

    private static String text(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
