package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.backend.loader.LoaderOption;
import net.litelauncher.backend.loader.LoaderType;
import net.litelauncher.backend.version.Version;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.ScrollCanvas;
import net.litelauncher.frontend.modules.scroll.ScrollableList;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModificationScene extends PopupScene {

    private static final List<LoaderType> LOADERS = List.of(
            LoaderType.OPTIFINE,
            LoaderType.FABRIC,
            LoaderType.FORGE,
            LoaderType.NEOFORGE,
            LoaderType.QUILT
    );

    private final LauncherStore store = LauncherStore.get();
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;
    private final MainScene main;
    private final Version version;
    private final ScrollableList<LoaderType> modificationList;
    private final Text loadingText;
    private final List<Text> titleTexts = new ArrayList<>();
    private final List<Text> subtitleTexts = new ArrayList<>();
    private final Map<LoaderType, LoaderOption> options = new EnumMap<>(LoaderType.class);

    private boolean contentAnimationsEnabled;
    private boolean loading = true;
    private boolean disposed;

    public ModificationScene(MainScene host, Version version) {
        super(host, "popup.modifications.title", "popup.modifications.subtitle");
        this.main = host;
        this.version = version;

        loadingText = new Text(host, 132, 168, 176, I18n.text("modifications.loading"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);

        modificationList = new ScrollableList<>(host, 130, 88, 184, 154, 30, 1, 172, 180,
                new ScrollableList.Source<>() {
                    @Override
                    public int size() {
                        return loading ? 0 : LOADERS.size();
                    }

                    @Override
                    public LoaderType get(int index) {
                        return LOADERS.get(index);
                    }
                },
                (surface, x, y, width, height, state, type, _) ->
                        drawModificationRow(surface, x, y, width, height, state, type),
                this::syncModificationRow,
                (canvas, _, index) -> renderModificationTexts(canvas, index)
        );
        modificationList.setRowEnabled((type, _) -> available(type));

        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
        loadOptions();
    }

    @Override
    public boolean disposeOnClose() {
        return true;
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        contentAnimationsEnabled = enabled;
        loadingText.setAnimationEnabled(enabled);
        for (Text text : titleTexts) text.setAnimationEnabled(enabled);
        for (Text text : subtitleTexts) text.setAnimationEnabled(enabled);
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        if (loading) loadingText.render(graphics);
        else modificationList.render(graphics);
    }

    private void syncModificationRow(LoaderType type, int index, int y, int rowWidth,
                                     ScrollableList<LoaderType> list) {
        syncText(titleTexts, index, y + 2, list.rowWidth(162, 170),
                type.title() + " " + version.id(), Palette.TITLE);
        syncText(subtitleTexts, index, y + 14, list.rowWidth(162, 170), subtitle(type), Palette.SUBTITLE);
        trimTexts(titleTexts, list.size());
        trimTexts(subtitleTexts, list.size());
    }

    private void renderModificationTexts(ScrollCanvas canvas, int index) {
        titleTexts.get(index).render(canvas);
        subtitleTexts.get(index).render(canvas);
    }

    private void drawModificationRow(PixelSurface surface, int x, int y, int width, int height,
                                     PixelButton.State state, LoaderType type) {
        if (available(type)) PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
        else PixelPainter.drawSelectedElement(surface, x, y, width, height, store.theme());
    }

    private String subtitle(LoaderType type) {
        LoaderOption option = options.get(type);
        if (option == null || !option.available()) {
            return I18n.format("modifications.unavailable", "version", version.id());
        }
        return option.version().version();
    }

    private boolean available(LoaderType type) {
        LoaderOption option = options.get(type);
        return !loading && option != null && option.available();
    }

    private void loadOptions() {
        loading = true;
        options.clear();
        modificationList.markDirty();
        store.resolveLoaderOptions(version, resolved -> {
            if (disposed) return;
            options.clear();
            for (LoaderOption option : resolved) if (option != null) options.put(option.type(), option);
            loading = false;
            modificationList.markDirty();
            host.repaint();
        });
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            loadingText.setText(I18n.text("modifications.loading"));
            refreshSubtitles();
            host.repaint();
        }
    }

    private void refreshSubtitles() {
        int count = Math.min(LOADERS.size(), subtitleTexts.size());
        for (int index = 0; index < count; index++) {
            subtitleTexts.get(index).setText(subtitle(LOADERS.get(index)));
        }
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        if (loading) return false;

        boolean dirty = modificationList.handleInput(mouse, false);
        dirty |= modificationList.handleRowsInput(mouse);

        int clicked = modificationList.consumeClickedIndex();
        if (clicked >= 0 && clicked < LOADERS.size()) {
            LoaderOption option = options.get(LOADERS.get(clicked));
            if (option != null && option.available()) {
                store.chooseLoaderVersion(option.version(), version);
                main.openVersionsPopup();
                return true;
            }
        }
        return dirty;
    }

    @Override
    protected void disposePopupContent() {
        disposed = true;
        store.unsubscribe(storeListener);
        loadingText.dispose();
        for (Text text : titleTexts) text.dispose();
        for (Text text : subtitleTexts) text.dispose();
        titleTexts.clear();
        subtitleTexts.clear();
    }

    private void syncText(List<Text> texts, int index, int y, int width, String value, Palette color) {
        if (index >= texts.size()) {
            Text text = new Text(host, 5, y, width, value,
                    Text.DisplayType.LINE, Text.LineAlignment.LEFT, color, 1, 0);
            text.setAnimationEnabled(contentAnimationsEnabled);
            texts.add(text);
            return;
        }

        Text text = texts.get(index);
        text.setPosition(5, y);
        text.setBoxWidth(width);
        text.setText(value);
        text.setAnimationEnabled(contentAnimationsEnabled);
    }

    private static void trimTexts(List<Text> texts, int count) {
        while (texts.size() > count) texts.removeLast().dispose();
    }
}
