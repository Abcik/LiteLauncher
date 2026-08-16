package net.litelauncher.frontend.scenes;

import net.litelauncher.LauncherStore;
import net.litelauncher.LiteLauncher;
import net.litelauncher.backend.LauncherState;
import net.litelauncher.backend.platform.BrowserLinks;
import net.litelauncher.backend.platform.OSUtils;
import net.litelauncher.frontend.Palette;
import net.litelauncher.frontend.PixelPainter;
import net.litelauncher.frontend.Utils;
import net.litelauncher.Theme;
import net.litelauncher.frontend.modules.button.PixelButton;
import net.litelauncher.frontend.modules.checkbox.CheckBox;
import net.litelauncher.frontend.modules.field.InputField;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.PixelScrollView;
import net.litelauncher.frontend.modules.scroll.ScrollCanvas;
import net.litelauncher.frontend.modules.slider.Slider;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.i18n.I18n;


public final class ParametersScene extends PopupScene {

    private static final int SCALE_TEXT = 9;
    private static final int MIN_MEMORY = 512;

    private static final int[][] PANELS = {
            {0, 0, 172, 51},
            {0, 52, 172, 51},
            {0, 104, 172, 38},
            {0, 143, 172, 70},
            {0, 214, 172, 126},
            {107, 17, 45, 16},
            {5, 69, 39, 16},
            {57, 69, 39, 16},
            {5, 121, 162, 16}
    };

    private final LauncherStore store = LauncherStore.get();
    private final MainScene main;
    private final LauncherStore.Listener storeListener = this::handleStoreEvent;
    private final int maxMemory = OSUtils.maxMemoryMb();
    private final int maxScale = OSUtils.maxLauncherScale(LauncherState.LOGICAL_HEIGHT);

    private final PixelScrollView scrollView;
    private final Slider memorySlider;
    private final CheckBox memoryAutoCheckBox;
    private final CheckBox fullscreenCheckBox;
    private final CheckBox closeAfterLaunchCheckBox;
    private final CheckBox instancesStorageSystemCheckBox;
    private final CheckBox customThemesCheckBox;
    private final PixelButton scaleDownButton;
    private final PixelButton scaleUpButton;
    private final PixelButton customThemesDeleteButton;
    private final PixelButton customThemesOpenButton;
    private final PixelButton websiteButton;
    private final PixelButton discordButton;
    private final PixelButton githubButton;
    private final PixelButton donateButton;

    private final Text[] scrollTexts;
    private final InputField memoryInput;
    private final InputField resolutionWidthInput;
    private final InputField resolutionHeightInput;
    private final InputField argumentsInput;

