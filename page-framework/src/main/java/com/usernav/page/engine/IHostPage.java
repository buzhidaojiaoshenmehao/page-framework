package com.usernav.page.engine;

import android.view.View;
import android.view.ViewGroup;
import com.usernav.page.internal.PageParameters;
import com.usernav.page.internal.StartPageRequest;

interface IHostPage {
    View getView();

    int handleBackPressed();

    boolean handleCreate(StartPageRequest request);

    View handleCreateView(ViewGroup container);

    void handleDestroy();

    void handleNewIntent();

    void handlePause(boolean causedByPageSwitch);

    void handleResult(int requestCode, int resultCode, PageParameters parameters);

    void handleResume(boolean causedByPageSwitch);

    void handleStart(boolean attachView, boolean resumePage, boolean causedByPageSwitch);

    void handleStop(boolean causedByPageSwitch);

    boolean isTransparent();
}
