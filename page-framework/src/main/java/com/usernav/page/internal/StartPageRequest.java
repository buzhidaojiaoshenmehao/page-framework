package com.usernav.page.internal;

import android.view.View;
import com.usernav.page.engine.IPageController;
import com.usernav.page.engine.IPageFramework;
import com.usernav.page.engine.Pages;

public final class StartPageRequest extends TransitionRequest {
    public final IPageFramework framework;
    public final Class<? extends IPageController> controllerClass;
    public final int launchFlags;
    public final int pageCount;
    public final Class<?> identityClass;
    public final PageParameters parameters;
    public final PageResultRequest resultRequest;
    public final String uniqueId;
    public View view;
    public final int maxCount;
    public final String maxCountId;

    public StartPageRequest(Pages.FrameworkFacade framework,
            Class<? extends IPageController> controllerClass, int launchFlags,
            PageParameters parameters, PageResultRequest resultRequest,
            PageAnimationOptions animationOptions) {
        super(animationOptions);
        this.framework = framework;
        this.controllerClass = controllerClass;
        this.launchFlags = launchFlags;
        this.identityClass = parameters != null ? parameters.identityClass : null;
        this.parameters = parameters;
        this.resultRequest = resultRequest;
        this.uniqueId = parameters != null ? parameters.uniqueId : null;
        Integer requestedPageCount = parameters != null && parameters.extras != null
                ? (Integer) parameters.extras.get("PAGE_COUNT") : null;
        this.pageCount = requestedPageCount != null ? requestedPageCount : 0;
        this.maxCount = parameters != null ? parameters.maxCount : 0;
        this.maxCountId = parameters != null ? parameters.maxCountId : null;
    }

    public final boolean hasFlag(int flag) {
        return (launchFlags & flag) == flag;
    }
}
