package net.litelauncher.backend;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackendExecutor {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void run(Runnable task) {
        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

}