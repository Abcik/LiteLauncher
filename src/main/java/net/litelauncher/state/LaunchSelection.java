package net.litelauncher.state;

import java.util.ArrayList;
import java.util.List;

public final class LaunchSelection {
    private final List<Runnable> listeners = new ArrayList<>();
    private String selectedBuild;

    public LaunchSelection(String selectedBuild) {
        this.selectedBuild = selectedBuild == null ? "" : selectedBuild.trim();
    }

    public String getSelectedBuild() {
        return selectedBuild;
    }

    public void setSelectedBuild(String selectedBuild) {
        String normalized = selectedBuild == null ? "" : selectedBuild.trim();
        if (!this.selectedBuild.equals(normalized)) {
            this.selectedBuild = normalized;
            notifyListeners();
        }
    }

    public void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
