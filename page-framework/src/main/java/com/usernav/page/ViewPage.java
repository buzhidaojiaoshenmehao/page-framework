package com.usernav.page;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.usernav.page.engine.IPageController;
import com.usernav.page.engine.IPageFramework;
import com.usernav.page.internal.PageAnimationOptions;
import com.usernav.page.internal.PageParameters;
import java.util.ArrayList;
import java.util.Objects;

/** View adapter driven by the restored engine. All methods and lifecycle queries use the main thread. */
public abstract class ViewPage implements IPageController {
    public interface BackHandler { boolean onBack(); }
    private PageNavigator navigator;
    private Context context;
    private LayoutInflater inflater;
    private PageBundle arguments;
    private View view;
    private boolean removing, destroyed, failed;
    private PageAnimationOptions exitAnimation;
    private final ArrayList<BackHandler> backHandlers = new ArrayList<>();
    private final PageResources resources = new PageResources(error -> report("resource", error));
    private final PageResources viewResources = new PageResources(error -> report("viewResource", error));

    @Override public final void attach(IPageFramework framework, Context context,
                                       LayoutInflater inflater, Object host) {
        if (!(host instanceof PageNavigator)) throw new IllegalArgumentException("A PageNavigator host is required");
        if (navigator != null || destroyed) throw new IllegalStateException("Page is already attached or destroyed");
        this.navigator = (PageNavigator) host;
        this.context = context;
        this.inflater = inflater;
    }
    @Override public final void onCreate(PageParameters params) {
        arguments = PageNavigator.bundle(params);
        rememberAnimation(arguments);
        try { onCreate(arguments); }
        catch (RuntimeException error) { creationFailed("create", error); throw error; }
    }
    protected void onCreate(PageBundle arguments) {}
    @Override public final View onCreateView(ViewGroup container) {
        try {
            View created = createView(inflater, container);
            if (created == null) throw new IllegalStateException("createView must return a View");
            if (created.getParent() != null)
                throw new IllegalStateException("Return a detached View (inflate with attachToRoot=false)");
            if (created.getLayoutParams() == null)
                created.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            view = created;
            navigator.pageCreated(this);
            return view;
        } catch (RuntimeException error) { creationFailed("createView", error); throw error; }
    }
    protected abstract View createView(LayoutInflater inflater, ViewGroup container);
    @Override public final void onNewParams(PageParameters params) {
        arguments = PageNavigator.bundle(params);
        if (arguments.getAnimation() != null || arguments.containsKey(PageNavigator.ANIMATION_OVERRIDE))
            rememberAnimation(arguments);
        try { onNewArguments(arguments); }
        catch (RuntimeException error) { report("newArguments", error); throw error; }
        finally { if (navigator != null) navigator.pageReused(this); }
    }
    protected void onNewArguments(PageBundle arguments) {}
    @Override public final void onPageResult(int requestCode, int resultCode, PageParameters data) {
        try { onResult(requestCode, resultCode, data == null ? null : PageNavigator.bundle(data)); }
        catch (RuntimeException error) { report("pageResult", error); throw error; }
    }
    protected void onResult(int requestCode, int resultCode, PageBundle data) {}

    /** The newest registered handler runs first, followed by the page's legacy back hook. */
    public final PageResources.Registration addBackHandler(BackHandler handler) {
        requireNavigator();
        Objects.requireNonNull(handler, "handler");
        backHandlers.add(handler);
        return viewResources.add(() -> backHandlers.remove(handler));
    }
    @Override public final int onBackPressed() {
        ArrayList<BackHandler> snapshot = new ArrayList<>(backHandlers);
        try {
            for (int i = snapshot.size() - 1; i >= 0; i--)
                if (backHandlers.contains(snapshot.get(i)) && snapshot.get(i).onBack()) return 1;
            return onHandleBack() ? 1 : 0;
        } catch (RuntimeException error) { report("back", error); return 1; }
    }
    protected boolean onHandleBack() { return false; }

    /** View cleanup runs with view/context available; asynchronous guards are already invalidated. */
    @Override public final void onRemoveView() {
        if (removing) return;
        removing = true;
        try { onViewRemoved(); } catch (RuntimeException error) { report("removeView", error); }
        finally {
            viewResources.close();
            backHandlers.clear();
            try {
                if (view != null) {
                    try { view.animate().cancel(); } catch (RuntimeException error) { report("cancelViewAnimator", error); }
                    try { view.clearAnimation(); } catch (RuntimeException error) { report("clearViewAnimation", error); }
                }
            } finally { view = null; }
        }
    }
    protected void onViewRemoved() {}
    @Override public final void onDestroy() {
        if (destroyed) return;
        PageNavigator host = navigator;
        try {
            onRemoveView();
            resources.close();
            try { onPageDestroyed(); } catch (RuntimeException error) { report("destroy", error); }
        } finally {
            destroyed = true;
            navigator = null;
            context = null;
            inflater = null;
            arguments = null;
            view = null;
            exitAnimation = null;
            if (host != null) host.pageDestroyed(this);
        }
    }
    protected void onPageDestroyed() {}

