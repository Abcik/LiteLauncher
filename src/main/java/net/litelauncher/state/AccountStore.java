package net.litelauncher.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AccountStore {
    public static final String MICROSOFT_PROVIDER_LABEL = "Microsoft account";

    private final List<AccountEntry> accounts = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private String currentAccount;

    public AccountStore() {
        accounts.add(new AccountEntry("Primary account"));
        currentAccount = accounts.get(0).displayName;
    }

    public List<String> getAccounts() {
        List<String> names = new ArrayList<>(accounts.size());
        for (AccountEntry account : accounts) {
            names.add(account.displayName);
        }
        return Collections.unmodifiableList(names);
    }

    public String getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(String displayName) {
        AccountEntry entry = findAccount(displayName);
        if (entry != null && !entry.displayName.equals(currentAccount)) {
            currentAccount = entry.displayName;
            notifyListeners();
        }
    }

    public void addMicrosoftAccount() {
        String displayName = createNextAccountName();
        accounts.add(new AccountEntry(displayName));
        currentAccount = displayName;
        notifyListeners();
    }

    public void removeAccount(String displayName) {
        if (displayName == null || accounts.size() <= 1) {
            return;
        }
        AccountEntry entry = findAccount(displayName);
        if (entry == null) {
            return;
        }
        accounts.remove(entry);
        if (displayName.equals(currentAccount)) {
            currentAccount = accounts.get(0).displayName;
        }
        notifyListeners();
    }

    public String getProviderLabel(String displayName) {
        return findAccount(displayName) == null ? "" : MICROSOFT_PROVIDER_LABEL;
    }

    public void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private AccountEntry findAccount(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (AccountEntry account : accounts) {
            if (account.displayName.equalsIgnoreCase(displayName)) {
                return account;
            }
        }
        return null;
    }

    private String createNextAccountName() {
        int index = accounts.size() + 1;
        String candidate = "Microsoft account " + index;
        while (findAccount(candidate) != null) {
            index++;
            candidate = "Microsoft account " + index;
        }
        return candidate;
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    private record AccountEntry(String displayName) {
    }
}
