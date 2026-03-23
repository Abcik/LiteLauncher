package net.litelauncher.ui;

import net.litelauncher.app.ApplicationContext;
import net.litelauncher.ui.components.WindowSurfacePanel;
import net.litelauncher.ui.modal.ModalCoordinator;
import net.litelauncher.ui.modal.OverlayHost;
import net.litelauncher.ui.popup.AccountPopup;
import net.litelauncher.ui.popup.LanguagePopup;
import net.litelauncher.ui.popup.SettingsPopup;
import net.litelauncher.ui.popup.VersionPopup;
import net.litelauncher.ui.theme.ThemeManager;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

public final class LauncherFrame extends JFrame {
    private final OverlayHost overlayHost = new OverlayHost();
    private final ModalCoordinator modalCoordinator = new ModalCoordinator(overlayHost);

    public LauncherFrame(ApplicationContext context) {
        super("LiteLauncher");
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 660));
        setSize(1120, 760);
        setLocationRelativeTo(null);

        AccountPopup accountPopup = new AccountPopup(context.accountStore());
        LanguagePopup languagePopup = new LanguagePopup(context.launcherSettings());
        SettingsPopup settingsPopup = new SettingsPopup(context.launcherSettings());
        VersionPopup versionPopup = new VersionPopup(context.versionCatalogService(), context.launcherSettings(), context.launchSelection());

        accountPopup.setCloseAction(overlayHost::hideModal);
        languagePopup.setCloseAction(overlayHost::hideModal);
        settingsPopup.setCloseAction(overlayHost::hideModal);
        versionPopup.setCloseAction(overlayHost::hideModal);

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        WindowSurfacePanel surface = new WindowSurfacePanel();
        surface.setLayout(new BorderLayout());
        surface.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 10));
        surface.add(new HeaderBar(this, context, modalCoordinator, accountPopup, languagePopup, settingsPopup), BorderLayout.NORTH);
        surface.add(new HomePanel(context, modalCoordinator, versionPopup), BorderLayout.CENTER);

        root.add(surface, BorderLayout.CENTER);
        setContentPane(root);
        setGlassPane(overlayHost);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateWindowShape();
            }
        });

        ThemeManager.getInstance().addListener(this::refreshTheme);
        updateWindowShape();
    }

    private void refreshTheme() {
        repaint();
        overlayHost.repaint();
    }

    private void updateWindowShape() {
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 40, 40));
    }
}
