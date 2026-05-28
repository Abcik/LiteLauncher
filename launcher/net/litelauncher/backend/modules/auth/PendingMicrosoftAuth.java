package net.litelauncher.backend.modules.auth;

import net.litelauncher.Language;
import net.litelauncher.frontend.Theme;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

record PendingMicrosoftAuth(
        String state,
        String codeVerifier,
        URI redirectUri,
        URI authorizationUrl,
        Theme theme,
        Language language,
        CompletableFuture<String> code,
        CompletableFuture<String> page
) {
}
