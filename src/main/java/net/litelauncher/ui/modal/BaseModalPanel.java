package net.litelauncher.ui.modal;

import javax.swing.JPanel;
import java.awt.Dimension;

public abstract class BaseModalPanel extends JPanel implements ModalView {
    private final String key;
    private Runnable closeAction;

    protected BaseModalPanel(String key, Dimension preferredSize) {
        this.key = key;
        setOpaque(false);
        setPreferredSize(preferredSize);
    }

    @Override
    public final String key() {
        return key;
    }

    @Override
    public final JPanel component() {
        return this;
    }

    public final void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    protected final void requestClose() {
        if (closeAction != null) {
            closeAction.run();
        }
    }
}
