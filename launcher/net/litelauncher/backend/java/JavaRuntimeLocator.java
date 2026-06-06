package net.litelauncher.backend.java;

import net.litelauncher.backend.platform.OSUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

final class JavaRuntimeLocator {

    Path findJava(Path root) {
        Path direct = javaExecutable(root);
        if (Files.isRegularFile(direct)) return direct;
        try (var stream = Files.find(root, 8, (path, attrs) -> {
            if (!attrs.isRegularFile() || path.getParent() == null || path.getFileName() == null) return false;
            String name = path.getFileName().toString();
            return name.equals(executableName()) && "bin".equals(path.getParent().getFileName().toString());
        })) {
            return stream.findFirst().orElse(direct);
        } catch (Exception ignored) {
            return direct;
        }
    }

    void makeExecutable(Path file) {
        try {
            if (file == null || OSUtils.os().windows() || !Files.exists(file)) return;
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
            );
            Files.setPosixFilePermissions(file, permissions);
        } catch (Exception ignored) {
        }
    }

    private Path javaExecutable(Path root) {
        return root.resolve("bin").resolve(executableName());
    }

    private String executableName() {
        return OSUtils.os().windows() ? "java.exe" : "java";
    }
}
