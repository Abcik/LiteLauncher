package net.litelauncher.backend.launch;

@FunctionalInterface
public interface LaunchProgress {
    void update(double progress, String action, String details);
}
