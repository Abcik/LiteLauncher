package net.litelauncher.backend.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.litelauncher.Language;
import net.litelauncher.backend.InformationMessages;
import net.litelauncher.Theme;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class MicrosoftCallbackServer {

    private final SecureRandom random = new SecureRandom();
    private final MicrosoftCallbackPageRenderer pages;

    private HttpServer server;
    private URI redirectUri;
    private PendingMicrosoftAuth pending;

    public MicrosoftCallbackServer(MicrosoftCallbackPageRenderer pages) {
        this.pages = pages == null ? MicrosoftCallbackPageRenderer.fallback() : pages;
    }

    public synchronized void open() throws AuthException {
        if (server != null) return;

        try {
            HttpServer activeServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            activeServer.createContext(MicrosoftAuthConfig.REDIRECT_PATH, this::handleCallback);
            activeServer.start();

            int port = activeServer.getAddress().getPort();
            if (port <= 0) {
                activeServer.stop(0);
                throw new AuthException("Microsoft callback server did not receive a valid port.");
            }

            server = activeServer;
            redirectUri = URI.create("http://localhost:" + port + MicrosoftAuthConfig.REDIRECT_PATH);
        } catch (IOException exception) {
            throw new AuthException("Unable to open Microsoft callback server.", exception);
        }
    }

    public void close() {
        HttpServer closingServer;
        PendingMicrosoftAuth closingPending;
        synchronized (this) {
            closingServer = server;
            closingPending = pending;
            server = null;
            redirectUri = null;
            pending = null;
        }

        if (closingPending != null) {
            AuthException exception = new AuthException("Microsoft sign-in was cancelled.");
            closingPending.code().completeExceptionally(exception);
            closingPending.page().complete(pages.errorHtml(closingPending.theme(), closingPending.language(), InformationMessages.WEB_AUTH_ERROR));
        }
        if (closingServer != null) closingServer.stop(0);
    }

    public synchronized PendingMicrosoftAuth start(Theme theme, Language language) throws AuthException {
        if (server == null || redirectUri == null) throw new AuthException("Microsoft callback server is not open.");
        if (pending != null) throw new AuthException("Microsoft sign-in is already running.");

        String state = UUID.randomUUID().toString();
        String codeVerifier = randomBase64Url(32);
        String codeChallenge = sha256Base64Url(codeVerifier);

        Map<String, String> query = new LinkedHashMap<>();
        query.put("client_id", MicrosoftAuthConfig.CLIENT_ID);
        query.put("response_type", "code");
        query.put("redirect_uri", redirectUri.toString());
        query.put("scope", MicrosoftAuthConfig.SCOPE);
        query.put("state", state);
        query.put("code_challenge", codeChallenge);
        query.put("code_challenge_method", "S256");
        query.put("prompt", "select_account");

        pending = new PendingMicrosoftAuth(
                state,
                codeVerifier,
                redirectUri,
                URI.create(MicrosoftAuthConfig.AUTHORIZE_URL + '?' + AuthUtils.formEncode(query)),
                theme == null ? Theme.LIGHT : theme,
                language == null ? Language.ENGLISH : language,
                new CompletableFuture<>(),
                new CompletableFuture<>()
        );
        return pending;
    }

    public synchronized void clear(PendingMicrosoftAuth auth) {
        if (pending == auth) pending = null;
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        PendingMicrosoftAuth auth;
        synchronized (this) {
            auth = pending;
        }

        if (auth == null) {
            writePage(exchange, 400, pages.errorHtml(Theme.LIGHT, Language.ENGLISH, InformationMessages.WEB_AUTH_ERROR));
            return;
        }

        try {
            Map<String, String> query = AuthUtils.parseQuery(exchange.getRequestURI().getRawQuery());
            String error = query.get("error");
            if (AuthUtils.hasText(error)) {
                String description = AuthUtils.firstText(query.get("error_description"), error);
                completeWithError(exchange, auth, "Microsoft returned an error: " + description);
                return;
            }

            String code = query.get("code");
            String state = query.get("state");
            if (!auth.state().equals(state) || !AuthUtils.hasText(code)) {
                completeWithError(exchange, auth, "Microsoft returned an invalid callback.");
                return;
            }

            auth.code().complete(code);
            writePage(exchange, 200, auth.page().get(5, TimeUnit.MINUTES));
        } catch (Exception exception) {
            if (!auth.code().isDone()) auth.code().completeExceptionally(new AuthException("Failed to process Microsoft callback.", exception));
            writePage(exchange, 500, pages.errorHtml(auth.theme(), auth.language(), InformationMessages.WEB_AUTH_ERROR));
        }
    }

    private void completeWithError(HttpExchange exchange, PendingMicrosoftAuth auth, String message) throws IOException {
        auth.code().completeExceptionally(new AuthException(message));
        writePage(exchange, 400, pages.errorHtml(auth.theme(), auth.language(), InformationMessages.WEB_AUTH_ERROR));
    }

    private void writePage(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, data.length);
        try (exchange; OutputStream stream = exchange.getResponseBody()) {
            stream.write(data);
        }
    }

    private String randomBase64Url(int bytes) {
        byte[] data = new byte[bytes];
        random.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String sha256Base64Url(String value) throws AuthException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new AuthException("Unable to prepare Microsoft sign-in.", exception);
        }
    }
}
