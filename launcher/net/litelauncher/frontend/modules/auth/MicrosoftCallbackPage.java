package net.litelauncher.frontend.modules.auth;

import net.litelauncher.Language;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.frontend.Theme;
import net.litelauncher.i18n.I18n;

/**
 * Public-safe Microsoft callback page.
 *
 * The official LiteLauncher callback page uses the private pixel renderer,
 * glyph atlas and product-specific layout. This replacement keeps the auth
 * flow auditable without publishing the visual implementation.
 */
public final class MicrosoftCallbackPage {

    private MicrosoftCallbackPage() {
    }

    public static String successHtml(Theme theme, Language language, String profileName, String skinPng, boolean slim) {
        String name = profileName == null || profileName.isBlank() ? "Player" : escape(profileName.trim());
        String greeting = escape(I18n.format(language == null ? Language.ENGLISH : language,
                "callback.greeting", "name", name));
        String title = escape(I18n.text(language == null ? Language.ENGLISH : language, "callback.successTitle"));
        String close = escape(I18n.text(language == null ? Language.ENGLISH : language, "callback.successClose"));
        return html("LiteLauncher", greeting, title, close, true);
    }

    public static String errorHtml(Theme theme, Language language, String message) {
        Language safeLanguage = language == null ? Language.ENGLISH : language;
        String title = escape(I18n.text(safeLanguage, "callback.failedTitle"));
        String details = escape(errorMessage(safeLanguage, message));
        String close = escape(I18n.text(safeLanguage, "callback.returnLauncher"));
        return html("LiteLauncher", title, details, close, false);
    }

    private static String errorMessage(Language language, String message) {
        if (InformationMessages.WEB_AUTH_ERROR.equals(message)) return I18n.text(language, "callback.unableComplete");
        if (InformationMessages.SIGN_IN_ERROR.equals(message)) return I18n.text(language, "callback.signInFailed");
        return message == null || message.isBlank() ? I18n.text(language, "callback.signInFailed") : message.trim();
    }

    private static String html(String pageTitle, String line1, String line2, String line3, boolean success) {
        String accent = success ? "#2563eb" : "#b91c1c";
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escape(pageTitle) + "</title>"
                + "<style>body{margin:0;min-height:100vh;display:grid;place-items:center;background:#f8fafc;color:#111827;font-family:system-ui,-apple-system,Segoe UI,sans-serif}.card{max-width:520px;margin:24px;padding:28px;border:1px solid #e5e7eb;border-radius:18px;background:white;box-shadow:0 10px 30px rgba(15,23,42,.08)}h1{margin:0 0 8px;color:"
                + accent + ";font-size:24px}p{margin:8px 0;color:#374151}</style>"
                + "</head><body><main class=\"card\"><h1>" + line1 + "</h1><p>" + line2 + "</p><p>" + line3 + "</p></main></body></html>";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