    public ParametersScene(MainScene host) {
        super(host, "popup.parameters.title", "popup.parameters.subtitle");
        this.main = host;

        scrollView = new PixelScrollView(130, 88, 184, 203).setContentPadding(0, 0, 0, 0);

        memoryInput = new InputField(host, 112, 19, 37, "1024", Palette.OUTLINE,
                Text.LineAlignment.CENTER, Math.max(6, String.valueOf(maxMemory).length()), Palette.SUBTITLE, Palette.GENERAL_BACKGROUND,
                Palette.ACCENT, Text.LineAlignment.CENTER, 1, 0
        );

        resolutionWidthInput = new InputField(host, 7, 71, 37, "854", Palette.OUTLINE,
                Text.LineAlignment.CENTER, 5, Palette.SUBTITLE, Palette.GENERAL_BACKGROUND,
                Palette.ACCENT, Text.LineAlignment.CENTER, 1, 0
        );

        resolutionHeightInput = new InputField(host, 59, 71, 37, "480", Palette.OUTLINE,
                Text.LineAlignment.CENTER, 5, Palette.SUBTITLE, Palette.GENERAL_BACKGROUND,
                Palette.ACCENT, Text.LineAlignment.CENTER, 1, 0
        );

        argumentsInput = new InputField(host, 9, 123, 154, I18n.text("parameters.jvmArguments"), Palette.OUTLINE,
                Text.LineAlignment.LEFT, 256, Palette.SUBTITLE, Palette.GENERAL_BACKGROUND,
                Palette.ACCENT, Text.LineAlignment.LEFT, 1, 0
        );

        memoryInput.setCharacterFilter(ParametersScene::isDigit);
        resolutionWidthInput.setCharacterFilter(ParametersScene::isDigit);
        resolutionHeightInput.setCharacterFilter(ParametersScene::isDigit);

        memorySlider = new Slider(host, 5, 21, 90);
        memoryAutoCheckBox = new CheckBox(host, 5, 37, store.autoMemory());
        fullscreenCheckBox = new CheckBox(host, 5, 89, store.fullscreen());
        scaleDownButton = new PixelButton(host, 5, 160, 9, 9, this::drawScaleDownButton);
        scaleUpButton = new PixelButton(host, 27, 160, 9, 9, this::drawScaleUpButton);
        closeAfterLaunchCheckBox = new CheckBox(host, 5, 173, store.closeAfterLaunch());
        instancesStorageSystemCheckBox = new CheckBox(host, 5, 186, store.instancesStorageSystem());
        customThemesCheckBox = new CheckBox(host, 5, 199, store.customThemes());
        customThemesDeleteButton = new PixelButton(host, 145, 199, 9, 9, this::drawCustomThemesDeleteButton);
        customThemesOpenButton = new PixelButton(host, 158, 199, 9, 9, this::drawCustomThemesOpenButton);

        websiteButton = new PixelButton(host, 0, 343, 100, 20, this::drawWebsiteButton);
        discordButton = new PixelButton(host, 104, 343, 20, 20, iconRenderer("discord"));
        githubButton = new PixelButton(host, 128, 344, 20, 20, iconRenderer("github"));
        donateButton = new PixelButton(host, 152, 344, 20, 20, iconRenderer("donate"));

        scrollTexts = new Text[]{
                scrollText(5, 2, 162, I18n.text("parameters.memory"), Palette.TITLE),
                scrollText(156, 19, 12, I18n.text("parameters.mb"), Palette.SUBTITLE),
                scrollText(18, 35, 149, I18n.text("parameters.automatically"), Palette.SUBTITLE),
                scrollText(5, 54, 162, I18n.text("parameters.screen"), Palette.TITLE),
                scrollText(18, 87, 149, I18n.text("parameters.fullscreen"), Palette.SUBTITLE),
                scrollText(48, 70, 12, I18n.text("parameters.resolutionSeparator"), Palette.OUTLINE),
                scrollText(5, 106, 162, I18n.text("parameters.launchArguments"), Palette.TITLE),
                scrollText(5, 145, 162, I18n.text("parameters.launcher"), Palette.TITLE),
                scrollText(5, 216, 162, I18n.text("parameters.information"), Palette.TITLE),

                scrollText(18, 158, 12, String.valueOf(store.scale()), Palette.SUBTITLE),
                scrollText(40, 158, 127, I18n.text("parameters.scaling"), Palette.SUBTITLE),
                scrollText(18, 171, 149, I18n.text("parameters.closeAfterLaunch"), Palette.SUBTITLE),
                scrollText(18, 184, 149, I18n.text("parameters.instancesStorageSystem"), Palette.SUBTITLE),

                scrollText(18, 197, 123, I18n.text("parameters.customThemes"), Palette.SUBTITLE),

                new Text(host, 5, 228, 162, I18n.text("parameters.description"),
                        Text.DisplayType.BLOCK, Text.LineAlignment.LEFT, Palette.SUBTITLE, 1, 0),
                new Text(host, 5, 324, 163, I18n.format("parameters.byVersion", "version", LauncherState.LAUNCHER_VERSION), Text.DisplayType.LINE, Text.LineAlignment.RIGHT, Palette.SUBTITLE, 1, 0),
                new Text(host, 5, 347, 90, I18n.text("parameters.visitWebsite"), Text.DisplayType.LINE, Text.LineAlignment.CENTER, Palette.ACCENT_TITLE, 1, 0)
        };

        syncControlsFromState();
        installStateHandlers();

        setPopupContentAnimationsEnabled(false);
        store.subscribe(storeListener);
    }

    public void setScrollOffset(int offset) {
        scrollView.setScrollOffset(offset);
    }

