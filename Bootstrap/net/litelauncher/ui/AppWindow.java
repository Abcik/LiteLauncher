package net.litelauncher.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Window;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

public final class AppWindow {

    public static final int WIDTH = 220;
    public static final int HEIGHT = 100;
    public static final int SCALE = 2;
    public static final int HEADER_DRAG_HEIGHT = 36;

    public static JFrame open(String title, PixelCanvas canvas) {
        JFrame window = new JFrame(title);
        window.setUndecorated(true);
        window.setResizable(false);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setBackground(new Color(0, 0, 0, 0));
        window.setContentPane(canvas);
        window.pack();
        applyShape(window, WIDTH, HEIGHT, SCALE);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        return window;
    }

    public static void close(Window window) {
        if (window == null) return;
        SwingUtilities.invokeLater(() -> {
            window.setVisible(false);
            window.dispose();
        });
    }

    public static void applyShape(Window window, int logicalWidth, int logicalHeight, int scale) {
        try {
            int right = logicalWidth - 1;
            int bottom = logicalHeight - 1;
            Area shape = new Area();
            int[][] zones = {
                    {10, 0, right - 10, 1},
                    {6, 2, right - 6, 3},
                    {4, 4, right - 4, 5},
                    {2, 6, right - 2, 9},
                    {0, 10, right, bottom - 10},
                    {2, bottom - 9, right - 2, bottom - 6},
                    {4, bottom - 5, right - 4, bottom - 4},
                    {6, bottom - 3, right - 6, bottom - 2},
                    {10, bottom - 1, right - 10, bottom}
            };
            for (int[] z : zones) {
                shape.add(new Area(new Rectangle2D.Double(
                        z[0] * scale,
                        z[1] * scale,
                        (z[2] - z[0] + 1) * scale,
                        (z[3] - z[1] + 1) * scale
                )));
            }
            window.setShape(shape);
        } catch (RuntimeException ignored) {
            // Keep a rectangular window on platforms where shaped windows are not supported.
        }
    }
}
