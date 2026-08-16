package net.litelauncher.backend.auth;

import java.io.Serial;

public final class AuthException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean expiredSession;

    public AuthException(String message) {
        this(message, null, false);
    }

    public AuthException(String message, Throwable cause) {
        this(message, cause, false);
    }

    private AuthException(String message, Throwable cause, boolean expiredSession) {
        super(message, cause);
        this.expiredSession = expiredSession;
    }

    public static AuthException expiredSession(String message) {
        return new AuthException(message, null, true);
    }

    public boolean expiredSession() {
        return expiredSession;
    }
}
