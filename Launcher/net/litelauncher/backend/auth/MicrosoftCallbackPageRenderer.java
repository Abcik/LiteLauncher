package net.litelauncher.backend.auth;

import net.litelauncher.Language;
import net.litelauncher.Theme;

public interface MicrosoftCallbackPageRenderer {

    String successHtml(Theme theme, Language language, String profileName, String skinPng, boolean slim);

    String errorHtml(Theme theme, Language language, String message);

    static MicrosoftCallbackPageRenderer fallback() {
        return new MicrosoftCallbackPageRenderer() {
            @Override
            public String successHtml(Theme theme, Language language, String profileName, String skinPng, boolean slim) {
                return page("LiteLauncher sign-in complete", "You can close this tab and return to LiteLauncher.");
            }

            @Override
            public String errorHtml(Theme theme, Language language, String message) {
                return page("LiteLauncher sign-in failed", message == null || message.isBlank()
                        ? "Return to LiteLauncher and try again."
                        : message);
            }
        };
    }

    private static String page(String title, String message) {
        String safeTitle = escape(title);
        String safeMessage = escape(message);
        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>LiteLauncher</title>"
                + "<style>body{margin:0;height:100vh;display:grid;place-items:center;font:16px system-ui,sans-serif;background:#f5f5f5;color:#1f1f1f}"
                + "main{max-width:520px;padding:32px;text-align:center}</style></head><body><main><h1>"
                + safeTitle + "</h1><p>" + safeMessage + "</p></main></body></html>";
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
