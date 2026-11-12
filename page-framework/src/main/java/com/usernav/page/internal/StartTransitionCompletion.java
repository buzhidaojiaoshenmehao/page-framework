package com.usernav.page.internal;

import com.usernav.page.engine.PageStack;
import java.util.ArrayList;

public final class StartTransitionCompletion implements Runnable {
    public final ArrayList pagesToDestroy;
    public final ArrayList pagesToStop;
    public final ArrayList pagesToStart;
    public final PageStack pageStack;

    public StartTransitionCompletion(PageStack pageStack, ArrayList pagesToDestroy,
            ArrayList pagesToStop, ArrayList pagesToStart) {
        this.pageStack = pageStack;
        this.pagesToDestroy = pagesToDestroy;
        this.pagesToStop = pagesToStop;
        this.pagesToStart = pagesToStart;
    }

    @Override
    public final void run() {
        PageStack.destroyPages(pagesToDestroy, false);
        PageStack.stopPages(pagesToStop, false);
        PageStack.startPages(pagesToStart, true, false);
        pageStack.transitionRunning = false;
    }
}
