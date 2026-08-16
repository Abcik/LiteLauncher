package net.litelauncher.frontend.modules.animation;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public final class AnimationTicker {

    private static final List<Animated> ITEMS = new ArrayList<>();
    private static final Timer TIMER = new Timer(16, _ -> tick());
    private static long lastTime;

    public static void register(Animated item) {
        if (item == null || ITEMS.contains(item)) return;
        ITEMS.add(item);
        sync();
    }

    public static void unregister(Animated item) {
        if (ITEMS.remove(item)) sync();
    }

    public static void sync() {
        for (Animated item : ITEMS) {
            if (!item.needsAnimation()) continue;
            if (!TIMER.isRunning()) {
                lastTime = System.currentTimeMillis();
                TIMER.start();
            }
            return;
        }
        TIMER.stop();
    }

    private static void tick() {
        long now = System.currentTimeMillis();
        long delta = now - lastTime;
        lastTime = now;

        boolean active = false;
        for (Animated item : ITEMS) {
            if (!item.needsAnimation()) continue;
            active = true;
            item.advance(delta);
        }

        if (!active) TIMER.stop();
    }
}
