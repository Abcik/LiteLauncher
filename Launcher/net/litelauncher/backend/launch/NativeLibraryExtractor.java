package net.litelauncher.backend.launch;

import net.litelauncher.backend.BackendUtils;
import net.litelauncher.backend.CancellationToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.concurrent.CancellationException;
import java.util.zip.ZipInputStream;

final class NativeLibraryExtractor {


    Path extract(Path natives, List<NativeLibrary> libraries, CancellationToken cancellation) throws GameLaunchException {
        try {
            checkCancelled(cancellation);
            Files.createDirectories(natives);
            for (NativeLibrary library : libraries) {
                checkCancelled(cancellation);
                extractNativeJar(library.jar(), natives, library.excludes(), cancellation);
            }
            return natives;
        } catch (CancellationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GameLaunchException("Unable to extract native libraries.", exception);
        }
    }

    private void extractNativeJar(Path jar, Path target, Set<String> excludes, CancellationToken cancellation) throws Exception {
        checkCancelled(cancellation);
        if (!Files.isRegularFile(jar)) throw new GameLaunchException("Native library is missing: " + jar.getFileName());
        Path root = target.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(jar))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                checkCancelled(cancellation);
                if (entry.isDirectory() || excluded(entry.getName(), excludes)) continue;
                Path out = safeResolve(root, entry.getName());
                if (Files.isRegularFile(out)) continue;
                Files.createDirectories(out.getParent());
                try (var output = Files.newOutputStream(out)) {
                    copy(zip, output, cancellation);
                }
            }
        }
    }

    private void copy(java.io.InputStream input, java.io.OutputStream output, CancellationToken cancellation) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            checkCancelled(cancellation);
            if (read > 0) output.write(buffer, 0, read);
        }
    }

    private void checkCancelled(CancellationToken cancellation) {
        if (cancellation != null) cancellation.throwIfCancelled();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Native extraction cancelled.");
    }

    private boolean excluded(String name, Set<String> excludes) {
        return excludes.stream().anyMatch(name::startsWith);
    }

    private Path safeResolve(Path root, String relative) throws GameLaunchException {
        try {
            return BackendUtils.safeResolve(root, relative);
        } catch (Exception exception) {
            throw new GameLaunchException("Invalid native library entry path.", exception);
        }
    }

    record NativeLibrary(Path jar, Set<String> excludes) {
    }
}
