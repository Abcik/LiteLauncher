package net.litelauncher.frontend.modules.auth;

import net.litelauncher.Language;
import net.litelauncher.frontend.Theme;

/**
 * Public-safe callback page renderer.
 *
 * The official build renders a custom pixel-art callback page. That visual implementation,
 * glyph rasterization and exact layout are redacted. This replacement keeps the auth flow
 * auditable without exposing the proprietary presentation layer.
 */
public final class MicrosoftCallbackPage {

    private MicrosoftCallbackPage() {
    }

    public static String successHtml(Theme theme, Language language, String profileName, String skinPng, boolean slim) {
        String safeName = profileName == null || profileName.isBlank() ? "Player" : escape(profileName.trim());
        return page("LiteLauncher sign-in complete", "Hi, " + safeName + "!", "Sign-in complete. You can close this page.", true);
    }

    public static String errorHtml(Theme theme, Language language, String message) {
        String safeMessage = message == null || message.isBlank() ? "Unable to complete sign-in." : escape(message.trim());
        return page("LiteLauncher sign-in failed", "Sign-in failed", safeMessage + " Return to LiteLauncher.", false);
    }

    private static String page(String title, String heading, String body, boolean success) {
        String accent = success ? "#2f855a" : "#c53030";
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escape(title) + "</title>"
                + "<style>body{margin:0;min-height:100vh;display:grid;place-items:center;font-family:system-ui,sans-serif;background:#111827;color:#f9fafb}.card{max-width:520px;padding:32px;border:1px solid #374151;border-radius:18px;background:#1f2937;box-shadow:0 20px 60px #0005}h1{margin:0 0 12px;color:" + accent + "}p{line-height:1.6}</style>"
                + "</head><body><main class=\"card\"><h1>" + escape(heading) + "</h1><p>" + escape(body) + "</p></main></body></html>";
    }

    private static String escape(String text) {
        return text == null ? "" : text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
