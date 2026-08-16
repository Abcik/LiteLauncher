package net.litelauncher.frontend.modules.animation;

public interface Animated {

    boolean needsAnimation();

    void advance(long deltaMs);
}
