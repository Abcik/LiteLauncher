package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.frontend.Palette;
import net.litelauncher.i18n.I18n;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.overlay.PopupContent;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.text.Text;

import javax.swing.JComponent;

public abstract class PopupScene implements PopupContent {

    public static final int[] DEFAULT_POPUP = {124, 45, 315, 298};
    public static final HeaderLayout DEFAULT_HEADER = new HeaderLayout(
            132, 49, 176, Text.DisplayType.LINE, Text.LineAlignment.LEFT, 2, 0,
            132, 73, 176, Text.DisplayType.LINE, Text.LineAlignment.LEFT, 1, 0
    );

    private final int[] bounds;
    protected final JComponent host;
    private final Text titleText;
    private final Text subtitleText;
    private final String titleKey;
    private final String subtitleKey;
    private boolean disposed;

    protected PopupScene(JComponent host, String title, String subtitle) {
        this(host, DEFAULT_POPUP, title, subtitle, DEFAULT_HEADER);
    }

    protected PopupScene(JComponent host, int[] bounds, String title, String subtitle, HeaderLayout header) {
        this.host = host;
        this.bounds = bounds;
        this.titleKey = title;
        this.subtitleKey = subtitle;

        titleText = new Text(host, header.titleX(), header.titleY(), header.titleWidth(), I18n.text(title),
                header.titleDisplayType(), header.titleAlignment(), Palette.TITLE,
                header.titleScale(), header.titleGlyphGap()
        );

        subtitleText = new Text(host, header.subtitleX(), header.subtitleY(), header.subtitleWidth(), I18n.text(subtitle),
                header.subtitleDisplayType(), header.subtitleAlignment(), Palette.SUBTITLE,
                header.subtitleScale(), header.subtitleGlyphGap()
        );

        setPopupHeaderAnimationsEnabled(false);
    }

    @Override
    public void render(PixelGraphics graphics, MouseState mouse) {
        renderPopupBase(graphics);
        renderPopupContent(graphics, mouse);
        renderPopupHeader(graphics);
    }

    protected void renderPopupBase(PixelGraphics graphics) {
        PixelPainter.drawPopup(graphics, bounds, LauncherStore.get().theme());
    }

    protected void renderPopupHeader(PixelGraphics graphics) {
        titleText.render(graphics);
        subtitleText.render(graphics);
    }

    protected void refreshPopupText() {
        titleText.setText(I18n.text(titleKey));
        subtitleText.setText(I18n.text(subtitleKey));
    }

    protected abstract void renderPopupContent(PixelGraphics graphics, MouseState mouse);

    private void setPopupHeaderAnimationsEnabled(boolean enabled) {
        titleText.setAnimationEnabled(enabled);
        subtitleText.setAnimationEnabled(enabled);
    }

    protected abstract void setPopupContentAnimationsEnabled(boolean enabled);

    protected final void setPopupAnimationsEnabled(boolean enabled) {
        setPopupHeaderAnimationsEnabled(enabled);
        setPopupContentAnimationsEnabled(enabled);
    }

    @Override
    public void onOpen() {
        setPopupAnimationsEnabled(true);
    }

    @Override
    public void onClose() {
        setPopupAnimationsEnabled(false);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        titleText.dispose();
        subtitleText.dispose();
        disposePopupContent();
    }

    protected abstract void disposePopupContent();

    @Override
    public int xMin() {
        return bounds[0];
    }

    @Override
    public int yMin() {
        return bounds[1];
    }

    @Override
    public int xMax() {
        return bounds[2];
    }

    @Override
    public int yMax() {
        return bounds[3];
    }

    public record HeaderLayout(
            int titleX,
            int titleY,
            int titleWidth,
            Text.DisplayType titleDisplayType,
            Text.LineAlignment titleAlignment,
            int titleScale,
            int titleGlyphGap,
            int subtitleX,
            int subtitleY,
            int subtitleWidth,
            Text.DisplayType subtitleDisplayType,
            Text.LineAlignment subtitleAlignment,
            int subtitleScale,
            int subtitleGlyphGap
    ) {
    }
}
