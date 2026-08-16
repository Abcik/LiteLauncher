package net.litelauncher.backend.download;

import java.io.Serial;

public final class DownloadException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean connectionProblem;

    public DownloadException(String message) {
        this(message, null, false);
    }

    public DownloadException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public DownloadException(String message, Throwable cause, boolean connectionProblem) {
        super(message, cause);
        this.connectionProblem = connectionProblem;
    }

    public boolean connectionProblem() {
        return connectionProblem;
    }
}