    @Override
    protected void setPopupContentAnimationsEnabled(boolean enabled) {
        for (Text text : scrollTexts) text.setAnimationEnabled(enabled);
        memoryInput.setAnimationEnabled(enabled);
        resolutionWidthInput.setAnimationEnabled(enabled);
        resolutionHeightInput.setAnimationEnabled(enabled);
        argumentsInput.setAnimationEnabled(enabled);
    }

    private void applyScale(int scale) {
        int nextScale = Math.clamp(scale, 1, maxScale);
        if (store.scale() == nextScale) return;
        store.setScale(nextScale);
        int offset = scrollView.getScrollOffset();
        LiteLauncher.reopen().openParametersPopup(offset);
    }

    private void handleStoreEvent(LauncherStore.Event event) {
        if (event == LauncherStore.Event.LANGUAGE_CHANGED) {
            refreshPopupText();
            refreshLanguageTexts();
            host.repaint();
            return;
        }

        if (event == LauncherStore.Event.SCALE_CHANGED) {
            scrollTexts[SCALE_TEXT].setText(String.valueOf(store.scale()));
            syncScaleButtons();
            host.repaint();
            return;
        }

        if (event == LauncherStore.Event.SETTINGS_CHANGED) {
            syncControlsFromState();
            host.repaint();
        }
    }


    private void refreshLanguageTexts() {
        scrollTexts[0].setText(I18n.text("parameters.memory"));
        scrollTexts[1].setText(I18n.text("parameters.mb"));
        scrollTexts[2].setText(I18n.text("parameters.automatically"));
        scrollTexts[3].setText(I18n.text("parameters.screen"));
        scrollTexts[4].setText(I18n.text("parameters.fullscreen"));
        scrollTexts[5].setText(I18n.text("parameters.resolutionSeparator"));
        scrollTexts[6].setText(I18n.text("parameters.launchArguments"));
        scrollTexts[7].setText(I18n.text("parameters.launcher"));
        scrollTexts[8].setText(I18n.text("parameters.information"));
        scrollTexts[10].setText(I18n.text("parameters.scaling"));
        scrollTexts[11].setText(I18n.text("parameters.closeAfterLaunch"));
        scrollTexts[12].setText(I18n.text("parameters.instancesStorageSystem"));
        scrollTexts[13].setText(I18n.text("parameters.customThemes"));
        scrollTexts[14].setText(I18n.text("parameters.description"));
        scrollTexts[15].setText(I18n.format("parameters.byVersion", "version", LauncherState.LAUNCHER_VERSION));
        scrollTexts[16].setText(I18n.text("parameters.visitWebsite"));
        argumentsInput.setPlaceholder(I18n.text("parameters.jvmArguments"));
    }

    private void installStateHandlers() {
        memoryInput.setChangeAction(() -> {
            Integer value = parseInteger(memoryInput.getValue());
            if (value == null) return;
            store.setMemoryAmount(value);
            memorySlider.setValue(memoryToSlider(store.memoryAmount()));
        });
        resolutionWidthInput.setChangeAction(() -> {
            Integer value = parseInteger(resolutionWidthInput.getValue());
            if (value != null) store.setScreenWidth(value);
        });
        resolutionHeightInput.setChangeAction(() -> {
            Integer value = parseInteger(resolutionHeightInput.getValue());
            if (value != null) store.setScreenHeight(value);
        });
        argumentsInput.setChangeAction(() -> store.setJvmArguments(argumentsInput.getValue()));

        memorySlider.setChangeAction(() -> {
            int memory = sliderToMemory(memorySlider.value());
            store.setMemoryAmount(memory);
            memoryInput.setValue(String.valueOf(store.memoryAmount()));
        });
        memoryAutoCheckBox.setChangeAction(() -> store.setAutoMemory(memoryAutoCheckBox.value()));
        fullscreenCheckBox.setChangeAction(() -> store.setFullscreen(fullscreenCheckBox.value()));
        closeAfterLaunchCheckBox.setChangeAction(() -> store.setCloseAfterLaunch(closeAfterLaunchCheckBox.value()));
        instancesStorageSystemCheckBox.setChangeAction(() -> store.setInstancesStorageSystem(instancesStorageSystemCheckBox.value()));
        customThemesCheckBox.setChangeAction(() -> store.setCustomThemes(customThemesCheckBox.value()));
    }

