package net.litelauncher.installer;

import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.UtilityLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class InstallerShortcuts {

    private static final String PRODUCT_NAME = "LiteLauncher";

    static void createLaunchScripts(UtilityLog log) throws IOException {
        if (OSUtils.isWindows()) createWindowsScript();
        else createUnixScript();
        log.info("Launch script created.");
    }

    static void createDesktopShortcut(UtilityLog log) throws Exception {
        if (OSUtils.isWindows()) {
            createWindowsShortcut();
            log.info("Windows shortcut created.");
            return;
        }

        if (OSUtils.isMac()) {
            createMacAppBundle();
            log.info("macOS app shortcut created.");
            return;
        }

        if (OSUtils.isLinux()) {
            createLinuxDesktopFile();
            log.info("Linux desktop shortcut created.");
            return;
        }

        throw new IOException("Unsupported OS: " + System.getProperty("os.name", "unknown"));
    }

    private static void createWindowsScript() throws IOException {
        String script =
                "@echo off\n" +
                        "setlocal\n" +
                        "set \"APP_HOME=%~dp0\"\n" +
                        "set \"JAVA_EXE=\"\n" +
                        "for /f \"delims=\" %%I in ('where javaw.exe 2^>nul') do (\n" +
                        "  set \"JAVA_EXE=%%I\"\n" +
                        "  goto foundJava\n" +
                        ")\n" +
                        "for /f \"delims=\" %%I in ('where java.exe 2^>nul') do (\n" +
                        "  set \"JAVA_EXE=%%I\"\n" +
                        "  goto foundJava\n" +
                        ")\n" +
                        ":foundJava\n" +
                        "if \"%JAVA_EXE%\"==\"\" (\n" +
                        "  echo Java was not found. Install Java and try again.\n" +
                        "  pause\n" +
                        "  exit /b 1\n" +
                        ")\n" +
                        "start \"\" /D \"%APP_HOME%\" \"%JAVA_EXE%\" -jar \"%APP_HOME%Bootstrap.jar\"\n";

        InstallerBackend.writeText(OSUtils.windowsScript(), script, true);
    }

    private static void createUnixScript() throws IOException {
        String appHome = OSUtils.bash(OSUtils.bootstrapDirectory().toAbsolutePath().toString());
        String javaCommand = OSUtils.isMac() ? "/usr/bin/java" : "java";

        String script =
                "#!/bin/bash\n" +
                        "export PATH=\"/usr/bin:/bin:/usr/sbin:/sbin:$PATH\"\n" +
                        "set -e\n" +
                        "APP_HOME=\"" + appHome + "\"\n" +
                        "cd \"$APP_HOME\"\n" +
                        "exec " + javaCommand + " -jar \"$APP_HOME/Bootstrap.jar\"\n";

        InstallerBackend.writeText(OSUtils.unixScript(), script, false);
        OSUtils.makeExecutable(OSUtils.unixScript());
    }

    private static void createWindowsShortcut() throws Exception {
        Path shortcut = OSUtils.desktopDirectory().resolve(PRODUCT_NAME + ".lnk");
        Path bootstrapJar = OSUtils.bootstrapJar();
        Path javaBinary = findCurrentJavaBinary();
        Path target = javaBinary == null ? OSUtils.windowsScript() : javaBinary;
        String arguments = javaBinary == null ? "" : "-jar \"" + bootstrapJar.toAbsolutePath() + "\"";
        Path icon = OSUtils.iconsDirectory().resolve("launcher.ico");
        Path workingDir = OSUtils.bootstrapDirectory();

        String command =
                "$WshShell = New-Object -ComObject WScript.Shell; " +
                        "$Shortcut = $WshShell.CreateShortcut('" + ps(shortcut.toAbsolutePath().toString()) + "'); " +
                        "$Shortcut.TargetPath = '" + ps(target.toAbsolutePath().toString()) + "'; " +
                        "$Shortcut.Arguments = '" + ps(arguments) + "'; " +
                        "$Shortcut.WorkingDirectory = '" + ps(workingDir.toAbsolutePath().toString()) + "'; " +
                        "$Shortcut.IconLocation = '" + ps(icon.toAbsolutePath().toString()) + "'; " +
                        "$Shortcut.WindowStyle = 1; " +
                        "$Shortcut.Save();";

        ProcessBuilder builder = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                command
        );
        int exitCode = builder.start().waitFor();
        if (exitCode != 0) throw new IOException("PowerShell shortcut creation failed with exit code " + exitCode);
    }

    private static Path findCurrentJavaBinary() {
        try {
            Path home = Path.of(System.getProperty("java.home", ""));
            Path javaw = home.resolve("bin").resolve("javaw.exe");
            if (Files.isRegularFile(javaw)) return javaw;

            Path java = home.resolve("bin").resolve("java.exe");
            if (Files.isRegularFile(java)) return java;
        } catch (Exception ignored) {
        }

        Path javaw = findInPath("javaw.exe");
        if (javaw != null) return javaw;
        return findInPath("java.exe");
    }

    private static Path findInPath(String fileName) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) return null;

        for (String entry : path.split(java.io.File.pathSeparator)) {
            try {
                Path file = Path.of(entry).resolve(fileName);
                if (Files.isRegularFile(file)) return file;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static void createMacAppBundle() throws IOException {
        Path appBundle = OSUtils.desktopDirectory().resolve(PRODUCT_NAME + ".app");
        Path contents = appBundle.resolve("Contents");
        Path macOs = contents.resolve("MacOS");
        Path resources = contents.resolve("Resources");

        Files.createDirectories(macOs);
        Files.createDirectories(resources);

        String appHome = OSUtils.bash(OSUtils.bootstrapDirectory().toAbsolutePath().toString());
        String executableScript =
                "#!/bin/bash\n" +
                        "export PATH=\"/usr/bin:/bin:/usr/sbin:/sbin:$PATH\"\n" +
                        "set -e\n" +
                        "APP_HOME=\"" + appHome + "\"\n" +
                        "cd \"$APP_HOME\"\n" +
                        "exec /usr/bin/java -jar \"$APP_HOME/Bootstrap.jar\"\n";

        Path executable = macOs.resolve(PRODUCT_NAME);
        InstallerBackend.writeText(executable, executableScript, false);
        OSUtils.makeExecutable(executable);

        String plist =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                        "<plist version=\"1.0\">\n" +
                        "<dict>\n" +
                        "    <key>CFBundleExecutable</key>\n" +
                        "    <string>" + PRODUCT_NAME + "</string>\n" +
                        "    <key>CFBundleIdentifier</key>\n" +
                        "    <string>net.litelauncher.desktop</string>\n" +
                        "    <key>CFBundleName</key>\n" +
                        "    <string>" + PRODUCT_NAME + "</string>\n" +
                        "    <key>CFBundleDisplayName</key>\n" +
                        "    <string>" + PRODUCT_NAME + "</string>\n" +
                        "    <key>CFBundlePackageType</key>\n" +
                        "    <string>APPL</string>\n" +
                        "    <key>CFBundleVersion</key>\n" +
                        "    <string>1.0</string>\n" +
                        "    <key>CFBundleShortVersionString</key>\n" +
                        "    <string>1.0</string>\n" +
                        "    <key>CFBundleIconFile</key>\n" +
                        "    <string>launcher.icns</string>\n" +
                        "    <key>LSMinimumSystemVersion</key>\n" +
                        "    <string>10.13</string>\n" +
                        "</dict>\n" +
                        "</plist>\n";

        InstallerBackend.writeText(contents.resolve("Info.plist"), plist, false);
        Files.copy(OSUtils.iconsDirectory().resolve("launcher.icns"), resources.resolve("launcher.icns"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void createLinuxDesktopFile() throws IOException {
        Path desktopFile = OSUtils.desktopDirectory().resolve(PRODUCT_NAME + ".desktop");
        Path script = OSUtils.unixScript();
        Path icon = OSUtils.iconsDirectory().resolve("launcher.png");

        String desktop =
                "[Desktop Entry]\n" +
                        "Type=Application\n" +
                        "Name=" + PRODUCT_NAME + "\n" +
                        "Comment=LiteLauncher\n" +
                        "Exec=\"" + script.toAbsolutePath() + "\"\n" +
                        "Path=" + OSUtils.bootstrapDirectory().toAbsolutePath() + "\n" +
                        "Icon=" + icon.toAbsolutePath() + "\n" +
                        "Terminal=false\n" +
                        "Categories=Game;\n" +
                        "StartupNotify=true\n";

        InstallerBackend.writeText(desktopFile, desktop, false);
        OSUtils.makeExecutable(desktopFile);
    }

    private static String ps(String value) {
        return value.replace("'", "''");
    }
}
