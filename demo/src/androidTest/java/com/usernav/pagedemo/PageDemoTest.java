package com.usernav.pagedemo;

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Device integration: exercises visible controls with the actual core queue and animations. */
@SuppressWarnings("deprecation")
public final class PageDemoTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity activity;
    public PageDemoTest() { super(MainActivity.class); }
    @Override protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(true);
        activity = getActivity();
        awaitText("Page 框架", true);
    }
    public void testPickerPassesArgumentsAndKeepsTheDraft() throws Throwable {
        runTestOnUiThread(() -> {
            EditText draft = (EditText) find(activity.getWindow().getDecorView(), "周末计划");
            assertNotNull(draft);
            draft.setText("我的草稿");
        });
        click("选择分类");
        awaitText("当前分类 · 设计\n选择新分类后返回表单。", true);
        click("开发");
        awaitText("已选分类 · 开发", true);
        awaitText("我的草稿", true);
        awaitText("分类选择", false);
    }

    public void testTransparentPanelAndRepeatedCloseKeepRoot() throws Throwable {
        click("打开透明面板");
        awaitText("透明筛选面板", true);
        awaitText("Page 框架", true);
        awaitText("页面栈 · 首页 → 透明筛选面板", true);
        runTestOnUiThread(() -> {
            View button = find(activity.getWindow().getDecorView(), "关闭面板");
            assertNotNull(button);
            button.performClick();
            button.performClick();
        });
        awaitText("透明筛选面板", false);
        awaitText("页面栈 · 首页", true);
    }

    public void testDefaultFinishPairsTheEntryAnimationAndDisposesPageTimer() throws Throwable {
        openReview();
        awaitPrefix("已打开 ");
        AtomicReference<TextView> timer = new AtomicReference<>();
        getInstrumentation().runOnMainSync(() ->
                timer.set(findPrefix(activity.getWindow().getDecorView(), "已打开 ")));
        click("返回编辑页（退场动画）");
        awaitText("确认订单", false);
        awaitText("编辑订单", true);
        AtomicReference<String> stoppedText = new AtomicReference<>();
        getInstrumentation().runOnMainSync(() -> stoppedText.set(timer.get().getText().toString()));
        SystemClock.sleep(1200);
        getInstrumentation().runOnMainSync(() ->
                assertEquals("Detached page timer must stop", stoppedText.get(), timer.get().getText().toString()));
    }

    public void testSingleTaskReusesTheEditorAndRemovesReviewPage() throws Throwable {
        openReview();
        awaitText("页面栈 · 首页 → 编辑订单 → 确认订单", true);
        click("确认并复用编辑页");
        awaitText("商品数量 · 2", true);
        awaitText("接收确认参数 · 1 次", true);
        awaitText("页面栈 · 首页 → 编辑订单", true);
        awaitText("确认订单", false);
    }

    private void openReview() throws Throwable {
        click("体验多步流程");
        awaitText("编辑订单", true);
        click("数量 +1");
        click("进入确认页");
        awaitText("确认订单", true);
        awaitText("本次确认数量 · 2", true);
    }
    private void click(String text) throws Throwable {
        runTestOnUiThread(() -> {
            View view = find(activity.getWindow().getDecorView(), text);
            assertNotNull("Visible control: " + text, view);
            assertTrue(view.performClick());
        });
    }
    private void awaitText(String text, boolean visible) {
        long deadline = SystemClock.uptimeMillis() + 6000;
        AtomicBoolean matches = new AtomicBoolean();
        do {
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(() -> matches.set(
                (find(activity.getWindow().getDecorView(), text) != null) == visible));
            if (matches.get()) return;
            SystemClock.sleep(40);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Timed out waiting for visibility " + visible + ": " + text);
    }
    private void awaitPrefix(String prefix) {
        long deadline = SystemClock.uptimeMillis() + 6000;
        AtomicBoolean matches = new AtomicBoolean();
        do {
            getInstrumentation().waitForIdleSync();
            getInstrumentation().runOnMainSync(() -> matches.set(findPrefix(activity.getWindow().getDecorView(), prefix) != null));
            if (matches.get()) return;
            SystemClock.sleep(40);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Timed out waiting for timer update");
    }
    private static View find(View view, String text) {
        if (!view.isShown()) return null;
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static TextView findPrefix(View view, String prefix) {
        if (!view.isShown()) return null;
        if (view instanceof TextView && ((TextView) view).getText().toString().startsWith(prefix)) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findPrefix(group.getChildAt(i), prefix);
                if (found != null) return found;
            }
        }
        return null;
    }
    @Override protected void tearDown() throws Exception {
        if (activity != null) getInstrumentation().runOnMainSync(() -> activity.finish());
        super.tearDown();
    }
}
