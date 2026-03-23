package net.litelauncher.ui;

import net.litelauncher.app.ApplicationContext;
import net.litelauncher.ui.components.AccountChipButton;
import net.litelauncher.ui.components.HeaderIconButton;
import net.litelauncher.ui.modal.ModalCoordinator;
import net.litelauncher.ui.modal.ModalView;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.util.IconFactory;
import net.litelauncher.ui.util.WindowDragSupport;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class HeaderBar extends JPanel {
    private static final int HEADER_HEIGHT = 70;
    private static final int HEADER_BUTTON_GAP = 4;

    private final JLabel logoLabel = new JLabel();
    private final HeaderIconButton minimizeButton = new HeaderIconButton(null, "Minimize", false);
    private final HeaderIconButton closeButton = new HeaderIconButton(null, "Close", true);
    private final HeaderIconButton settingsButton = new HeaderIconButton(null, "Settings", false);
    private final HeaderIconButton themeButton = new HeaderIconButton(null, "Toggle theme", false);
    private final HeaderIconButton languageButton = new HeaderIconButton(null, "Language", false);
    private final AccountChipButton accountChipButton;

    public HeaderBar(JFrame frame,
                     ApplicationContext context,
                     ModalCoordinator modalCoordinator,
                     ModalView accountPopup,
                     ModalView languagePopup,
                     ModalView settingsPopup) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, HEADER_HEIGHT));

        WindowDragSupport dragSupport = new WindowDragSupport(frame);
        dragSupport.install(this);

        accountChipButton = new AccountChipButton(context.accountStore());
        themeButton.addActionListener(event -> ThemeManager.getInstance().toggleTheme());
        languageButton.addActionListener(event -> modalCoordinator.toggle(languagePopup));
        minimizeButton.addActionListener(event -> frame.setState(JFrame.ICONIFIED));
        closeButton.addActionListener(event -> frame.dispose());
        accountChipButton.addActionListener(event -> modalCoordinator.toggle(accountPopup));
        settingsButton.addActionListener(event -> modalCoordinator.toggle(settingsPopup));

        add(createLeftSection(dragSupport), BorderLayout.WEST);
        add(createDragFiller(dragSupport), BorderLayout.CENTER);
        add(createRightSection(dragSupport), BorderLayout.EAST);

        context.accountStore().addListener(accountChipButton::refreshText);
        ThemeManager.getInstance().addListener(this::applyTheme);
        applyTheme();
    }

    private JPanel createLeftSection(WindowDragSupport dragSupport) {
        logoLabel.setHorizontalAlignment(SwingConstants.LEFT);
        dragSupport.install(logoLabel);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(logoLabel, BorderLayout.WEST);
        dragSupport.install(panel);
        return panel;
    }

    private JPanel createDragFiller(WindowDragSupport dragSupport) {
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        dragSupport.install(filler);
        return filler;
    }

    private JPanel createRightSection(WindowDragSupport dragSupport) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(accountChipButton);
        row.add(Box.createHorizontalStrut(HEADER_BUTTON_GAP));
        row.add(themeButton);
        row.add(Box.createHorizontalStrut(HEADER_BUTTON_GAP));
        row.add(languageButton);
        row.add(Box.createHorizontalStrut(HEADER_BUTTON_GAP));
        row.add(settingsButton);
        row.add(Box.createHorizontalStrut(HEADER_BUTTON_GAP));
        row.add(minimizeButton);
        row.add(Box.createHorizontalStrut(2));
        row.add(closeButton);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.add(row, BorderLayout.EAST);
        dragSupport.install(rightPanel);
        return rightPanel;
    }

    private void applyTheme() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        logoLabel.setIcon(IconFactory.logoWordmarkIcon(palette));
        themeButton.setIcon(IconFactory.bulbIcon(palette));
        languageButton.setIcon(IconFactory.globeIcon(palette));
        settingsButton.setIcon(IconFactory.settingsIcon(palette));
        minimizeButton.setIcon(IconFactory.minimizeIcon(palette));
        closeButton.setIcon(IconFactory.closeIcon(palette));
        accountChipButton.refreshText();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ThemeManager.getInstance().palette().separator);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        g2.dispose();
    }
}