    // Kept overridable for compatibility. State queries read the core state, so legacy
    // subclasses do not need to call super from these callbacks.
    @Override public void onStart() {}
    @Override public void onResume() {}
    @Override public void onPause() {}
    @Override public void onStop() {}
    @Override public void onPageSwitch(boolean hostLifecycle) {}
    @Override public void onTransparentDismissed() {}

    protected void onConfigurationChanged(Configuration configuration) {}
    protected void onWindowFocusChanged(boolean hasFocus) {}
    protected void onLowMemory() {}
    protected void onTrimMemory(int level) {}
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {}
    protected void onMultiWindowModeChanged(boolean inMultiWindow, Configuration configuration) {}
    protected void onPictureInPictureModeChanged(boolean inPictureInPicture, Configuration configuration) {}

    final void dispatchConfiguration(Configuration value) { callback("configuration", () -> onConfigurationChanged(value)); }
    final void dispatchWindowFocus(boolean value) { callback("windowFocus", () -> onWindowFocusChanged(value)); }
    final void dispatchLowMemory() { callback("lowMemory", this::onLowMemory); }
    final void dispatchTrimMemory(int value) { callback("trimMemory", () -> onTrimMemory(value)); }
    final void dispatchActivityResult(int request, int result, Intent data) {
        callback("activityResult", () -> onActivityResult(request, result, data));
    }
    final void dispatchMultiWindow(boolean value, Configuration configuration) {
        callback("multiWindow", () -> onMultiWindowModeChanged(value, configuration));
    }
    final void dispatchPictureInPicture(boolean value, Configuration configuration) {
        callback("pictureInPicture", () -> onPictureInPictureModeChanged(value, configuration));
    }

    public final PageState state() {
        PageNavigator.requireMainThread();
        if (destroyed || removing || failed) return PageState.DESTROYED;
        return navigator == null ? PageState.INITIALIZED : navigator.pageState(this);
    }
    public final boolean isAlive() {
        PageNavigator.requireMainThread();
        return navigator != null && !removing && !destroyed && !failed;
    }
    public final boolean isStarted() { PageState state = state(); return state == PageState.STARTED || state == PageState.RESUMED; }
    public final boolean isResumed() { return state() == PageState.RESUMED; }
    /** Stack visibility while the host is started; not a test for actual on-screen pixels. */
    public final boolean isVisible() { return isAlive() && view != null && view.getVisibility() == View.VISIBLE && navigator.pageVisible(this); }
    public final boolean isFinishing() { return isAlive() && navigator.isFinishing(this); }
    public final PageResources resources() { return resources; }
    public final PageResources viewResources() { return viewResources; }
    public final PageNavigator navigator() { return navigator; }
    public final Context context() { return context; }
    public final PageBundle arguments() { return arguments; }
    public final View view() { return view; }
    public final void startPage(Class<? extends ViewPage> type, PageBundle bundle) { requireNavigator().startFrom(this, type, bundle); }
    public final void startPage(String action, PageBundle bundle) { requireNavigator().startFrom(this, action, bundle); }
    public final void startPageForResult(Class<? extends ViewPage> type, PageBundle bundle, int code) {
        requireNavigator().startPageForResult(this, type, bundle, code);
    }
    public final void startPageForResult(String action, PageBundle bundle, int code) {
        requireNavigator().startPageForResult(this, action, bundle, code);
    }
    public final void startActivityForResult(Intent intent, int requestCode) {
        requireNavigator().startActivityForResult(this, intent, requestCode);
    }
    public final void setResult(int code, PageBundle data) { requireNavigator().setPageResult(this, code, data); }
    public final void finish() { requireNavigator().finishPage(this); }
    final PageAnimationOptions exitAnimation() { return exitAnimation; }

    private PageNavigator requireNavigator() {
        PageNavigator.requireMainThread();
        if (!isAlive()) throw new IllegalStateException("Page is not alive");
        return navigator;
    }
    private void rememberAnimation(PageBundle bundle) {
        PageAnimationOptions animation = bundle.getAnimation();
        exitAnimation = animation == null ? null : new PageAnimationOptions(animation.provider);
    }
    private void creationFailed(String operation, RuntimeException error) {
        failed = true;
        report(operation, error);
        viewResources.close();
        resources.close();
    }
    private void callback(String operation, Runnable action) {
        if (!isAlive()) return;
        try { action.run(); } catch (RuntimeException error) { report(operation, error); }
    }
    private void report(String operation, RuntimeException error) {
        if (navigator != null) navigator.reportError(this, operation, error);
    }
}
