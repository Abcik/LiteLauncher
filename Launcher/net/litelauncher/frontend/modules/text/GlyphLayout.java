package net.litelauncher.frontend.modules.text;

public final class GlyphLayout {

    private static final TextFont FONT = TextFont.PIXELS;

    private final int[] stops;
    private final int width;

    private GlyphLayout(int[] stops, int width) {
        this.stops = stops;
        this.width = width;
    }

    public static GlyphLayout line(String text, int scale, int gap) {
        String value = text == null ? "" : text;
        int[] stops = new int[value.length() + 1];
        int width = 0;

        for (int index = 0; index < value.length(); index++) {
            stops[index] = width;
            width += FONT.glyph(value.charAt(index)).advance() * scale;
            if (index + 1 < value.length()) width += gap;
        }

        stops[value.length()] = width;
        return new GlyphLayout(stops, width);
    }

    public static int width(String text, int scale, int gap) {
        return line(text, scale, gap).width();
    }

    public static int align(int boxWidth, int textWidth, Text.LineAlignment align) {
        if (align == Text.LineAlignment.CENTER) return (boxWidth - textWidth) / 2;
        if (align == Text.LineAlignment.RIGHT) return boxWidth - textWidth;
        return 0;
    }

    public int width() {
        return width;
    }

    public int imageWidth() {
        return Math.max(1, width);
    }

    public int[] stops() {
        return stops;
    }

    public int hit(int x) {
        if (x <= 0) return 0;
        for (int index = 1; index < stops.length; index++) {
            if (x < (stops[index - 1] + stops[index]) / 2) return index - 1;
        }
        return stops.length - 1;
    }

    public int pixel(int index) {
        int x = stops[index] - 1;
        return Math.max(0, x);
    }
}
