package net.litelauncher.backend.launch;

import net.litelauncher.backend.InformationMessages;

public final class GameLaunchException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String informationMessage;

    public GameLaunchException(String message) {
        this(message, null, InformationMessages.LAUNCH_ERROR);
    }

    public GameLaunchException(String message, Throwable cause) {
        this(message, cause, InformationMessages.LAUNCH_ERROR);
    }

    public GameLaunchException(String message, String informationMessage) {
        this(message, null, informationMessage);
    }

    public GameLaunchException(String message, Throwable cause, String informationMessage) {
        super(message, cause);
        this.informationMessage = informationMessage == null || informationMessage.isBlank()
                ? InformationMessages.LAUNCH_ERROR
                : informationMessage;
    }

    public String informationMessage() {
        return informationMessage;
    }
}
