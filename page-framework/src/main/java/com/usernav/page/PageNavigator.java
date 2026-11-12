package com.usernav.page;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.usernav.page.engine.HostLifecycle;
import com.usernav.page.engine.IPageAnimationListener;
import com.usernav.page.engine.IPageAnimationProvider;
import com.usernav.page.engine.IPageController;
import com.usernav.page.engine.IPageFramework;
import com.usernav.page.engine.Pages;
import com.usernav.page.internal.PageAnimationOptions;
import com.usernav.page.internal.PageIdentity;
import com.usernav.page.internal.PageParameters;
import com.usernav.page.internal.PageResultRequest;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Public host API. Stack operations still delegate to the restored Pages engine. */
public final class PageNavigator {
    static final String ANIMATION_OVERRIDE = "com.usernav.page.animationOverride";
    private Pages pages;
    private IPageFramework engine;
    private Context context;
    private final Handler notifications = new Handler(Looper.getMainLooper());
    private final HashMap<String, Class<? extends ViewPage>> routes = new HashMap<>();
    private final HashMap<Class<? extends ViewPage>, PagePolicy> policies = new HashMap<>();
    private final ArrayList<NavigationInterceptor> interceptors = new ArrayList<>();
    private final ArrayList<PageObserver> observers = new ArrayList<>();
    private final Set<ViewPage> finishing = Collections.newSetFromMap(new IdentityHashMap<>());
    private final HashMap<Integer, WeakReference<ViewPage>> activityResults = new HashMap<>();
    private boolean destroyed, notificationPending, inPolicy, inObserver;

    public PageNavigator(Context context, ViewGroup container) {
        requireMainThread();
        this.context = Objects.requireNonNull(context, "context");
        pages = new Pages(context, LayoutInflater.from(context), Objects.requireNonNull(container, "container"));
        HostLifecycle.attachHost(pages, this);
        engine = HostLifecycle.framework(pages);
    }
    public void register(String action, Class<? extends ViewPage> type) {
        checkAlive();
        validate(type);
        if (Objects.requireNonNull(action, "action").trim().isEmpty())
            throw new IllegalArgumentException("Action must not be empty");
        routes.put(action, type);
    }
    public void register(String action, Class<? extends ViewPage> type, PagePolicy policy) {
        register(action, type);
        setDefaultPolicy(type, policy);
    }
    public void setDefaultPolicy(Class<? extends ViewPage> type, PagePolicy policy) {
        checkAlive();
        validate(type);
        if (policy == null) policies.remove(type); else policies.put(type, policy);
    }
    public void addInterceptor(NavigationInterceptor interceptor) {
        checkAlive();
        Objects.requireNonNull(interceptor, "interceptor");
        if (!interceptors.contains(interceptor)) interceptors.add(interceptor);
    }
    public void removeInterceptor(NavigationInterceptor interceptor) { requireMainThread(); interceptors.remove(interceptor); }
    public void addObserver(PageObserver observer) {
        checkAlive();
        Objects.requireNonNull(observer, "observer");
        if (!observers.contains(observer)) observers.add(observer);
    }
    public void removeObserver(PageObserver observer) { requireMainThread(); observers.remove(observer); }

