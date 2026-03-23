package net.litelauncher.ui.modal;

import net.litelauncher.ui.components.WindowSurfacePanel;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public final class OverlayHost extends JPanel {
    private static final int SHELL_INSET = 16;
    private static final int GAUSSIAN_RADIUS = 4;
    private static final float GAUSSIAN_SIGMA = 2.5f;
    private static final int SURFACE_ARC = 34;
    private static final int SURFACE_BLUR_INSET = 2;

    private final JPanel shellPanel = new JPanel(new GridBagLayout());
    private Runnable dismissAction;
    private String activeKey;
    private BufferedImage blurredBackdrop;
    private Rectangle blurredBackdropBounds;

    public OverlayHost() {
        super(new GridBagLayout());
        setOpaque(false);
        setVisible(false);

        shellPanel.setOpaque(false);
        shellPanel.setBorder(BorderFactory.createEmptyBorder(SHELL_INSET, SHELL_INSET, SHELL_INSET, SHELL_INSET));
        add(shellPanel);

        MouseAdapter outsideClickHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!isClickInsideContent(event)) {
                    hideModal();
                }
            }
        };
        addMouseListener(outsideClickHandler);
        addMouseMotionListener(outsideClickHandler);
        registerKeyboardAction(event -> hideModal(), KeyStroke.getKeyStroke("ESCAPE"), WHEN_IN_FOCUSED_WINDOW);
    }

    public void toggleModal(String key, JComponent content, Runnable beforeShow, Runnable onDismiss) {
        if (isShowing(key)) {
            hideModal();
        } else {
            showModal(key, content, beforeShow, onDismiss);
        }
    }

    public boolean isShowing(String key) {
        return isVisible() && key != null && key.equals(activeKey);
    }

    public void showModal(String key, JComponent content, Runnable beforeShow, Runnable onDismiss) {
        if (content == null) {
            return;
        }
        if (beforeShow != null) {
            beforeShow.run();
        }
        refreshBlurredBackdrop();
        activeKey = key;
        dismissAction = onDismiss;
        shellPanel.removeAll();
        shellPanel.add(content);
        prepareContentForDisplay(content);
        setVisible(true);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(content::requestFocusInWindow);
    }

    public void hideModal() {
        if (!isVisible()) {
            return;
        }
        Runnable action = dismissAction;
        dismissAction = null;
        activeKey = null;
        blurredBackdrop = null;
        blurredBackdropBounds = null;
        setVisible(false);
        shellPanel.removeAll();
        if (action != null) {
            action.run();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (blurredBackdrop != null && blurredBackdropBounds != null) {
            int x = blurredBackdropBounds.x + SURFACE_BLUR_INSET;
            int y = blurredBackdropBounds.y + SURFACE_BLUR_INSET;
            int width = Math.max(0, blurredBackdropBounds.width - SURFACE_BLUR_INSET * 2);
            int height = Math.max(0, blurredBackdropBounds.height - SURFACE_BLUR_INSET * 2);
            if (width > 0 && height > 0) {
                RoundRectangle2D.Float clip = new RoundRectangle2D.Float(x, y, width, height, SURFACE_ARC, SURFACE_ARC);
                g2.clip(clip);
                g2.drawImage(blurredBackdrop, blurredBackdropBounds.x, blurredBackdropBounds.y,
                    blurredBackdropBounds.width, blurredBackdropBounds.height, null);
            }
        } else {
            ThemePalette palette = ThemeManager.getInstance().palette();
            g2.setColor(withAlpha(palette.dark ? Color.BLACK : palette.frameBase, palette.dark ? 90 : 64));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }

    private void prepareContentForDisplay(JComponent content) {
        Dimension contentSize = content.getPreferredSize();
        if (contentSize != null) {
            content.setSize(contentSize);
        }
        layoutTree(content);
        Dimension shellSize = shellPanel.getPreferredSize();
        if (shellSize != null) {
            shellPanel.setSize(shellSize);
        }
        layoutTree(shellPanel);
    }

    private void layoutTree(Component component) {
        component.invalidate();
        if (component instanceof Container) {
            Container container = (Container) component;
            container.doLayout();
            for (Component child : container.getComponents()) {
                Dimension preferredSize = child.getPreferredSize();
                if (preferredSize != null) {
                    child.setSize(preferredSize);
                }
                layoutTree(child);
            }
            container.validate();
        }
    }

    private void refreshBlurredBackdrop() {
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) {
            blurredBackdrop = null;
            blurredBackdropBounds = null;
            return;
        }

        WindowSurfacePanel surface = findWindowSurface(rootPane.getContentPane());
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) {
            blurredBackdrop = null;
            blurredBackdropBounds = null;
            return;
        }

        Rectangle surfaceBounds = SwingUtilities.convertRectangle(surface.getParent(), surface.getBounds(), this);
        BufferedImage snapshot = new BufferedImage(surface.getWidth(), surface.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = snapshot.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        surface.paint(g2);
        g2.dispose();

        blurredBackdrop = blurBackdrop(snapshot);
        blurredBackdropBounds = surfaceBounds;
    }

    private WindowSurfacePanel findWindowSurface(Component component) {
        if (component instanceof WindowSurfacePanel) {
            return (WindowSurfacePanel) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                WindowSurfacePanel surface = findWindowSurface(child);
                if (surface != null) {
                    return surface;
                }
            }
        }
        return null;
    }

    private BufferedImage blurBackdrop(BufferedImage source) {
        BufferedImage working = copyImage(source);
        BufferedImage softened = applyGaussianBlur(working, GAUSSIAN_RADIUS, GAUSSIAN_SIGMA);

        ThemePalette palette = ThemeManager.getInstance().palette();
        Graphics2D graphics = softened.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setComposite(AlphaComposite.SrcOver.derive(palette.dark ? 0.10f : 0.05f));
        graphics.setColor(palette.dark ? Color.BLACK : palette.frameBase);
        graphics.fillRect(0, 0, softened.getWidth(), softened.getHeight());
        graphics.dispose();
        return softened;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(ThemeManager.getInstance().palette().surfaceBackground);
        graphics.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private BufferedImage applyGaussianBlur(BufferedImage source, int radius, float sigma) {
        if (radius <= 0) {
            return source;
        }
        float[] kernelValues = createGaussianKernel(radius, sigma);
        ConvolveOp horizontal = new ConvolveOp(new Kernel(kernelValues.length, 1, kernelValues), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, kernelValues.length, kernelValues), ConvolveOp.EDGE_NO_OP, null);

        BufferedImage horizontalPass = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        horizontal.filter(source, horizontalPass);

        BufferedImage verticalPass = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        vertical.filter(horizontalPass, verticalPass);
        return verticalPass;
    }

    private float[] createGaussianKernel(int radius, float sigma) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        float sigmaSquared = sigma * sigma;
        float normalizer = (float) (Math.sqrt(2.0d * Math.PI) * sigma);
        float total = 0f;

        for (int i = 0; i < size; i++) {
            int distance = i - radius;
            float value = (float) (Math.exp(-(distance * distance) / (2.0f * sigmaSquared)) / normalizer);
            kernel[i] = value;
            total += value;
        }
        for (int i = 0; i < size; i++) {
            kernel[i] /= total;
        }
        return kernel;
    }

    private boolean isClickInsideContent(MouseEvent event) {
        Component deepest = SwingUtilities.getDeepestComponentAt(this, event.getX(), event.getY());
        return deepest != null && deepest != this && SwingUtilities.isDescendingFrom(deepest, shellPanel);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
