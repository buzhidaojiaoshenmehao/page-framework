package com.zleilabs.pagedemo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.usernav.page.PageBundle;
import com.usernav.page.PageNavigator;
import com.usernav.page.PageObserver;
import com.usernav.page.PagePolicy;
import com.usernav.page.ViewPage;

import java.util.ArrayList;
import java.util.List;

/** Owns the page container and forwards Activity lifecycle events. */
public final class MainActivity extends Activity {
    private PageNavigator navigator;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = DemoUi.column(this);
        layout.setBackgroundColor(DemoUi.SURFACE);
        getWindow().setStatusBarColor(DemoUi.INK);
        getWindow().setNavigationBarColor(DemoUi.INK);

        FrameLayout container = new FrameLayout(this);
        layout.addView(container, new LinearLayout.LayoutParams(-1, 0, 1f));
        TextView stackView = DemoUi.text(this, "", 14, DemoUi.INK);
        stackView.setBackgroundColor(DemoUi.DIAGNOSTIC);
        stackView.setPadding(DemoUi.dp(this, 20), DemoUi.dp(this, 10),
                DemoUi.dp(this, 20), DemoUi.dp(this, 10));
        layout.addView(stackView, new LinearLayout.LayoutParams(-1, -2));
        setContentView(layout);

        navigator = new PageNavigator(this, container);
        navigator.register(DemoPages.PICK_ACTION, DemoPages.OptionPickerPage.class);
        navigator.setDefaultPolicy(DemoPages.FilterPanelPage.class, PagePolicy.transparent(null));
        navigator.addObserver(new PageObserver() {
            @Override public void onStackChanged(List<ViewPage> pages) {
                List<String> names = new ArrayList<>(pages.size());
                for (ViewPage page : pages) {
                    names.add(((DemoPages.DemoPage) page).title());
                }
                stackView.setText("页面栈 · " + TextUtils.join(" → ", names));
            }
        });

        PageBundle first = new PageBundle();
        first.putBoolean(PageBundle.KEY_EXECUTE_IMMEDIATELY, true);
        navigator.startPage(DemoPages.HomePage.class, first);
    }

    @Override protected void onStart() { super.onStart(); navigator.onStart(); }
    @Override protected void onResume() { super.onResume(); navigator.onResume(); }
    @Override protected void onPause() { navigator.onPause(); super.onPause(); }
    @Override protected void onStop() { navigator.onStop(); super.onStop(); }
    @Override protected void onDestroy() { navigator.onDestroy(); super.onDestroy(); }
    @Override public void onBackPressed() { if (!navigator.onBackPressed()) super.onBackPressed(); }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (navigator != null && !navigator.isDestroyed()) navigator.onConfigurationChanged(configuration);
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (navigator != null && !navigator.isDestroyed()) navigator.onWindowFocusChanged(hasFocus);
    }
    @Override public void onLowMemory() {
        super.onLowMemory();
        if (navigator != null && !navigator.isDestroyed()) navigator.onLowMemory();
    }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (navigator != null && !navigator.isDestroyed()) navigator.onTrimMemory(level);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (navigator != null && !navigator.isDestroyed()) navigator.onActivityResult(requestCode, resultCode, data);
    }
    @android.annotation.TargetApi(26)
    @Override public void onMultiWindowModeChanged(boolean value, Configuration configuration) {
        super.onMultiWindowModeChanged(value, configuration);
        if (navigator != null && !navigator.isDestroyed()) navigator.onMultiWindowModeChanged(value, configuration);
    }
    @android.annotation.TargetApi(26)
    @Override public void onPictureInPictureModeChanged(boolean value, Configuration configuration) {
        super.onPictureInPictureModeChanged(value, configuration);
        if (navigator != null && !navigator.isDestroyed()) navigator.onPictureInPictureModeChanged(value, configuration);
    }

}
