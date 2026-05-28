package net.litelauncher.backend.modules.launch;

@FunctionalInterface
public interface LaunchProgress {
    void update(double progress, String action, String details);
}
