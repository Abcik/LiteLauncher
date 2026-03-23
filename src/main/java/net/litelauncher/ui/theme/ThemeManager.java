package net.litelauncher.ui.theme;

import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

public final class ThemeManager {
    private static final ThemeManager INSTANCE = new ThemeManager();

    private final List<Runnable> listeners = new ArrayList<>();
    private ThemeMode mode = ThemeMode.DARK;

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public ThemeMode getMode() {
        return mode;
    }

    public ThemePalette palette() {
        return ThemePalette.forMode(mode);
    }

    public void installCurrentTheme() {
        installBaseLookAndFeel();
        UiTypography.installSwingDefaults();
        installUiDefaults(palette());
    }

    public void toggleTheme() {
        setMode(mode.toggle());
    }

    public void setMode(ThemeMode newMode) {
        if (newMode == null || newMode == mode) {
            return;
        }
        mode = newMode;
        installCurrentTheme();
        refreshOpenWindows();
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void installBaseLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize base Swing look and feel", exception);
        }
    }

    private void installUiDefaults(ThemePalette palette) {
        UIManager.put("Panel.background", palette.surfaceBackground);
        UIManager.put("Viewport.background", palette.surfaceBackground);
        UIManager.put("Separator.foreground", palette.separator);
        UIManager.put("Label.foreground", palette.textPrimary);
        UIManager.put("ToolTip.background", palette.tooltipBackground);
        UIManager.put("ToolTip.foreground", palette.tooltipText);
        UIManager.put("OptionPane.background", palette.surfaceBackground);
        UIManager.put("OptionPane.foreground", palette.textPrimary);
        UIManager.put("OptionPane.messageForeground", palette.textPrimary);
        UIManager.put("OptionPane.messageBackground", palette.surfaceBackground);
        UIManager.put("OptionPane.buttonFont", UIManager.getFont("Button.font"));
    }

    private void refreshOpenWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            refreshComponentTree(window);
            window.invalidate();
            window.repaint();
        }
    }

    private void refreshComponentTree(Component component) {
        if (component instanceof JScrollBar scrollBar) {
            ThemeStyler.styleScrollBar(scrollBar, palette());
        } else if (component instanceof JViewport) {
            component.setBackground(palette().surfaceBackground);
        } else if (component instanceof JComboBox) {
            component.repaint();
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                refreshComponentTree(child);
            }
        }
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
