package com.usernav.page.engine;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import com.usernav.page.internal.FinishPageRequest;
import com.usernav.page.internal.PageLogger;
import com.usernav.page.internal.PageParameters;
import com.usernav.page.internal.PageTransitionDispatcher;
import java.util.ArrayList;

/** Removes a page immediately or starts its exit transition before removal. */
public final class FinishPageAction implements PageStack.StackAction {
    public final FinishPageRequest request;
    public final PageStack pageStack;

    public FinishPageAction(PageStack pageStack, FinishPageRequest request) {
        this.pageStack = pageStack;
        this.request = request;
    }

    @Override
    public final boolean handleAction() {
        PageStack.PageRecord record = pageStack.findPage(request.pageIdentity);
        if (record == null) {
            PageLogger.error("PageStack", "doFinish: found is null. params.mPageId = " + request.pageIdentity);
            return true;
        }
        request.pageRecord = record;
        request.view = record.view;
        if (record.state == 6) {
            PageLogger.error("PageStack", "doFinish: page is finished, do nothing: c="
                    + record.startRequest.controllerClass);
            return true;
        }
        if (pageStack.transitionRunning) {
            PageLogger.warn("PageStack", "doFinish: pending");
            return false;
        }
        boolean finishImmediately = pageStack.hostState <= 2 || record.state < 2
                || record.state >= 6 || !request.animationRequested;
        ArrayList<IHostPage> previousPages = pageStack.getPreviousPages(record, false);
        if (!finishImmediately) {
            startExitTransition(record, previousPages);
            return true;
        }
        record.handleStop(false);
        PageParameters parameters = record.startRequest.parameters;
        if (parameters != null && parameters.stackActionListener != null) {
            PageStack.PageRecord previousRecord = previousPages == null || previousPages.isEmpty()
                    ? null : (PageStack.PageRecord) previousPages.get(previousPages.size() - 1);
            if (previousRecord == null && record.isTransparent()) {
                for (int index = pageStack.records.size() - 1; index >= 0; index--) {
                    PageStack.PageRecord candidate = pageStack.records.get(index);
                    if (!candidate.isTransparent()) {
                        previousRecord = candidate;
                        break;
                    }
                }
            }
            parameters.stackActionListener.onPageBack(record.controller,
                    previousRecord == null ? null : previousRecord.controller,
                    previousRecord == null ? null : previousRecord.resultData);
        }
        record.handleDestroy();
        if (record.isTransparent() && !pageStack.records.isEmpty()) {
            pageStack.records.get(pageStack.records.size() - 1).controller.onTransparentDismissed();
        }
        PageStack.startPages(previousPages, true, false);
        return true;
    }

    private void startExitTransition(PageStack.PageRecord record, ArrayList<IHostPage> previousPages) {
        pageStack.transitionRunning = true;
        record.handlePause(false);
        record.state = 5;
        IPageAnimationProvider animationProvider = request.animationProvider;
        PageTransitionDispatcher dispatcher = pageStack.transitionDispatcher;
        ViewGroup container = dispatcher.container;
        FinishTransitionCompletion completion = new FinishTransitionCompletion(pageStack, record, previousPages);
        Animation exitAnimation = animationProvider.leaveForRemove(container, record.view);
        exitAnimation.setAnimationListener(new PageTransitionDispatcher.AnimationCallback(
                new PageTransitionDispatcher.CompletionCallback(dispatcher, completion), request.animationListener));
        record.view.startAnimation(exitAnimation);
        if (previousPages != null) {
            for (IHostPage previousPage : previousPages) {
                View previousView = previousPage.getView();
                previousView.startAnimation(animationProvider.enterForRemove(container, previousView));
            }
        }
    }
}
