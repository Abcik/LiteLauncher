package net.litelauncher.ui.text;

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

    public int width() {
        return width;
    }
}
