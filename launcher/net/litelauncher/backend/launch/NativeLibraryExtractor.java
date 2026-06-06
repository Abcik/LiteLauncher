package net.litelauncher.backend.launch;

import net.litelauncher.backend.platform.OSUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class NativeLibraryExtractor {

    // Native extraction is isolated because modded/custom versions may need more rules later,
    // while GameLaunchService should stay focused on the readable launch sequence.

    Path extract(String versionId, List<NativeLibrary> libraries) throws GameLaunchException {
        Path natives = OSUtils.nativesDirectory(versionId);
        try {
            Files.createDirectories(natives);
            for (NativeLibrary library : libraries) extractNativeJar(library.jar(), natives, library.excludes());
            return natives;
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to extract native libraries.", exception);
        }
    }

    private void extractNativeJar(Path jar, Path target, Set<String> excludes) throws Exception {
        if (!Files.isRegularFile(jar)) throw new GameLaunchException("Native library is missing: " + jar.getFileName());
        Path root = target.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(jar))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.isDirectory() || excluded(entry.getName(), excludes)) continue;
                Path out = safeResolve(root, entry.getName());
                if (Files.isRegularFile(out)) continue;
                Files.createDirectories(out.getParent());
                Files.copy(zip, out);
            }
        }
    }

    private boolean excluded(String name, Set<String> excludes) {
        for (String exclude : excludes) if (name.startsWith(exclude)) return true;
        return false;
    }

    private Path safeResolve(Path root, String relative) throws GameLaunchException {
        Path out = root.resolve(relative == null ? "" : relative).toAbsolutePath().normalize();
        if (!out.startsWith(root) || out.equals(root)) throw new GameLaunchException("Invalid native library entry path.");
        return out;
    }

    record NativeLibrary(Path jar, Set<String> excludes) {
    }
}
