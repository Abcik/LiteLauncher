package net.litelauncher.frontend.scenes;

import net.litelauncher.backend.platform.LauncherPaths;
import net.litelauncher.frontend.Palette;
import net.litelauncher.LauncherStore;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.version.Version;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.Theme;
import net.litelauncher.frontend.Utils;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.loading.LoadingProgress;
import net.litelauncher.frontend.modules.overlay.PopupContent;
import net.litelauncher.frontend.modules.overlay.PopupLayer;
import net.litelauncher.frontend.modules.render.PixelCanvas;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MainScene extends PixelCanvas {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int HEADER_DRAG_HEIGHT = 36;

    private final LauncherStore store = LauncherStore.get();
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;

    private final Text logoText;
    private final PopupLayer popupLayer;
    private ParametersScene parametersScene;
    private ThemeCustomizationScene themeCustomizationScene;
    private LanguageScene languageScene;
    private ProfilesScene profilesScene;
    private VersionsScene versionsScene;

    private final PixelButton closeButton;
    private final PixelButton minimizeButton;
    private final PixelButton parametersButton;
    private final PixelButton languageButton;
    private final PixelButton folderButton;
    private final PixelButton profileButton;
    private final PixelButton themeButton;
    private final PixelButton playButton;
    private final PixelButton versionsButton;
    private final Text profileText;
    private final Text versionText;
    private final Text playText;
    private final Text launcherVersionText;
    private final LoadingProgress loadingProgress;

    public MainScene() {
        super(LauncherState.LOGICAL_WIDTH, LauncherState.LOGICAL_HEIGHT, LauncherStore.get().scale());

        logoText = new Text(this, 34, 11, 90, "LiteLauncher", Text.DisplayType.LINE, Text.LineAlignment.LEFT, Palette.TITLE, 1, 0);

        closeButton = new PixelButton(this, 414, 10, 16, 16, this::drawCloseButton);
        minimizeButton = new PixelButton(this, 394, 10, 16, 16, this::drawMinimizeButton);
        parametersButton = new PixelButton(this, 374, 10, 16, 16, this::drawParametersButton);
        languageButton = new PixelButton(this, 334, 10, 16, 16, this::drawLanguageButton);
        folderButton = new PixelButton(this, 354, 10, 16, 16, this::drawFolderButton);
        profileButton = new PixelButton(this, 294, 10, 16, 16, this::drawProfileButton);
        themeButton = new PixelButton(this, 314, 10, 16, 16, this::drawThemeButton);
        playButton = new PixelButton(this, 150, 240, 140, 32, this::drawPlayButton);
        versionsButton = new PixelButton(this, 166, 278, 108, 16, this::drawVersionsButton);

        popupLayer = new PopupLayer(this, this::drawBackground);

        profileText = new Text(this, 210, 12, 80, profileTitle(), Text.DisplayType.LINE, Text.LineAlignment.RIGHT, Palette.TITLE, 1, 0);
        versionText = new Text(this, 171, 280, 100, versionTitle(), Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.SUBTITLE, 1, 0);
        playText = new Text(this, 158, 244, 124, I18n.text("main.play"), Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.ACCENT_TITLE, 2, 0);
        launcherVersionText = new Text(this, 382, 310, 48, LauncherState.LAUNCHER_VERSION, Text.DisplayType.LINE, Text.LineAlignment.RIGHT, Palette.ELEMENT_BACKGROUND, 1, 0);
        loadingProgress = new LoadingProgress(this);

        store.subscribe(storeListener);
    }

    @Override
    protected void render(PixelGraphics graphics, MouseState mouse) {
        buildScene(graphics, mouse, store.theme());
    }

    @Override
    protected void renderOverlay(Graphics2D graphics, MouseState mouse, int scale) {
        popupLayer.renderOverlay(graphics, scale);
    }

    @Override
    protected boolean handleInput(MouseState mouse) {
        syncLaunchState();
        if (popupLayer.isOpen()) return popupLayer.handleInput(mouse);

        boolean dirty = handleButtons(mouse,
                closeButton,
                minimizeButton,
                parametersButton,
                languageButton,
                folderButton,
                profileButton,
                themeButton,
                playButton,
                versionsButton
        );

        if (closeButton.consumeClick()) {
            closeButton.reset();
            closeWindow();
            return true;
        }

        if (minimizeButton.consumeClick()) {
            minimizeButton.reset();
            minimizeWindow();
            return true;
        }

        if (parametersButton.consumeClick()) {
            ParametersScene scene = parametersScene();
            scene.setScrollOffset(0);
            openPopup(scene, mouse);
            return true;
        }

        if (languageButton.consumeClick()) {
            openPopup(languageScene(), mouse);
            return true;
        }

        if (folderButton.consumeClick()) {
            folderButton.reset();
            openMinecraftFolder();
            return true;
        }

        if (profileButton.consumeClick()) {
            openPopup(profilesScene(), mouse);
            return true;
        }

        if (themeButton.consumeClick()) {
            themeButton.reset();
            toggleTheme();
            return true;
        }

        if (playButton.consumeClick()) {
            playButton.reset();
            if (store.launchBusy()) store.cancelLaunch();
            else store.launchSelectedGame(this::openErrorPopup);
            return true;
        }

        if (versionsButton.consumeClick()) {
            openPopup(versionsScene(), mouse);
            return true;
        }

        return dirty;
    }


    private static boolean handleButtons(MouseState mouse, PixelButton... buttons) {
        boolean dirty = false;
        for (PixelButton button : buttons) dirty |= button.handleInput(mouse);
        return dirty;
    }

    @Override
    protected boolean canStartWindowDrag(int logicalX, int logicalY) {
        return logicalY >= 0
                && logicalY < HEADER_DRAG_HEIGHT
                && !headerButtonAt(logicalX, logicalY);
    }

    private boolean headerButtonAt(int x, int y) {
        return closeButton.contains(x, y)
                || minimizeButton.contains(x, y)
                || parametersButton.contains(x, y)
                || languageButton.contains(x, y)
                || folderButton.contains(x, y)
                || profileButton.contains(x, y)
                || themeButton.contains(x, y);
    }

    public void showLoading(double progress, String actionText, String detailsText) {
        loadingProgress.setProgress(progress);
        loadingProgress.setActionText(actionText);
        loadingProgress.setDetailsText(detailsText);
        loadingProgress.setVisible(true);
    }

    public void hideLoading() {
        loadingProgress.setVisible(false);
    }

    public void refreshFromState() {
        profileText.setText(profileTitle());
        versionText.setText(versionTitle());
        repaint();
    }

    public void disposeScene() {
        store.unsubscribe(storeListener);
        popupLayer.dispose();
        dispose(parametersScene);
        dispose(themeCustomizationScene);
        dispose(languageScene);
        dispose(profilesScene);
        dispose(versionsScene);

        logoText.dispose();
        profileText.dispose();
        versionText.dispose();
        playText.dispose();
        launcherVersionText.dispose();
        loadingProgress.dispose();
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        switch (event) {
            case SELECTED_PROFILE_CHANGED, SELECTED_VERSION_CHANGED -> refreshFromState();
            case LANGUAGE_CHANGED -> refreshLanguage();
            case THEME_CHANGED, THEME_COLORS_CHANGED -> refreshTheme();
            case LAUNCH_PROGRESS_CHANGED, GAME_STATUS_CHANGED  -> {
                syncLaunchState();
                repaint();
            }
            default -> repaint();
        }
    }

    private void refreshLanguage() {
        syncLaunchState();
        refreshFromState();
    }

    private void refreshTheme() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) window.setBackground(Palette.OUTLINE.color(store.theme()));
        repaint();
    }

    private String profileTitle() {
        Profile profile = store.selectedProfile();
        return profile == null ? I18n.text("main.signIn") : profile.username();
    }

    private String versionTitle() {
        Version version = store.selectedVersion();
        return version == null ? "" : version.title();
    }

    private void openPopup(PopupContent content, MouseState mouse) {
        popupLayer.open(content, () -> onPopupOpened(mouse));
    }

    public void openParametersPopup(int scrollOffset) {
        ParametersScene scene = parametersScene();
        scene.setScrollOffset(scrollOffset);
        popupLayer.open(scene, this::resetMainInteraction);
    }

    public void openThemeCustomizationPopup() {
        popupLayer.open(themeCustomizationScene(), this::resetMainInteraction);
    }

    public void openAppearancePopup(Profile profile) {
        if (profile == null) return;
        PopupContent scene = profile.microsoft()
                ? new AppearanceScene(this, profile)
                : new OfflineAppearanceScene(this, profile);
        popupLayer.open(scene, this::resetMainInteraction);
    }

    public void openModificationPopup(Version version) {
        if (version == null) return;
        popupLayer.open(new ModificationScene(this, version), this::resetMainInteraction);
    }

    public void openVersionsPopup() {
        popupLayer.open(versionsScene(), this::resetMainInteraction);
    }

    public void openDeleteConfirmationPopup(Version version) {
        if (version == null) return;
        popupLayer.open(new DeleteConfirmationScene(this, version), this::resetMainInteraction);
    }

    public void closePopup() {
        popupLayer.close();
    }

    public void openErrorPopup(String message) {
        popupLayer.open(new InformationScene(this, "common.error", message), this::resetMainInteraction);
    }

    private ParametersScene parametersScene() {
        if (parametersScene == null) parametersScene = new ParametersScene(this);
        return parametersScene;
    }

    private ThemeCustomizationScene themeCustomizationScene() {
        if (themeCustomizationScene == null) themeCustomizationScene = new ThemeCustomizationScene(this);
        return themeCustomizationScene;
    }

    private LanguageScene languageScene() {
        if (languageScene == null) languageScene = new LanguageScene(this);
        return languageScene;
    }

    private ProfilesScene profilesScene() {
        if (profilesScene == null) profilesScene = new ProfilesScene(this);
        return profilesScene;
    }

    private VersionsScene versionsScene() {
        if (versionsScene == null) versionsScene = new VersionsScene(this);
        return versionsScene;
    }

    private void dispose(PopupContent content) {
        if (content != null) content.dispose();
    }

    private void onPopupOpened(MouseState mouse) {
        resetMainInteraction();
        if (mouse != null) {
            mouse.clearCursorRequest();
            mouse.consume();
        }
    }

    private void resetMainInteraction() {
        closeButton.reset();
        minimizeButton.reset();
        parametersButton.reset();
        languageButton.reset();
        folderButton.reset();
        profileButton.reset();
        themeButton.reset();
        playButton.reset();
        versionsButton.reset();
    }

    private void syncLaunchState() {
        boolean busy = store.launchBusy();
        playText.setText(I18n.text(busy ? "main.cancel" : "main.play"));
        playButton.setEnabled(!store.launchControlLocked());
        versionsButton.setEnabled(!busy);
        if (busy) showLoading(store.launchProgress(), store.launchActionText(), store.launchDetailsText());
        else hideLoading();
    }

    private void buildScene(PixelGraphics graphics, MouseState mouse, Theme theme) {
        syncLaunchState();
        drawBackground(graphics, Palette.GENERAL_BACKGROUND.color(theme));
        drawOutlines(graphics, Palette.OUTLINE.color(theme));

        graphics.image(10, 9, 27, 26, Utils.getLocalIcon("logo", theme));

        profileButton.render(graphics);
        themeButton.render(graphics);
        languageButton.render(graphics);
        folderButton.render(graphics);
        parametersButton.render(graphics);
        minimizeButton.render(graphics);
        closeButton.render(graphics);

        graphics.image(80, 60, 359, 215, Utils.getLocalIcon("illustration"));

        playButton.render(graphics);
        versionsButton.render(graphics);

        logoText.render(graphics);
        profileText.render(graphics);
        versionText.render(graphics);
        playText.render(graphics);
        launcherVersionText.render(graphics);
        if (store.gameRunning()) PixelPainter.drawGameRunningIndicator(PixelPainter.direct(graphics), 10, 314, store.theme());
        if (loadingProgress.isVisible()) loadingProgress.render(graphics);
        popupLayer.render(graphics, mouse);
    }

    private void toggleTheme() {
        store.setTheme(store.theme() == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }

    private void openMinecraftFolder() {
        try {
            Path directory = LauncherPaths.minecraftDirectory();
            Files.createDirectories(directory);
            OSUtils.openFile(directory);
        } catch (Exception exception) {
            LauncherLog.error("Unable to open Minecraft directory.", exception);
            openErrorPopup("Unable to open .minecraft folder.");
        }
    }

    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) return;
        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }

    private void minimizeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Frame frame) frame.setState(Frame.ICONIFIED);
    }

    private void drawOutlines(PixelGraphics graphics, Color color) {
        graphics.paint(10, 34, 429, 35, color);
    }

    private void drawBackground(PixelGraphics graphics, Color color) {
        PixelPainter.drawWindowBackground(graphics, color);
    }

    private void drawBackground(PixelGraphics graphics, Theme theme) {
        drawBackground(graphics, Palette.POPUP_FOGGING.color(theme));
    }

    private void drawProfileButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        BufferedImage avatar = store.selectedProfileAvatar();
        if (avatar == null) surface.image(x, y, x + width - 1, y + height - 1, Utils.getLocalIcon("profile"));
        else surface.image(x, y, avatar);

        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawProfileOverlay(surface, x, y, animation);
    }

    private void drawThemeButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawThemeIcon(surface, x, y, Palette.OUTLINE.color(store.theme()), store.theme());
        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawThemeIcon(surface, x, y, animation, store.theme());
    }

    private void drawLanguageButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawLanguageIcon(surface, x, y, Palette.OUTLINE.color(store.theme()));
        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawLanguageIcon(surface, x, y, animation);
    }

    private void drawFolderButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawFolderIcon(surface, x, y, Palette.OUTLINE.color(store.theme()));
        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawFolderIcon(surface, x, y, animation);
    }

    private void drawParametersButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawParametersIcon(surface, x, y, Palette.OUTLINE.color(store.theme()));
        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawParametersIcon(surface, x, y, animation);
    }

    private void drawMinimizeButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawMinimizeIcon(surface, x, y, Palette.OUTLINE.color(store.theme()));
        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawMinimizeIcon(surface, x, y, animation);
    }

    private void drawCloseButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawCloseIcon(surface, x, y, Palette.OUTLINE.color(store.theme()));
        if (state == PixelButton.State.HOVERED || state == PixelButton.State.PRESSED) {
            PixelPainter.drawCloseIcon(surface, x, y, Palette.DANGER.color(store.theme()));
        }
    }

    private void drawPlayButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        if (store.launchBusy()) PixelPainter.drawDangerButton(surface, x, y, width, height, state, store.theme());
        else PixelPainter.drawPlayButton(surface, x, y, width, height, state, store.theme());
    }

    private void drawVersionsButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
    }

}
