package net.litelauncher.ui.components;

import net.litelauncher.state.AccountStore;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;
import net.litelauncher.ui.util.IconFactory;

import javax.swing.JButton;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class AccountChipButton extends JButton {
    private static final int HEIGHT = 42;
    private static final int MIN_WIDTH = 112;
    private static final int RIGHT_PADDING = 14;
    private static final float HOVER_ARC = 12f;
    private static final float AVATAR_SIZE = 26f;
    private static final float AVATAR_X = 12f;
    private static final int TEXT_X = 48;

    private final AccountStore accountStore;

    public AccountChipButton(AccountStore accountStore) {
        super(accountStore.getCurrentAccount());
        this.accountStore = accountStore;
        setHorizontalAlignment(SwingConstants.LEFT);
        setToolTipText("Accounts");
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
        setFont(UiTypography.medium(UiTypography.BODY));
        refreshText();
    }

    @Override
    public JToolTip createToolTip() {
        return new LiteToolTip(this);
    }

    public void refreshText() {
        setText(accountStore.getCurrentAccount());
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        String text = getText() == null ? "" : getText();
        FontMetrics metrics = getFontMetrics(getFont());
        int width = Math.max(MIN_WIDTH, TEXT_X + metrics.stringWidth(text) + RIGHT_PADDING);
        return new Dimension(width, HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(MIN_WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ThemePalette palette = ThemeManager.getInstance().palette();
        if (getModel().isPressed()) {
            g2.setColor(palette.controlPressed);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Math.round(HOVER_ARC), Math.round(HOVER_ARC));
        } else if (getModel().isRollover()) {
            g2.setColor(palette.controlHover);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Math.round(HOVER_ARC), Math.round(HOVER_ARC));
        }
        BufferedImage avatar = IconFactory.profileAvatarImage(palette);
        int avatarY = Math.round((getHeight() - AVATAR_SIZE) / 2f);
        g2.drawImage(avatar, Math.round(AVATAR_X), avatarY, null);
        UiTypography.applyTextRendering(g2);
        g2.setFont(getFont());
        g2.setColor(palette.textPrimary);
        FontMetrics fontMetrics = g2.getFontMetrics();
        int baseline = (getHeight() - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();
        String visualText = getText();
        if (visualText != null && !visualText.isEmpty()) {
            g2.drawString(visualText, TEXT_X, baseline);
        }
        g2.dispose();
    }
}
