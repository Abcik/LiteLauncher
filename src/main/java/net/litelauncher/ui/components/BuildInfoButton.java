package net.litelauncher.ui.components;

import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.UiTypography;
import net.litelauncher.ui.util.BuildTextFormatter;

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
import java.awt.geom.RoundRectangle2D;

public final class BuildInfoButton extends JButton {
    private static final int HEIGHT = 48;
    private static final int MIN_WIDTH = 252;
    private static final int MAX_WIDTH = 252;
    private static final int HORIZONTAL_PADDING = 34;
    private static final float HOVER_ARC = 16f;
    private static final float HOVER_INSET = 0f;
    private static final int SEGMENT_GAP = 14;

    private String buildText;

    public BuildInfoButton(String buildText) {
        this.buildText = BuildTextFormatter.normalize(buildText);
        setToolTipText("Version manager");
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(SwingConstants.CENTER);
        setMargin(new Insets(0, 14, 0, 14));
        setFont(UiTypography.regular(UiTypography.BODY));
        super.setText(null);
        Dimension size = measureSize();
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        ThemeManager.getInstance().addListener(this::refreshVisualText);
        refreshVisualText();
    }

    @Override
    public JToolTip createToolTip() {
        return new LiteToolTip(this);
    }

    public void setBuildText(String buildText) {
        this.buildText = BuildTextFormatter.normalize(buildText);
        refreshVisualText();
        Dimension size = measureSize();
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        revalidate();
        repaint();
    }

    public void refreshVisualText() {
        repaint();
    }

    @Override
    public String getText() {
        return buildText;
    }

    @Override
    public Dimension getPreferredSize() {
        return measureSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return measureSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return measureSize();
    }

    private Dimension measureSize() {
        BuildTextFormatter.Parts parts = BuildTextFormatter.parts(buildText);
        FontMetrics versionMetrics = getFontMetrics(UiTypography.regular(UiTypography.BODY));
        FontMetrics typeMetrics = getFontMetrics(UiTypography.regular(UiTypography.BODY));
        int textWidth = versionMetrics.stringWidth(parts.version());
        if (!parts.type().isEmpty()) {
            textWidth += SEGMENT_GAP + typeMetrics.stringWidth(parts.type());
        }
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, textWidth + HORIZONTAL_PADDING));
        return new Dimension(width, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        ThemePalette palette = ThemeManager.getInstance().palette();
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float width = Math.max(0, getWidth() - HOVER_INSET * 2f);
        if (getModel().isPressed()) {
            g2.setColor(palette.controlPressed);
            g2.fill(new RoundRectangle2D.Float(HOVER_INSET, 0, width, getHeight(), HOVER_ARC, HOVER_ARC));
        } else if (getModel().isRollover()) {
            g2.setColor(palette.controlHover);
            g2.fill(new RoundRectangle2D.Float(HOVER_INSET, 0, width, getHeight(), HOVER_ARC, HOVER_ARC));
        }

        paintBuildText(g2, palette);
        g2.dispose();
    }

    private void paintBuildText(Graphics2D g2, ThemePalette palette) {
        BuildTextFormatter.Parts parts = BuildTextFormatter.parts(buildText);
        UiTypography.applyCrispTextRendering(g2);

        FontMetrics versionMetrics = g2.getFontMetrics(UiTypography.regular(UiTypography.BODY));
        FontMetrics typeMetrics = g2.getFontMetrics(UiTypography.regular(UiTypography.BODY));

        int textWidth = versionMetrics.stringWidth(parts.version());
        if (!parts.type().isEmpty()) {
            textWidth += SEGMENT_GAP + typeMetrics.stringWidth(parts.type());
        }

        int drawX = (getWidth() - textWidth) / 2;
        int baseline = (getHeight() - Math.max(versionMetrics.getHeight(), typeMetrics.getHeight())) / 2
                + Math.max(versionMetrics.getAscent(), typeMetrics.getAscent());

        g2.setFont(UiTypography.regular(UiTypography.BODY));
        g2.setColor(palette.textPrimary);
        g2.drawString(parts.version(), drawX, baseline);
        drawX += versionMetrics.stringWidth(parts.version());

        if (!parts.type().isEmpty()) {
            drawX += SEGMENT_GAP;
            g2.setFont(UiTypography.regular(UiTypography.BODY));
            g2.setColor(palette.textSecondary);
            g2.drawString(parts.type(), drawX, baseline);
        }
    }
}
