package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.backend.version.Version;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

public final class DeleteConfirmationScene extends PopupScene {

    private static final int[] POPUP = {124, 101, 315, 216};
    private static final HeaderLayout HEADER = new HeaderLayout(
            132, 105, 176, Text.DisplayType.LINE, Text.LineAlignment.CENTER, 2, 0,
            130, 129, 180, Text.DisplayType.BLOCK, Text.LineAlignment.CENTER, 1, 0
    );

    private final LauncherStore store = LauncherStore.get();
    private final MainScene main;
    private final Version version;
    private final PixelButton yesButton;
    private final PixelButton noButton;
    private final Text yesText;
    private final Text noText;

    public DeleteConfirmationScene(MainScene host, Version version) {
        super(host, POPUP, "deleteConfirmation.title",
                version != null && version.modpack() ? "deleteConfirmation.modpack" : "deleteConfirmation.instance", HEADER);
        this.main = host;
        this.version = version;

        yesButton = new PixelButton(host, 141, 193, 72, 16, this::drawButton);
        noButton = new PixelButton(host, 227, 193, 72, 16, this::drawButton);
        yesText = new Text(host, 145, 195, 64, I18n.text("deleteConfirmation.yes"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);
        noText = new Text(host, 231, 195, 64, I18n.text("deleteConfirmation.no"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);
        setPopupContentAnimationsEnabled(false);
    }

    @Override
    public boolean closeOnBackdrop() {
        return false;
    }

    @Override
    public boolean disposeOnClose() {
        return true;
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        yesText.setAnimationEnabled(enabled);
        noText.setAnimationEnabled(enabled);
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        yesButton.render(graphics);
        noButton.render(graphics);
        yesText.render(graphics);
        noText.render(graphics);
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        boolean dirty = yesButton.handleInput(mouse);
        dirty |= noButton.handleInput(mouse);

        if (yesButton.consumeClick()) {
            if (version != null) store.deleteVersion(version);
            main.openVersionsPopup();
            return true;
        }
        if (noButton.consumeClick()) {
            main.openVersionsPopup();
            return true;
        }
        return dirty;
    }

    @Override
    protected void disposePopupContent() {
        yesText.dispose();
        noText.dispose();
    }

    private void drawButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
    }
}
