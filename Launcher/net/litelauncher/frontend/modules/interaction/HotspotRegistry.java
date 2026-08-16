package net.litelauncher.frontend.modules.interaction;

import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public final class HotspotRegistry {

    private final Map<Object, Hotspot> hotspots = new LinkedHashMap<>();
    private final LinkedHashSet<Object> activeKeys = new LinkedHashSet<>();

    public void begin() {
        activeKeys.clear();
    }

    public void set(Object key, int xMin, int yMin, int xMax, int yMax) {
        set(key, xMin, yMin, xMax, yMax, MouseCursor.DEFAULT);
    }

    public void set(Object key, int xMin, int yMin, int xMax, int yMax, MouseCursor cursor) {
        set(key, xMin, yMin, xMax, yMax, cursor, cursor);
    }

    public void set(Object key, int xMin, int yMin, int xMax, int yMax, MouseCursor hoverCursor, MouseCursor dragCursor) {
        activeKeys.add(key);
        Hotspot hotspot = hotspots.computeIfAbsent(key, _ -> new Hotspot(0, 0, -1, -1));
        hotspot.setBounds(xMin, yMin, xMax, yMax);
        hotspot.setCursor(hoverCursor, dragCursor);
    }

    public void hide(Object key) {
        activeKeys.add(key);
        Hotspot hotspot = hotspots.computeIfAbsent(key, _ -> new Hotspot(0, 0, -1, -1));
        hotspot.setBounds(0, 0, -1, -1);
        hotspot.reset();
    }

    public void end() {
        hotspots.entrySet().removeIf(entry -> !activeKeys.contains(entry.getKey()));
    }

    public boolean update(MouseState mouse) {
        boolean dirty = false;
        for (Object key : activeKeys) {
            Hotspot hotspot = hotspots.get(key);
            if (hotspot != null) dirty |= hotspot.update(mouse);
        }
        return dirty;
    }

    public boolean hovered(Object key) {
        Hotspot hotspot = hotspots.get(key);
        return hotspot != null && hotspot.isHovered();
    }

    public boolean consumeClick(Object key) {
        Hotspot hotspot = hotspots.get(key);
        return hotspot != null && hotspot.consumeClick();
    }

    public boolean consumeClick(Object key, MouseState mouse) {
        Hotspot hotspot = hotspots.get(key);
        return hotspot != null && hotspot.consumeClick(mouse);
    }
}
