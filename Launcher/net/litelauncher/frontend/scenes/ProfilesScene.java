package net.litelauncher.frontend.scenes;

import net.litelauncher.backend.InformationMessages;
import net.litelauncher.backend.LauncherLog;

import net.litelauncher.LauncherStore;
import net.litelauncher.backend.auth.Profile;
import net.litelauncher.backend.platform.BrowserLinks;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.field.InputField;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.ScrollCanvas;
import net.litelauncher.frontend.modules.scroll.ScrollableList;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ProfilesScene extends PopupScene {

    private final LauncherStore store = LauncherStore.get();
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;
    private final MainScene main;

    private final ScrollableList<Profile> accountList;

    private final InputField offlineInput;
    private final PixelButton microsoftButton;
    private final PixelButton buyMinecraftButton;
    private final Text enterText;
    private final Text microsoftText;
    private final Text emptyAccountText;
    private final Text buyMinecraftText;

    private final List<PixelButton> deleteButtons = new ArrayList<>();
    private final List<PixelButton> appearanceButtons = new ArrayList<>();
    private final List<Text> usernameTexts = new ArrayList<>();
    private final List<Text> typeTexts = new ArrayList<>();

    private boolean opened;
    private boolean authRunning;

    public ProfilesScene(MainScene host) {
        super(host, "popup.profiles.title", "popup.profiles.subtitle");
        this.main = host;

        accountList = new ScrollableList<>(host, 130, 88, 184, 151, 30, 1, 172, 180,
                new ScrollableList.Source<>() {
                    @Override
                    public int size() {
                        return store.profiles().size();
                    }

                    @Override
                    public Profile get(int index) {
                        return store.profiles().get(index);
                    }
                },
                (surface, x, y, width, height, state, profile, _) ->
                        drawAccountRow(surface, x, y, width, height, state, profile),
                this::syncAccountRow,
                (canvas, _, index) -> renderAccountTexts(canvas, index)
        );
        accountList.setRowEnabled((profile, _) -> !store.isSelectedProfile(profile));

        offlineInput = new InputField(host, 139, 250, 162, I18n.text("profiles.offlineUsername"), Palette.OUTLINE,
                Text.LineAlignment.LEFT, 16, Palette.SUBTITLE, Palette.GENERAL_BACKGROUND,
                Palette.ACCENT, Text.LineAlignment.LEFT, 1, 0
        );
        offlineInput.setCharacterFilter(Profile::isOfflineUsernameCharacter);
        offlineInput.setEnterAction(this::addOfflineAccount);

        microsoftButton = new PixelButton(host, 135, 268, 170, 18, this::drawMicrosoftButton);
        buyMinecraftButton = new PixelButton(host, 151, 160, 138, 18, this::drawMicrosoftButton);

        enterText = new Text(host, 268, 249, 28, I18n.text("profiles.enter"),
                Text.DisplayType.LINE, Text.LineAlignment.RIGHT,
                Palette.OUTLINE, 1, 0
        );

        microsoftText = new Text(host, 140, 271, 160, I18n.text("profiles.microsoftSignIn"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER,
                Palette.ACCENT_TITLE, 1, 0
        );

        emptyAccountText = new Text(host, 151, 141, 138, I18n.text("profiles.emptyAccount"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER,
                Palette.TITLE, 1, 0
        );

        buyMinecraftText = new Text(host, 152, 163, 138, I18n.text("profiles.buyMinecraft"),
                Text.DisplayType.LINE, Text.LineAlignment.CENTER,
                Palette.ACCENT_TITLE, 1, 0
        );

        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        enterText.setAnimationEnabled(enabled);
        microsoftText.setAnimationEnabled(enabled);
        emptyAccountText.setAnimationEnabled(enabled);
        buyMinecraftText.setAnimationEnabled(enabled);
        offlineInput.setAnimationEnabled(enabled);
        for (Text text : usernameTexts) text.setAnimationEnabled(enabled);
        for (Text text : typeTexts) text.setAnimationEnabled(enabled);
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        accountList.render(graphics);
        drawEmptyAccountBlock(graphics);
        drawBottomControls(graphics);
    }

    private void drawBottomControls(PixelGraphics graphics) {
        PixelSurface surface = PixelPainter.direct(graphics);

        PixelPainter.drawElement(surface, 130, 243, 180, 48,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));
        PixelPainter.drawElement(surface, 135, 248, 170, 16,
                Palette.ELEMENT_BACKGROUND.color(store.theme()), Palette.OUTLINE.color(store.theme()));
        PixelPainter.drawEnterArrow(surface, 294, 254, Palette.OUTLINE.color(store.theme()));

        offlineInput.render(graphics);
        microsoftButton.render(graphics);
        enterText.render(graphics);
        microsoftText.render(graphics);
    }

    private void drawEmptyAccountBlock(PixelGraphics graphics) {
        if (!store.profiles().isEmpty()) return;

        buyMinecraftButton.render(graphics);
        emptyAccountText.render(graphics);
        buyMinecraftText.render(graphics);
    }

    private void syncAccountRow(Profile account, int index, int y, int rowWidth,
                                ScrollableList<Profile> list) {
        int textWidth = list.rowWidth(152, 160);

        if (index >= deleteButtons.size()) {
            deleteButtons.add(new PixelButton(host, rowWidth - 11, y + 5, 5, 5, this::drawDeleteButton));
        } else {
            deleteButtons.get(index).setBounds(rowWidth - 11, y + 5, 5, 5);
        }

        if (index >= appearanceButtons.size()) {
            appearanceButtons.add(new PixelButton(host, rowWidth - 20, y + 5, 5, 5, this::drawAppearanceButton));
        } else {
            appearanceButtons.get(index).setBounds(rowWidth - 20, y + 5, 5, 5);
        }

        syncText(usernameTexts, index, y + 2, textWidth, account.username(), Palette.TITLE);
        syncText(typeTexts, index, y + 15, textWidth,
                account.microsoft() ? I18n.text("profiles.microsoftAccount") : I18n.text("profiles.offlineAccount"), Palette.SUBTITLE);

        trimButtons(deleteButtons, list.size());
        trimButtons(appearanceButtons, list.size());
        trimTexts(usernameTexts, list.size());
        trimTexts(typeTexts, list.size());
    }

    private void renderAccountTexts(ScrollCanvas canvas, int index) {
        appearanceButtons.get(index).render(canvas);
        deleteButtons.get(index).render(canvas);
        usernameTexts.get(index).render(canvas);
        typeTexts.get(index).render(canvas);
    }

    private void drawAccountRow(PixelSurface surface, int x, int y, int width, int height,
                                PixelButton.State state, Profile account) {
        if (store.isSelectedProfile(account)) drawSelectedAccountButton(surface, x, y, width, height);
        else drawAccountButton(surface, x, y, width, height, state);
    }

    private void drawAccountButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawStateElement(surface, x, y, width, height, state, store.theme());
    }

    private void drawSelectedAccountButton(PixelSurface surface, int x, int y, int width, int height) {
        PixelPainter.drawSelectedElement(surface, x, y, width, height, store.theme());
    }

    private void drawDeleteButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        Color color = state == PixelButton.State.HOVERED ? Palette.DANGER.color(store.theme()) : Palette.OUTLINE.color(store.theme());
        PixelPainter.drawDeleteIcon(surface, x, y, color);
    }

    private void drawAppearanceButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawMenuIcon(surface, x, y, Palette.OUTLINE.color(store.theme()));
        Color animation = PixelPainter.defaultStateColor(state, store.theme());
        if (animation != null) PixelPainter.drawMenuIcon(surface, x, y, animation);
    }

    private void drawMicrosoftButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawAccentButton(surface, x, y, width, height, state, store.theme());
    }

    private void addOfflineAccount() {
        String username = offlineInput.getValue().trim();
        if (username.length() < 3) return;

        offlineInput.setValue("");
        offlineInput.blur();
        store.addProfile(Profile.offline(username));
    }

    private void signInWithMicrosoft() {
        if (authRunning) return;
        authRunning = true;
        store.signInWithMicrosoft(this::showAuthError);
    }

    private void showAuthError(String message) {
        if (!opened) return;
        authRunning = false;
        main.closePopup();
        main.openErrorPopup(message == null || message.isBlank() ? InformationMessages.text(InformationMessages.SIGN_IN_ERROR) : message);
    }

    private void buyMinecraft() {
        BrowserLinks.open(BrowserLinks.BUY_MINECRAFT);
    }


    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            refreshLanguageTexts();
            accountList.markDirty();
            host.repaint();
        } else if (event == LauncherStore.Event.PROFILES_CHANGED) {
            authRunning = false;
            accountList.markDirty();
            host.repaint();
        } else if (event == LauncherStore.Event.SELECTED_PROFILE_CHANGED) {
            host.repaint();
        }
    }


    private void refreshLanguageTexts() {
        offlineInput.setPlaceholder(I18n.text("profiles.offlineUsername"));
        enterText.setText(I18n.text("profiles.enter"));
        microsoftText.setText(I18n.text("profiles.microsoftSignIn"));
        emptyAccountText.setText(I18n.text("profiles.emptyAccount"));
        buyMinecraftText.setText(I18n.text("profiles.buyMinecraft"));
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        boolean dirty = accountList.handleInput(mouse, false);

        int count = store.profiles().size();
        for (int index = 0; index < count; index++) {
            dirty |= appearanceButtons.get(index).handleInput(mouse, accountList.scrollView());
            dirty |= deleteButtons.get(index).handleInput(mouse, accountList.scrollView());
        }
        dirty |= accountList.handleRowsInput(mouse);

        if (store.profiles().isEmpty()) dirty |= buyMinecraftButton.handleInput(mouse);

        dirty |= offlineInput.handleInput(mouse);
        if (!authRunning) dirty |= microsoftButton.handleInput(mouse);

        for (int index = 0; index < count; index++) {
            Profile profile = store.profiles().get(index);
            if (appearanceButtons.get(index).consumeClick()) {
                accountList.consumeClickedIndex();
                main.openAppearancePopup(profile);
                return true;
            }
        }

        for (int index = 0; index < count; index++) {
            if (deleteButtons.get(index).consumeClick()) {
                store.deleteProfile(index);
                return true;
            }
        }

        int clicked = accountList.consumeClickedIndex();
        if (clicked >= 0 && clicked < store.profiles().size()) {
            store.selectProfile(clicked);
            return true;
        }

        if (store.profiles().isEmpty() && buyMinecraftButton.consumeClick()) {
            buyMinecraft();
            return true;
        }

        if (!authRunning && microsoftButton.consumeClick()) {
            signInWithMicrosoft();
            return true;
        }

        return dirty;
    }

    @Override
    public void onOpen() {
        super.onOpen();
        opened = true;
        authRunning = false;
        try {
            store.openMicrosoftCallbackServer();
        } catch (Exception exception) {
            LauncherLog.error("Unable to open Microsoft callback server.", exception);
            showAuthError(InformationMessages.text(InformationMessages.SIGN_IN_ERROR));
        }
    }

    @Override
    public void onClose() {
        opened = false;
        authRunning = false;
        store.closeMicrosoftCallbackServer();
        super.onClose();
        offlineInput.blur();
    }

    @Override
    protected void disposePopupContent() {
        store.unsubscribe(storeListener);
        offlineInput.dispose();
        enterText.dispose();
        microsoftText.dispose();
        emptyAccountText.dispose();
        buyMinecraftText.dispose();

        for (Text text : usernameTexts) text.dispose();
        for (Text text : typeTexts) text.dispose();
        usernameTexts.clear();
        typeTexts.clear();
        deleteButtons.clear();
        appearanceButtons.clear();
    }

    private void syncText(List<Text> texts, int index, int y, int width, String value, Palette color) {
        if (index >= texts.size()) {
            texts.add(new Text(host, 5, y, width, value,
                    Text.DisplayType.LINE, Text.LineAlignment.LEFT,
                    color, 1, 0
            ));
            return;
        }

        Text text = texts.get(index);
        text.setPosition(5, y);
        text.setBoxWidth(width);
        text.setText(value);
    }

    private static void trimButtons(List<PixelButton> buttons, int count) {
        while (buttons.size() > count) buttons.removeLast();
    }

    private static void trimTexts(List<Text> texts, int count) {
        while (texts.size() > count) texts.removeLast().dispose();
    }
}
