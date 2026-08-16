package net.litelauncher.frontend.modules.auth;

import net.litelauncher.Language;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.frontend.Palette;
import net.litelauncher.Theme;
import net.litelauncher.frontend.modules.text.TextRasterizer;
import net.litelauncher.i18n.I18n;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

public final class MicrosoftCallbackPage {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 128;
    
    public static String successHtml(Theme theme, Language language, String profileName, String skinPng, boolean slim) {
        return html(theme, successLines(language, profileName), true, skinPng, slim);
    }

    public static String errorHtml(Theme theme, Language language, String message) {
        return html(theme, errorLines(language, message), false, null, false);
    }

    private static String html(Theme theme, String[] lines, boolean success, String skinPng, boolean slim) {
        Theme safeTheme = theme == null ? Theme.LIGHT : theme;
        String canvas = pngBase64(drawCanvas(safeTheme, lines, success, skinPng, slim));
        String logo = logoBase64(safeTheme);
        String background = hex(Palette.GENERAL_BACKGROUND.color(safeTheme));

        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>LiteLauncher</title>"
                + "<style>html{--scale:1}body{margin:0;width:100vw;height:100vh;overflow:hidden;background:" + background + ";display:flex;align-items:center;justify-content:center}"
                + "img{image-rendering:pixelated;image-rendering:crisp-edges}.logo{position:fixed;left:calc(6px*var(--scale));top:calc(5px*var(--scale));width:calc(18px*var(--scale));height:calc(18px*var(--scale))}"
                + ".canvas{width:calc(" + WIDTH + "px*var(--scale));height:calc(" + HEIGHT + "px*var(--scale))}</style>"
                + "<script>function s(){document.documentElement.style.setProperty('--scale',Math.max(1,Math.floor((innerWidth/2)/" + WIDTH + ")))}addEventListener('resize',s);addEventListener('load',s);</script>"
                + "</head><body><img class=\"logo\" src=\"data:image/png;base64," + logo + "\" alt=\"\"><img class=\"canvas\" src=\"data:image/png;base64," + canvas + "\" alt=\"\"></body></html>";
    }

    private static BufferedImage drawCanvas(Theme theme, String[] lines, boolean success, String skinPng, boolean slim) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        prepare(graphics);

        graphics.setColor(Palette.GENERAL_BACKGROUND.color(theme));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        if (success) {
            BufferedImage icon = SkinAvatar.create(skinPng, slim);
            if (icon == null) icon = defaultProfileIcon();
            if (icon != null) graphics.drawImage(icon, (WIDTH - icon.getWidth()) / 2, 36, null);
        }

        int y = 68;
        int index = 0;
        for (String line : lines) {
            BufferedImage text = TextRasterizer.tint(TextRasterizer.rasterizeLineMask(line, 1, 0),
                    index == 0 ? Palette.TITLE.color(theme) : (index == 1 ? Palette.ACCENT.color(theme) : Palette.SUBTITLE.color(theme))
            );
            graphics.drawImage(text, (WIDTH - text.getWidth()) / 2, y, null);
            y += 12;
            index++;
        }

        graphics.dispose();
        return image;
    }

    private static String[] successLines(Language language, String profileName) {
        Language safeLanguage = language == null ? Language.ENGLISH : language;
        String name = profileName == null || profileName.isBlank() ? "Player" : profileName.trim();
        return new String[]{
                I18n.format(safeLanguage, "callback.greeting", "name", name),
                I18n.text(safeLanguage, "callback.successTitle"),
                I18n.text(safeLanguage, "callback.successClose")
        };
    }

    private static String[] errorLines(Language language, String message) {
        Language safeLanguage = language == null ? Language.ENGLISH : language;
        String text = errorMessage(safeLanguage, message);
        if (text.length() > 36) text = text.substring(0, 36) + "...";

        return new String[]{
                I18n.text(safeLanguage, "callback.failedTitle"),
                text,
                I18n.text(safeLanguage, "callback.returnLauncher")
        };
    }

    private static String errorMessage(Language language, String message) {
        if (InformationMessages.WEB_AUTH_ERROR.equals(message)) return I18n.text(language, "callback.unableComplete");
        if (InformationMessages.SIGN_IN_ERROR.equals(message)) return I18n.text(language, "callback.signInFailed");
        return message == null || message.isBlank() ? I18n.text(language, "callback.signInFailed") : message.trim();
    }

    private static BufferedImage defaultProfileIcon() {
        try (InputStream stream = MicrosoftCallbackPage.class.getClassLoader().getResourceAsStream("assets/common/profile.png")) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (Exception _) {
            return null;
        }
    }

    private static String logoBase64(Theme theme) {
        String path = "assets/" + theme.identifier() + "/logo.png";
        try (InputStream stream = MicrosoftCallbackPage.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return "";
            return Base64.getEncoder().encodeToString(stream.readAllBytes());
        } catch (Exception _) {
            return "";
        }
    }

    private static String pngBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception _) {
            return "";
        }
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void prepare(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }
}
