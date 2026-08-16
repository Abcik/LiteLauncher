package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.platform.BrowserLinks;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.checkbox.CheckBox;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.skin.DefaultPlayerSkin;
import net.litelauncher.frontend.modules.skin.SkinModelView;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import java.awt.Graphics2D;
import java.util.Objects;

public final class OfflineAppearanceScene extends PopupScene {

    private static final int MODEL_X = 175;
    private static final int MODEL_Y = 88;
    private static final int MODEL_W = 90;
    private static final int MODEL_H = 160;
    private static final int MODEL_INSET = 4;
    private static final int CONTROLS_X = 130;
    private static final int CONTROLS_Y = 252;
    private static final int CONTROLS_W = 180;
    private static final int CONTROLS_H = 39;

    private final LauncherStore store = LauncherStore.get();
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;
    private final MainScene main;
    private final String profileId;
    private final CheckBox elyByCheckBox;
    private final Text elyByText;
    private final Text changeSkinText;
    private final PixelButton changeSkinButton;

    private Profile profile;
    private SkinModelView modelView;
    private boolean syncingCheckBox;

    public OfflineAppearanceScene(MainScene host, Profile profile) {
        super(host, "popup.appearance.title", "popup.appearance.subtitle");
        this.main = host;
        this.profile = profile;
        this.profileId = profile == null ? null : profile.id();

        elyByCheckBox = new CheckBox(host, 135, 257, profile != null && profile.elyBy());
        elyByCheckBox.setChangeAction(this::setElyByEnabled);
        elyByText = new Text(host, 148, 255, 157, I18n.text("appearance.useElyBy"),
                Text.DisplayType.LINE, Text.LineAlignment.LEFT, Palette.SUBTITLE, 1, 0);
        changeSkinText = new Text(host, 139, 272, 162, I18n.text("appearance.changeSkinOnElyBy"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.ACCENT_TITLE, 1, 0);
        changeSkinButton = new PixelButton(host, 135, 270, 170, 16, this::drawChangeSkinButton);

        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
    }

    @Override public boolean disposeOnClose() {
        return true;
    }

    @Override public void onOpen() {
        super.onOpen();
        DefaultPlayerSkin.Skin skin = store.offlineSkin(profile);
        modelView = new SkinModelView(host, MODEL_X + MODEL_INSET, MODEL_Y + MODEL_INSET,
                MODEL_W - MODEL_INSET * 2, MODEL_H - MODEL_INSET * 2,
                skin.image(), skin.slim(), null);
        modelView.setInputBounds(MODEL_X, MODEL_Y, MODEL_W, MODEL_H);
        store.refreshOfflineSkin(profile);
        host.repaint();
    }

    @Override public void onClose() {
        disposeModelView();
        super.onClose();
    }

    @Override protected void setPopupContentAnimationsEnabled(boolean enabled) {
        elyByText.setAnimationEnabled(enabled);
        changeSkinText.setAnimationEnabled(enabled);
    }

    @Override protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        PixelSurface surface = PixelPainter.direct(graphics);
        PixelPainter.drawElement(surface, MODEL_X, MODEL_Y, MODEL_W, MODEL_H,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));
        PixelPainter.drawElement(surface, CONTROLS_X, CONTROLS_Y, CONTROLS_W, CONTROLS_H,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));

        elyByCheckBox.render(graphics);
        elyByText.render(graphics);
        changeSkinButton.render(graphics);
        changeSkinText.render(graphics);
    }

    @Override public void renderOverlay(Graphics2D graphics, int scale) {
        if (modelView != null) modelView.renderOverlay(graphics, scale);
    }

    @Override public boolean handleInput(MouseState mouse) {
        boolean dirty = false;
        if (modelView != null) dirty |= modelView.handleInput(mouse);
        dirty |= elyByCheckBox.handleInput(mouse);
        dirty |= changeSkinButton.handleInput(mouse);

        if (changeSkinButton.consumeClick()) {
            BrowserLinks.open(BrowserLinks.ELY_BY_LOGIN);
            return true;
        }
        return dirty;
    }

    private void setElyByEnabled() {
        if (!syncingCheckBox) store.setOfflineElyBy(profile, elyByCheckBox.value());
    }

    private void syncCheckBox() {
        syncingCheckBox = true;
        try {
            elyByCheckBox.setValue(profile != null && profile.elyBy());
        } finally {
            syncingCheckBox = false;
        }
    }

    private Profile currentProfile() {
        for (Profile item : store.profiles()) {
            if (item != null && Objects.equals(profileId, item.id())) return item;
        }
        return null;
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            elyByText.setText(I18n.text("appearance.useElyBy"));
            changeSkinText.setText(I18n.text("appearance.changeSkinOnElyBy"));
            host.repaint();
        } else if (event == LauncherStore.Event.THEME_CHANGED || event == LauncherStore.Event.SCALE_CHANGED) {
            host.repaint();
        } else if (event == LauncherStore.Event.PROFILES_CHANGED) {
            Profile updated = currentProfile();
            if (updated == null || updated.microsoft()) {
                main.closePopup();
                return;
            }
            profile = updated;
            syncCheckBox();
            updateModelSkin();
            host.repaint();
        } else if (event == LauncherStore.Event.PROFILE_APPEARANCE_CHANGED) {
            updateModelSkin();
            host.repaint();
        }
    }

    private void updateModelSkin() {
        if (modelView == null || profile == null) return;
        DefaultPlayerSkin.Skin skin = store.offlineSkin(profile);
        modelView.setSkin(skin.image(), skin.slim());
    }

    private void disposeModelView() {
        if (modelView == null) return;
        modelView.dispose();
        modelView = null;
        host.repaint();
    }

    private void drawChangeSkinButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawAccentButton(surface, x, y, width, height, state, store.theme());
    }

    @Override protected void disposePopupContent() {
        store.unsubscribe(storeListener);
        disposeModelView();
        elyByText.dispose();
        changeSkinText.dispose();
    }
}
