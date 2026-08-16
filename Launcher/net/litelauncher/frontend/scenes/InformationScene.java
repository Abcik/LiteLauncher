package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import javax.swing.JComponent;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InformationScene extends PopupScene {

    private static final int[] POPUP = {124, 101, 315, 216};

    private final LauncherStore store = LauncherStore.get();

    private static final HeaderLayout HEADER = new HeaderLayout(
            132, 105, 176, Text.DisplayType.LINE, Text.LineAlignment.CENTER, 2, 0,
            130, 129, 180, Text.DisplayType.BLOCK, Text.LineAlignment.CENTER, 1, 0
    );

    private final PixelButton openLogButton;
    private final Text openLogText;

    public InformationScene(JComponent host, String title, String subtitle) {
        super(host, POPUP, title, subtitle, HEADER);
        openLogButton = new PixelButton(host, 166, 193, 108, 16, this::drawOpenLogButton);
        openLogText = new Text(host, 170, 195, 100, I18n.text("common.openLogFile"), Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);
        setPopupContentAnimationsEnabled(false);
    }

    @Override
    public boolean disposeOnClose() {
        return true;
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        openLogText.setAnimationEnabled(enabled);
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        openLogButton.render(graphics);
        openLogText.render(graphics);
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        boolean dirty = openLogButton.handleInput(mouse);
        if (openLogButton.consumeClick()) {
            openLauncherLog();
            return true;
        }
        return dirty;
    }

    @Override
    protected void disposePopupContent() {
        openLogText.dispose();
    }

    private void drawOpenLogButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
    }

    private void openLauncherLog() {
        Path logFile = LauncherLog.file();
        try {
            if (!Files.isRegularFile(logFile)) LauncherLog.info("Log file requested before it existed.");
            OSUtils.openFile(logFile);
        } catch (Exception exception) {
            LauncherLog.error("Unable to open launcher log file: " + logFile, exception);
        }
    }

}
