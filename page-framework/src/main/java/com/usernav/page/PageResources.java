package com.usernav.page;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Objects;

/** Main-thread resource scope. Disposes in reverse order, including resources added after close. */
public final class PageResources implements AutoCloseable {
    interface Errors { void report(RuntimeException error); }
    private final ArrayList<Registration> entries = new ArrayList<>();
    private final Errors errors;
    private Handler handler;
    private boolean closed;

    PageResources(Errors errors) { this.errors = errors; }

    public Registration add(Runnable cleanup) {
        PageNavigator.requireMainThread();
        Registration entry = new Registration(this, Objects.requireNonNull(cleanup, "cleanup"));
        if (closed) entry.close(); else entries.add(entry);
        return entry;
    }

    /** Uses a private Handler; cancellation never removes another page's callbacks. */
    public Registration postDelayed(Runnable task, long delayMillis) {
        PageNavigator.requireMainThread();
        Objects.requireNonNull(task, "task");
        if (delayMillis < 0) throw new IllegalArgumentException("delayMillis must be non-negative");
        if (handler == null) handler = new Handler(Looper.getMainLooper());
        Registration[] entry = new Registration[1];
        Runnable guarded = () -> {
            if (closed || entry[0].closed) return;
            entry[0].close();
            try { task.run(); } catch (RuntimeException error) { report(error); }
        };
        entry[0] = add(() -> handler.removeCallbacks(guarded));
        if (!closed) handler.postDelayed(guarded, delayMillis);
        return entry[0];
    }

    public Registration post(Runnable task) { return postDelayed(task, 0); }
    public boolean isClosed() { PageNavigator.requireMainThread(); return closed; }

    @Override public void close() {
        PageNavigator.requireMainThread();
        if (closed) return;
        closed = true;
        ArrayList<Registration> snapshot = new ArrayList<>(entries);
        for (int i = snapshot.size() - 1; i >= 0; i--) snapshot.get(i).close();
        entries.clear();
    }

    private void report(RuntimeException error) {
        Log.e("PageFramework", "Page resource callback failed", error);
        if (errors != null) errors.report(error);
    }

    public static final class Registration implements AutoCloseable {
        private PageResources owner;
        private Runnable cleanup;
        private boolean closed;
        private Registration(PageResources owner, Runnable cleanup) {
            this.owner = owner;
            this.cleanup = cleanup;
        }
        public boolean isClosed() { PageNavigator.requireMainThread(); return closed; }
        @Override public void close() {
            PageNavigator.requireMainThread();
            if (closed) return;
            closed = true;
            PageResources scope = owner;
            Runnable action = cleanup;
            owner = null;
            cleanup = null;
            scope.entries.remove(this);
            try { action.run(); } catch (RuntimeException error) { scope.report(error); }
        }
    }
}
