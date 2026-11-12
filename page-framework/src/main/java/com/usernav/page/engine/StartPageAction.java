package com.usernav.page.engine;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import com.usernav.page.internal.PageLogger;
import com.usernav.page.internal.PageParameters;
import com.usernav.page.internal.PageTelemetry;
import com.usernav.page.internal.PageTransitionDispatcher;
import com.usernav.page.internal.StartPageRequest;
import com.usernav.page.internal.StartTransitionCompletion;
import java.util.ArrayList;

/** Executes one queued start, including reuse, transparent groups and paired transitions. */
public final class StartPageAction implements PageStack.StackAction {
    private static final int FLAG_STANDARD = 1;
    private static final int FLAG_SINGLE_INSTANCE = 2;
    private static final int FLAG_SINGLE_TASK = 4;
    private static final int FLAG_SINGLE_TOP = 8;
    private static final int FLAG_TRANSPARENT = 16;

    public final StartPageRequest request;
    public final PageStack pageStack;

    public StartPageAction(PageStack pageStack, StartPageRequest request) {
        this.pageStack = pageStack;
        this.request = request;
    }

    @Override public boolean handleAction() {
        PageTelemetry.recordEvent(3, 1, "U_PageStack_doStart_start", "", "", 0);
        boolean handled = start();
        PageTelemetry.recordEvent(3, 1, "U_PageStack_doStart_end", "", "", 0);
        PageTelemetry.recordEvent(3, 1, "U_Choreographer_doFrame_start", "", "", 0);
        return handled;
    }

    private boolean start() {
        if (pageStack.transitionRunning) {
            PageLogger.warn("PageStack", "doStart: pending");
            return false;
        }
        boolean animate = request.animationRequested && pageStack.hostState > PageStack.STATE_STARTED;
        if (pageStack.hostState < PageStack.STATE_STARTED) hideTopGroup();

        ArrayList<PageStack.PageRecord> reusedPages = null;
        ArrayList<IHostPage> pagesToDestroy = null;
        ArrayList<IHostPage> pagesToStop = null;
        PageStack.PageRecord reusedRecord = null;
        ArrayList<PageStack.PageRecord> records = pageStack.records;

        if (request.hasFlag(FLAG_SINGLE_INSTANCE)) {
            int reuseIndex = findReuseIndex(0);
            if (reuseIndex >= 0) {
                reusedRecord = records.get(reuseIndex);
                int lastOpaqueIndex = records.size() - 1;
                while (lastOpaqueIndex >= reuseIndex + 1 && records.get(lastOpaqueIndex).isTransparent()) lastOpaqueIndex--;
                if (lastOpaqueIndex < reuseIndex + 1) lastOpaqueIndex = -1;
                if (lastOpaqueIndex >= 0) {
                    pagesToStop = new ArrayList<>();
                    for (int index = lastOpaqueIndex; index < records.size(); index++) pagesToStop.add(records.get(index));
                }
                reusedPages = new ArrayList<>();
                reusedPages.add(reusedRecord);
                pageStack.appendTransparentPages(reusedPages, reuseIndex + 1,
                        lastOpaqueIndex < 0 ? records.size() : lastOpaqueIndex);
            }
        } else if (request.hasFlag(FLAG_SINGLE_TOP)) {
            reusedRecord = findTopMatch();
            if (reusedRecord != null) {
                reusedPages = new ArrayList<>();
                reusedPages.add(reusedRecord);
                pageStack.appendTransparentPages(reusedPages, records.indexOf(reusedRecord) + 1, records.size());
            }
        } else if (request.hasFlag(FLAG_SINGLE_TASK)) {
            int reuseIndex = findReuseIndex(request.pageCount);
            if (reuseIndex >= 0) {
                reusedRecord = records.get(reuseIndex);
                int firstOpaqueIndex = reuseIndex + 1;
                while (firstOpaqueIndex < records.size() && records.get(firstOpaqueIndex).isTransparent()) firstOpaqueIndex++;
                if (firstOpaqueIndex >= records.size()) firstOpaqueIndex = -1;
                if (firstOpaqueIndex >= 0) {
                    pagesToDestroy = new ArrayList<>();
                    for (int index = records.size() - 1; index >= firstOpaqueIndex; index--) pagesToDestroy.add(records.get(index));
                }
                reusedPages = new ArrayList<>();
                reusedPages.add(reusedRecord);
                pageStack.appendTransparentPages(reusedPages, reuseIndex + 1,
                        firstOpaqueIndex < 0 ? records.size() : firstOpaqueIndex);
            }
        } else if (!request.hasFlag(FLAG_TRANSPARENT) && !request.hasFlag(FLAG_STANDARD)) {
            PageLogger.error("PageStack", "doStart: unknown flags=" + request.launchFlags);
            return true;
        }

        ArrayList<IHostPage> pagesToStart;
        if (reusedRecord != null) {
            IPageController previousOpaquePage = pageStack.getOpaquePage(0);
            IPageController reusedController = reusedRecord.controller;
            for (PageStack.PageRecord record : reusedPages) records.remove(record);
            records.addAll(reusedPages);
            pagesToStart = new ArrayList<IHostPage>(reusedPages);
            PageParameters parameters = request.parameters;
            reusedRecord.pendingParameters = parameters;
            if (isEmpty(pagesToDestroy) && isEmpty(pagesToStop)) {
                for (IHostPage page : pagesToStart) page.handleNewIntent();
                pagesToStart = null;
                animate = false;
            } else {
                PageStack.stopPages(pagesToStart, false);
            }
            if (parameters != null && parameters.stackActionListener != null)
                parameters.stackActionListener.onPageBack(previousOpaquePage, reusedController, parameters);
        } else {
            PageStack.PageRecord newRecord = new PageStack.PageRecord(pageStack, request);
            request.pageRecord = newRecord;
            pagesToStop = pageStack.getPreviousPages(newRecord, true);
            pagesToStart = new ArrayList<>();
            pagesToStart.add(newRecord);
            if (request.maxCount > 0) {
                pagesToDestroy = findExcessInstances();
                if (pagesToStop != null) pagesToStop.removeAll(pagesToDestroy);
                if (pagesToDestroy.isEmpty()) pagesToDestroy = null;
            }
            if (!newRecord.handleCreate(request)) return discardFailedPage(newRecord);
            View createdView = newRecord.handleCreateView(pageStack.transitionDispatcher.container);
            if (createdView == null) return discardFailedPage(newRecord);
            request.view = createdView;
        }
        if (animate) animateTransition(pagesToDestroy, pagesToStop, pagesToStart);
        else {
            PageStack.destroyPages(pagesToDestroy, false);
            PageStack.stopPages(pagesToStop, false);
            PageStack.startPages(pagesToStart, true, false);
        }
        return true;
    }

