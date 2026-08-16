package net.litelauncher.backend;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean();

    public void cancel() {
        cancelled.set(true);
    }

    public boolean cancelled() {
        return cancelled.get() || Thread.currentThread().isInterrupted();
    }

    public void throwIfCancelled() {
        if (cancelled()) throw new CancellationException("Operation cancelled.");
    }
}
