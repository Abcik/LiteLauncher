package net.litelauncher.bootstrap;

import net.litelauncher.backend.BootstrapLog;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.ui.AppWindow;
import net.litelauncher.ui.MouseState;
import net.litelauncher.ui.Palette;
import net.litelauncher.ui.PixelButton;
import net.litelauncher.ui.PixelCanvas;
import net.litelauncher.ui.PixelPainter;
import net.litelauncher.ui.PixelSurface;
import net.litelauncher.ui.PixelText;
import net.litelauncher.ui.ProgressBar;
import net.litelauncher.ui.WindowChrome;

import javax.swing.SwingUtilities;
import java.awt.Window;

public final class Bootstrap {

    private Bootstrap() {
    }

    public static void main(String[] args) {

        OSUtils.disableOsScaling();

        BootstrapLog log = new BootstrapLog(OSUtils.logsDirectory().resolve("litelauncher_bootstrap.log"));
        log.start("Bootstrap started");

        BootstrapUi ui = new BootstrapUi(log);
        Thread thread = new Thread(() -> {
            try {
                BootstrapBackend.updateAndLaunch(ui::updateProgress, log);
                ui.close();
            } catch (Exception exception) {
                log.error("Bootstrap failed.", exception);
                String message = exception instanceof BootstrapException && exception.getMessage() != null && !exception.getMessage().isBlank()
                        ? exception.getMessage()
                        : "Unable to start LiteLauncher.";
                ui.showError(message);
            }
        }, "LiteLauncher Bootstrap");
        thread.start();
    }

    private static final class BootstrapUi {

        private final BootstrapLog log;
        private volatile boolean uiRequested;
        private BootstrapScene scene;

        private BootstrapUi(BootstrapLog log) {
            this.log = log;
        }

        private void updateProgress(double value, String details) {
            uiRequested = true;
            SwingUtilities.invokeLater(() -> {
                BootstrapScene target = scene();
                target.updateProgress(value, details);
            });
        }

        private void showError(String message) {
            uiRequested = true;
            SwingUtilities.invokeLater(() -> scene().showError(message));
        }

        private void close() {
            if (!uiRequested) return;
            SwingUtilities.invokeLater(() -> {
                if (scene != null) scene.closeWindow();
            });
        }

        private BootstrapScene scene() {
            if (scene == null) {
                scene = new BootstrapScene(log);
                AppWindow.open("LiteLauncher", scene);
            }
            return scene;
        }
    }

    private static final class BootstrapScene extends PixelCanvas {

        private final BootstrapLog log;
        private final WindowChrome chrome;
        private final PixelButton logButton;
        private final PixelText mainText;
        private final PixelText detailsText;
        private final PixelText buttonText;
        private final ProgressBar progressBar;

        private Stage stage = Stage.UPDATING;
        private double progress;

        private BootstrapScene(BootstrapLog log) {
            super(AppWindow.WIDTH, AppWindow.HEIGHT, AppWindow.SCALE);
            this.log = log;
            this.chrome = new WindowChrome(this);
            this.logButton = new PixelButton(this, 40, 68, 140, 18, this::drawLogButton);
            this.mainText = new PixelText(this, 20, 50, 180, "Updating...", PixelText.Alignment.CENTER, Palette.TITLE);
            this.detailsText = new PixelText(this, 30, 70, 160, "Checking files... 0%", PixelText.Alignment.LEFT, Palette.TITLE);
            this.buttonText = new PixelText(this, 40, 71, 140, "Open log file", PixelText.Alignment.CENTER, Palette.ACCENT_TITLE);
            this.progressBar = new ProgressBar(30, 84, 160, 6);
        }

        @Override
        protected void render(PixelSurface surface, MouseState mouse) {
            PixelPainter.drawWindow(surface, AppWindow.WIDTH, AppWindow.HEIGHT);
            chrome.render(surface);
            mainText.render(surface);

            if (stage == Stage.UPDATING) {
                progressBar.setProgress(progress);
                detailsText.render(surface);
                progressBar.render(surface);
                return;
            }

            logButton.render(surface);
            buttonText.render(surface);
        }

        @Override
        protected boolean handleInput(MouseState mouse) {
            boolean dirty = chrome.handleInput(mouse);

            if (stage == Stage.ERROR) {
                dirty |= logButton.handleInput(mouse);
                if (logButton.consumeClick()) {
                    log.open();
                    return true;
                }
            }

            return dirty;
        }

        @Override
        protected boolean canStartWindowDrag(int logicalX, int logicalY) {
            return logicalY >= 0
                    && logicalY < AppWindow.HEADER_DRAG_HEIGHT
                    && !chrome.containsButton(logicalX, logicalY);
        }

        private void updateProgress(double value, String details) {
            progress = value;
            detailsText.setText(details);
            repaint();
        }

        private void showError(String text) {
            stage = Stage.ERROR;
            mainText.setBounds(20, 50, 180);
            mainText.setText(text);
            mainText.setColor(Palette.DANGER);
            repaint();
        }

        private void closeWindow() {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) AppWindow.close(window);
        }

        private void drawLogButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
            PixelPainter.drawAccentButton(surface, x, y, width, height, state);
        }

        private enum Stage {
            UPDATING, ERROR
        }
    }
}
