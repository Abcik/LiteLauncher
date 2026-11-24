package net.litelauncher.backend.auth;

import java.io.Serial;

public final class AuthException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean expiredSession;
    private final boolean accessTokenRejected;

    public AuthException(String message) {
        this(message, null, false, false);
    }

    public AuthException(String message, Throwable cause) {
        this(message, cause, false, false);
    }

    private AuthException(String message, Throwable cause, boolean expiredSession, boolean accessTokenRejected) {
        super(message, cause);
        this.expiredSession = expiredSession;
        this.accessTokenRejected = accessTokenRejected;
    }

    public static AuthException expiredSession(String message) {
        return new AuthException(message, null, true, false);
    }

    public boolean expiredSession() {
        return expiredSession;
    }

    public static AuthException accessTokenRejected(String message) {
        return new AuthException(message, null, false, true);
    }

    public boolean accessTokenRejected() {
        return accessTokenRejected;
    }
}