    private int findReuseIndex(int occurrence) {
        if (!TextUtils.isEmpty(request.uniqueId)) return pageStack.findUniqueIdIndex(occurrence, request.uniqueId);
        if (request.identityClass != null) return pageStack.findIdentityIndex(request.identityClass, occurrence);
        return pageStack.findControllerIndex(request.controllerClass);
    }

    private PageStack.PageRecord findTopMatch() {
        for (int index = pageStack.records.size() - 1; index >= 0; index--) {
            PageStack.PageRecord record = pageStack.records.get(index);
            StartPageRequest candidate = record.startRequest;
            boolean matches;
            if (!TextUtils.isEmpty(request.uniqueId)) matches = request.uniqueId.equals(candidate.uniqueId);
            else if (request.identityClass != null)
                matches = candidate.identityClass == request.identityClass && TextUtils.isEmpty(candidate.uniqueId);
            else matches = candidate.controllerClass == request.controllerClass && TextUtils.isEmpty(candidate.uniqueId);
            if (matches) return record;
            if (!record.isTransparent()) break;
        }
        return null;
    }

    private ArrayList<IHostPage> findExcessInstances() {
        ArrayList<IHostPage> excessPages = new ArrayList<>();
        int remainingCount = request.maxCount;
        for (int index = pageStack.records.size() - 1; index >= 0; index--) {
            PageStack.PageRecord record = pageStack.records.get(index);
            StartPageRequest candidate = record.startRequest;
            boolean matches;
            if (!TextUtils.isEmpty(request.maxCountId)) matches = request.maxCountId.equals(candidate.maxCountId);
            else if (!TextUtils.isEmpty(request.uniqueId)) matches = request.uniqueId.equals(candidate.uniqueId);
            else if (request.identityClass != null) matches = request.identityClass == candidate.identityClass;
            else matches = request.controllerClass == candidate.controllerClass;
            if (matches && --remainingCount <= 0) excessPages.add(record);
        }
        return excessPages;
    }

    private void hideTopGroup() {
        int firstIndex = pageStack.records.size() - 1;
        while (firstIndex >= 0 && pageStack.records.get(firstIndex).isTransparent()) firstIndex--;
        if (firstIndex < 0) return;
        for (int index = firstIndex; index < pageStack.records.size(); index++)
            pageStack.records.get(index).view.setVisibility(View.GONE);
    }

    private boolean discardFailedPage(PageStack.PageRecord record) {
        record.handleDestroy();
        PageLogger.warn("PageStack", "doStart: creating the page or view failed");
        return true;
    }

    private void animateTransition(ArrayList<IHostPage> pagesToDestroy, ArrayList<IHostPage> pagesToStop,
                                   ArrayList<IHostPage> pagesToStart) {
        pageStack.transitionRunning = true;
        PageTransitionDispatcher dispatcher = pageStack.transitionDispatcher;
        ViewGroup container = dispatcher.container;
        IPageAnimationProvider provider = request.animationProvider;
        ArrayList<View> leavingViews = PageStack.collectViews(pagesToDestroy != null ? pagesToDestroy : pagesToStop);
        ArrayList<View> enteringViews = PageStack.collectViews(pagesToStart);
        StartTransitionCompletion completion = new StartTransitionCompletion(pageStack, pagesToDestroy, pagesToStop, pagesToStart);
        if (leavingViews != null) for (View view : leavingViews) view.startAnimation(provider.leaveForAdd(container, view));
        for (int index = 0; index < enteringViews.size(); index++) {
            View view = enteringViews.get(index);
            Animation animation = provider.enterForAdd(container, view);
            if (index == 0) animation.setAnimationListener(new PageTransitionDispatcher.AnimationCallback(
                    new PageTransitionDispatcher.CompletionCallback(dispatcher, completion), request.animationListener));
            view.startAnimation(animation);
            if (container.indexOfChild(view) < 0) container.addView(view, view.getLayoutParams());
            else view.setVisibility(View.VISIBLE);
        }
    }

    private static boolean isEmpty(ArrayList<?> pages) { return pages == null || pages.isEmpty(); }
}
