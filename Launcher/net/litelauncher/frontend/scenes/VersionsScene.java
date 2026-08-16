package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.backend.version.Version;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.checkbox.CheckBox;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.ScrollCanvas;
import net.litelauncher.frontend.modules.scroll.ScrollableList;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import javax.swing.TransferHandler;
import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VersionsScene extends PopupScene {

    private static final int LIST_X = 130;
    private static final int LIST_Y = 88;
    private static final int LIST_WIDTH = 184;
    private static final int LIST_HEIGHT = 166;
    private static final int LIST_HEIGHT_WITH_DROP_FIELD = 132;

    private static final int DROP_X = 130;
    private static final int DROP_Y = 224;
    private static final int DROP_WIDTH = 180;
    private static final int DROP_HEIGHT = 30;

    private final LauncherStore store = LauncherStore.get();
    private final MainScene mainHost;
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;

    private final ScrollableList<Version> versionList;
    private final CheckBox releasesCheckBox;
    private final CheckBox snapshotsCheckBox;
    private final CheckBox legacyCheckBox;
    private final CheckBox modpacksCheckBox;

    private final Text releasesText;
    private final Text snapshotsText;
    private final Text legacyText;
    private final Text modpacksText;
    private final Text dropTextLine1;
    private final Text dropTextLine2;

    private final List<PixelButton> deleteButtons = new ArrayList<>();
    private final List<PixelButton> modificationButtons = new ArrayList<>();
    private final List<Text> titleTexts = new ArrayList<>();
    private final List<Text> subtitleTexts = new ArrayList<>();
    private final TransferHandler modpackDropHandler = new ModpackDropHandler();

    private TransferHandler previousTransferHandler;
    private boolean contentAnimationsEnabled;
    private boolean compactList;
    private int animatedStart = -1;
    private int animatedEnd = -1;

    public VersionsScene(MainScene host) {
        super(host, "popup.versions.title", "popup.versions.subtitle");
        mainHost = host;

        versionList = new ScrollableList<>(host, LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT, 30, 1, 172, 180,
                new ScrollableList.Source<>() {
                    @Override
                    public int size() {
                        return store.versions().size();
                    }

                    @Override
                    public Version get(int index) {
                        return store.versions().get(index);
                    }
                },
                (surface, x, y, width, height, state, version, _) ->
                        drawVersionRow(surface, x, y, width, height, state, version),
                this::syncVersionRow,
                this::renderVersionTexts
        );
        versionList.setRowEnabled((version, _) -> !store.isSelectedVersion(version));

        releasesCheckBox = new CheckBox(host, 135, 264, store.releaseFilter());
        snapshotsCheckBox = new CheckBox(host, 135, 277, store.snapshotFilter());
        legacyCheckBox = new CheckBox(host, 220, 264, store.legacyFilter());
        modpacksCheckBox = new CheckBox(host, 220, 277, store.modpackFilter());

        releasesText = lineText(148, 262, 69, "versions.releases", Text.LineAlignment.LEFT);
        snapshotsText = lineText(148, 275, 69, "versions.snapshots", Text.LineAlignment.LEFT);
        legacyText = lineText(233, 262, 69, "versions.legacy", Text.LineAlignment.LEFT);
        modpacksText = lineText(233, 275, 69, "versions.modpacks", Text.LineAlignment.LEFT);
        dropTextLine1 = lineText(135, 226, 170, "versions.dropModpackLine1", Text.LineAlignment.CENTER);
        dropTextLine2 = lineText(135, 238, 170, "versions.dropModpackLine2", Text.LineAlignment.CENTER);

        releasesCheckBox.setChangeAction(() -> store.setReleaseFilter(releasesCheckBox.value()));
        snapshotsCheckBox.setChangeAction(() -> store.setSnapshotFilter(snapshotsCheckBox.value()));
        legacyCheckBox.setChangeAction(() -> store.setLegacyFilter(legacyCheckBox.value()));
        modpacksCheckBox.setChangeAction(() -> store.setModpackFilter(modpacksCheckBox.value()));

        syncListLayout();
        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
    }

    public void setScrollOffset(int offset) {
        versionList.setScrollOffset(offset);
    }

    @Override
    public void onOpen() {
        super.onOpen();
        syncListLayout();
        previousTransferHandler = host.getTransferHandler();
        host.setTransferHandler(modpackDropHandler);
        store.refreshLocalVersions();
    }

    @Override
    public void onClose() {
        if (host.getTransferHandler() == modpackDropHandler) host.setTransferHandler(previousTransferHandler);
        previousTransferHandler = null;
        super.onClose();
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        contentAnimationsEnabled = enabled;
        releasesText.setAnimationEnabled(enabled);
        snapshotsText.setAnimationEnabled(enabled);
        legacyText.setAnimationEnabled(enabled);
        modpacksText.setAnimationEnabled(enabled);
        dropTextLine1.setAnimationEnabled(enabled);
        dropTextLine2.setAnimationEnabled(enabled);

        if (enabled) syncVisibleTextAnimations();
        else disableVersionTextAnimations();
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        syncListLayout();
        versionList.render(graphics);
        syncVisibleTextAnimations();
        if (store.modpackFilter()) drawDropField(graphics);
        drawBottomControls(graphics);
    }

    private void drawDropField(PixelGraphics graphics) {
        PixelPainter.drawElement(PixelPainter.direct(graphics), DROP_X, DROP_Y, DROP_WIDTH, DROP_HEIGHT,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));
        dropTextLine1.render(graphics);
        dropTextLine2.render(graphics);
    }

    private void drawBottomControls(PixelGraphics graphics) {
        PixelPainter.drawElement(PixelPainter.direct(graphics), 130, 258, 180, 33,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));

        releasesCheckBox.render(graphics);
        snapshotsCheckBox.render(graphics);
        legacyCheckBox.render(graphics);
        modpacksCheckBox.render(graphics);

        releasesText.render(graphics);
        snapshotsText.render(graphics);
        legacyText.render(graphics);
        modpacksText.render(graphics);
    }

    private void syncVersionRow(Version version, int index, int y, int rowWidth,
                                ScrollableList<Version> list) {
        boolean animated = contentAnimationsEnabled && list.isRowVisible(index);
        boolean deletable = canDelete(version);
        boolean modificationAvailable = version.modificationInstallersAvailable();
        int titleWidth = deletable && modificationAvailable
                ? list.rowWidth(143, 151)
                : deletable || modificationAvailable ? list.rowWidth(152, 160) : list.rowWidth(162, 170);
        int subtitleWidth = list.rowWidth(162, 170);

        if (index >= deleteButtons.size()) {
            deleteButtons.add(new PixelButton(host, rowWidth - 11, y + 5, 5, 5, this::drawDeleteButton));
        } else {
            deleteButtons.get(index).setBounds(rowWidth - 11, y + 5, 5, 5);
        }

        int modificationX = modificationAvailable && !deletable ? rowWidth - 11 : rowWidth - 20;
        if (index >= modificationButtons.size()) {
            modificationButtons.add(new PixelButton(host, modificationX, y + 5, 5, 5, this::drawModificationButton));
        } else {
            modificationButtons.get(index).setBounds(modificationX, y + 5, 5, 5);
        }

        syncText(titleTexts, index, y + 2, titleWidth, version.title(), Palette.TITLE, Text.LineAlignment.LEFT, animated);
        syncText(subtitleTexts, index, y + 14, subtitleWidth, version.subtitle(), Palette.SUBTITLE, Text.LineAlignment.LEFT, animated);

        trimButtons(deleteButtons, list.size());
        trimButtons(modificationButtons, list.size());
        trimTexts(titleTexts, list.size());
        trimTexts(subtitleTexts, list.size());
    }

    private void renderVersionTexts(ScrollCanvas canvas, Version version, int index) {
        if (version.modificationInstallersAvailable()) modificationButtons.get(index).render(canvas);
        if (canDelete(version)) deleteButtons.get(index).render(canvas);
        titleTexts.get(index).render(canvas);
        subtitleTexts.get(index).render(canvas);
    }

    private void drawVersionRow(PixelSurface surface, int x, int y, int width, int height,
                                PixelButton.State state, Version version) {
        if (store.isSelectedVersion(version)) drawSelectedVersionButton(surface, x, y, width, height);
        else drawVersionButton(surface, x, y, width, height, state);
    }

    private void drawVersionButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
    }

    private void drawSelectedVersionButton(PixelSurface surface, int x, int y, int width, int height) {
        PixelPainter.drawSelectedElement(surface, x, y, width, height, store.theme());
    }

    private void drawDeleteButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        Color color = state == PixelButton.State.HOVERED ? Palette.DANGER.color(store.theme()) : Palette.OUTLINE.color(store.theme());
        PixelPainter.drawDeleteIcon(surface, x, y, color);
    }

    private void drawModificationButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawMenuIcon(surface, x, y, Palette.OUTLINE.color(store.theme()));
        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawMenuIcon(surface, x, y, animation);
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            refreshLanguageTexts();
            versionList.markDirty();
            host.repaint();
        } else if (event == LauncherStore.Event.VERSIONS_CHANGED) {
            versionList.markDirty();
            host.repaint();
        } else if (event == LauncherStore.Event.FILTERS_CHANGED) {
            syncFiltersFromState();
            syncListLayout();
            versionList.markDirty();
            host.repaint();
        } else if (event == LauncherStore.Event.SELECTED_VERSION_CHANGED) {
            host.repaint();
        }
    }

    private void refreshLanguageTexts() {
        releasesText.setText(I18n.text("versions.releases"));
        snapshotsText.setText(I18n.text("versions.snapshots"));
        legacyText.setText(I18n.text("versions.legacy"));
        modpacksText.setText(I18n.text("versions.modpacks"));
        dropTextLine1.setText(I18n.text("versions.dropModpackLine1"));
        dropTextLine2.setText(I18n.text("versions.dropModpackLine2"));
    }

    private void syncFiltersFromState() {
        releasesCheckBox.setValue(store.releaseFilter());
        snapshotsCheckBox.setValue(store.snapshotFilter());
        legacyCheckBox.setValue(store.legacyFilter());
        modpacksCheckBox.setValue(store.modpackFilter());
    }

    private void syncListLayout() {
        boolean compact = store.modpackFilter();
        if (compactList == compact) return;
        compactList = compact;
        versionList.setBounds(LIST_X, LIST_Y, LIST_WIDTH,
                compact ? LIST_HEIGHT_WITH_DROP_FIELD : LIST_HEIGHT);
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        syncListLayout();
        List<Version> versionsBeforeFilters = store.versions();
        boolean dirty = releasesCheckBox.handleInput(mouse);
        dirty |= snapshotsCheckBox.handleInput(mouse);
        dirty |= legacyCheckBox.handleInput(mouse);
        dirty |= modpacksCheckBox.handleInput(mouse);

        if (versionsBeforeFilters != store.versions()) {
            syncListLayout();
            versionList.markDirty();
            return true;
        }

        dirty |= versionList.handleInput(mouse, false);
        syncVisibleTextAnimations();
        List<Version> versions = store.versions();
        for (int index = 0; index < versions.size(); index++) {
            Version version = versions.get(index);
            if (version.modificationInstallersAvailable()) dirty |= modificationButtons.get(index).handleInput(mouse, versionList.scrollView());
            if (canDelete(version)) dirty |= deleteButtons.get(index).handleInput(mouse, versionList.scrollView());
        }
        dirty |= versionList.handleRowsInput(mouse);

        for (int index = 0; index < versions.size(); index++) {
            Version version = versions.get(index);
            if (version.modificationInstallersAvailable() && modificationButtons.get(index).consumeClick()) {
                versionList.consumeClickedIndex();
                mainHost.openModificationPopup(version);
                return true;
            }
        }

        for (int index = 0; index < versions.size(); index++) {
            Version version = versions.get(index);
            if (canDelete(version) && deleteButtons.get(index).consumeClick()) {
                if (requiresDeleteConfirmation(version)) mainHost.openDeleteConfirmationPopup(version);
                else store.deleteVersion(version);
                return true;
            }
        }

        int clicked = versionList.consumeClickedIndex();
        if (clicked >= 0 && clicked < store.versions().size()) {
            store.selectVersion(clicked);
            return true;
        }

        return dirty;
    }

    @Override
    protected void disposePopupContent() {
        if (host.getTransferHandler() == modpackDropHandler) host.setTransferHandler(previousTransferHandler);
        store.unsubscribe(storeListener);
        releasesText.dispose();
        snapshotsText.dispose();
        legacyText.dispose();
        modpacksText.dispose();
        dropTextLine1.dispose();
        dropTextLine2.dispose();

        for (Text text : titleTexts) text.dispose();
        for (Text text : subtitleTexts) text.dispose();
        deleteButtons.clear();
        modificationButtons.clear();
        titleTexts.clear();
        subtitleTexts.clear();
    }

    private Text lineText(int x, int y, int width, String key, Text.LineAlignment alignment) {
        return new Text(host, x, y, width, I18n.text(key),
                Text.DisplayType.LINE, alignment, Palette.SUBTITLE, 1, 0);
    }

    private void syncText(List<Text> texts, int index, int y, int width, String value,
                          Palette color, Text.LineAlignment alignment, boolean animated) {
        if (index >= texts.size()) {
            Text text = new Text(host, 5, y, width, value,
                    Text.DisplayType.LINE, alignment, color, 1, 0
            );
            text.setAnimationEnabled(animated);
            texts.add(text);
            return;
        }

        Text text = texts.get(index);
        text.setPosition(5, y);
        text.setBoxWidth(width);
        text.setText(value);
        text.setAnimationEnabled(animated);
    }

    private void syncVisibleTextAnimations() {
        if (!contentAnimationsEnabled) return;

        int start = versionList.firstVisibleIndex();
        int end = versionList.lastVisibleIndexExclusive();
        if (start == animatedStart && end == animatedEnd) return;

        if (animatedStart >= 0 && animatedEnd >= 0) {
            for (int index = animatedStart; index < animatedEnd; index++) {
                if (index >= start && index < end) continue;
                setVersionTextAnimation(index, false);
            }
        }

        for (int index = start; index < end; index++) setVersionTextAnimation(index, true);
        animatedStart = start;
        animatedEnd = end;
    }

    private void disableVersionTextAnimations() {
        for (Text text : titleTexts) text.setAnimationEnabled(false);
        for (Text text : subtitleTexts) text.setAnimationEnabled(false);
        animatedStart = -1;
        animatedEnd = -1;
    }

    private void setVersionTextAnimation(int index, boolean enabled) {
        setTextAnimation(titleTexts, index, enabled);
        setTextAnimation(subtitleTexts, index, enabled);
    }

    private void setTextAnimation(List<Text> texts, int index, boolean enabled) {
        if (index < 0 || index >= texts.size()) return;
        texts.get(index).setAnimationEnabled(enabled);
    }

    private boolean insideDrop(Point point) {
        int scale = Math.max(1, store.scale());
        int x = point.x / scale;
        int y = point.y / scale;
        return x >= DROP_X && x < DROP_X + DROP_WIDTH && y >= DROP_Y && y < DROP_Y + DROP_HEIGHT;
    }

    private static boolean isMrpack(File file) {
        return file != null && file.isFile()
                && file.getName().toLowerCase(Locale.ROOT).endsWith(".mrpack");
    }

    private static boolean canDelete(Version version) {
        return version != null && (version.modpack() || version.loaded() || version.custom());
    }

    private boolean requiresDeleteConfirmation(Version version) {
        return version != null && (version.modpack() || store.instancesStorageSystem());
    }

    private static void trimButtons(List<PixelButton> buttons, int count) {
        while (buttons.size() > count) buttons.removeLast();
    }

    private static void trimTexts(List<Text> texts, int count) {
        while (texts.size() > count) texts.removeLast().dispose();
    }

    private final class ModpackDropHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            if (!store.modpackFilter()) return false;
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
                    if (item instanceof File file && isMrpack(file)) {
                        store.importModpack(file.toPath(), mainHost::openErrorPopup);
                        return true;
                    }
                }
            } catch (Exception _) {
            }
            return false;
        }
    }
}
