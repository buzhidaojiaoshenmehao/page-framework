package com.usernav.page;

import android.app.Activity;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import com.usernav.page.engine.IPageAnimationProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Integration tests execute the restored engine source, with Android's View/Looper shadows. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class PageNavigatorTest {
    static List<String> events;
    PageNavigator nav;
    FrameLayout container;
    @Before public void setup() {
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(false);
        events = new ArrayList<>();
        container = new FrameLayout(RuntimeEnvironment.getApplication());
        nav = new PageNavigator(container.getContext(), container);
        nav.onStart();
        nav.onResume();
    }
    @After public void cleanup() { if (nav != null) nav.onDestroy(); idle(); }
    private void idle() { shadowOf(Looper.getMainLooper()).idle(); }
    private TestPage push(Class<? extends TestPage> type) { nav.startPage(type, null); idle(); return (TestPage) nav.getTopPage(); }
    private PageBundle mode(int flag) { PageBundle b = new PageBundle(); b.setFlags(flag); return b; }

    @Test public void pushAndFinishPreserveLifecycleAndViews() {
        TestPage a = push(A.class);
        TestPage b = push(B.class);
        assertEquals(Arrays.asList("A.create", "A.start", "A.resume", "B.create", "A.pause", "A.stop", "B.start", "B.resume"), events);
        assertEquals(2, container.getChildCount());
        assertEquals(View.GONE, a.view().getVisibility());
        b.finish(); idle();
        assertSame(a, nav.getTopPage());
        assertEquals(1, container.getChildCount());
        assertEquals(View.VISIBLE, a.view().getVisibility());
        assertTrue(events.containsAll(Arrays.asList("B.pause", "B.stop", "B.destroy")));
        assertNull(b.navigator());
        assertNull(b.view());
    }
    @Test public void syncKeysKeepOriginalSchedulingSemantics() {
        nav.startPage(A.class, null);
        assertNull(nav.getTopPage());
        idle();
        PageBundle immediate = new PageBundle();
        immediate.putBoolean(PageBundle.KEY_EXECUTE_IMMEDIATELY, true);
        nav.startPage(B.class, immediate);
        assertTrue(nav.getTopPage() instanceof A); // Nonempty stack still schedules a handler message.
        idle();
        PageBundle sync = new PageBundle();
        sync.putBoolean(PageBundle.KEY_START_PAGE_SYNC, true);
        nav.startPage(C.class, sync);
        assertTrue(nav.getTopPage() instanceof C);
    }
    @Test public void executeImmediatelyCreatesFirstPageInline() {
        PageBundle b = new PageBundle();
        b.putBoolean(PageBundle.KEY_EXECUTE_IMMEDIATELY, true);
        nav.startPage(A.class, b);
        assertTrue(nav.getTopPage() instanceof A);
    }
    @Test public void transparentLayersRetainVisibleMapAndNotifyDismissal() {
        TestPage a = push(A.class);
        nav.startPage(T1.class, mode(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT)); idle();
        TestPage t1 = (TestPage) nav.getTopPage();
        nav.startPage(T2.class, mode(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT)); idle();
        TestPage t2 = (TestPage) nav.getTopPage();
        assertEquals(Arrays.asList(t2, t1, a), nav.getVisiblePages());
        assertEquals(View.VISIBLE, a.view().getVisibility());
        t2.finish(); idle();
        assertSame(t1, nav.getTopPage());
        assertEquals(1, t1.transparentDismissals);
        assertEquals(Arrays.asList(t1, a), nav.getVisiblePages());
    }
    @Test public void singleTopReusesTopAndDeliversNewArgumentsOnce() {
        TestPage a = push(A.class);
        PageBundle b = mode(PageBundle.FLAG_LAUNCH_MODE_SINGLE_TOP);
        b.putString("value", "new");
        nav.startPage(A.class, b); idle();
        assertSame(a, nav.getTopPage());
        assertEquals(1, nav.getPageStack().size());
        assertEquals(1, a.newArguments);
        assertEquals("new", a.arguments().getString("value"));
    }
    @Test public void singleTaskDestroysPagesAboveExistingTarget() {
        TestPage a = push(A.class);
        TestPage b = push(B.class);
        TestPage c = push(C.class);
        nav.startPage(A.class, mode(PageBundle.FLAG_LAUNCH_MODE_SINGLE_TASK)); idle();
        assertEquals(Arrays.asList(a), nav.getPageStack());
        assertNull(b.navigator());
        assertNull(c.navigator());
        assertEquals(1, a.newArguments);
    }
    @Test public void singleInstanceMovesExistingPageToTopWithoutDestroyingOtherPages() {
        TestPage a = push(A.class);
        TestPage b = push(B.class);
        TestPage c = push(C.class);
        nav.startPage(A.class, mode(PageBundle.FLAG_LAUNCH_MODE_SINGLE_INSTANCE)); idle();
        assertEquals(Arrays.asList(b, c, a), nav.getPageStack());
        assertNotNull(b.navigator());
        assertNotNull(c.navigator());
        assertEquals(1, a.newArguments);
    }
    @Test public void resultIsDeliveredOnlyAfterChildFinishes() {
        TestPage a = push(A.class);
        nav.startPageForResult(a, B.class, null, 7); idle();
        TestPage child = (TestPage) nav.getTopPage();
        PageBundle result = PageBundle.forPair("poi", "destination");
        child.setResult(Activity.RESULT_OK, result);
        assertEquals(0, a.resultCount);
        child.finish(); idle();
        assertEquals(1, a.resultCount);
        assertEquals(7, a.requestCode);
        assertEquals(Activity.RESULT_OK, a.resultCode);
        assertEquals("destination", a.result.getString("poi"));
    }
    @Test public void animationCompletionUnlocksAndDrainsQueuedActions() {
        // Disable Robolectric's automatic View animation runner to control the actual listener boundary.
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(true);
        push(A.class);
        TrackingProvider provider = new TrackingProvider();
        PageBundle b = new PageBundle();
        PageNavigator.setAnimation(b, provider, null);
        nav.startPage(B.class, b); idle();
        TestPage child = (TestPage) nav.getTopPage();
        assertFalse(events.contains("B.resume"));
        nav.startPage(C.class, null); idle();
        assertSame(child, nav.getTopPage());
        assertNotNull(provider.enter.listener);
        provider.enter.listener.onAnimationEnd(provider.enter);
        idle();
        assertTrue(nav.getTopPage() instanceof C);
        assertTrue(events.contains("B.resume"));
    }
    @Test public void originalDefaultAnimationProviderRetainsAllFourTransitions() {
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(true);
        com.usernav.page.internal.SlidePageAnimationProvider provider = new com.usernav.page.internal.SlidePageAnimationProvider();
        View view = new View(container.getContext());
        for (Animation animation : Arrays.asList(provider.enterForAdd(container, view),
                provider.leaveForAdd(container, view), provider.enterForRemove(container, view),
                provider.leaveForRemove(container, view))) {
            assertTrue(animation instanceof android.view.animation.TranslateAnimation);
            assertEquals(300, animation.getDuration());
        }
        PageBundle bundle = new PageBundle();
        PageNavigator.useDefaultAnimation(bundle);
        assertNotNull(bundle.getAnimation());
        push(A.class);
        nav.startPage(B.class, bundle); idle();
        assertTrue(nav.getTopPage() instanceof B);
        Animation actual = nav.getTopPage().view().getAnimation();
        assertNotNull(actual);
        long frameTime = android.os.SystemClock.uptimeMillis();
        actual.getTransformation(frameTime, new android.view.animation.Transformation());
        actual.getTransformation(frameTime + 301, new android.view.animation.Transformation());
        idle();
        assertTrue(events.contains("B.resume"));
    }
    @Test public void animatedFinishDefersResultUntilOriginalEndCallback() {
        org.robolectric.shadows.ShadowView.setUseRealViewAnimations(true);
        TestPage a = push(A.class);
        nav.startPageForResult(a, B.class, null, 9); idle();
        TestPage child = (TestPage) nav.getTopPage();
        child.setResult(Activity.RESULT_OK, PageBundle.forPair("value", "done"));
        TrackingProvider provider = new TrackingProvider();
        nav.finishPage(child, provider, null); idle();
        assertEquals(0, a.resultCount);
        assertSame(child, nav.getTopPage());
        provider.leave.listener.onAnimationEnd(provider.leave); idle();
        assertSame(a, nav.getTopPage());
        assertEquals(1, a.resultCount);
        assertEquals("done", a.result.getString("value"));
    }
    @Test public void hostLifecycleAndDestroyReleaseStackAndCancelQueuedStarts() {
        TestPage a = push(A.class);
        nav.onPause(); nav.onStop();
        assertEquals(Arrays.asList("A.create", "A.start", "A.resume", "A.pause", "A.stop"), events);
        nav.onStart(); nav.onResume();
        assertEquals("A.resume", events.get(events.size() - 1));
        nav.startPage(B.class, null);
        nav.onDestroy(); idle();
        assertFalse(events.contains("B.create"));
        assertNull(a.navigator());
        assertNull(a.context());
        assertEquals(0, container.getChildCount());
        try { nav.startPage(C.class, null); fail("Must reject use after destruction"); }
        catch (IllegalStateException expected) {}
    }
    @Test public void backFinishesChildAndLetsActivityHandleRoot() {
        TestPage a = push(A.class);
        assertFalse(nav.onBackPressed());
        push(B.class);
        assertTrue(nav.onBackPressed()); idle();
        assertSame(a, nav.getTopPage());
        a.consumeBack = true;
        assertTrue(nav.onBackPressed());
    }
    @Test public void rejectsBackgroundThreadCallsAndUnknownFlags() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread t = new Thread(() -> { try { nav.startPage(A.class, null); } catch (Throwable e) { failure.set(e); } });
        t.start(); t.join();
        assertTrue(failure.get() instanceof IllegalStateException);
        try { nav.startPage(A.class, mode(PageBundle.FLAG_LAUNCH_MODE_SINGLE_TASK_WITHOUT_REUSE)); fail(); }
        catch (IllegalArgumentException expected) {}
    }
    @Test public void originalPageBundleSupportsTypedValuesCloneAndFallback() {
        PageBundle b = new PageBundle();
        b.putInt("int", 12); b.putBoolean("boolean", true); b.putString("str", "hello");
        b.putObject("object", container);
        PageBundle clone = (PageBundle) b.clone();
        b.putInt("int", 18);
        assertEquals(12, clone.getInt("int"));
        assertTrue(clone.getBoolean("boolean"));
        assertSame(container, clone.getObject("object"));
        assertEquals(99, clone.getInt("str", 99));
    }
    public static class TestPage extends ViewPage {
        int newArguments, transparentDismissals, resultCount, requestCode, resultCode;
        PageBundle result;
        boolean consumeBack;
        String name() { return getClass().getSimpleName(); }
        @Override protected void onCreate(PageBundle b) { events.add(name() + ".create"); }
        @Override protected View createView(LayoutInflater inflater, ViewGroup container) { return new View(context()); }
        @Override public void onStart() { events.add(name() + ".start"); }
        @Override public void onResume() { events.add(name() + ".resume"); }
        @Override public void onPause() { events.add(name() + ".pause"); }
        @Override public void onStop() { events.add(name() + ".stop"); }
        @Override protected void onPageDestroyed() { events.add(name() + ".destroy"); }
        @Override protected void onNewArguments(PageBundle b) { newArguments++; }
        @Override public void onTransparentDismissed() { transparentDismissals++; }
        @Override protected boolean onHandleBack() { return consumeBack; }
        @Override protected void onResult(int request, int code, PageBundle data) {
            resultCount++; requestCode = request; resultCode = code; result = data;
        }
    }
    public static class A extends TestPage {}
    public static class B extends TestPage {}
    public static class C extends TestPage {}
    public static class T1 extends TestPage {}
    public static class T2 extends TestPage {}
    public static class TrackingAnimation extends Animation {
        AnimationListener listener;
        public TrackingAnimation() { setDuration(10000); }
        @Override public void setAnimationListener(AnimationListener listener) {
            super.setAnimationListener(listener); this.listener = listener;
        }
    }
    public static class TrackingProvider implements IPageAnimationProvider {
        TrackingAnimation enter, leave;
        @Override public Animation enterForAdd(ViewGroup group, View view) { enter = new TrackingAnimation(); return enter; }
        @Override public Animation leaveForAdd(ViewGroup group, View view) { return new TrackingAnimation(); }
        @Override public Animation enterForRemove(ViewGroup group, View view) { return new TrackingAnimation(); }
        @Override public Animation leaveForRemove(ViewGroup group, View view) { leave = new TrackingAnimation(); return leave; }
    }
}
