package com.usernav.page.engine;

import android.view.View;
import android.util.Log;
import com.usernav.page.PageState;
import java.util.ArrayList;

/** Connects host lifecycle events to the page records and releases queued work on destruction. */
public final class HostLifecycle {
    private HostLifecycle() {}
    public static void attachHost(Pages pages, Object host) { pages.pageStack.host = host; }
    public static IPageFramework framework(Pages pages) { return pages.framework; }
    public static boolean isBusy(Pages pages) {
        return pages.pageStack.transitionRunning || !pages.pageStack.pendingActions.isEmpty();
    }
    public static boolean isHostStarted(Pages pages) { return pages.pageStack.hostState >= 2; }
    public static PageState state(Pages pages, IPageController controller) {
        for (PageStack.PageRecord page : pages.pageStack.records) {
            if (page.controller != controller) continue;
            switch (page.state) {
                case 1: return PageState.CREATED;
                case 2: case 3: case 5: return PageState.STARTED;
                case 4: return PageState.RESUMED;
                case 6: return PageState.DESTROYED;
                default: return PageState.INITIALIZED;
            }
        }
        return PageState.DESTROYED;
    }
    public static void pause(Pages pages) { pages.pause(); }
    public static void start(Pages pages) {
        PageStack stack = pages.pageStack;
        stack.hostState = 2;
        for (IHostPage page : stack.getVisiblePages(stack.records.size() - 1)) page.handleStart(true, false, true);
    }
    public static void resume(Pages pages) {
        PageStack stack = pages.pageStack;
        stack.hostState = 4;
        for (IHostPage page : stack.getVisiblePages(stack.records.size() - 1)) page.handleResume(true);
    }
    public static void stop(Pages pages) {
        PageStack stack = pages.pageStack;
        stack.hostState = 1;
        PageStack.stopPages(stack.getVisiblePages(stack.records.size() - 1), true);
    }
    public static int back(Pages pages) {
        PageStack stack = pages.pageStack;
        return stack.records.isEmpty() ? 0 : stack.records.get(stack.records.size() - 1).handleBackPressed();
    }
    public static void destroy(Pages pages) {
        PageStack stack = pages.pageStack;
        stack.pendingActions.clear();
        stack.actionHandler.removeCallbacksAndMessages(null);
        stack.transitionDispatcher.handler.removeCallbacksAndMessages(null);
        for (PageStack.PageRecord page : new ArrayList<>(stack.records)) {
            View view = page.view;
            if (view != null) view.clearAnimation();
        }
        try {
            ArrayList<IHostPage> snapshot = new ArrayList<IHostPage>(stack.records);
            for (IHostPage page : snapshot) attempt(() -> page.handlePause(true));
            for (IHostPage page : snapshot) attempt(() -> page.handleStop(true));
            for (IHostPage page : snapshot) attempt(page::handleDestroy);
        } finally {
            stack.records.clear();
            stack.pendingActions.clear();
            stack.actionHandler.removeCallbacksAndMessages(null);
            stack.transitionDispatcher.handler.removeCallbacksAndMessages(null);
            stack.transitionDispatcher.container.removeAllViews();
            stack.transitionRunning = false;
            stack.hostState = 1;
            stack.host = null;
            pages.framework.pagesReference.clear();
        }
    }
    private static void attempt(Runnable action) {
        try { action.run(); }
        catch (RuntimeException error) { Log.e("PageFramework", "Core host cleanup failed", error); }
    }
}
