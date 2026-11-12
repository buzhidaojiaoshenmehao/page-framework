package com.usernav.page.engine;

import android.content.Context;
import android.os.Looper;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.usernav.page.PageBundle;
import com.usernav.page.internal.FinishPageRequest;
import com.usernav.page.internal.PageAnimationOptions;
import com.usernav.page.internal.PageIdentity;
import com.usernav.page.internal.PageInitializationState;
import com.usernav.page.internal.PageLogger;
import com.usernav.page.internal.PageParameters;
import com.usernav.page.internal.PageResultRequest;
import com.usernav.page.internal.PageTransitionDispatcher;
import com.usernav.page.internal.StartPageRequest;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/** Owns the page stack and exposes its weakly referenced framework facade. */
public final class Pages {
    public final PageStack pageStack;
    public final FrameworkFacade framework;

    public static final class CalledFromWrongThreadException extends AndroidRuntimeException {
        public CalledFromWrongThreadException(String message) {
            // The retained constructor does not pass its argument to the superclass.
            super();
        }
    }

    public static final class FrameworkFacade implements IPageFramework {
        public WeakReference<Pages> pagesReference;

        @Override
        public final void finishPage(PageIdentity pageIdentity, PageAnimationOptions animationOptions) {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "finishPage, mPages.get() = null. id = " + pageIdentity);
                return;
            }
            checkMainThread();
            if (pageIdentity == null) {
                PageLogger.error("Pages", "finish page: id is null");
                return;
            }
            IPageController controller = pageIdentity.controllerReference == null
                    ? null : pageIdentity.controllerReference.get();
            if (controller == null) {
                View view = pageIdentity.viewReference == null ? null : pageIdentity.viewReference.get();
                if (view == null) {
                    PageLogger.error("Pages", "finish page: id is empty, not controller or view, id = " + pageIdentity);
                    return;
                }
            }
            FinishPageRequest request = new FinishPageRequest(pageIdentity, animationOptions);
            PageStack stack = pages.pageStack;
            if (request.animationProvider == null) {
                request.animationProvider = stack.defaultAnimationProvider;
            }
            stack.pendingActions.add(new FinishPageAction(stack, request));
            stack.actionHandler.obtainMessage(0).sendToTarget();
        }

        @Override
        public final ArrayList<Class<?>> getInternalClassStacks() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalClassStacks, mPages.get() = null.");
                return null;
            }
            checkMainThread();
            ArrayList<Class<?>> controllerClasses = new ArrayList<>();
            for (PageStack.PageRecord record : pages.pageStack.records) {
                controllerClasses.add(record.startRequest.controllerClass);
            }
            return controllerClasses;
        }

        @Override
        public final ArrayList<Class<?>> getInternalIdentStacks() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalIdentStacks, mPages.get() = null.");
                return null;
            }
            checkMainThread();
            ArrayList<Class<?>> identityClasses = new ArrayList<>();
            for (PageStack.PageRecord record : pages.pageStack.records) {
                identityClasses.add(record.startRequest.identityClass);
            }
            return identityClasses;
        }

        @Override
        public final ArrayList<IPageController> getInternalPageStacks() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalPageStacks, mPages.get() = null.");
                return null;
            }
            checkMainThread();
            ArrayList<IPageController> controllers = new ArrayList<>();
            for (PageStack.PageRecord record : pages.pageStack.records) {
                controllers.add(record.controller);
            }
            return controllers;
        }

        @Override
        public final View getInternalPageView(int positionFromTop) {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalPageView, mPages.get() = null. index = " + positionFromTop);
                return null;
            }
            checkMainThread();
            ArrayList<PageStack.PageRecord> records = pages.pageStack.records;
            if (records.size() > 0 && positionFromTop < records.size()) {
                return records.get(records.size() - 1 - positionFromTop).view;
            }
            return null;
        }

        @Override
        public final Class<?> getInternalTopClass() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalTopClass, mPages.get() = null.");
                return null;
            }
            checkMainThread();
            ArrayList<PageStack.PageRecord> records = pages.pageStack.records;
            return records.isEmpty() ? null : records.get(records.size() - 1).startRequest.controllerClass;
        }

        @Override
        public final Class<?> getInternalTopIdent() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalTopIdent, mPages.get() = null.");
                return null;
            }
            checkMainThread();
            ArrayList<PageStack.PageRecord> records = pages.pageStack.records;
            return records.isEmpty() ? null : records.get(records.size() - 1).startRequest.identityClass;
        }

        @Override
        public final IPageController getInternalTopPage() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalTopPage, mPages.get() = null.");
                return null;
            }
            ArrayList<PageStack.PageRecord> records = pages.pageStack.records;
            return records.isEmpty() ? null : records.get(records.size() - 1).controller;
        }

        @Override
        public final ArrayList<IPageController> getInternalTopVisiblePages() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getInternalTopVisiblePages, mPages.get() = null.");
                return null;
            }
            checkMainThread();
            ArrayList<IPageController> visibleControllers = new ArrayList<>();
            ArrayList<PageStack.PageRecord> records = pages.pageStack.records;
            for (int index = records.size() - 1; index >= 0; index--) {
                PageStack.PageRecord record = records.get(index);
                visibleControllers.add(record.controller);
                if (!record.isTransparent()) break;
            }
            return visibleControllers;
        }

        @Override
        public final IPageController getOpaquePage(int positionFromTop) {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getOpaquePage, mPages.get() = null. posFromTop = " + positionFromTop);
                return null;
            }
            checkMainThread();
            return pages.pageStack.getOpaquePage(positionFromTop);
        }

        @Override
        public final ArrayList<PageParameters> getPageParams() {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "getPageParams, mPages.get() = null.");
                return null;
            }
            checkMainThread();
            ArrayList<PageStack.PageRecord> records = pages.pageStack.records;
            ArrayList<PageParameters> parameters = new ArrayList<>(records.size());
            for (PageStack.PageRecord record : records) parameters.add(record.startRequest.parameters);
            return parameters;
        }

        @Override
        public final void setPageResult(PageIdentity pageIdentity, int resultCode, PageParameters resultData) {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "setPageResult, mPages.get() = null. id = " + pageIdentity
                        + ", resultCode = " + resultCode + ", data = " + resultData);
                return;
            }
            checkMainThread();
            if (pageIdentity == null) {
                PageLogger.error("Pages", "set page result: id is null");
                return;
            }
            PageStack stack = pages.pageStack;
            if (Looper.getMainLooper() == Looper.myLooper()) {
                stack.setPageResult(pageIdentity, resultCode, resultData);
            } else {
                stack.pendingActions.add(new SetPageResultAction(stack, pageIdentity, resultCode, resultData));
                stack.actionHandler.obtainMessage(0).sendToTarget();
            }
        }

        @Override
        public final void startPage(Class<? extends IPageController> controllerClass, int launchFlags,
                PageParameters parameters, PageResultRequest resultRequest, PageAnimationOptions animationOptions) {
            Pages pages = pagesReference.get();
            if (pages == null) {
                PageLogger.error("Framework", "startPage, mPages.get() = null. cls = " + controllerClass);
                return;
            }
            checkMainThread();
            if (controllerClass == null) {
                PageLogger.error("Pages", "start page: cls is null");
                return;
            }
            if (resultRequest != null && resultRequest.callerIdentity == null) resultRequest = null;
            StartPageRequest request = new StartPageRequest(pages.framework, controllerClass,
                    launchFlags, parameters, resultRequest, animationOptions);
            PageStack stack = pages.pageStack;
            if (request.animationProvider == null) request.animationProvider = stack.defaultAnimationProvider;
            stack.pendingActions.add(new StartPageAction(stack, request));
            if (stack.records.isEmpty() && parameters != null
                    && hasLaunchOption(parameters, PageBundle.KEY_EXECUTE_IMMEDIATELY)) {
                stack.drainActions();
            } else if (parameters != null && hasLaunchOption(parameters, PageBundle.KEY_EXECUTE_IMMEDIATELY)) {
                stack.actionHandler.sendEmptyMessageAtTime(0, 0L);
            } else if (parameters != null && hasLaunchOption(parameters, PageBundle.KEY_START_PAGE_SYNC)) {
                stack.drainActions();
            } else {
                stack.actionHandler.obtainMessage(0).sendToTarget();
            }
        }

        private static boolean hasLaunchOption(PageParameters parameters, String optionKey) {
            HashMap<String, Object> extras = parameters.extras;
            PageBundle bundle = extras == null ? null : (PageBundle) extras.get("CUSCTOM_BUNDLE");
            return bundle != null && bundle.getBoolean(optionKey);
        }
    }

    public Pages(Context context, LayoutInflater layoutInflater, ViewGroup container) {
        checkMainThread();
        pageStack = new PageStack(context, layoutInflater, new PageTransitionDispatcher(container));
        framework = new FrameworkFacade();
        framework.pagesReference = new WeakReference<>(this);
    }

    public static void checkMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // The retained engine reads initialization state here; the public navigator enforces threading.
            int initializationToken = PageInitializationState.initializationToken;
        }
    }

    public final void pause() {
        checkMainThread();
        PageStack stack = pageStack;
        if (stack.hostState != 4) {
            PageLogger.warn("PageStack", "pauseTop with wrong Activity state. mActivityState = " + stack.hostState);
        }
        PageLogger.debug("PageStack", "pauseTop. mActivityState = " + stack.hostState + ", new mActivityState = 2");
        stack.hostState = 2;
        for (IHostPage page : stack.getVisiblePages(stack.records.size() - 1)) page.handlePause(true);
    }
}
