package net.litelauncher.ui;

@FunctionalInterface
public interface TaskProgress {
    void update(double progress, String details);
}
