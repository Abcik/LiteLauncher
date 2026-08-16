package net.litelauncher.backend.launch;

import net.litelauncher.backend.download.DownloadFile;

import java.nio.file.Path;

record ElyByAuthlibPlan(ElyByAuthlibOverride override, Path runtimePath) {

    static ElyByAuthlibPlan unavailable() {
        return new ElyByAuthlibPlan(null, null);
    }

    static ElyByAuthlibPlan planned(ElyByAuthlibOverride override) {
        return override == null ? unavailable() : new ElyByAuthlibPlan(override, null);
    }

    ElyByAuthlibPlan activate(Path runtimePath) {
        return override == null || runtimePath == null ? unavailable() : new ElyByAuthlibPlan(override, runtimePath);
    }

    String sourceVersion() {
        return override == null ? "" : override.sourceVersion();
    }

    boolean active() {
        return override != null && runtimePath != null;
    }

    DownloadFile download() throws GameLaunchException {
        if (override == null) throw new GameLaunchException("Ely.by Authlib override is unavailable.");
        return override.download();
    }
}
