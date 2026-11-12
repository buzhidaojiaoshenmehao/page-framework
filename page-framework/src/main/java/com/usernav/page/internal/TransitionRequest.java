package com.usernav.page.internal;

import com.usernav.page.engine.IPageAnimationListener;
import com.usernav.page.engine.IPageAnimationProvider;
import com.usernav.page.engine.PageStack;

public class TransitionRequest {
    public final boolean animationRequested;
    public final IPageAnimationListener animationListener;
    public IPageAnimationProvider animationProvider;
    public PageStack.PageRecord pageRecord;

    public TransitionRequest(PageAnimationOptions animationOptions) {
        this.animationRequested = animationOptions != null;
        this.animationListener = animationOptions != null ? animationOptions.listener : null;
        this.animationProvider = animationOptions != null ? animationOptions.provider : null;
    }
}
