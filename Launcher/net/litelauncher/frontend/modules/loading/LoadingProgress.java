package net.litelauncher.frontend.modules.loading;

import net.litelauncher.frontend.Palette;
import net.litelauncher.LauncherStore;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.text.Text;

import javax.swing.JComponent;
import java.awt.Color;

public final class LoadingProgress {

    private final JComponent host;
    private final Text actionText;
    private final Text detailsText;
    private final int barX;
    private final int barY;
    private final int barWidth;
    private final int barHeight;

    private boolean visible;
    private double progress;

    public LoadingProgress(JComponent host) {
        this(host,
                62, 299, 200,
                262, 299, 116,
                60, 314, 320, 6);
    }

    public LoadingProgress(JComponent host,
                           int actionX, int actionY, int actionWidth,
                           int detailsX, int detailsY, int detailsWidth,
                           int barX, int barY, int barWidth, int barHeight) {
        this.host = host;
        this.barX = barX;
        this.barY = barY;
        this.barWidth = barWidth;
        this.barHeight = barHeight;

        actionText = new Text(host, actionX, actionY, actionWidth, "",
                Text.DisplayType.LINE, Text.LineAlignment.LEFT,
                Palette.TITLE, 1, 0
        );

        detailsText = new Text(host, detailsX, detailsY, detailsWidth, "",
                Text.DisplayType.LINE, Text.LineAlignment.RIGHT,
                Palette.OUTLINE, 1, 0
        );
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        host.repaint();
    }

    public void setProgress(double progress) {
        double value = Math.clamp(progress, 0.0, 1.0);
        if (this.progress == value) return;
        this.progress = value;
        host.repaint();
    }

    public void setActionText(String text) {
        actionText.setText(text == null ? "" : text);
    }

    public void setDetailsText(String text) {
        detailsText.setText(text == null ? "" : text);
    }

    public void setAnimationEnabled(boolean enabled) {
        actionText.setAnimationEnabled(enabled);
        detailsText.setAnimationEnabled(enabled);
    }

    public void dispose() {
        actionText.dispose();
        detailsText.dispose();
    }

    public void render(PixelGraphics graphics) {
        if (!visible) return;
        PixelSurface surface = PixelSurface.direct(graphics);
        drawTrack(surface, barX, barY, barWidth, barHeight);
        drawBar(surface, barX, barY, fillWidth(), barHeight);
        actionText.render(graphics);
        detailsText.render(graphics);
    }

    private int fillWidth() {
        return (int) Math.round(barWidth * progress);
    }


    private void drawTrack(PixelSurface surface, int x, int y, int width, int height) {
        Color outline = Palette.OUTLINE.color(LauncherStore.get().theme());
        if (width <= 0 || height <= 0) return;

        surface.paint(x + 1, y, x + width - 2, y, outline);
        surface.paint(x, y + 1, x + width - 1, y + height - 2, outline);
        surface.paint(x + 1, y + height - 1, x + width - 2, y + height - 1, outline);
    }

    private void drawBar(PixelSurface surface, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        Color glare = Palette.ACCENT_GLARE.color(LauncherStore.get().theme());
        Color accent = Palette.ACCENT.color(LauncherStore.get().theme());
        Color shadow = Palette.ACCENT_SHADOW.color(LauncherStore.get().theme());

        if (width == 1) {
            surface.paint(x, y + 1, x, y + height - 2, glare);
            return;
        }

        surface.paint(x, y + 1, x, y + height - 2, glare);
        surface.paint(x + 1, y, x + width - 2, y, glare);
        surface.paint(x + 1, y + 1, x + width - 2, y + height - 2, accent);
        surface.paint(x + 1, y + height - 1, x + width - 2, y + height - 1, shadow);
        surface.paint(x + width - 1, y + 1, x + width - 1, y + height - 2, shadow);
    }
}
