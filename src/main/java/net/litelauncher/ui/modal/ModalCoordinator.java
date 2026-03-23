package net.litelauncher.ui.modal;

public final class ModalCoordinator {
    private final OverlayHost overlayHost;

    public ModalCoordinator(OverlayHost overlayHost) {
        this.overlayHost = overlayHost;
    }

    public void toggle(ModalView view) {
        overlayHost.toggleModal(view.key(), view.component(), view::beforeOpen, view::onDismiss);
    }

    public void hide() {
        overlayHost.hideModal();
    }
}