    private void syncControlsFromState() {
        memoryInput.setValue(String.valueOf(store.memoryAmount()));
        memorySlider.setValue(memoryToSlider(store.memoryAmount()));
        memoryAutoCheckBox.setValue(store.autoMemory());
        memoryInput.setTextColor(store.autoMemory() ? Palette.OUTLINE : Palette.SUBTITLE);
        if (store.autoMemory()) {
            memoryInput.resetInput();
            memorySlider.resetInput();
        }
        resolutionWidthInput.setValue(String.valueOf(store.screenWidth()));
        resolutionHeightInput.setValue(String.valueOf(store.screenHeight()));
        fullscreenCheckBox.setValue(store.fullscreen());
        argumentsInput.setValue(store.jvmArguments());
        closeAfterLaunchCheckBox.setValue(store.closeAfterLaunch());
        instancesStorageSystemCheckBox.setValue(store.instancesStorageSystem());
        customThemesCheckBox.setValue(store.customThemes());
        syncScaleButtons();
    }

    private void syncScaleButtons() {
        scaleDownButton.setEnabled(store.scale() > 1);
        scaleUpButton.setEnabled(store.scale() < maxScale);
    }

    private double memoryToSlider(int memory) {
        if (maxMemory <= MIN_MEMORY) return 0;
        int clamped = Math.clamp(memory, MIN_MEMORY, maxMemory);
        return (clamped - MIN_MEMORY) / (double) (maxMemory - MIN_MEMORY);
    }

    private int sliderToMemory(double value) {
        if (maxMemory <= MIN_MEMORY || value <= 0) return MIN_MEMORY;
        if (value >= 1) return maxMemory;

        int memory = (int) Math.round(MIN_MEMORY + value * (maxMemory - MIN_MEMORY));
        int rounded = ((memory + 128) / 256) * 256;
        return Math.clamp(rounded, MIN_MEMORY, maxMemory);
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private void buildScrollable(ScrollCanvas canvas, Theme theme) {
        PixelPainter.drawElements(canvas, PANELS,
                Palette.ELEMENT_BACKGROUND.color(theme), Palette.OUTLINE.color(theme));

        scaleDownButton.render(canvas);
        scaleUpButton.render(canvas);
        customThemesDeleteButton.render(canvas);
        customThemesOpenButton.render(canvas);
        websiteButton.render(canvas);
        discordButton.render(canvas);
        githubButton.render(canvas);
        donateButton.render(canvas);
        memoryAutoCheckBox.render(canvas);
        fullscreenCheckBox.render(canvas);
        closeAfterLaunchCheckBox.render(canvas);
        instancesStorageSystemCheckBox.render(canvas);
        customThemesCheckBox.render(canvas);

        drawScrollText(canvas);
        memoryInput.render(canvas);
        resolutionWidthInput.render(canvas);
        resolutionHeightInput.render(canvas);
        argumentsInput.render(canvas);
        memorySlider.render(canvas);
    }

    private Text scrollText(int x, int y, int boxWidth, String value, Palette color) {
        return new Text(host, x, y, boxWidth, value, Text.DisplayType.LINE, Text.LineAlignment.LEFT, color, 1, 0);
    }

    private void drawScaleDownButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawSmallAccentButton(surface, x, y, state, store.theme());
        PixelPainter.drawChevronDown(surface, x + 2, y + 3, scaleDownButton.isEnabled() ? Palette.ACCENT_TITLE.color(store.theme()) : Palette.ACCENT_SUBTITLE.color(store.theme()));
    }