    public void startPage(String action, PageBundle bundle) { startPage(resolve(action), bundle); }
    public void startPage(Class<? extends ViewPage> type, PageBundle bundle) {
        checkAlive();
        start((ViewPage) engine.getInternalTopPage(), type, bundle, null, null);
    }
    void startFrom(ViewPage caller, String action, PageBundle bundle) { startFrom(caller, resolve(action), bundle); }
    void startFrom(ViewPage caller, Class<? extends ViewPage> type, PageBundle bundle) {
        checkAttached(caller);
        start(caller, type, bundle, null, null);
    }
    public void startPageForResult(ViewPage caller, String action, PageBundle bundle, int code) {
        startPageForResult(caller, resolve(action), bundle, code);
    }
    public void startPageForResult(ViewPage caller, Class<? extends ViewPage> type,
                                   PageBundle bundle, int requestCode) {
        checkAttached(caller);
        start(caller, type, bundle, new PageResultRequest(requestCode, identity(caller)), requestCode);
    }
    private void start(ViewPage caller, Class<? extends ViewPage> type, PageBundle bundle,
                       PageResultRequest result, Integer requestCode) {
        checkNavigation();
        validate(type);
        PageBundle actual = bundle == null ? new PageBundle() : copy(bundle);
        PagePolicy policy = policies.get(type);
        if (actual.getFlags() == 0 && policy != null) actual.setFlags(policy.flags);
        if (actual.getAnimation() == null && !actual.containsKey(ANIMATION_OVERRIDE)
                && policy != null && policy.animation != null)
            actual.setAnimationProvider(new PageAnimationOptions(policy.animation));
        int flags = coreFlags(actual.getFlags());
        NavigationRequest request = new NavigationRequest(NavigationRequest.Operation.START, caller, type, actual, requestCode);
        if (!allow(request)) return;
        try { engine.startPage(type, flags, params(type, actual), result, actual.getAnimation()); }
        catch (RuntimeException error) { reportError(caller, "start", error); throw error; }
    }
    /** The no-argument finish pairs with the provider used to open this page. */
    public void finishPage(ViewPage page) {
        checkAttached(page);
        finish(page, page.exitAnimation());
    }
    /** Explicit null animation disables the paired exit animation for this operation. */
    public void finishPage(ViewPage page, IPageAnimationProvider animation, IPageAnimationListener listener) {
        checkAttached(page);
        finish(page, animation == null ? null : new PageAnimationOptions(listener, animation));
    }
    private void finish(ViewPage page, PageAnimationOptions animation) {
        checkNavigation();
        if (finishing.contains(page)) return;
        NavigationRequest request = new NavigationRequest(NavigationRequest.Operation.FINISH,
                page, page.getClass(), page.arguments(), null);
        if (!allow(request)) return;
        finishing.add(page);
        try { engine.finishPage(identity(page), animation); }
        catch (RuntimeException error) {
            finishing.remove(page);
            reportError(page, "finish", error);
            throw error;
        }
    }
    public void setPageResult(ViewPage page, int resultCode, PageBundle data) {
        checkAttached(page);
        engine.setPageResult(identity(page), resultCode,
                data == null ? null : params(page.getClass(), copy(data)));
    }
    public boolean onBackPressed() {
        checkNavigation();
        if (HostLifecycle.isBusy(pages)) return true;
        if (HostLifecycle.back(pages) != 0) return true;
        if (engine.getInternalPageStacks().size() <= 1) return false;
        finishPage((ViewPage) engine.getInternalTopPage());
        return true;
    }
    public ViewPage getTopPage() { checkAlive(); return (ViewPage) engine.getInternalTopPage(); }
    public List<IPageController> getPageStack() { checkAlive(); return engine.getInternalPageStacks(); }
    public List<IPageController> getVisiblePages() { checkAlive(); return engine.getInternalTopVisiblePages(); }
    public boolean isBusy() { checkAlive(); return HostLifecycle.isBusy(pages); }
    public boolean isDestroyed() { requireMainThread(); return destroyed; }
    public void onStart() { checkNavigation(); HostLifecycle.start(pages); }
    public void onResume() { checkNavigation(); HostLifecycle.resume(pages); }
    public void onPause() { checkNavigation(); HostLifecycle.pause(pages); }
    public void onStop() { checkNavigation(); HostLifecycle.stop(pages); }

