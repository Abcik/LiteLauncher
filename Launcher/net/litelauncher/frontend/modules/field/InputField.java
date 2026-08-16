package net.litelauncher.frontend.modules.field;

import net.litelauncher.frontend.Palette;
import net.litelauncher.LauncherStore;
import net.litelauncher.Theme;
import net.litelauncher.frontend.modules.animation.Animated;
import net.litelauncher.frontend.modules.animation.AnimationTicker;
import net.litelauncher.frontend.modules.input.MouseCursor;
import net.litelauncher.frontend.modules.input.MouseState;
import net.litelauncher.frontend.modules.interaction.Hotspot;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;
import net.litelauncher.frontend.modules.scroll.PixelScrollView;
import net.litelauncher.frontend.modules.text.GlyphLayout;
import net.litelauncher.frontend.modules.text.Text;
import net.litelauncher.frontend.modules.text.TextRasterizer;

import javax.swing.JComponent;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class InputField implements Animated {

    private static final int CARET_BLINK_MS = 500;
    private static InputField activeField;

    private final JComponent host;
    private final int maxLength;
    private Palette textColor;
    private final Palette selectedTextColor;
    private final Palette selectionColor;
    private Theme renderedTheme;
    private long renderedPaletteRevision;
    private final Text.LineAlignment align;
    private final int fontScale;
    private final int glyphGapX;
    private final int lineHeight;
    private final Text valueText;
    private final Text placeholderText;
    private final Hotspot hotspot = new Hotspot(0, 0, 0, 0, MouseCursor.TEXT);

    private final int x;
    private final int y;
    private final int boxWidth;
    private String value = "";
    private int caretIndex;
    private int anchorIndex;
    private int editOffset;
    private int caretBlinkElapsed;
    private boolean caretVisible = true;
    private boolean dragging;
    private Runnable enterAction;
    private Runnable changeAction;
    private CharacterFilter characterFilter;
    private CharacterMapper characterMapper;
    private boolean animationEnabled = true;
    private KeyAdapter keyHandler;
    private FocusAdapter focusHandler;
    private boolean disposed;

    private GlyphLayout layout;
    private BufferedImage normalImage;
    private BufferedImage selectedImage;

    public InputField(
            JComponent host,
            int x,
            int y,
            int boxWidth,
            String placeholder,
            Palette placeholderColor,
            Text.LineAlignment placeholderAlign,
            int maxLength,
            Palette textColor,
            Palette selectedTextColor,
            Palette selectionColor,
            Text.LineAlignment align,
            int fontScale,
            int glyphGapX
    ) {
        this.host = host;
        this.x = x;
        this.y = y;
        this.boxWidth = Math.max(1, boxWidth);
        this.maxLength = Math.max(0, maxLength);
        this.textColor = textColor;
        this.selectedTextColor = selectedTextColor;
        this.selectionColor = selectionColor;
        this.renderedTheme = LauncherStore.get().theme();
        this.renderedPaletteRevision = Palette.revision();
        this.align = align;
        this.fontScale = Math.max(1, fontScale);
        this.glyphGapX = glyphGapX;
        this.lineHeight = TextRasterizer.lineHeight(this.fontScale);
        this.layout = GlyphLayout.line("", this.fontScale, this.glyphGapX);
        rebuildImages();

        this.valueText = new Text(host, x, y, this.boxWidth, "", Text.DisplayType.LINE, align, textColor, this.fontScale, this.glyphGapX);
        this.placeholderText = new Text(host, x, y, this.boxWidth, placeholder, Text.DisplayType.LINE, placeholderAlign, placeholderColor, this.fontScale, this.glyphGapX);

        syncHotspot(x, y);
        installKeyboardHandlers();
        AnimationTicker.register(this);
    }

    public void setValue(String value) {
        String next = value == null ? "" : value;
        if (next.length() > maxLength) next = next.substring(0, maxLength);
        if (next.equals(this.value)) return;
        this.value = next;
        caretIndex = next.length();
        anchorIndex = caretIndex;
        rebuild();
        editOffset = 0;
        notifyChanged();
        repaint();
    }

    public String getValue() {
        return value;
    }

    public void setPlaceholder(String placeholder) {
        placeholderText.setText(placeholder == null ? "" : placeholder);
    }

    public void setEnterAction(Runnable enterAction) {
        this.enterAction = enterAction;
    }

    public void setChangeAction(Runnable changeAction) {
        this.changeAction = changeAction;
    }

    public void setCharacterFilter(CharacterFilter characterFilter) {
        this.characterFilter = characterFilter;
    }

    public void setCharacterMapper(CharacterMapper characterMapper) {
        this.characterMapper = characterMapper;
    }

    public void setTextColor(Palette textColor) {
        Palette value = textColor == null ? Palette.SUBTITLE : textColor;
        if (this.textColor == value) return;

        this.textColor = value;
        valueText.setColor(value);
        rebuildImages();
        repaint();
    }

    private boolean active() {
        return activeField == this;
    }

    public void setAnimationEnabled(boolean enabled) {
        if (animationEnabled == enabled) return;

        animationEnabled = enabled;
        valueText.setAnimationEnabled(enabled);
        placeholderText.setAnimationEnabled(enabled);
        if (!enabled) {
            blur();
            dragging = false;
            hotspot.reset();
        }
        AnimationTicker.sync();
    }

    public void blur() {
        if (active()) deactivate();
    }

    public boolean resetInput() {
        boolean dirty = dragging || hotspot.isHovered() || active();
        blur();
        dragging = false;
        hotspot.reset();
        if (dirty) repaint();
        return dirty;
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (active()) activeField = null;
        dragging = false;
        AnimationTicker.unregister(this);
        valueText.dispose();
        placeholderText.dispose();
        if (keyHandler != null) host.removeKeyListener(keyHandler);
        if (focusHandler != null) host.removeFocusListener(focusHandler);
        AnimationTicker.sync();
    }

    public void render(PixelGraphics graphics) {
        render(PixelSurface.direct(graphics));
    }

    public void render(PixelSurface surface) {
        if (!active()) {
            (value.isEmpty() ? placeholderText : valueText).render(surface);
            return;
        }

        renderActive(surface, x, y);
    }

    @Override
    public boolean needsAnimation() {
        return animationEnabled && active();
    }

    @Override
    public void advance(long deltaMs) {
        if (!animationEnabled || !active()) return;
        caretBlinkElapsed += (int) deltaMs;
        if (caretBlinkElapsed < CARET_BLINK_MS) return;
        caretBlinkElapsed -= CARET_BLINK_MS;
        caretVisible = !caretVisible;
        repaint();
    }

    public boolean handleInput(MouseState mouse) {
        return handleInput(mouse, x, y, true, null);
    }

    public boolean handleInput(MouseState mouse, PixelScrollView scrollView) {
        int screenX = scrollView.screenX(x);
        int screenY = scrollView.screenY(y);
        boolean visible = scrollView.isContentBoundsVisible(x, y, x + boxWidth - 1, y + lineHeight - 1);
        return handleInput(mouse, screenX, screenY, visible, scrollView);
    }

    private void renderActive(PixelSurface surface, int x, int y) {
        ensureTheme();
        int drawX = x + startX();

        surface.pushClip(x, y, x + boxWidth - 1, y + lineHeight - 1);

        int selectionXMin = drawX + layout.pixel(selectionStart());
        int selectionXMax = drawX + layout.stops()[selectionEnd()] - 1;
        int visibleSelectionXMin = Math.max(selectionXMin, x);
        int visibleSelectionXMax = Math.min(selectionXMax, x + boxWidth - 1);

        if (hasSelection() && visibleSelectionXMin <= visibleSelectionXMax) {
            surface.paint(visibleSelectionXMin, y, visibleSelectionXMax, y + lineHeight - 1, selectionColor.color(LauncherStore.get().theme()));
        }

        surface.image(drawX, y, normalImage);

        if (hasSelection() && visibleSelectionXMin <= visibleSelectionXMax) {
            surface.pushClip(visibleSelectionXMin, y, visibleSelectionXMax, y + lineHeight - 1);
            surface.image(drawX, y, selectedImage);
            surface.popClip();
        }

        if (!hasSelection() && caretVisible) {
            int caretX = drawX + layout.pixel(caretIndex);
            surface.paint(caretX, y, caretX, y + lineHeight - 1, textColor.color(LauncherStore.get().theme()));
        }

        surface.popClip();
    }

    private boolean handleInput(MouseState mouse, int screenX, int screenY, boolean inputEnabled, PixelScrollView scrollView) {
        if (!animationEnabled) {
            if (active()) return deactivate();
            dragging = false;
            hotspot.reset();
            return false;
        }

        syncHotspot(screenX, screenY);

        int mouseX = mouse.getLogicalX();
        int mouseY = mouse.getLogicalY();
        boolean insideScrollContent = scrollView == null || scrollView.containsContentPoint(mouseX, mouseY);
        boolean dirty = hotspot.update(mouse, inputEnabled && insideScrollContent);
        dirty |= hotspot.consumeClick();
        boolean canHit = inputEnabled && !mouse.isConsumed() && insideScrollContent;
        boolean inside = canHit && hotspot.contains(mouseX, mouseY);

        if (mouse.isLeftPressed()) {
            if (!inside) return (active() && deactivate()) || dirty;
            boolean activated = !active();
            activate();
            dragging = true;
            anchorIndex = indexAt(mouseX - screenX);
            mouse.consume();
            return move(anchorIndex, true) || activated || dirty;
        }

        if (!mouse.isLeftDown()) {
            boolean wasDragging = dragging;
            dragging = false;
            return dirty || wasDragging;
        }

        if (dragging && active()) {
            mouse.consume();
            return move(indexAt(mouseX - screenX), true) || dirty;
        }

        return dirty;
    }

    private void installKeyboardHandlers() {
        keyHandler = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent event) {
                if (!active()) return;
                char character = event.getKeyChar();
                if (character < 32 || character == 127) return;
                character = map(character);
                if (!allowed(character)) {
                    event.consume();
                    return;
                }
                replace(selectionStart(), selectionEnd(), String.valueOf(character));
                event.consume();
            }

            @Override
            public void keyPressed(KeyEvent event) {
                if (!active()) return;

                switch (event.getKeyCode()) {
                    case KeyEvent.VK_LEFT ->
                            move(hasSelection() && !event.isShiftDown() ? selectionStart() : Math.max(0, caretIndex - 1), event.isShiftDown());
                    case KeyEvent.VK_RIGHT ->
                            move(hasSelection() && !event.isShiftDown() ? selectionEnd() : Math.min(value.length(), caretIndex + 1), event.isShiftDown());
                    case KeyEvent.VK_HOME -> move(0, event.isShiftDown());
                    case KeyEvent.VK_END -> move(value.length(), event.isShiftDown());
                    case KeyEvent.VK_BACK_SPACE -> erase(true);
                    case KeyEvent.VK_DELETE -> erase(false);
                    case KeyEvent.VK_ENTER -> {
                        if (enterAction == null) return;
                        enterAction.run();
                    }
                    case KeyEvent.VK_A -> {
                        if (shortcutDown(event)) {
                            anchorIndex = 0;
                            move(value.length(), true);
                        } else return;
                    }
                    case KeyEvent.VK_C -> {
                        if (shortcutDown(event)) copySelectionToClipboard();
                        else return;
                    }
                    case KeyEvent.VK_V -> {
                        if (shortcutDown(event)) pasteFromClipboard();
                        else return;
                    }
                    default -> {
                        return;
                    }
                }
                event.consume();
            }
        };

        focusHandler = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                if (active()) deactivate();
            }
        };

        host.addKeyListener(keyHandler);
        host.addFocusListener(focusHandler);
    }

    private void activate() {
        activeField = this;
        host.requestFocusInWindow();
        wakeCaret();
        AnimationTicker.sync();
    }

    private boolean deactivate() {
        activeField = null;
        dragging = false;
        repaint();
        AnimationTicker.sync();
        return true;
    }

    private boolean move(int target, boolean keepSelection) {
        int nextAnchor = keepSelection ? anchorIndex : target;
        boolean changed = caretIndex != target || anchorIndex != nextAnchor || !caretVisible || caretBlinkElapsed != 0;
        caretIndex = target;
        anchorIndex = nextAnchor;
        syncEditOffset();
        wakeCaret();
        if (changed) repaint();
        return changed;
    }

    private void erase(boolean backward) {
        if (hasSelection()) {
            replace(selectionStart(), selectionEnd(), "");
            return;
        }
        if (backward && caretIndex > 0) replace(caretIndex - 1, caretIndex, "");
        if (!backward && caretIndex < value.length()) replace(caretIndex, caretIndex + 1, "");
    }

    private void copySelectionToClipboard() {
        if (!hasSelection()) return;
        String selected = value.substring(selectionStart(), selectionEnd());
        if (selected.isEmpty()) return;

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
        } catch (RuntimeException _) {
            // Clipboard can be temporarily unavailable if another application locks it.
        }
    }

    private void pasteFromClipboard() {
        String text = clipboardText();
        if (text == null || text.isEmpty()) return;

        int start = selectionStart();
        int end = selectionEnd();
        int capacity = maxLength - (value.length() - (end - start));
        if (capacity <= 0) return;

        String insert = insertableText(text, capacity);
        if (insert.isEmpty()) return;
        replace(start, end, insert);
    }

    private String clipboardText() {
        try {
            if (!Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor)) return null;
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (IOException | UnsupportedFlavorException | RuntimeException _) {
            return null;
        }
    }

    private String insertableText(String text, int capacity) {
        StringBuilder result = new StringBuilder(Math.min(text.length(), capacity));
        for (int i = 0; i < text.length() && result.length() < capacity; i++) {
            char character = text.charAt(i);
            if (character < 32 || character == 127) continue;
            character = map(character);
            if (!allowed(character)) continue;
            result.append(character);
        }
        return result.toString();
    }

    private boolean shortcutDown(KeyEvent event) {
        return event.isControlDown() || event.isMetaDown();
    }

    private void replace(int start, int end, String insert) {
        if (!allowed(insert)) return;
        if (value.length() - (end - start) + insert.length() > maxLength) return;
        value = value.substring(0, start) + insert + value.substring(end);
        caretIndex = start + insert.length();
        anchorIndex = caretIndex;
        rebuild();
        syncEditOffset();
        wakeCaret();
        notifyChanged();
        repaint();
    }

    private void rebuild() {
        layout = GlyphLayout.line(value, fontScale, glyphGapX);
        rebuildImages();
        valueText.setText(value);
    }

    private void rebuildImages() {
        renderedTheme = LauncherStore.get().theme();
        renderedPaletteRevision = Palette.revision();
        BufferedImage mask = TextRasterizer.rasterizeLineMask(value, fontScale, glyphGapX);
        normalImage = TextRasterizer.tint(mask, textColor.color(renderedTheme));
        selectedImage = TextRasterizer.tint(mask, selectedTextColor.color(renderedTheme));
    }

    private void ensureTheme() {
        if (renderedTheme == LauncherStore.get().theme() && renderedPaletteRevision == Palette.revision()) return;
        rebuildImages();
    }

    private int indexAt(int localX) {
        if (layout.width() > boxWidth) return layout.hit(localX + editOffset);
        return layout.hit(localX - GlyphLayout.align(boxWidth, layout.width(), align));
    }

    private void syncEditOffset() {
        if (layout.width() <= boxWidth) {
            editOffset = 0;
            return;
        }

        int caret = layout.pixel(caretIndex);
        if (caret < editOffset) editOffset = caret;
        if (caret >= editOffset + boxWidth) editOffset = caret - boxWidth + 1;
    }

    private int startX() {
        return layout.width() > boxWidth ? -editOffset : GlyphLayout.align(boxWidth, layout.width(), align);
    }

    private void syncHotspot(int screenX, int screenY) {
        hotspot.setBounds(screenX, screenY, screenX + boxWidth - 1, screenY + lineHeight - 1);
    }

    private boolean allowed(String text) {
        if (text == null || characterFilter == null) return true;
        for (int i = 0; i < text.length(); i++) {
            if (!allowed(text.charAt(i))) return false;
        }
        return true;
    }

    private boolean allowed(char character) {
        return characterFilter == null || characterFilter.allowed(character);
    }

    private char map(char character) {
        return characterMapper == null ? character : characterMapper.map(character);
    }

    private int selectionStart() {
        return Math.min(anchorIndex, caretIndex);
    }

    private int selectionEnd() {
        return Math.max(anchorIndex, caretIndex);
    }

    private boolean hasSelection() {
        return caretIndex != anchorIndex;
    }

    private void wakeCaret() {
        caretVisible = true;
        caretBlinkElapsed = 0;
        AnimationTicker.sync();
    }

    private void notifyChanged() {
        if (changeAction != null) changeAction.run();
    }

    private void repaint() {
        host.repaint();
    }

    @FunctionalInterface
    public interface CharacterFilter {
        boolean allowed(char character);
    }

    @FunctionalInterface
    public interface CharacterMapper {
        char map(char character);
    }
}

