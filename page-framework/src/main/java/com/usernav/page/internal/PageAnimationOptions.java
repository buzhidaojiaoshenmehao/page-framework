package com.usernav.page.internal;

import com.usernav.page.engine.IPageAnimationListener;
import com.usernav.page.engine.IPageAnimationProvider;

public final class PageAnimationOptions {
    public final IPageAnimationListener listener;
    public final IPageAnimationProvider provider;

    public PageAnimationOptions(IPageAnimationListener listener, IPageAnimationProvider provider) {
        this.listener = listener;
        this.provider = provider;
    }

    public PageAnimationOptions(IPageAnimationProvider provider) {
        this(null, provider);
    }
}
