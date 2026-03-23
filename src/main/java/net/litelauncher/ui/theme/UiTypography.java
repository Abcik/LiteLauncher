package net.litelauncher.ui.theme;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public final class UiTypography {
    public static final float MICRO = 14f;
    public static final float BODY = 18f;
    public static final float EMPHASIS = 23f;
    public static final float TITLE = 28f;
    public static final float HERO = 42f;
    public static final float DEFAULT_FONT_SIZE = BODY;

    private static final String BASE_FONT_FAMILY = resolveBaseFamily();

    private UiTypography() {
    }

    public static void configureGlobalRendering() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static Font regular(float size) {
        return resolveFont(Font.PLAIN, size);
    }

    public static Font medium(float size) {
        return resolveFont(Font.BOLD, size);
    }

    public static Font bold(float size) {
        return resolveFont(Font.BOLD, size);
    }

    public static Font resolveFont(int style, float size) {
        int resolvedStyle = (style & Font.BOLD) != 0 ? Font.BOLD : Font.PLAIN;
        if ((style & Font.ITALIC) != 0) {
            resolvedStyle |= Font.ITALIC;
        }
        return new Font(BASE_FONT_FAMILY, resolvedStyle, Math.round(normalizeSize(size)));
    }

    public static float normalizeSize(float size) {
        float sanitized = Float.isNaN(size) || Float.isInfinite(size) ? BODY : size;
        return Math.max(MICRO, sanitized);
    }

    public static void applyTextRendering(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    public static void applyCrispTextRendering(Graphics2D graphics) {
        applyTextRendering(graphics);
    }

    public static void installSwingDefaults() {
        FontUIResource regular = new FontUIResource(regular(DEFAULT_FONT_SIZE));
        FontUIResource heading = new FontUIResource(medium(DEFAULT_FONT_SIZE));

        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, regular);
            }
        }

        UIManager.put("defaultFont", regular);
        UIManager.put("Label.font", regular);
        UIManager.put("Button.font", heading);
        UIManager.put("ToggleButton.font", heading);
        UIManager.put("ComboBox.font", regular);
        UIManager.put("TextField.font", regular);
        UIManager.put("FormattedTextField.font", regular);
        UIManager.put("PasswordField.font", regular);
        UIManager.put("CheckBox.font", regular);
        UIManager.put("RadioButton.font", regular);
        UIManager.put("ToolTip.font", regular);
        UIManager.put("OptionPane.messageFont", regular);
        UIManager.put("OptionPane.buttonFont", heading);
    }

    private static String resolveBaseFamily() {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : new String[]{"Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", Font.SANS_SERIF}) {
            if (installed.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }
}
