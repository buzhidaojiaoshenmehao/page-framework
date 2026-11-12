package com.usernav.page.engine;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.usernav.page.internal.PageParameters;

public interface IPageController {
    void attach(IPageFramework framework, Context context, LayoutInflater layoutInflater, Object host);

    int onBackPressed();

    void onCreate(PageParameters parameters);

    View onCreateView(ViewGroup container);

    void onDestroy();

    void onNewParams(PageParameters parameters);

    void onPageResult(int requestCode, int resultCode, PageParameters parameters);

    void onPageSwitch(boolean causedByPageSwitch);

    void onPause();

    void onRemoveView();

    void onResume();

    void onStart();

    void onStop();

    void onTransparentDismissed();
}