    public void onConfigurationChanged(Configuration configuration) {
        checkAlive();
        Objects.requireNonNull(configuration, "configuration");
        for (ViewPage page : snapshot()) page.dispatchConfiguration(new Configuration(configuration));
    }
    /** Host-window event. Pages combine this with isVisible()/isResumed() for interaction. */
    public void onWindowFocusChanged(boolean hasFocus) {
        checkAlive();
        for (ViewPage page : snapshot()) page.dispatchWindowFocus(hasFocus);
    }
    public void onLowMemory() { checkAlive(); for (ViewPage page : snapshot()) page.dispatchLowMemory(); }
    public void onTrimMemory(int level) { checkAlive(); for (ViewPage page : snapshot()) page.dispatchTrimMemory(level); }
    public void onMultiWindowModeChanged(boolean value, Configuration configuration) {
        checkAlive();
        Objects.requireNonNull(configuration, "configuration");
        for (ViewPage page : snapshot()) page.dispatchMultiWindow(value, new Configuration(configuration));
    }
    public void onPictureInPictureModeChanged(boolean value, Configuration configuration) {
        checkAlive();
        Objects.requireNonNull(configuration, "configuration");
        for (ViewPage page : snapshot()) page.dispatchPictureInPicture(value, new Configuration(configuration));
    }
    /** One pending request per code. A dead owner's code stays reserved until its result returns. */
    @SuppressWarnings("deprecation")
    public void startActivityForResult(ViewPage caller, Intent intent, int requestCode) {
        checkAttached(caller);
        checkNavigation();
        if (requestCode < 0 || requestCode > 65535) throw new IllegalArgumentException("requestCode must be 0..65535");
        if (activityResults.containsKey(requestCode)) throw new IllegalStateException("Activity requestCode is already pending");
        Activity activity = activity();
        activityResults.put(requestCode, new WeakReference<>(caller));
        try { activity.startActivityForResult(Objects.requireNonNull(intent, "intent"), requestCode); }
        catch (RuntimeException error) {
            activityResults.remove(requestCode);
            reportError(caller, "startActivity", error);
            throw error;
        }
    }
    /** False means this request did not originate from this navigator. Never falls back to top. */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        checkAlive();
        WeakReference<ViewPage> owner = activityResults.remove(requestCode);
        if (owner == null) return false;
        ViewPage page = owner.get();
        if (page != null && page.navigator() == this && page.isAlive())
            page.dispatchActivityResult(requestCode, resultCode, data);
        return true;
    }
    public void onDestroy() {
        requireMainThread();
        if (destroyed) return;
        checkNavigation();
        destroyed = true;
        try { HostLifecycle.destroy(pages); }
        finally {
            notifications.removeCallbacksAndMessages(null);
            notificationPending = false;
            routes.clear();
            policies.clear();
            finishing.clear();
            activityResults.clear();
            interceptors.clear();
            observers.clear();
            pages = null;
            engine = null;
            context = null;
        }
    }

    public static void useDefaultAnimation(PageBundle bundle) {
        bundle.putBoolean(ANIMATION_OVERRIDE, true);
        bundle.setAnimationProvider(new PageAnimationOptions((IPageAnimationProvider) null));
    }
    public static void setAnimation(PageBundle bundle, IPageAnimationProvider provider,
                                    IPageAnimationListener listener) {
        bundle.putBoolean(ANIMATION_OVERRIDE, true);
        bundle.setAnimationProvider(provider == null ? null : new PageAnimationOptions(listener, provider));
    }
    public static void withoutAnimation(PageBundle bundle) { setAnimation(bundle, null, null); }

    PageState pageState(ViewPage page) {
        return pages == null ? PageState.DESTROYED : HostLifecycle.state(pages, page);
    }
    boolean pageVisible(ViewPage page) {
        return !destroyed && HostLifecycle.isHostStarted(pages) && engine.getInternalTopVisiblePages().contains(page);
    }
    boolean isFinishing(ViewPage page) { return finishing.contains(page); }
    void pageCreated(ViewPage page) { notifyObservers(observer -> observer.onPageCreated(page)); changed(); }
    void pageReused(ViewPage page) { notifyObservers(observer -> observer.onPageReused(page)); changed(); }
    void pageDestroyed(ViewPage page) {
        finishing.remove(page);
        notifyObservers(observer -> observer.onPageDestroyed(page));
        changed();
    }
    void reportError(ViewPage page, String operation, Throwable error) {
        Log.e("PageFramework", "Page callback failed: " + operation, error);
        notifyObservers(observer -> observer.onError(page, operation, error));
    }
    private void changed() {
        if (destroyed || notificationPending) return;
        notificationPending = true;
        notifications.post(() -> {
            notificationPending = false;
            if (!destroyed) {
                List<ViewPage> stack = Collections.unmodifiableList(snapshot());
                notifyObservers(observer -> observer.onStackChanged(stack));
            }
        });
    }
    private boolean allow(NavigationRequest request) {
        notifyObservers(observer -> observer.onNavigationRequested(request));
        boolean allowed = true;
        inPolicy = true;
        try {
            for (NavigationInterceptor interceptor : new ArrayList<>(interceptors))
                if (!interceptor.allow(request)) { allowed = false; break; }
        } catch (RuntimeException error) {
            reportError(request.source(), "interceptor", error);
            throw error;
        } finally { inPolicy = false; }
        if (!allowed) notifyObservers(observer -> observer.onNavigationBlocked(request));
        return allowed;
    }
    private interface ObserverCall { void run(PageObserver observer); }
    private void notifyObservers(ObserverCall call) {
        boolean previous = inObserver;
        inObserver = true;
        try {
            for (PageObserver observer : new ArrayList<>(observers)) {
                try { call.run(observer); }
                catch (RuntimeException error) { Log.e("PageFramework", "Observer failed", error); }
            }
        } finally { inObserver = previous; }
    }
    private ArrayList<ViewPage> snapshot() {
        ArrayList<ViewPage> result = new ArrayList<>();
        for (IPageController page : engine.getInternalPageStacks()) result.add((ViewPage) page);
        return result;
    }
    private Activity activity() {
        Context current = context;
        Set<Context> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (current != null && seen.add(current)) {
            if (current instanceof Activity) return (Activity) current;
            current = current instanceof ContextWrapper ? ((ContextWrapper) current).getBaseContext() : null;
        }
        throw new IllegalStateException("External Activity results require an Activity context");
    }
    private Class<? extends ViewPage> resolve(String action) {
        checkAlive();
        Class<? extends ViewPage> type = routes.get(action);
        if (type == null) throw new IllegalArgumentException("Unregistered page action: " + action);
        return type;
    }
    static PageBundle bundle(PageParameters params) {
        if (params == null || params.extras == null) return new PageBundle();
        Object value = params.extras.get("CUSCTOM_BUNDLE");
        return value instanceof PageBundle ? (PageBundle) value : new PageBundle();
    }
    /** The original copy constructor omits stack/animation metadata. Keep it explicitly. */
    static PageBundle copy(PageBundle source) {
        PageBundle result = new PageBundle(source);
        result.setFlags(source.getFlags(), source.getPageCnt());
        result.setAnimationProvider(source.getAnimation());
        result.setUniqueID(source.getUniqueID());
        result.setMaxCount(source.getMaxCount());
        result.setMaxCountID(source.getMaxCountID());
        result.setTransitionAnimationStyle(source.getTransitionAnimationStyle());
        result.setNeedPushAnimation(source.isNeedPushAnimation());
        return result;
    }
    private static PageParameters params(Class<?> type, PageBundle bundle) {
        PageParameters params = new PageParameters();
        params.extras = new HashMap<>();
        params.extras.put("CUSCTOM_BUNDLE", bundle);
        params.extras.put("PAGE_COUNT", bundle.getPageCnt());
        params.identityClass = type;
        params.uniqueId = bundle.getUniqueID();
        params.maxCount = bundle.getMaxCount();
        params.maxCountId = bundle.getMaxCountID();
        return params;
    }
    static int coreFlags(int flags) {
        switch (flags) {
            case 0: case PageBundle.FLAG_LAUNCH_MODE_STANDARD: return 1;
            case PageBundle.FLAG_LAUNCH_MODE_SINGLE_TOP: return 8;
            case PageBundle.FLAG_LAUNCH_MODE_SINGLE_TASK: return 4;
            case PageBundle.FLAG_LAUNCH_MODE_SINGLE_INSTANCE: return 2;
            case PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT: return 16;
            default: throw new IllegalArgumentException("Unsupported PageBundle launch flag: " + flags);
        }
    }
    private static PageIdentity identity(IPageController page) {
        PageIdentity id = new PageIdentity();
        id.controllerReference = new WeakReference<>(page);
        return id;
    }
    private static void validate(Class<? extends ViewPage> type) {
        Objects.requireNonNull(type, "type");
        if (!Modifier.isPublic(type.getModifiers()) || Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Page must be public and concrete: " + type.getName());
        try { type.getConstructor(); }
        catch (NoSuchMethodException e) { throw new IllegalArgumentException("Page needs a public no-argument constructor", e); }
    }
    private void checkAttached(ViewPage page) {
        checkAlive();
        if (page == null || page.navigator() != this || !page.isAlive()
                || !engine.getInternalPageStacks().contains(page))
            throw new IllegalArgumentException("Page is not in this navigator");
    }
    private void checkNavigation() {
        checkAlive();
        if (inPolicy || inObserver) throw new IllegalStateException("Post navigation outside policy/observer callbacks");
    }
    private void checkAlive() {
        requireMainThread();
        if (destroyed) throw new IllegalStateException("PageNavigator has been destroyed");
    }
    static void requireMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("PageNavigator must be used on the main thread");
    }
}
