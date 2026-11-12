package com.usernav.page.engine;

import android.view.View;
import com.usernav.page.internal.PageAnimationOptions;
import com.usernav.page.internal.PageIdentity;
import com.usernav.page.internal.PageParameters;
import com.usernav.page.internal.PageResultRequest;
import java.util.ArrayList;

public interface IPageFramework {
    void finishPage(PageIdentity pageIdentity, PageAnimationOptions animationOptions);

    ArrayList<Class<?>> getInternalClassStacks();

    ArrayList<Class<?>> getInternalIdentStacks();

    ArrayList<IPageController> getInternalPageStacks();

    View getInternalPageView(int offsetFromTop);

    Class<?> getInternalTopClass();

    Class<?> getInternalTopIdent();

    IPageController getInternalTopPage();

    ArrayList<IPageController> getInternalTopVisiblePages();

    IPageController getOpaquePage(int offsetFromTop);

    ArrayList<PageParameters> getPageParams();

    void setPageResult(PageIdentity pageIdentity, int resultCode, PageParameters parameters);

    void startPage(Class<? extends IPageController> controllerClass, int launchFlags,
            PageParameters parameters, PageResultRequest resultRequest,
            PageAnimationOptions animationOptions);
}
