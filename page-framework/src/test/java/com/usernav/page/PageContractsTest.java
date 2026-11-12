package com.usernav.page;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.usernav.page.engine.IPageAnimationListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class PageContractsTest {
    PageNavigator nav;
    FrameLayout container;
    static List<String> trace;
    @Before public void setup() {
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(false);
        trace = new ArrayList<>();
        container = new FrameLayout(RuntimeEnvironment.getApplication());
        nav = new PageNavigator(container.getContext(), container);
        nav.onStart(); nav.onResume();
    }
    @After public void cleanup() { nav.onDestroy(); idle(); }
    void idle() { shadowOf(Looper.getMainLooper()).idle(); }
    Basic push(Class<? extends Basic> type) { nav.startPage(type, null); idle(); return (Basic) nav.getTopPage(); }
    PageBundle transparent() { PageBundle b = new PageBundle(); b.setFlags(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT); return b; }

    @Test public void stateIsAccurateInsideLegacyOverridesThatDoNotCallSuper() {
        assertEquals(PageState.INITIALIZED, new Basic().state());
        Basic first = push(Basic.class);
        assertTrue(first.isAlive()); assertTrue(first.isStarted()); assertTrue(first.isResumed());
        assertEquals(PageState.RESUMED, first.stateInResume);
        assertTrue(first.isVisible());
        Basic child = push(Second.class);
        assertEquals(PageState.CREATED, first.state());
        assertFalse(first.isStarted()); assertFalse(first.isVisible());
        child.finish(); idle();
        assertTrue(first.isResumed());
        assertEquals(PageState.DESTROYED, child.state()); assertFalse(child.isAlive());
        try { child.finish(); fail(); } catch (IllegalStateException expected) {}
    }
    @Test public void visibilityDistinguishesTransparentCoverAndStoppedHost() {
        Basic root = push(Basic.class);
        nav.startPage(Second.class, transparent()); idle();
        assertTrue(root.isVisible());
        nav.onPause(); nav.onStop();
        assertFalse(root.isVisible()); assertFalse(nav.getTopPage().isVisible());
        nav.onStart(); nav.onResume();
        assertTrue(root.isVisible()); assertTrue(nav.getTopPage().isVisible());
    }
    @Test public void cleanupContinuesAfterFailuresAndKeepsReferencesForTheirDocumentedScope() {
        push(Basic.class);
        Basic page = push(ThrowingRemoval.class);
        ArrayList<String> errors = new ArrayList<>();
        nav.addObserver(new PageObserver() {
            @Override public void onError(ViewPage p, String operation, Throwable error) { errors.add(operation); }
        });
        page.viewResources().add(() -> { assertNotNull(page.view()); assertNotNull(page.context()); trace.add("view.first"); });
        page.viewResources().add(() -> { trace.add("view.bad"); throw new IllegalStateException("cleanup fixture"); });
        page.resources().add(() -> { assertNull(page.view()); assertNotNull(page.context()); trace.add("page.resource"); });
        page.finish(); idle();
        assertEquals(Arrays.asList("remove", "view.bad", "view.first", "page.resource", "destroy"), trace);
        assertEquals(Arrays.asList("removeView", "viewResource"), errors);
        assertNull(page.context()); assertNull(page.navigator());
        assertTrue(page.viewResources().isClosed()); assertTrue(page.resources().isClosed());
    }
    @Test public void resourceRegistrationIsIdempotentAndCanDisposeOtherRegistrations() {
        Basic page = push(Basic.class);
        PageResources scope = page.resources();
        PageResources.Registration first = scope.add(() -> trace.add("first"));
        scope.add(() -> { first.close(); scope.add(() -> trace.add("late")); trace.add("last"); });
        scope.close(); scope.close(); first.close();
        assertEquals(Arrays.asList("first", "late", "last"), trace);
        scope.add(() -> trace.add("after"));
        assertEquals("after", trace.get(3));
    }
    @Test public void finishingOnePageCancelsOnlyItsScopedCallbacks() {
        Basic root = push(Basic.class);
        Basic child = push(Second.class);
        root.resources().postDelayed(() -> trace.add("root.task"), 500);
        child.resources().postDelayed(() -> trace.add("child.task"), 500);
        child.finish(); idle();
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
        assertEquals(Arrays.asList("root.task"), trace);
    }
    @Test public void hostDestructionDisposesHiddenPagesAndCancelsPendingCallbacks() {
        Basic root = push(Basic.class);
        Basic child = push(Second.class);
        root.resources().add(() -> trace.add("root.dispose"));
        child.resources().add(() -> trace.add("child.dispose"));
        root.resources().postDelayed(() -> trace.add("unexpected"), 1000);
        nav.onDestroy(); shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS);
        assertTrue(trace.containsAll(Arrays.asList("root.dispose", "child.dispose")));
        assertFalse(trace.contains("unexpected")); assertTrue(nav.isDestroyed());
    }
    @Test public void backHandlersRunNewestFirstBeforeTheLegacyHook() {
        Basic page = push(Basic.class);
        page.addBackHandler(() -> { trace.add("older"); return true; });
        PageResources.Registration newest = page.addBackHandler(() -> { trace.add("newer"); return false; });
        assertTrue(nav.onBackPressed());
        assertEquals(Arrays.asList("newer", "older"), trace);
        newest.close(); trace.clear();
        assertTrue(nav.onBackPressed()); assertEquals(Arrays.asList("older"), trace);
    }
    @Test public void actionForResultReturnsToTheCallingPage() {
        Basic root = push(Basic.class);
        nav.register("pick", Second.class);
        root.startPageForResult("pick", PageBundle.forPair("query", "park"), 27); idle();
        Basic child = (Basic) nav.getTopPage();
        child.setResult(Activity.RESULT_OK, PageBundle.forPair("value", "selected"));
        assertEquals(0, root.results);
        child.finish(); idle();
        assertEquals(1, root.results); assertEquals(27, root.request);
        assertEquals("selected", root.result.getString("value"));
    }
    @Test public void defaultTransparentPolicyDoesNotMutateTheCallersBundle() {
        Basic root = push(Basic.class);
        nav.register("panel", Second.class, PagePolicy.transparent(null));
        PageBundle caller = new PageBundle();
        nav.startPage("panel", caller); idle();
        assertTrue(root.isVisible()); assertEquals(0, caller.getFlags());
        assertEquals(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT, nav.getTopPage().arguments().getFlags());
    }
    @Test public void explicitStandardFlagOverridesTheClassDefault() {
        Basic root = push(Basic.class);
        nav.setDefaultPolicy(Second.class, PagePolicy.transparent(null));
        PageBundle explicit = new PageBundle(); explicit.setFlags(PageBundle.FLAG_LAUNCH_MODE_STANDARD);
        nav.startPage(Second.class, explicit); idle();
        assertFalse(root.isVisible());
    }
    @Test public void queuedNavigationPreservesStackMetadataWhenTheCallerChangesItsBundle() {
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(true);
        Basic root = push(Basic.class);
        PageNavigatorTest.TrackingProvider provider = new PageNavigatorTest.TrackingProvider();
        PageBundle source = new PageBundle();
        source.setFlags(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT, 3);
        source.setUniqueID("page-id"); source.setMaxCount(2); source.setMaxCountID("group");
        source.setTransitionAnimationStyle(4); source.setNeedPushAnimation(true);
        PageNavigator.setAnimation(source, provider, null);
        nav.startPage(Second.class, source);
        source.setFlags(PageBundle.FLAG_LAUNCH_MODE_STANDARD, 0);
        source.setUniqueID("changed"); source.setMaxCount(99); source.setMaxCountID("changed");
        source.setTransitionAnimationStyle(0); source.setNeedPushAnimation(false);
        idle();
        PageBundle copy = nav.getTopPage().arguments();
        assertTrue(root.isVisible());
        assertEquals(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT, copy.getFlags()); assertEquals(3, copy.getPageCnt());
        assertEquals("page-id", copy.getUniqueID()); assertEquals(2, copy.getMaxCount());
        assertEquals("group", copy.getMaxCountID()); assertEquals(4, copy.getTransitionAnimationStyle());
        assertTrue(copy.isNeedPushAnimation()); assertSame(source.getAnimation(), copy.getAnimation());
        assertNotNull(provider.enter);
        provider.enter.listener.onAnimationEnd(provider.enter); idle();
        assertTrue(nav.getTopPage().isResumed());
    }
    @Test public void queuedNavigationDoesNotObserveLaterCallerArgumentMutation() {
        push(Basic.class);
        PageBundle args = PageBundle.forPair("value", "before");
        nav.startPage(Second.class, args); args.putString("value", "after"); idle();
        assertEquals("before", nav.getTopPage().arguments().getString("value"));
    }
    @Test public void finishAutomaticallyPairsTheOpeningProviderWithoutReplayingTheOpeningListener() {
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(true);
        Basic root = push(Basic.class);
        PageNavigatorTest.TrackingProvider provider = new PageNavigatorTest.TrackingProvider();
        int[] enterFinished = {0};
        PageBundle args = new PageBundle();
        PageNavigator.setAnimation(args, provider, new IPageAnimationListener() {
            public void onStarted() {} public void onFinished() { enterFinished[0]++; }
        });
        nav.startPage(Second.class, args); idle();
        provider.enter.listener.onAnimationEnd(provider.enter); idle();
        Basic child = (Basic) nav.getTopPage();
        child.finish(); idle();
        assertSame(child, nav.getTopPage()); assertNotNull(provider.leave);
        provider.leave.listener.onAnimationEnd(provider.leave); idle();
        assertSame(root, nav.getTopPage()); assertEquals(1, enterFinished[0]);
    }
    @Test public void explicitNullFinishAnimationDisablesThePairedTransition() {
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(true);
        Basic root = push(Basic.class);
        PageNavigatorTest.TrackingProvider provider = new PageNavigatorTest.TrackingProvider();
        PageBundle args = new PageBundle(); PageNavigator.setAnimation(args, provider, null);
        nav.startPage(Second.class, args); idle();
        provider.enter.listener.onAnimationEnd(provider.enter); idle();
        nav.finishPage(nav.getTopPage(), null, null); idle();
        assertSame(root, nav.getTopPage()); assertNull(provider.leave);
    }
    @Test public void explicitAnimationOptOutOverridesAPageDefault() {
        push(Basic.class);
        PageNavigatorTest.TrackingProvider provider = new PageNavigatorTest.TrackingProvider();
        nav.setDefaultPolicy(Second.class, PagePolicy.transparent(provider));
        PageBundle args = new PageBundle(); PageNavigator.withoutAnimation(args);
        nav.startPage(Second.class, args); idle();
        assertNull(provider.enter); assertTrue(nav.getTopPage().isResumed());
    }
    @Test public void interceptionBlocksStartAndFinishWithoutLeavingAFinishingPage() {
        Basic root = push(Basic.class);
        NavigationInterceptor block = request -> false;
        nav.addInterceptor(block);
        root.startPageForResult(Second.class, null, 7); idle();
        assertSame(root, nav.getTopPage()); assertEquals(0, root.results);
        nav.removeInterceptor(block);
        Basic child = push(Second.class);
        nav.addInterceptor(block);
        assertTrue(nav.onBackPressed()); idle();
        assertSame(child, nav.getTopPage()); assertFalse(child.isFinishing());
        nav.removeInterceptor(block); child.finish(); idle(); assertSame(root, nav.getTopPage());
    }
    @Test public void duplicateFinishRequestsAreSubmittedOnlyOnce() {
        push(Basic.class); Basic child = push(Second.class);
        int[] requests = {0};
        nav.addObserver(new PageObserver() {
            @Override public void onNavigationRequested(NavigationRequest request) {
                if (request.operation() == NavigationRequest.Operation.FINISH) requests[0]++;
            }
        });
        child.finish(); child.finish(); child.finish();
        assertTrue(child.isFinishing()); idle();
        assertEquals(1, requests[0]); assertEquals(1, nav.getPageStack().size());
    }
    @Test public void observersSeeCommittedImmutableSnapshotsAndCannotBreakTheCoreQueue() {
        ArrayList<List<ViewPage>> snapshots = new ArrayList<>();
        nav.addObserver(new PageObserver() {
            @Override public void onPageCreated(ViewPage page) { throw new IllegalStateException("observer fixture"); }
            @Override public void onStackChanged(List<ViewPage> pages) { snapshots.add(pages); }
        });
        Basic root = push(Basic.class); Basic child = push(Second.class);
        assertEquals(Arrays.asList(root, child), snapshots.get(snapshots.size() - 1));
        try { snapshots.get(0).clear(); fail(); } catch (UnsupportedOperationException expected) {}
        child.finish(); idle(); assertEquals(Arrays.asList(root), snapshots.get(snapshots.size() - 1));
    }
    @Test public void requestSnapshotsCannotChangeTheActualFlagsOrArguments() {
        push(Basic.class);
        nav.addInterceptor(request -> {
            PageBundle snapshot = request.arguments();
            snapshot.setFlags(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT);
            snapshot.putString("value", "changed");
            return true;
        });
        nav.startPage(Second.class, PageBundle.forPair("value", "original")); idle();
        assertEquals("original", nav.getTopPage().arguments().getString("value"));
        assertEquals(1, nav.getVisiblePages().size());
    }
    @Test public void systemEventsReachHiddenPagesWithIndependentConfigurationsAndHostWindowFocus() {
        Basic root = push(MutatingConfiguration.class); Basic child = push(Second.class);
        Configuration config = new Configuration(); config.orientation = Configuration.ORIENTATION_PORTRAIT;
        nav.onConfigurationChanged(config); nav.onWindowFocusChanged(true);
        nav.onTrimMemory(10); nav.onLowMemory();
        assertEquals(Configuration.ORIENTATION_PORTRAIT, child.orientation);
        assertEquals(Configuration.ORIENTATION_PORTRAIT, config.orientation);
        assertTrue(root.focus); assertTrue(child.focus);
        assertEquals(10, root.trim); assertEquals(10, child.trim);
        assertEquals(1, root.lowMemory); assertEquals(1, child.lowMemory);
    }
    @Test public void failingEventCallbackIsReportedAndDoesNotPreventOtherPagesReceivingIt() {
        push(BadConfiguration.class); Basic child = push(Second.class);
        ArrayList<String> errors = new ArrayList<>();
        nav.addObserver(new PageObserver() {
            @Override public void onError(ViewPage page, String operation, Throwable error) { errors.add(operation); }
        });
        Configuration config = new Configuration(); config.orientation = Configuration.ORIENTATION_LANDSCAPE;
        nav.onConfigurationChanged(config);
        assertEquals(Configuration.ORIENTATION_LANDSCAPE, child.orientation);
        assertEquals(Arrays.asList("configuration"), errors);
    }
    @Test public void multiWindowAndPictureInPictureEventsAreBridged() {
        Basic page = push(Basic.class);
        nav.onMultiWindowModeChanged(true, new Configuration());
        nav.onPictureInPictureModeChanged(true, new Configuration());
        assertTrue(page.multiWindow); assertTrue(page.pip);
    }
    @Test public void externalActivityResultReturnsToItsHiddenOwner() {
        useActivityHost(); Basic root = push(Basic.class);
        root.startActivityForResult(new Intent("example.PICK"), 37);
        Basic child = push(Second.class);
        assertTrue(nav.onActivityResult(37, Activity.RESULT_OK, new Intent().putExtra("value", "picked")));
        assertEquals(1, root.activityResults); assertEquals(0, child.activityResults);
        assertFalse(nav.onActivityResult(37, Activity.RESULT_OK, null));
    }
    @Test public void lateExternalResultCannotBeDeliveredToANewPageReusingItsRequestCode() {
        useActivityHost(); Basic root = push(Basic.class); Basic child = push(Second.class);
        child.startActivityForResult(new Intent("example.PICK"), 37);
        child.finish(); idle();
        try { root.startActivityForResult(new Intent("example.PICK"), 37); fail(); }
        catch (IllegalStateException expected) {}
        assertTrue(nav.onActivityResult(37, Activity.RESULT_OK, null));
        assertEquals(0, root.activityResults);
        root.startActivityForResult(new Intent("example.PICK"), 37);
        assertTrue(nav.onActivityResult(37, Activity.RESULT_OK, null));
        assertEquals(1, root.activityResults);
    }
    @Test public void policyCannotReenterNavigationAndCorruptRequestOrder() {
        Basic root = push(Basic.class);
        nav.addInterceptor(request -> { nav.startPage(Third.class, null); return true; });
        try { nav.startPage(Second.class, null); fail(); } catch (IllegalStateException expected) {}
        idle(); assertSame(root, nav.getTopPage());
    }
    @Test public void observersCannotDestroyTheHostDuringCoreConstruction() {
        nav.addObserver(new PageObserver() {
            @Override public void onPageCreated(ViewPage page) { nav.onDestroy(); }
        });
        Basic root = push(Basic.class);
        assertFalse(nav.isDestroyed()); assertTrue(root.isResumed());
    }
    @Test public void aCreationFailureReleasesRegisteredResourcesAndDoesNotPreventHostCleanup() {
        Basic root = push(Basic.class);
        root.resources().add(() -> trace.add("root.released"));
        nav.startPage(BadCreate.class, null); idle();
        assertTrue(trace.contains("failed.released"));
        nav.onDestroy();
        assertTrue(trace.contains("root.released")); assertNull(root.context());
        assertEquals(0, container.getChildCount());
    }

    void useActivityHost() {
        nav.onDestroy();
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        container = new FrameLayout(activity);
        activity.setContentView(container);
        nav = new PageNavigator(activity, container); nav.onStart(); nav.onResume();
    }
    public static class Basic extends ViewPage {
        PageState stateInResume;
        int results, request, activityResults, orientation, trim, lowMemory;
        boolean focus, multiWindow, pip;
        PageBundle result;
        @Override protected View createView(LayoutInflater inflater, ViewGroup container) { return new View(context()); }
        @Override public void onResume() { stateInResume = state(); }
        @Override protected void onResult(int requestCode, int resultCode, PageBundle data) { results++; request = requestCode; result = data; }
        @Override protected void onConfigurationChanged(Configuration configuration) { orientation = configuration.orientation; }
        @Override protected void onWindowFocusChanged(boolean value) { focus = value; }
        @Override protected void onLowMemory() { lowMemory++; }
        @Override protected void onTrimMemory(int value) { trim = value; }
        @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) { activityResults++; }
        @Override protected void onMultiWindowModeChanged(boolean value, Configuration configuration) { multiWindow = value; }
        @Override protected void onPictureInPictureModeChanged(boolean value, Configuration configuration) { pip = value; }
    }
    public static class Second extends Basic {}
    public static class Third extends Basic {}
    public static class ThrowingRemoval extends Basic {
        @Override protected void onViewRemoved() { assertFalse(isAlive()); trace.add("remove"); throw new IllegalStateException("remove fixture"); }
        @Override protected void onPageDestroyed() { assertNull(view()); assertNotNull(context()); trace.add("destroy"); }
    }
    public static class MutatingConfiguration extends Basic {
        @Override protected void onConfigurationChanged(Configuration configuration) { configuration.orientation = Configuration.ORIENTATION_LANDSCAPE; }
    }
    public static class BadConfiguration extends Basic {
        @Override protected void onConfigurationChanged(Configuration configuration) { throw new IllegalStateException("event fixture"); }
    }
    public static class BadCreate extends Basic {
        @Override protected void onCreate(PageBundle arguments) {
            resources().add(() -> trace.add("failed.released"));
            throw new IllegalStateException("creation fixture");
        }
    }
}