    private void drawScaleUpButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawSmallAccentButton(surface, x, y, state, store.theme());
        PixelPainter.drawChevronUp(surface, x + 2, y + 2, scaleUpButton.isEnabled() ? Palette.ACCENT_TITLE.color(store.theme()) : Palette.ACCENT_SUBTITLE.color(store.theme()));
    }

    private void drawCustomThemesDeleteButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawSmallAccentButton(surface, x, y, state, store.theme());
        PixelPainter.drawDeleteIcon(surface, x + 2, y + 2, Palette.ACCENT_TITLE.color(store.theme()));
    }

    private void drawCustomThemesOpenButton(PixelSurface surface, int x, int y, PixelButton.State state) {
        PixelPainter.drawSmallAccentButton(surface, x, y, state, store.theme());
        java.awt.Color color = Palette.ACCENT_TITLE.color(store.theme());
        surface.paint(x + 2, y + 2, x + 6, y + 2, color);
        surface.paint(x + 2, y + 4, x + 6, y + 4, color);
        surface.paint(x + 2, y + 6, x + 6, y + 6, color);
    }

    private void drawWebsiteButton(PixelSurface surface, int x, int y, int width, int height, PixelButton.State state) {
        PixelPainter.drawAccentButton(surface, x, y + 1, width, height - 2, state, store.theme());
    }

    private PixelButton.Renderer iconRenderer(String icon) {
        return (surface, x, y, width, height, _) ->
                surface.image(x, y, x + width - 1, y + height - 1, Utils.getLocalIcon(icon));
    }

    private void drawScrollText(ScrollCanvas canvas) {
        for (Text text : scrollTexts) text.render(canvas);
    }

    @Override
    protected void renderPopupContent(PixelGraphics graphics, MouseState mouse) {
        scrollView.render(graphics, canvas -> buildScrollable(canvas, store.theme()));
    }

    @Override
    public boolean handleInput(MouseState mouse) {
        boolean dirty = scrollView.handleInput(mouse, canvas -> buildScrollable(canvas, store.theme()));
        dirty |= store.autoMemory() ? memoryInput.resetInput() : memoryInput.handleInput(mouse, scrollView);
        dirty |= store.autoMemory() ? memorySlider.resetInput() : memorySlider.handleInput(mouse, scrollView);
        dirty |= resolutionWidthInput.handleInput(mouse, scrollView);
        dirty |= resolutionHeightInput.handleInput(mouse, scrollView);
        dirty |= argumentsInput.handleInput(mouse, scrollView);
        dirty |= memoryAutoCheckBox.handleInput(mouse, scrollView);
        dirty |= fullscreenCheckBox.handleInput(mouse, scrollView);
        dirty |= closeAfterLaunchCheckBox.handleInput(mouse, scrollView);
        dirty |= instancesStorageSystemCheckBox.handleInput(mouse, scrollView);
        dirty |= customThemesCheckBox.handleInput(mouse, scrollView);
        dirty |= scaleDownButton.handleInput(mouse, scrollView);
        dirty |= scaleUpButton.handleInput(mouse, scrollView);
        dirty |= customThemesDeleteButton.handleInput(mouse, scrollView);
        dirty |= customThemesOpenButton.handleInput(mouse, scrollView);
        dirty |= websiteButton.handleInput(mouse, scrollView);
        dirty |= discordButton.handleInput(mouse, scrollView);
        dirty |= githubButton.handleInput(mouse, scrollView);
        dirty |= donateButton.handleInput(mouse, scrollView);

        if (scaleUpButton.consumeClick()) {
            applyScale(store.scale() + 1);
            return true;
        }

        if (scaleDownButton.consumeClick()) {
            applyScale(store.scale() - 1);
            return true;
        }

        if (customThemesDeleteButton.consumeClick()) {
            store.deleteCustomThemeColors();
            return true;
        }

        if (customThemesOpenButton.consumeClick()) {
            main.openThemeCustomizationPopup();
            return true;
        }

        if (websiteButton.consumeClick()) {
            BrowserLinks.open(BrowserLinks.WEBSITE);
            return true;
        }

        if (discordButton.consumeClick()) {
            BrowserLinks.open(BrowserLinks.DISCORD);
            return true;
        }

        if (githubButton.consumeClick()) {
            BrowserLinks.open(BrowserLinks.GITHUB);
            return true;
        }

        if (donateButton.consumeClick()) {
            BrowserLinks.open(BrowserLinks.DONATE);
            return true;
        }

        return dirty;
    }

    @Override
    public void onClose() {
        super.onClose();
        memoryInput.blur();
        resolutionWidthInput.blur();
        resolutionHeightInput.blur();
        argumentsInput.blur();
    }

    @Override
    protected void disposePopupContent() {
        store.unsubscribe(storeListener);
        for (Text text : scrollTexts) text.dispose();
        memoryInput.dispose();
        resolutionWidthInput.dispose();
        resolutionHeightInput.dispose();
        argumentsInput.dispose();
    }
}
