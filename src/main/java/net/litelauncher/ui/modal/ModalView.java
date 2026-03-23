package net.litelauncher.ui.modal;

import javax.swing.JComponent;

public interface ModalView {
    String key();
    JComponent component();
    default void beforeOpen() {}
    default void onDismiss() {}
}
