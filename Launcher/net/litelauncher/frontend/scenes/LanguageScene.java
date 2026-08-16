package net.litelauncher.frontend.scenes;

import net.litelauncher.Language;
import net.litelauncher.LauncherStore;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.ScrollCanvas;
import net.litelauncher.frontend.modules.scroll.ScrollableList;
import net.litelauncher.frontend.modules.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class LanguageScene extends PopupScene {

    private final LauncherStore store = LauncherStore.get();
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;

    private final ScrollableList<Language> languageList;
    private final List<Text> languageTexts = new ArrayList<>();

    public LanguageScene(MainScene host) {
        super(host, "popup.language.title", "popup.language.subtitle");

        Language[] values = Language.values();
        languageList = new ScrollableList<>(host, 130, 88, 184, 203, 18, 1, 172, 180,
                new ScrollableList.Source<>() {
                    @Override
                    public int size() {
                        return values.length;
                    }

                    @Override
                    public Language get(int index) {
                        return values[index];
                    }
                },
                (surface, x, y, width, height, state, language, _) ->
                        drawLanguageRow(surface, x, y, width, height, state, language),
                this::syncLanguageRow,
                (canvas, _, index) -> renderLanguageText(canvas, index)
        );
        languageList.setRowEnabled((language, _) -> store.language() != language);

        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
    }

    public void setScrollOffset(int offset) {
        languageList.setScrollOffset(offset);
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        for (Text text : languageTexts) text.setAnimationEnabled(enabled);
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        languageList.render(graphics);
    }

    private void syncLanguageRow(Language language, int index, int y, int rowWidth, ScrollableList<Language> list) {
        int textWidth = list.rowWidth(164, 172);
        int textY = y + 3;

        if (index >= languageTexts.size()) {
            languageTexts.add(new Text(host, 4, textY, textWidth, language.title(),
                    Text.DisplayType.LINE, Text.LineAlignment.CENTER,
                    Palette.SUBTITLE, 1, 0
            ));
        } else {
            Text text = languageTexts.get(index);
            text.setPosition(4, textY);
            text.setBoxWidth(textWidth);
            text.setText(language.title());
        }
        trimTexts(languageTexts, list.size());
    }

    private void renderLanguageText(ScrollCanvas canvas, int index) {
        languageTexts.get(index).render(canvas);
    }

    private void drawLanguageRow(PixelSurface surface, int x, int y, int width, int height,
                                 PixelButton.State state, Language language) {
        if (store.language() == language) drawSelectedLanguageRow(surface, x, y, width, height);
        else drawPlainLanguageRow(surface, x, y, width, height, state);
    }

    private void drawPlainLanguageRow(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
    }

    private void drawSelectedLanguageRow(PixelSurface surface, int x, int y, int width, int height) {
        PixelPainter.drawSelectedElement(surface, x, y, width, height, store.theme());
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            host.repaint();
        }
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        boolean dirty = languageList.handleInput(mouse);

        int clicked = languageList.consumeClickedIndex();
        if (clicked >= 0) {
            store.setLanguage(Language.values()[clicked]);
            return true;
        }

        return dirty;
    }

    @Override
    protected void disposePopupContent() {
        store.unsubscribe(storeListener);
        for (Text text : languageTexts) text.dispose();
        languageTexts.clear();
    }

    private static void trimTexts(List<Text> texts, int count) {
        while (texts.size() > count) texts.removeLast().dispose();
    }
}
