package net.litelauncher.backend.java;

import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.launch.GameLaunchException;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.backend.platform.OperatingSystem;

import java.nio.file.Path;
import java.util.Locale;

final class AdoptiumRuntimeProvider {

    String runtimeId(int major) {
        return "jre-" + major;
    }

    Path archivePath(String runtimeId) {
        return OSUtils.javaDirectory().resolve(runtimeId + archiveExtension());
    }

    String archiveExtension() {
        return OSUtils.os().windows() ? ".zip" : ".tar.gz";
    }

    String downloadUrl(int major) throws GameLaunchException {
        return "https://api.adoptium.net/v3/binary/latest/" + major + "/ga/"
                + adoptiumOs() + "/" + adoptiumArch()
                + "/jre/hotspot/normal/eclipse?project=jdk";
    }

    private String adoptiumOs() throws GameLaunchException {
        String os = OperatingSystem.current().adoptiumName();
        if (!os.isBlank()) return os;
        throw new GameLaunchException("Unable to download Java runtime.", InformationMessages.JAVA_ERROR);
    }

    private String adoptiumArch() throws GameLaunchException {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
        if (arch.contains("amd64") || arch.contains("x86_64") || arch.equals("x64")) return "x64";
        throw new GameLaunchException("Unable to download Java runtime.", InformationMessages.JAVA_ERROR);
    }
}
