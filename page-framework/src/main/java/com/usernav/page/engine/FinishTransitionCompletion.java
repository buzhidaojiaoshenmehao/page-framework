package com.usernav.page.engine;

import com.usernav.page.internal.PageParameters;
import java.util.ArrayList;

/** Completes the deferred page removal after its exit animation finishes. */
public final class FinishTransitionCompletion implements Runnable {
    public final PageStack.PageRecord pageRecord;
    public final ArrayList<IHostPage> pagesToStart;
    public final PageStack pageStack;

    public FinishTransitionCompletion(PageStack pageStack, PageStack.PageRecord pageRecord,
            ArrayList<IHostPage> pagesToStart) {
        this.pageStack = pageStack;
        this.pageRecord = pageRecord;
        this.pagesToStart = pagesToStart;
    }

    @Override
    public final void run() {
        pageRecord.handleStop(false);
        PageParameters parameters = pageRecord.startRequest == null ? null : pageRecord.startRequest.parameters;
        if (parameters != null && parameters.stackActionListener != null) {
            PageStack.PageRecord previousRecord = pagesToStart == null || pagesToStart.isEmpty()
                    ? null : (PageStack.PageRecord) pagesToStart.get(pagesToStart.size() - 1);
            parameters.stackActionListener.onPageBack(pageRecord.controller,
                    previousRecord == null ? null : previousRecord.controller,
                    previousRecord == null ? null : previousRecord.resultData);
        }
        pageRecord.handleDestroy();
        if (pageRecord.isTransparent() && !pageStack.records.isEmpty()) {
            pageStack.records.get(pageStack.records.size() - 1).controller.onTransparentDismissed();
        }
        PageStack.startPages(pagesToStart, true, false);
        pageStack.transitionRunning = false;
    }
}
