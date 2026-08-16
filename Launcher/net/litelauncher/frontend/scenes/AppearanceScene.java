package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.auth.MinecraftCape;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.auth.SkinImageUtils;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.ScrollCanvas;
import net.litelauncher.frontend.modules.scroll.ScrollableList;
import net.litelauncher.frontend.modules.skin.SkinModelView;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import javax.imageio.ImageIO;
import javax.swing.TransferHandler;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serial;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class AppearanceScene extends PopupScene {

    private static final int MODEL_X = 130;
    private static final int MODEL_Y = 88;
    private static final int MODEL_W = 90;
    private static final int MODEL_H = 160;
    private static final int MODEL_INSET = 4;
    private static final int DROP_X = 130;
    private static final int DROP_Y = 252;
    private static final int DROP_W = 180;
    private static final int DROP_H = 39;
    private static final int MAX_SKIN_BYTES = 1024 * 1024;

    private final LauncherStore store = LauncherStore.get();
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;
    private final MainScene main;
    private final String profileId;
    private Profile profile;

    private final ScrollableList<CapeItem> capeList;
    private final Text dropText;
    private final Text formatText;
    private final Text fileText;
    private final Text modelText;
    private final Text applyText;
    private final PixelButton cancelButton;
    private final PixelButton modelLeftButton;
    private final PixelButton modelRightButton;
    private final PixelButton applyButton;

    private final List<Text> capeTexts = new ArrayList<>();
    private SkinModelView modelView;
    private TransferHandler previousTransferHandler;
    private byte[] pendingSkinBytes;
    private BufferedImage pendingSkinImage;
    private boolean pendingSlim;
    private boolean applyingSkin;
    private boolean applyingCape;
    private String selectedCapeId;
    private List<CapeItem> cachedCapeItems = List.of();
    private boolean capeItemsDirty = true;
    private boolean opened;

    public AppearanceScene(MainScene host, Profile profile) {
        super(host, "popup.appearance.title", "popup.appearance.subtitle");
        this.main = host;
        this.profile = profile;
        this.profileId = profile == null ? null : profile.id();
        this.pendingSlim = profile != null && profile.slim();
        this.selectedCapeId = profile == null ? null : profile.activeCapeId();

        capeList = new ScrollableList<>(host, 224, 88, 90, 160, 26, 1, 78, 86,
                new ScrollableList.Source<>() {
                    @Override public int size() { return capeItems().size(); }
                    @Override public CapeItem get(int index) { return capeItems().get(index); }
                },
                (surface, x, y, width, height, state, item, _) ->
                        drawCapeRow(surface, x, y, width, height, state, item),
                this::syncCapeRow,
                this::renderCapeTexts
        );
        capeList.setRowEnabled((item, _) -> !applyingCape && !item.selected());

        dropText = new Text(host, 135, 259, 170, I18n.text("appearance.dropSkin"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);
        formatText = new Text(host, 135, 271, 170, I18n.text("appearance.formatSkin"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);

        fileText = new Text(host, 135, 255, 71, "", Text.DisplayType.LINE, Text.LineAlignment.LEFT, Palette.SUBTITLE, 1, 0);
        modelText = new Text(host, 238, 255, 54, modelName(), Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);
        applyText = new Text(host, 139, 272, 162, I18n.text("appearance.apply"), Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.ACCENT_TITLE, 1, 0);

        cancelButton = new PixelButton(host, 210, 259, 5, 5, this::drawCancelButton);
        modelLeftButton = new PixelButton(host, 225, 257, 9, 9, this::drawModelLeftButton);
        modelRightButton = new PixelButton(host, 296, 257, 9, 9, this::drawModelRightButton);
        applyButton = new PixelButton(host, 135, 270, 170, 16, this::drawApplyButton);

        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
    }

    @Override public boolean disposeOnClose() { return true; }

    @Override public void onOpen() {
        super.onOpen();
        opened = true;
        modelView = new SkinModelView(host, MODEL_X + MODEL_INSET, MODEL_Y + MODEL_INSET,
                MODEL_W - MODEL_INSET * 2, MODEL_H - MODEL_INSET * 2,
                profileSkin(), profile != null && profile.slim(), activeCapeImage());
        modelView.setInputBounds(MODEL_X, MODEL_Y, MODEL_W, MODEL_H);
        previousTransferHandler = host.getTransferHandler();
        host.setTransferHandler(new SkinDropHandler());
        host.repaint();
    }

    @Override public void onClose() {
        opened = false;
        host.setTransferHandler(previousTransferHandler);
        disposeModelView();
        super.onClose();
    }

    @Override protected void setPopupContentAnimationsEnabled(boolean enabled) {
        dropText.setAnimationEnabled(enabled);
        formatText.setAnimationEnabled(enabled);
        fileText.setAnimationEnabled(enabled);
        modelText.setAnimationEnabled(enabled);
        applyText.setAnimationEnabled(enabled);
        for (Text text : capeTexts) text.setAnimationEnabled(enabled);
    }

    @Override protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        PixelSurface surface = PixelPainter.direct(graphics);
        PixelPainter.drawElement(surface, MODEL_X, MODEL_Y, MODEL_W, MODEL_H,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));
        capeList.render(graphics);
        drawDropBlock(graphics);
    }

    @Override public void renderOverlay(Graphics2D graphics, int scale) {
        if (modelView != null) modelView.renderOverlay(graphics, scale);
    }

    private void disposeModelView() {
        if (modelView == null) return;
        modelView.dispose();
        modelView = null;
        host.repaint();
    }

    private void drawDropBlock(PixelGraphics graphics) {
        PixelSurface surface = PixelPainter.direct(graphics);
        PixelPainter.drawElement(surface, DROP_X, DROP_Y, DROP_W, DROP_H,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));

        if (pendingSkinBytes == null) {
            dropText.render(graphics);
            formatText.render(graphics);
            return;
        }

        fileText.render(graphics);
        cancelButton.render(graphics);
        modelLeftButton.render(graphics);
        modelRightButton.render(graphics);
        modelText.render(graphics);
        applyButton.render(graphics);
        applyText.render(graphics);
    }

    @Override public boolean handleInput(MouseState mouse) {
        boolean dirty = false;
        if (modelView != null) dirty |= modelView.handleInput(mouse);
        if (!applyingCape) {
            dirty |= capeList.handleInput(mouse);
            int clicked = capeList.consumeClickedIndex();
            if (clicked >= 0) {
                applyCape(capeItems().get(clicked));
                return true;
            }
        }

        if (pendingSkinBytes != null && !applyingSkin) {
            dirty |= cancelButton.handleInput(mouse);
            dirty |= modelLeftButton.handleInput(mouse);
            dirty |= modelRightButton.handleInput(mouse);
            dirty |= applyButton.handleInput(mouse);

            if (cancelButton.consumeClick()) {
                clearPendingSkin();
                return true;
            }
            if (modelLeftButton.consumeClick() || modelRightButton.consumeClick()) {
                togglePendingModel();
                return true;
            }
            if (applyButton.consumeClick()) {
                applySkin();
                return true;
            }
        }
        return dirty;
    }

    private void syncCapeRow(CapeItem item, int index, int y, int rowWidth, ScrollableList<CapeItem> list) {
        int textX = item.noCape() ? 5 : 19;
        int textWidth = item.noCape() ? rowWidth - 10 : rowWidth - 24;
        syncText(capeTexts, index, textX, y + 7, textWidth, item.name(), Palette.SUBTITLE);
        trimTexts(capeTexts, list.size());
    }

    private void renderCapeTexts(ScrollCanvas canvas, CapeItem item, int index) {
        if (item.icon() != null) canvas.image(5, index * 27 + 5, item.icon());
        capeTexts.get(index).render(canvas);
    }

    private void drawCapeRow(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state, CapeItem item) {
        if (applyingCape) state = PixelButton.State.NORMAL;
        if (item.selected()) PixelPainter.drawSelectedElement(surface, x, y, width, height, store.theme());
        else PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
    }

    private void applyCape(CapeItem item) {
        if (item == null || Objects.equals(selectedCapeId, item.id())) return;
        applyingCape = true;
        selectedCapeId = item.id();
        capeItemsDirty = true;
        if (modelView != null) modelView.setCape(item.noCape() ? null : item.fullImage());
        capeList.markDirty();
        host.repaint();
        store.setMicrosoftCape(profile, item.id(), this::showError, () -> {
            if (!opened) return;
            applyingCape = false;
            profile = currentProfile();
            selectedCapeId = profile == null ? null : profile.activeCapeId();
            capeItemsDirty = true;
            capeList.markDirty();
            host.repaint();
        });
    }

    private void applySkin() {
        applyingSkin = true;
        applyButton.setEnabled(false);
        store.uploadMicrosoftSkin(profile, pendingSkinBytes, pendingSlim, this::showError, () -> {
            if (!opened) return;
            applyingSkin = false;
            applyButton.setEnabled(true);
            clearPendingOnly();
            profile = currentProfile();
            if (modelView != null) modelView.setSkin(profileSkin(), profile != null && profile.slim());
            host.repaint();
        });
    }

    private void showError(String message) {
        if (!opened) return;
        main.openErrorPopup(message == null || message.isBlank()
                ? InformationMessages.text(InformationMessages.SIGN_IN_ERROR)
                : message);
    }

    private void togglePendingModel() {
        pendingSlim = !pendingSlim;
        modelText.setText(modelName());
        if (modelView != null) modelView.setSkin(pendingSkinImage, pendingSlim);
        host.repaint();
    }

    private void setPendingSkin(File file) {
        try {
            if (file == null || !file.getName().toLowerCase().endsWith(".png")) {
                showError(I18n.text("appearance.invalidSkin"));
                return;
            }
            long fileSize = Files.size(file.toPath());
            if (fileSize <= 0 || fileSize > MAX_SKIN_BYTES) {
                showError(I18n.text("appearance.skinTooLarge"));
                return;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null && image.getWidth() == 64 && image.getHeight() == 32) {
                image = SkinImageUtils.convertLegacySkin(image);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                bytes = output.toByteArray();
            }
            if (!validSkin(image)) {
                showError(I18n.text("appearance.invalidSkin"));
                return;
            }
            pendingSkinBytes = bytes;
            pendingSkinImage = image;
            pendingSlim = profile != null && profile.slim();
            fileText.setText(file.getName());
            modelText.setText(modelName());
            if (modelView != null) modelView.setSkin(pendingSkinImage, pendingSlim);
            host.repaint();
        } catch (Exception _) {
            showError(I18n.text("appearance.skinReadError"));
        }
    }

    private void clearPendingSkin() {
        clearPendingOnly();
        if (modelView != null) modelView.setSkin(profileSkin(), profile != null && profile.slim());
        host.repaint();
    }

    private void clearPendingOnly() {
        pendingSkinBytes = null;
        pendingSkinImage = null;
        fileText.setText("");
    }

    private List<CapeItem> capeItems() {
        if (!capeItemsDirty) return cachedCapeItems;

        ArrayList<CapeItem> items = new ArrayList<>();
        String active = selectedCapeId;
        items.add(new CapeItem(null, I18n.text("appearance.noCape"), null, null, true, active == null));
        if (profile != null) {
            for (MinecraftCape cape : profile.capes()) {
                BufferedImage image = image(cape.png());
                items.add(new CapeItem(cape.id(), cape.name(), capeIcon(image), image, false, Objects.equals(active, cape.id())));
            }
        }
        cachedCapeItems = List.copyOf(items);
        capeItemsDirty = false;

        return cachedCapeItems;
    }

    private BufferedImage profileSkin() {
        return image(profile == null ? null : profile.skinPng());
    }

    private BufferedImage activeCapeImage() {
        if (profile == null || profile.activeCape() == null) return null;
        return image(profile.activeCape().png());
    }

    private Profile currentProfile() {
        for (Profile item : store.profiles()) if (item != null && Objects.equals(profileId, item.id())) return item;
        return null;
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            dropText.setText(I18n.text("appearance.dropSkin"));
            formatText.setText(I18n.text("appearance.formatSkin"));
            modelText.setText(modelName());
            applyText.setText(I18n.text("appearance.apply"));
            capeItemsDirty = true;
            capeList.markDirty();
            host.repaint();
        } else if (event == LauncherStore.Event.THEME_CHANGED || event == LauncherStore.Event.SCALE_CHANGED) {
            host.repaint();
        } else if (event == LauncherStore.Event.PROFILES_CHANGED) {
            Profile updated = currentProfile();
            if (updated == null) {
                main.closePopup();
                return;
            }
            profile = updated;
            selectedCapeId = applyingCape ? selectedCapeId : profile.activeCapeId();
            capeItemsDirty = true;
            capeList.markDirty();
            host.repaint();
        }
    }

    @Override protected void disposePopupContent() {
        store.unsubscribe(storeListener);
        disposeModelView();
        dropText.dispose();
        formatText.dispose();
        fileText.dispose();
        modelText.dispose();
        applyText.dispose();
        for (Text text : capeTexts) text.dispose();
        capeTexts.clear();
    }

    private String modelName() {
        return pendingSlim ? I18n.text("appearance.slim") : I18n.text("appearance.classic");
    }

    private void drawCancelButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        Color color = state == PixelButton.State.HOVERED ? Palette.DANGER.color(store.theme()) : Palette.OUTLINE.color(store.theme());
        PixelPainter.drawDeleteIcon(surface, x, y, color);
    }

    private void drawModelLeftButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawSmallAccentButton(surface, x, y, state, store.theme());
        PixelPainter.drawChevronLeft(surface, x + 2, y + 2, Palette.ACCENT_TITLE.color(store.theme()));
    }

    private void drawModelRightButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawSmallAccentButton(surface, x, y, state, store.theme());
        PixelPainter.drawChevronRight(surface, x + 3, y + 2, Palette.ACCENT_TITLE.color(store.theme()));
    }

    private void drawApplyButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawAccentButton(surface, x, y, width, height, state, store.theme());
    }

    private void syncText(List<Text> texts, int index, int x, int y, int width, String value, Palette color) {
        if (index >= texts.size()) {
            texts.add(new Text(host, x, y, width, value, Text.DisplayType.LINE, Text.LineAlignment.LEFT, color, 1, 0));
            return;
        }
        Text text = texts.get(index);
        text.setPosition(x, y);
        text.setBoxWidth(width);
        text.setText(value);
    }

    private static void trimTexts(List<Text> texts, int count) {
        while (texts.size() > count) texts.removeLast().dispose();
    }

    private static boolean validSkin(BufferedImage image) {
        return image != null && image.getWidth() == 64 && image.getHeight() == 64;
    }

    private static BufferedImage image(String base64) {
        try {
            if (base64 == null || base64.isBlank()) return null;
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        } catch (Exception _) {
            return null;
        }
    }

    private static BufferedImage capeIcon(BufferedImage cape) {
        if (cape == null || cape.getWidth() < 11 || cape.getHeight() < 17) return null;
        return cape.getSubimage(1, 1, 10, 16);
    }

    private boolean insideDrop(int logicalX, int logicalY) {
        return logicalX >= DROP_X && logicalX < DROP_X + DROP_W && logicalY >= DROP_Y && logicalY < DROP_Y + DROP_H;
    }

    private record CapeItem(String id, String name, BufferedImage icon, BufferedImage fullImage, boolean noCape, boolean selected) {
    }

    private final class SkinDropHandler extends TransferHandler {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override public boolean canImport(TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false;
            java.awt.Point point = support.getDropLocation().getDropPoint();
            int scale = Math.max(1, store.scale());
            if (!insideDrop(point.x / scale, point.y / scale)) return false;
            support.setDropAction(COPY);
            return true;
        }

        @Override public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                Transferable transferable = support.getTransferable();
                Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (!(data instanceof List<?> files)) return false;
                for (Object item : files) {
                    if (item instanceof File file) {
                        setPendingSkin(file);
                        return pendingSkinBytes != null;
                    }
                }
            } catch (Exception _) {
            }
            return false;
        }
    }
}
