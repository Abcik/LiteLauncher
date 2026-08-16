package net.litelauncher.ui;

public final class ProgressBar {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private double progress;

    public ProgressBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setProgress(double progress) {
        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;
        this.progress = progress;
    }

    public void render(PixelSurface surface) {
        PixelPainter.drawProgressTrack(surface, x, y, width, height);
        PixelPainter.drawProgressFill(surface, x, y, (int) Math.round(width * progress), height);
    }
}
