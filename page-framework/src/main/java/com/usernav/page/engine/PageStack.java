package com.usernav.page.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.usernav.page.internal.PageIdentity;
import com.usernav.page.internal.PageLogger;
import com.usernav.page.internal.PageParameters;
import com.usernav.page.internal.PageResultRequest;
import com.usernav.page.internal.PageTelemetry;
import com.usernav.page.internal.PageTransitionDispatcher;
import com.usernav.page.internal.SlidePageAnimationProvider;
import com.usernav.page.internal.StartPageRequest;
import java.util.ArrayList;

/** Source reconstruction of the extracted stack and its page lifecycle state machine. */
public final class PageStack {
    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_CREATED = 1;
    public static final int STATE_STARTED = 2;
    public static final int STATE_ENTERING = 3;
    public static final int STATE_RESUMED = 4;
    public static final int STATE_LEAVING = 5;
    public static final int STATE_DESTROYED = 6;

    public final Context context;
    public final LayoutInflater layoutInflater;
    public final PageTransitionDispatcher transitionDispatcher;
    public final ArrayList<PageRecord> records = new ArrayList<>();
    public final ArrayList<StackAction> pendingActions = new ArrayList<>();
    public boolean suppressTransparentExpansion;
    public final ActionQueueHandler actionHandler;
    public final IPageAnimationProvider defaultAnimationProvider;
    public Object host;
    public int hostState = STATE_CREATED;
    public boolean transitionRunning;

    public interface StackAction { boolean handleAction(); }

    public static final class ActionQueueHandler extends Handler {
        public final PageStack pageStack;

        public ActionQueueHandler(PageStack pageStack) { this.pageStack = pageStack; }

        @Override public void handleMessage(Message message) {
            if (message.what == 0) pageStack.drainActions();
            else PageLogger.error("Pages", "Unexpected action message: " + message.what);
        }
    }

    public static final class PageRecord implements IHostPage {
        public final StartPageRequest startRequest;
        public IPageController controller;
        public View view;
        public int state = STATE_INITIALIZED;
        public PageParameters pendingParameters;
        public int resultCode;
        public PageParameters resultData;
        public final PageStack pageStack;

        public PageRecord(PageStack pageStack, StartPageRequest startRequest) {
            this.pageStack = pageStack;
            this.startRequest = startRequest;
        }

        public String describe() {
            return controller + " ident=" + (startRequest == null ? "null" : startRequest.identityClass);
        }

        @Override public View getView() { return view; }

        @Override public int handleBackPressed() {
            try { return controller.onBackPressed(); }
            catch (Throwable error) { reportFailure("handleBackPressed", error); return 0; }
        }

        @SuppressWarnings("deprecation")
        @Override public boolean handleCreate(StartPageRequest request) {
            try {
                controller = request.controllerClass.newInstance();
            } catch (Throwable error) {
                reportFailure("handleCreate newInstance", error);
                return false;
            }
            state = STATE_CREATED;
            pageStack.records.add(this);
            try {
                controller.attach(startRequest.framework, pageStack.context, pageStack.layoutInflater, pageStack.host);
                if (startRequest.resultRequest != null)
                    startRequest.parameters.requestCode = startRequest.resultRequest.requestCode;
                controller.onCreate(startRequest.parameters);
                return true;
            } catch (Throwable error) {
                reportFailure("handleCreate", error);
                return false;
            }
        }

        @Override public View handleCreateView(ViewGroup container) {
            View createdView = null;
            try {
                createdView = controller.onCreateView(container);
                if (createdView == null) PageLogger.error("PageStack", "handleCreateView returned null");
            } catch (Throwable error) { reportFailure("handleCreateView", error); }
            view = createdView;
            return createdView;
        }

        @Override public void handleDestroy() {
            if (state != STATE_CREATED) {
                reportWrongState("handleDestroy");
                return;
            }
            state = STATE_DESTROYED;
            controller.onRemoveView();
            pageStack.transitionDispatcher.container.removeView(view);
            try { controller.onDestroy(); }
            catch (Throwable error) { reportFailure("handleDestroy", error); }
            pageStack.records.remove(this);
            PageResultRequest resultRequest = startRequest.resultRequest;
            if (resultRequest != null) {
                PageRecord caller = pageStack.findPage(resultRequest.callerIdentity);
                if (caller == null || caller.state == STATE_DESTROYED) {
                    PageLogger.error("PageStack", "Result target no longer exists: " + resultRequest.callerIdentity);
                } else if (caller.state == STATE_ENTERING || caller.state == STATE_LEAVING) {
                    // The extracted implementation only logs this case; it has no deferred-result queue.
                    PageLogger.warn("PageStack", "Result target is animating: " + caller.describe());
                } else {
                    caller.handleResult(resultRequest.requestCode, resultCode, resultData);
                }
            }
            pageStack.schedulePendingActions();
        }

