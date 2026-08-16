package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.Theme;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.field.InputField;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.PixelScrollView;
import net.litelauncher.frontend.modules.scroll.ScrollCanvas;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import javax.swing.TransferHandler;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ThemeCustomizationScene extends PopupScene {

    private static final int LIST_X = 130;
    private static final int LIST_Y = 88;
    private static final int LIST_WIDTH = 184;
    private static final int LIST_HEIGHT = 169;
    private static final int DROP_X = 130;
    private static final int DROP_Y = 261;
    private static final int DROP_WIDTH = 180;
    private static final int DROP_HEIGHT = 30;
    private static final int ROW_WIDTH = 172;
    private static final int ROW_HEIGHT = 25;
    private static final int ROW_GAP = 1;
    private static final int ROW_STRIDE = ROW_HEIGHT + ROW_GAP;

    private static final int NAME_X = 4;
    private static final int TEXT_Y = 6;
    private static final int HASH_X = 99;
    private static final int HASH_WIDTH = 8;
    private static final int FIELD_PANEL_X = 109;
    private static final int FIELD_PANEL_Y = 5;
    private static final int FIELD_PANEL_WIDTH = 45;
    private static final int FIELD_PANEL_HEIGHT = 15;
    private static final int FIELD_X = 114;
    private static final int FIELD_WIDTH = 37;
    private static final int PREVIEW_X = 158;
    private static final int PREVIEW_Y = 8;

    private final LauncherStore store = LauncherStore.get();
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;
    private final MainScene mainHost;
    private final PixelScrollView scrollView;
    private final Palette[] colors = Palette.values();
    private final List<Text> nameTexts = new ArrayList<>();
    private final List<Text> hashTexts = new ArrayList<>();
    private final List<InputField> colorInputs = new ArrayList<>();
    private final Text dropTextLine1;
    private final Text dropTextLine2;
    private final TransferHandler themeDropHandler = new ThemeDropHandler();

    private TransferHandler previousTransferHandler;
    private Theme editingTheme;
    private boolean syncing;

    public ThemeCustomizationScene(MainScene host) {
        super(host, "popup.themeCustomization.title", "popup.themeCustomization.subtitle");

        mainHost = host;
        scrollView = new PixelScrollView(LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT)
                .setContentPadding(0, 0, 0, 0)
                .setExpectedContentHeight(colors.length * ROW_STRIDE - ROW_GAP);

        for (int index = 0; index < colors.length; index++) {
            Palette palette = colors[index];
            int rowY = index * ROW_STRIDE;

            nameTexts.add(new Text(host, NAME_X, rowY + TEXT_Y, 92, palette.name(),
                    Text.DisplayType.LINE, Text.LineAlignment.LEFT, Palette.SUBTITLE, 1, 0));
            hashTexts.add(new Text(host, HASH_X, rowY + TEXT_Y, HASH_WIDTH, "#",
                    Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.OUTLINE, 1, 0));

            InputField input = new InputField(host, FIELD_X, rowY + TEXT_Y, FIELD_WIDTH, "",
                    Palette.OUTLINE, Text.LineAlignment.CENTER, 6, Palette.SUBTITLE,
                    Palette.GENERAL_BACKGROUND, Palette.ACCENT, Text.LineAlignment.CENTER, 1, 0);
            input.setCharacterMapper(ThemeCustomizationScene::uppercaseHexCharacter);
            input.setCharacterFilter(ThemeCustomizationScene::isHexCharacter);
            input.setChangeAction(() -> applyInput(palette, input));
            colorInputs.add(input);
        }

        dropTextLine1 = new Text(host, 135, DROP_Y + 2, 170, I18n.text("themeCustomization.dropFileLine1"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);
        dropTextLine2 = new Text(host, 135, DROP_Y + 14, 170, I18n.text("themeCustomization.dropFileLine2"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);

        syncFromTheme(store.theme());
        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
    }

    @Override
    public void onOpen() {
        syncFromTheme(store.theme());
        if (host.getTransferHandler() != themeDropHandler) {
            previousTransferHandler = host.getTransferHandler();
            host.setTransferHandler(themeDropHandler);
        }
        super.onOpen();
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        for (Text text : nameTexts) text.setAnimationEnabled(enabled);
        for (Text text : hashTexts) text.setAnimationEnabled(enabled);
        for (InputField input : colorInputs) input.setAnimationEnabled(enabled);
        dropTextLine1.setAnimationEnabled(enabled);
        dropTextLine2.setAnimationEnabled(enabled);
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        scrollView.render(graphics, this::buildRows);
        drawDropField(graphics);
    }

    private void drawDropField(PixelGraphics graphics) {
        PixelPainter.drawElement(PixelPainter.direct(graphics), DROP_X, DROP_Y, DROP_WIDTH, DROP_HEIGHT,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));
        dropTextLine1.render(graphics);
        dropTextLine2.render(graphics);
    }

    private void buildRows(ScrollCanvas canvas) {
        Theme theme = editingTheme == null ? store.theme() : editingTheme;
        for (int index = 0; index < colors.length; index++) {
            int rowY = index * ROW_STRIDE;
            Palette palette = colors[index];

            PixelPainter.drawElement(canvas, 0, rowY, ROW_WIDTH, ROW_HEIGHT,
                    Palette.ELEMENT_BACKGROUND.color(theme), Palette.OUTLINE.color(theme));
            PixelPainter.drawElement(canvas, FIELD_PANEL_X, rowY + FIELD_PANEL_Y,
                    FIELD_PANEL_WIDTH, FIELD_PANEL_HEIGHT,
                    Palette.ELEMENT_BACKGROUND.color(theme), Palette.OUTLINE.color(theme));

            nameTexts.get(index).render(canvas);
            hashTexts.get(index).render(canvas);
            colorInputs.get(index).render(canvas);
            drawColorPreview(canvas, PREVIEW_X, rowY + PREVIEW_Y,
                    palette.customizationColor(theme), Palette.OUTLINE.color(theme));
        }
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        boolean dirty = scrollView.handleInput(mouse, this::buildRows);
        for (InputField input : colorInputs) dirty |= input.handleInput(mouse, scrollView);
        return dirty;
    }

    private void applyInput(Palette palette, InputField input) {
        if (syncing || palette == null || input == null) return;

        String value = input.getValue();
        Theme theme = editingTheme == null ? store.theme() : editingTheme;

        if (value.isEmpty()) {
            if (palette.setCustomHex(theme, null)) store.notifyThemeColorsChanged();
            return;
        }

        if (value.length() != 6) return;

        if (value.equals(palette.defaultHex(theme))) {
            boolean changed = palette.setCustomHex(theme, null);
            syncing = true;
            try {
                input.setValue("");
            } finally {
                syncing = false;
            }
            if (changed) store.notifyThemeColorsChanged();
            return;
        }

        if (palette.setCustomHex(theme, value)) store.notifyThemeColorsChanged();
    }

    private void syncFromTheme(Theme theme) {
        editingTheme = theme == null ? Theme.LIGHT : theme;
        syncing = true;
        try {
            for (int index = 0; index < colors.length; index++) {
                Palette palette = colors[index];
                InputField input = colorInputs.get(index);
                input.setPlaceholder(palette.defaultHex(editingTheme));
                String custom = palette.customHex(editingTheme);
                input.setValue(custom == null ? "" : custom);
            }
        } finally {
            syncing = false;
        }
        host.repaint();
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            dropTextLine1.setText(I18n.text("themeCustomization.dropFileLine1"));
            dropTextLine2.setText(I18n.text("themeCustomization.dropFileLine2"));
            host.repaint();
        } else if (event == LauncherStore.Event.THEME_CHANGED) {
            syncFromTheme(store.theme());
        } else if (event == LauncherStore.Event.THEME_COLORS_CHANGED) {
            host.repaint();
        }
    }

    @Override
    public void onClose() {
        if (host.getTransferHandler() == themeDropHandler) host.setTransferHandler(previousTransferHandler);
        previousTransferHandler = null;
        super.onClose();
        for (InputField input : colorInputs) input.blur();
    }

    @Override
    protected void disposePopupContent() {
        if (host.getTransferHandler() == themeDropHandler) host.setTransferHandler(previousTransferHandler);
        previousTransferHandler = null;
        store.unsubscribe(storeListener);
        dropTextLine1.dispose();
        dropTextLine2.dispose();
        for (Text text : nameTexts) text.dispose();
        for (Text text : hashTexts) text.dispose();
        for (InputField input : colorInputs) input.dispose();
        nameTexts.clear();
        hashTexts.clear();
        colorInputs.clear();
    }

    private boolean insideDrop(Point point) {
        if (point == null) return false;
        int scale = Math.max(1, store.scale());
        int x = point.x / scale;
        int y = point.y / scale;
        return x >= DROP_X && x < DROP_X + DROP_WIDTH && y >= DROP_Y && y < DROP_Y + DROP_HEIGHT;
    }

    private static boolean isJson(File file) {
        return file != null && file.isFile()
                && file.getName().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private void importThemeFile(File file) {
        try {
            Palette.importCustomThemeColors(file.toPath());
            syncFromTheme(store.theme());
            store.notifyThemeColorsChanged();
        } catch (Exception exception) {
            LauncherLog.error("Unable to import custom theme colors: " + file, exception);
            mainHost.openErrorPopup(I18n.text("themeCustomization.importError"));
        }
    }

    private final class ThemeDropHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false;
            if (!insideDrop(support.getDropLocation().getDropPoint())) return false;
            support.setDropAction(COPY);
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                Transferable transferable = support.getTransferable();
                Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (!(data instanceof List<?> files)) return false;
                for (Object item : files) {
                    if (item instanceof File file && isJson(file)) {
                        importThemeFile(file);
                        return true;
                    }
                }
            } catch (Exception exception) {
                LauncherLog.error("Unable to read dropped theme file.", exception);
                mainHost.openErrorPopup(I18n.text("themeCustomization.importError"));
            }
            return false;
        }
    }

    private static char uppercaseHexCharacter(char character) {
        if (character >= 'a' && character <= 'f') return (char) (character - ('a' - 'A'));
        return character;
    }

    private static boolean isHexCharacter(char character) {
        return (character >= '0' && character <= '9') || (character >= 'A' && character <= 'F');
    }

    private static void drawColorPreview(PixelSurface surface, int x, int y, java.awt.Color color, java.awt.Color outline) {
        surface.paint(x + 2, y, x + 6, y, outline);
        surface.paint(x + 1, y + 1, x + 7, y + 1, outline);
        surface.paint(x, y + 2, x + 8, y + 6, outline);
        surface.paint(x + 1, y + 7, x + 7, y + 7, outline);
        surface.paint(x + 2, y + 8, x + 6, y + 8, outline);

        surface.paint(x + 2, y + 1, x + 6, y + 1, color);
        surface.paint(x + 1, y + 2, x + 7, y + 6, color);
        surface.paint(x + 2, y + 7, x + 6, y + 7, color);
    }
}
