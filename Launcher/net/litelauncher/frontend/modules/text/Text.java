package net.litelauncher.frontend.modules.text;

import net.litelauncher.frontend.Palette;
import net.litelauncher.LauncherStore;
import net.litelauncher.Theme;
import net.litelauncher.frontend.modules.animation.Animated;
import net.litelauncher.frontend.modules.animation.AnimationTicker;
import net.litelauncher.frontend.modules.render.PixelGraphics;
import net.litelauncher.frontend.modules.render.PixelSurface;

import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Text implements Animated {

    private static final int PAUSE_MS = 720;
    private static final double SPEED_PER_MS = 0.025;

    private final JComponent host;
    private int x;
    private int y;
    private int boxWidth;
    private final DisplayType displayType;
    private final LineAlignment align;
    private Palette color;
    private Theme renderedTheme;
    private long renderedPaletteRevision;
    private final int fontScale;
    private final int glyphGapX;

    private String text;

    private int contentWidth = 1;
    private int overflow;
    private int offset;
    private int pauseRemaining = PAUSE_MS;
    private boolean movingForward = true;
    private double movementCarry;

    private boolean layoutDirty = true;
    private boolean frameDirty = true;
    private boolean animationEnabled = true;

    private BufferedImage contentImage = TextRasterizer.emptyImage(1, 1);
    private BufferedImage frameImage = TextRasterizer.emptyImage(1, 1);

    public Text(
            JComponent host,
            int x,
            int y,
            int boxWidth,
            String text,
            DisplayType displayType,
            LineAlignment align,
            Palette color,
            int fontScale,
            int glyphGapX
    ) {
        this.host = Objects.requireNonNull(host);
        this.x = x;
        this.y = y;
        this.boxWidth = Math.max(1, boxWidth);
        this.text = text == null ? "" : text;
        this.displayType = Objects.requireNonNull(displayType);
        this.align = Objects.requireNonNull(align);
        this.color = Objects.requireNonNull(color);
        this.renderedTheme = LauncherStore.get().theme();
        this.renderedPaletteRevision = Palette.revision();
        this.fontScale = Math.max(1, fontScale);
        this.glyphGapX = glyphGapX;

        AnimationTicker.register(this);
    }

    public void setText(String text) {
        String value = text == null ? "" : text;
        if (this.text.equals(value)) return;

        this.text = value;
        this.layoutDirty = true;
        AnimationTicker.sync();
        host.repaint();
    }


    public void setColor(Palette color) {
        Palette value = Objects.requireNonNull(color);
        if (this.color == value) return;

        this.color = value;
        this.layoutDirty = true;
        AnimationTicker.sync();
        host.repaint();
    }

    public void setPosition(int x, int y) {
        if (this.x == x && this.y == y) return;
        this.x = x;
        this.y = y;
        host.repaint();
    }

    public void setBoxWidth(int boxWidth) {
        int value = Math.max(1, boxWidth);
        if (this.boxWidth == value) return;

        this.boxWidth = value;
        this.layoutDirty = true;
        AnimationTicker.sync();
        host.repaint();
    }

    public void setAnimationEnabled(boolean enabled) {
        if (this.animationEnabled == enabled) return;

        boolean wasAnimating = needsAnimation();
        this.animationEnabled = enabled;
        if (!enabled) stopAnimation();

        if (wasAnimating || needsAnimation()) AnimationTicker.sync();
    }

    public void stopAnimation() {
        resetAnimation();
        frameDirty = true;
    }

    public void dispose() {
        AnimationTicker.unregister(this);
    }

    public void render(PixelGraphics graphics) {
        render(PixelSurface.direct(graphics));
    }

    public void render(PixelSurface surface) {
        prepare();
        if (frameDirty) composeFrame();
        surface.image(x, y, frameImage);
    }

    @Override
    public boolean needsAnimation() {
        prepare();
        return animationEnabled && displayType == DisplayType.LINE && overflow > 0;
    }

    @Override
    public void advance(long deltaMs) {
        prepare();
        if (displayType != DisplayType.LINE || overflow == 0) return;

        if (pauseRemaining > 0) {
            pauseRemaining = Math.max(0, pauseRemaining - (int) deltaMs);
            return;
        }

        movementCarry += deltaMs * SPEED_PER_MS;
        int step = (int) movementCarry;
        if (step == 0) return;

        movementCarry -= step;

        int previousOffset = offset;

        if (movingForward) {
            offset = Math.min(overflow, offset + step);
            if (offset == overflow) {
                movingForward = false;
                pauseRemaining = PAUSE_MS;
            }
        } else {
            offset = Math.max(0, offset - step);
            if (offset == 0) {
                movingForward = true;
                pauseRemaining = PAUSE_MS;
            }
        }

        if (offset != previousOffset) {
            frameDirty = true;
            host.repaint();
        }
    }

    private void prepare() {
        Theme theme = LauncherStore.get().theme();
        long paletteRevision = Palette.revision();
        if (renderedTheme != theme || renderedPaletteRevision != paletteRevision) {
            renderedTheme = theme;
            renderedPaletteRevision = paletteRevision;
            layoutDirty = true;
        }
        if (!layoutDirty) return;

        rebuild();
        layoutDirty = false;
        frameDirty = true;
    }

    private void rebuild() {
        String[] lines = displayType == DisplayType.BLOCK ? wrap(text, boxWidth) : new String[]{text.replace('\n', ' ')};

        contentWidth = 1;
        for (String line : lines) contentWidth = Math.max(contentWidth, width(line));

        int contentHeight = Math.max(1, lines.length * lineHeight());
        overflow = displayType == DisplayType.LINE ? Math.max(0, contentWidth - boxWidth) : 0;

        contentImage = TextRasterizer.rasterizeLines(lines, contentWidth, color.color(renderedTheme), fontScale, glyphGapX);
        frameImage = TextRasterizer.emptyImage(boxWidth, contentHeight);

        resetAnimation();
    }

    private void resetAnimation() {
        offset = 0;
        pauseRemaining = PAUSE_MS;
        movingForward = true;
        movementCarry = 0.0;
    }

    private void composeFrame() {
        Graphics2D graphics = frameImage.createGraphics();
        prepareGraphics(graphics);

        graphics.setComposite(java.awt.AlphaComposite.Clear);
        graphics.fillRect(0, 0, frameImage.getWidth(), frameImage.getHeight());

        graphics.setComposite(java.awt.AlphaComposite.SrcOver);
        graphics.drawImage(contentImage, startX(), 0, null);
        graphics.dispose();

        frameDirty = false;
    }

    private int startX() {
        if (displayType == DisplayType.LINE && overflow > 0) return -offset;
        if (align == LineAlignment.CENTER) return (frameImage.getWidth() - contentWidth) / 2;
        if (align == LineAlignment.RIGHT) return frameImage.getWidth() - contentWidth;
        return 0;
    }

    private int width(String text) {
        return GlyphLayout.width(text, fontScale, glyphGapX);
    }

    private int glyphAdvance(int codePoint) {
        return TextRasterizer.glyphAdvance(codePoint, fontScale);
    }

    private int lineHeight() {
        return TextRasterizer.lineHeight(fontScale);
    }

    private String[] wrap(String text, int maxWidth) {
        String[] paragraphs = text.split("\\n", -1);
        List<String> result = new ArrayList<>();
        int spaceWidth = glyphAdvance(' ');

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }

            StringBuilder current = new StringBuilder();
            int currentWidth = 0;

            for (String word : paragraph.split(" ")) {
                int wordWidth = width(word);

                if (current.isEmpty()) {
                    if (wordWidth <= maxWidth) {
                        current.append(word);
                        currentWidth = wordWidth;
                    } else {
                        currentWidth = appendBrokenWord(word, maxWidth, result, current);
                    }
                    continue;
                }

                int candidateWidth = currentWidth + glyphGapX + spaceWidth;
                if (!word.isEmpty()) {
                    candidateWidth += glyphGapX + wordWidth;
                }

                if (candidateWidth <= maxWidth) {
                    current.append(' ').append(word);
                    currentWidth = candidateWidth;
                    continue;
                }

                result.add(current.toString());
                current.setLength(0);

                if (wordWidth <= maxWidth) {
                    current.append(word);
                    currentWidth = wordWidth;
                } else {
                    currentWidth = appendBrokenWord(word, maxWidth, result, current);
                }
            }

            result.add(current.toString());
        }

        return result.toArray(String[]::new);
    }

    private int appendBrokenWord(String word, int maxWidth, List<String> result, StringBuilder tail) {
        tail.setLength(0);
        int tailWidth = 0;

        for (int index = 0; index < word.length(); ) {
            int codePoint = word.codePointAt(index);
            index += Character.charCount(codePoint);

            int advance = glyphAdvance(codePoint);
            int nextWidth = tailWidth + (tail.isEmpty() ? advance : glyphGapX + advance);

            if (!tail.isEmpty() && nextWidth > maxWidth) {
                result.add(tail.toString());
                tail.setLength(0);
                tail.appendCodePoint(codePoint);
                tailWidth = advance;
                continue;
            }

            if (!tail.isEmpty()) tailWidth += glyphGapX;

            tail.appendCodePoint(codePoint);
            tailWidth += advance;
        }

        return tailWidth;
    }

    private static void prepareGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }

    public enum DisplayType {
        LINE, BLOCK
    }

    public enum LineAlignment {
        LEFT, CENTER, RIGHT
    }
}