        @Override public void handleNewIntent() {
            if (pendingParameters == null) return;
            try { controller.onNewParams(pendingParameters); }
            catch (Throwable error) { reportFailure("handleNewIntent", error); }
            pendingParameters = null;
        }

        @Override public void handlePause(boolean hostLifecycle) {
            if (state != STATE_RESUMED) { reportWrongState("handlePause"); return; }
            state = STATE_STARTED;
            try {
                controller.onPageSwitch(hostLifecycle);
                controller.onPause();
            } catch (Throwable error) { reportFailure("handlePause", error); }
        }

        @Override public void handleResult(int requestCode, int resultCode, PageParameters resultData) {
            try { controller.onPageResult(requestCode, resultCode, resultData); }
            catch (Throwable error) { reportFailure("handleResult", error); }
        }

        @Override public void handleResume(boolean hostLifecycle) {
            if (pageStack.hostState < STATE_RESUMED || (state != STATE_STARTED && state != STATE_ENTERING)) {
                reportWrongState("handleResume");
                return;
            }
            state = STATE_RESUMED;
            handleNewIntent();
            try {
                controller.onPageSwitch(hostLifecycle);
                controller.onResume();
            } catch (Throwable error) { reportFailure("handleResume", error); }
            pageStack.schedulePendingActions();
        }

        @Override public void handleStart(boolean attachView, boolean resume, boolean hostLifecycle) {
            if (pageStack.hostState < STATE_STARTED) { reportWrongState("handleStart"); return; }
            if (state == STATE_CREATED) {
                state = STATE_STARTED;
                if (attachView && startRequest != null) {
                    View pageView = startRequest.view;
                    pageView.clearAnimation();
                    ViewGroup container = pageStack.transitionDispatcher.container;
                    if (container.indexOfChild(pageView) < 0) container.addView(pageView, pageView.getLayoutParams());
                    pageView.setVisibility(View.VISIBLE);
                }
                handleNewIntent();
                try { controller.onStart(); }
                catch (Throwable error) { reportFailure("handleStart", error); }
            } else { reportWrongState("handleStart"); }
            if (resume) {
                if (state == STATE_STARTED || state == STATE_ENTERING) handleResume(hostLifecycle);
                else reportWrongState("handleStart resume");
            }
        }

        @Override public void handleStop(boolean hostLifecycle) {
            handlePause(hostLifecycle);
            if (state != STATE_STARTED && state != STATE_LEAVING && state != STATE_ENTERING) {
                reportWrongState("handleStop");
                return;
            }
            state = STATE_CREATED;
            if (pageStack.hostState > STATE_CREATED) view.setVisibility(View.GONE);
            try { controller.onStop(); }
            catch (Throwable error) { reportFailure("handleStop", error); }
        }

        @Override public boolean isTransparent() { return startRequest.hasFlag(16); }

        private void reportFailure(String operation, Throwable error) {
            PageLogger.errorWithCause(operation + " failed, page=" + describe() + ", exception=" + error.getMessage(), error);
        }

        private void reportWrongState(String operation) {
            PageLogger.warn("PageStack", operation + " ignored, hostState=" + pageStack.hostState
                    + ", pageState=" + state + ", page=" + describe());
        }
    }

    public PageStack(Context context, LayoutInflater layoutInflater, PageTransitionDispatcher transitionDispatcher) {
        this.context = context;
        this.layoutInflater = layoutInflater;
        this.transitionDispatcher = transitionDispatcher;
        defaultAnimationProvider = new SlidePageAnimationProvider();
        actionHandler = new ActionQueueHandler(this);
    }

    public static ArrayList<View> collectViews(ArrayList<? extends IHostPage> pages) {
        if (pages == null) return null;
        ArrayList<View> views = new ArrayList<>();
        for (IHostPage page : pages) views.add(page.getView());
        return views;
    }

    public static void destroyPages(ArrayList<? extends IHostPage> pages, boolean hostLifecycle) {
        if (pages == null) return;
        for (IHostPage page : pages) page.handlePause(hostLifecycle);
        for (IHostPage page : pages) page.handleStop(hostLifecycle);
        for (IHostPage page : pages) page.handleDestroy();
    }

    public static void startPages(ArrayList<? extends IHostPage> pages, boolean resume, boolean hostLifecycle) {
        if (pages == null) return;
        if (!pages.isEmpty()) PageTelemetry.recordEvent(3, 1, "U_PageStack_startPagesImmediately_start", "", "", 0);
        for (IHostPage page : pages) page.handleStart(true, resume, hostLifecycle);
        if (!pages.isEmpty()) PageTelemetry.recordEvent(3, 1, "U_PageStack_startPagesImmediately_end", "", "", 0);
    }

