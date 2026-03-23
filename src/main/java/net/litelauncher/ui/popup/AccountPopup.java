package net.litelauncher.ui.popup;

import net.litelauncher.state.AccountStore;
import net.litelauncher.ui.components.ActionButton;
import net.litelauncher.ui.components.HeaderIconButton;
import net.litelauncher.ui.components.PopupInteractiveRow;
import net.litelauncher.ui.components.PopupMetrics;
import net.litelauncher.ui.components.PopupScaffold;
import net.litelauncher.ui.components.SurfacePanel;
import net.litelauncher.ui.modal.BaseModalPanel;
import net.litelauncher.ui.theme.ThemeManager;
import net.litelauncher.ui.theme.ThemePalette;
import net.litelauncher.ui.theme.ThemeStyler;
import net.litelauncher.ui.theme.UiTypography;
import net.litelauncher.ui.util.IconFactory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

public final class AccountPopup extends BaseModalPanel {
    private static final int FOOTER_ROW_HEIGHT = 52;

    private final AccountStore accountStore;
    private final JLabel titleLabel = new JLabel("Accounts");
    private final JLabel subtitleLabel = new JLabel("Manage connected Microsoft accounts.");
    private final JPanel accountsContainer = new JPanel();
    private final JScrollPane accountsScrollPane;
    private final HeaderIconButton closeButton = new HeaderIconButton(null, "Close", false);
    private final ActionButton microsoftButton = createFooterButton("Add Microsoft account", ActionButton.Variant.ACCENT);
    private final List<String> renderedAccounts = new ArrayList<>();

    public AccountPopup(AccountStore accountStore) {
        super("accounts", new Dimension(590, 560));
        this.accountStore = accountStore;
        setLayout(new BorderLayout());

        SurfacePanel panel = PopupScaffold.createRootPanel();
        panel.add(PopupScaffold.createHeader(titleLabel, subtitleLabel, closeButton), BorderLayout.NORTH);
        accountsContainer.setOpaque(false);
        accountsContainer.setLayout(new BoxLayout(accountsContainer, BoxLayout.Y_AXIS));
        accountsScrollPane = PopupScaffold.createScrollPane(accountsContainer);
        panel.add(accountsScrollPane, BorderLayout.CENTER);
        panel.add(createFooter(), BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);

        bindActions();

        accountStore.addListener(this::handleStoreChanged);
        ThemeManager.getInstance().addListener(this::applyTheme);
        rebuildAccounts();
        applyTheme();
    }

    @Override
    public void beforeOpen() {
        accountsScrollPane.getVerticalScrollBar().setValue(0);
        handleStoreChanged();
    }

    private void bindActions() {
        closeButton.addActionListener(event -> requestClose());
        microsoftButton.addActionListener(event -> accountStore.addMicrosoftAccount());
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(microsoftButton, BorderLayout.CENTER);
        return footer;
    }

    private void handleStoreChanged() {
        List<String> currentAccounts = accountStore.getAccounts();
        if (currentAccounts.equals(renderedAccounts)) {
            refreshAccountSelection();
            return;
        }
        rebuildAccounts();
    }

    private void rebuildAccounts() {
        renderedAccounts.clear();
        renderedAccounts.addAll(accountStore.getAccounts());
        accountsContainer.removeAll();
        boolean first = true;
        for (String account : renderedAccounts) {
            if (!first) {
                accountsContainer.add(Box.createVerticalStrut(PopupMetrics.STACK_GAP));
            }
            accountsContainer.add(createAccountRow(account));
            first = false;
        }
        accountsContainer.revalidate();
        accountsContainer.repaint();
        applyTheme();
        SwingUtilities.invokeLater(this::syncRowHoverStates);
    }

    private void refreshAccountSelection() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        for (Component component : accountsContainer.getComponents()) {
            refreshAccountComponent(component, palette);
        }
        accountsContainer.repaint();
    }

    private void syncRowHoverStates() {
        for (Component component : accountsContainer.getComponents()) {
            syncRowHoverState(component);
        }
    }

    private void syncRowHoverState(Component component) {
        if (component instanceof AccountRow row) {
            row.syncHoverFromPointer();
            return;
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                syncRowHoverState(child);
            }
        }
    }

    private Component createAccountRow(String accountName) {
        AccountRow row = new AccountRow(accountName,
                accountName.equals(accountStore.getCurrentAccount()),
                () -> accountStore.setCurrentAccount(accountName));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void applyTheme() {
        ThemePalette palette = ThemeManager.getInstance().palette();
        PopupScaffold.applyHeaderTheme(titleLabel, subtitleLabel, closeButton, palette);
        ThemeStyler.styleScrollPane(accountsScrollPane, palette, accountsContainer, PopupScaffold.SCROLLBAR_CONTENT_GAP);
        for (Component component : accountsContainer.getComponents()) {
            refreshAccountComponent(component, palette);
        }
        repaint();
    }

    private void refreshAccountComponent(Component component, ThemePalette palette) {
        if (component instanceof AccountRow row) {
            row.setSelected(row.accountName.equals(accountStore.getCurrentAccount()));
            row.refreshTheme(palette);
            return;
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                refreshAccountComponent(child, palette);
            }
        }
    }

    private ActionButton createFooterButton(String text, ActionButton.Variant variant) {
        ActionButton button = new ActionButton(text, variant);
        button.setPreferredSize(new Dimension(0, FOOTER_ROW_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, FOOTER_ROW_HEIGHT));
        return button;
    }

    private final class AccountRow extends PopupInteractiveRow {
        private final String accountName;
        private final JLabel titleLabel = new JLabel();
        private final JLabel stateLabel = new JLabel();
        private final HeaderIconButton deleteButton = new HeaderIconButton(null, "Delete account", false);

        private AccountRow(String accountName, boolean selected, Runnable onClick) {
            super(PopupMetrics.DEFAULT_ARC, PopupMetrics.STANDARD_ROW_PADDING, true);
            this.accountName = accountName;
            setLayout(new BorderLayout(12, 0));
            PopupMetrics.applyStandardSize(this, PopupMetrics.standardRowSize(), PopupMetrics.STANDARD_ROW_HEIGHT);
            titleLabel.setFont(UiTypography.medium(PopupMetrics.ROW_TITLE_SIZE));
            stateLabel.setFont(UiTypography.regular(PopupMetrics.ROW_SUBTITLE_SIZE));
            add(PopupMetrics.createCenteredTwoLineText(titleLabel, stateLabel), BorderLayout.CENTER);
            add(deleteButton, BorderLayout.EAST);
            setOnClick(onClick);
            deleteButton.addActionListener(event -> accountStore.removeAccount(accountName));
            setSelected(selected);
            refreshTheme(ThemeManager.getInstance().palette());
        }

        private void setSelected(boolean selected) {
            setSelectedState(selected);
            refreshTheme(ThemeManager.getInstance().palette());
        }

        private void refreshTheme(ThemePalette palette) {
            Color titleColor = isSelectedState() ? PopupMetrics.ACCENT_TEXT : palette.textPrimary;
            Color subtitleColor = isSelectedState() ? PopupMetrics.ACCENT_TEXT : palette.textSecondary;
            titleLabel.setText(accountName);
            stateLabel.setText(accountStore.getProviderLabel(accountName));
            titleLabel.setForeground(titleColor);
            stateLabel.setForeground(subtitleColor);
            deleteButton.setIcon(IconFactory.closeIcon(palette));
            deleteButton.setEnabled(accountStore.getAccounts().size() > 1);
            repaint();
        }
    }
}
