package com.usernav.page;

import java.util.List;

/** Main-thread notifications. Stack snapshots follow core queue commits, not request submission. */
public interface PageObserver {
    default void onNavigationRequested(NavigationRequest request) {}
    default void onNavigationBlocked(NavigationRequest request) {}
    default void onPageCreated(ViewPage page) {}
    default void onPageReused(ViewPage page) {}
    default void onPageDestroyed(ViewPage page) {}
    default void onStackChanged(List<ViewPage> pages) {}
    default void onError(ViewPage page, String operation, Throwable error) {}
}