    public static void stopPages(ArrayList<? extends IHostPage> pages, boolean hostLifecycle) {
        if (pages == null) return;
        for (IHostPage page : pages) page.handlePause(hostLifecycle);
        for (IHostPage page : pages) page.handleStop(hostLifecycle);
    }

    public void setPageResult(PageIdentity identity, int resultCode, PageParameters resultData) {
        PageRecord record = findPage(identity);
        if (record == null) { PageLogger.error("PageStack", "Result page not found: " + identity); return; }
        record.resultCode = resultCode;
        record.resultData = resultData;
    }

    public ArrayList<IHostPage> getVisiblePages(int topIndex) {
        ArrayList<IHostPage> visiblePages = new ArrayList<>();
        for (int index = topIndex; index >= 0; index--) {
            PageRecord record = records.get(index);
            visiblePages.add(record);
            if (!record.isTransparent()) break;
        }
        return visiblePages;
    }

    public void appendTransparentPages(ArrayList<PageRecord> pages, int startIndex, int endIndex) {
        if (suppressTransparentExpansion) return;
        for (int index = startIndex; index < endIndex; index++) {
            PageRecord record = records.get(index);
            if (!record.isTransparent()) return;
            pages.add(record);
        }
    }

    public IPageController getOpaquePage(int offset) {
        for (int index = records.size() - 1; index >= 0; index--) {
            PageRecord record = records.get(index);
            if (!record.isTransparent() && --offset < 0) return record.controller;
        }
        return null;
    }

    public int findUniqueIdIndex(int minimumDepth, String uniqueId) {
        for (int index = records.size() - 1; index >= 0; index--) {
            minimumDepth--;
            String recordId = records.get(index).startRequest.uniqueId;
            if (!TextUtils.isEmpty(recordId) && recordId.equals(uniqueId) && minimumDepth <= 0) return index;
        }
        return -1;
    }

    public int findControllerIndex(Class<? extends IPageController> controllerClass) {
        for (int index = records.size() - 1; index >= 0; index--) {
            StartPageRequest request = records.get(index).startRequest;
            if (request.controllerClass == controllerClass && TextUtils.isEmpty(request.uniqueId)) return index;
        }
        return -1;
    }

    public int findIdentityIndex(Class<?> identityClass, int minimumDepth) {
        for (int index = records.size() - 1; index >= 0; index--) {
            minimumDepth--;
            StartPageRequest request = records.get(index).startRequest;
            if (request.identityClass == identityClass && minimumDepth <= 0 && TextUtils.isEmpty(request.uniqueId)) return index;
        }
        return -1;
    }

    /** Returns active pages covered by a new opaque page, or hidden pages uncovered by a removal. */
    public ArrayList<IHostPage> getPreviousPages(PageRecord record, boolean adding) {
        int recordIndex = records.indexOf(record);
        if (adding) {
            if (recordIndex >= 0) { PageLogger.error("PageStack", "Adding an existing record is unsupported"); return null; }
            if (record.isTransparent()) return null;
            ArrayList<IHostPage> previousPages = new ArrayList<>();
            for (int index = records.size() - 1; index >= 0; index--) {
                PageRecord previous = records.get(index);
                if (previous.state != STATE_STARTED && previous.state != STATE_LEAVING
                        && previous.state != STATE_ENTERING && previous.state != STATE_RESUMED) break;
                previousPages.add(previous);
            }
            return previousPages;
        }
        for (int index = records.size() - 1; index > recordIndex; index--)
            if (!records.get(index).isTransparent()) return null;
        for (int index = recordIndex - 1; index >= 0; index--) {
            PageRecord previous = records.get(index);
            if (!previous.isTransparent()) {
                if (previous.state == STATE_STARTED || previous.state == STATE_RESUMED) return null;
                break;
            }
        }
        return getVisiblePages(recordIndex - 1);
    }

    public PageRecord findPage(PageIdentity identity) {
        IPageController controller = identity.controllerReference == null ? null : identity.controllerReference.get();
        if (controller != null) {
            for (PageRecord record : records) {
                if (record.controller != controller) continue;
                if (record.view != null) return record;
                PageLogger.warn("PageStack", "Page record has no view: " + controller);
                return null;
            }
            PageLogger.warn("PageStack", "Controller not found: " + controller);
            return null;
        }
        View view = identity.viewReference == null ? null : identity.viewReference.get();
        if (view != null) for (PageRecord record : records) if (record.view == view) return record;
        PageLogger.warn("PageStack", "Page view not found: " + view);
        return null;
    }

    public void drainActions() {
        while (!pendingActions.isEmpty() && pendingActions.get(0).handleAction()) pendingActions.remove(0);
    }

    private void schedulePendingActions() {
        if (!pendingActions.isEmpty()) actionHandler.obtainMessage(0).sendToTarget();
    }
}
