package net.litelauncher;

import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.scenes.MainScene;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Taskbar;
import java.awt.Window;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;

public final class LiteLauncher {

    private static final String WINDOW_ICON = "assets/common/icon_window.png";
    private static final String TASKBAR_ICON = "assets/common/icon_taskbar.png";
    private static final int[][] WINDOW_SHAPE_ZONES = {
            {10, 0, 429, 1},
            {6, 2, 433, 3},
            {4, 4, 435, 5},
            {2, 6, 437, 9},
            {0, 10, 439, 319},
            {2, 320, 437, 323},
            {4, 324, 435, 325},
            {6, 326, 433, 327},
            {10, 328, 429, 329}
    };

    public static JFrame window;
    private static boolean closing;

    private LiteLauncher() {
    }

    public static void main(String[] args) {
        OSUtils.setApplicationName(LauncherState.TITLE);
        OSUtils.disableOsScaling();
        LauncherLog.start("Launcher started");
        LauncherStore.get();
        SwingUtilities.invokeLater(() -> {
            try {
                openLauncher();
            } catch (RuntimeException exception) {
                LauncherLog.error("Unable to open launcher window.", exception);
                throw exception;
            }
        });
    }

    private static MainScene openLauncher() {
        window = new JFrame(LauncherState.TITLE);
        applyLauncherIcon(window);
        window.setUndecorated(true);
        window.setResizable(false);
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeLauncher();
            }
        });
        window.setBackground(Palette.OUTLINE.color(LauncherStore.get().theme()));

        MainScene mainScene = new MainScene();
        window.setContentPane(mainScene);
        window.pack();
        applyLauncherShape(window);

        window.setLocationRelativeTo(null);
        window.setVisible(true);
        LauncherStore.get().refreshLauncherPresence();
        return mainScene;
    }

    private static void applyLauncherIcon(JFrame window) {
        BufferedImage windowIcon = loadImage(WINDOW_ICON);
        if (windowIcon != null) window.setIconImage(windowIcon);

        applyTaskbarIcon(loadImage(TASKBAR_ICON));
    }

    private static BufferedImage loadImage(String resourcePath) {
        try (InputStream stream = LiteLauncher.class.getClassLoader().getResourceAsStream(resourcePath)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (Exception exception) {
            LauncherLog.error("Unable to load image resource: " + resourcePath, exception);
            return null;
        }
    }

    private static void applyTaskbarIcon(BufferedImage icon) {
        if (icon == null || !Taskbar.isTaskbarSupported()) return;

        Taskbar taskbar = Taskbar.getTaskbar();
        if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return;

        try {
            taskbar.setIconImage(icon);
        } catch (RuntimeException exception) {
            LauncherLog.error("Unable to apply taskbar icon.", exception);
        }
    }

    private static void applyLauncherShape(Window window) {
        try {
            int scale = LauncherStore.get().scale();
            Area shape = new Area();
            for (int[] zone : WINDOW_SHAPE_ZONES) {
                shape.add(new Area(new Rectangle2D.Double(
                        zone[0] * scale,
                        zone[1] * scale,
                        (zone[2] - zone[0] + 1) * scale,
                        (zone[3] - zone[1] + 1) * scale
                )));
            }
            window.setShape(shape);
        } catch (RuntimeException _) {
        }
    }

    private static void closeLauncher() {
        if (closing) return;
        closing = true;

        try {
            LauncherStore.get().shutdownBeforeExit();
        } catch (RuntimeException exception) {
            LauncherLog.error("Unable to shutdown launcher services.", exception);
        }

        if (window != null && window.getContentPane() instanceof MainScene mainScene) mainScene.disposeScene();
        if (window != null) {
            window.setVisible(false);
            window.dispose();
        }
        System.exit(0);
    }

    public static MainScene reopen() {
        if (window != null) {
            if (window.getContentPane() instanceof MainScene mainScene) mainScene.disposeScene();
            window.setVisible(false);
            window.dispose();
        }

        return openLauncher();
    }
}
