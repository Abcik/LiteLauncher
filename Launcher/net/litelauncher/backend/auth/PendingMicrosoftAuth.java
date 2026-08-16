package net.litelauncher.backend.auth;

import net.litelauncher.Language;
import net.litelauncher.Theme;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public record PendingMicrosoftAuth(
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
