package net.litelauncher.frontend.modules.animation;

import javax.swing.Timer;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

public final class AnimationTicker {

    /*
     * ~30 FPS. Every tick repaints the whole pixel canvas, so this is the main idle CPU cost.
     * Scrolling text moves 1px per 40ms and the skin turns slowly: 60 FPS gave nothing visible.
     */
    private static final int FRAME_MS = 33;

    private static final List<Animated> ITEMS = new ArrayList<>();
    private static final Timer TIMER = new Timer(FRAME_MS, _ -> tick());
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
        if (hidden()) return;

        boolean active = false;
        for (Animated item : ITEMS) {
            if (!item.needsAnimation()) continue;
            active = true;
            item.advance(delta);
        }

        if (!active) TIMER.stop();
    }

    /** Nothing to animate while the launcher is minimized or hidden (e.g. while the game runs). */
    private static boolean hidden() {
        for (Frame frame : Frame.getFrames()) {
            if (frame.isShowing() && (frame.getExtendedState() & Frame.ICONIFIED) == 0) return false;
        }
        return true;
    }

    private AnimationTicker() {
    }
}
