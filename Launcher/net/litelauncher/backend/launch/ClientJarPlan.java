package net.litelauncher.backend.launch;

import java.nio.file.Path;

record ClientJarPlan(String id, Path originalPath, Path runtimePath) {

    ClientJarPlan {
        if (runtimePath == null) runtimePath = originalPath;
    }

    static ClientJarPlan original(String id, Path path) {
        return new ClientJarPlan(id, path, path);
    }

    ClientJarPlan withRuntime(Path path) {
        return new ClientJarPlan(id, originalPath, path == null ? originalPath : path);
    }

    boolean patched() {
        return originalPath != null && runtimePath != null && !originalPath.equals(runtimePath);
    }

    String runtimeFileName() {
        return runtimePath == null ? "" : runtimePath.getFileName().toString();
    }
}
