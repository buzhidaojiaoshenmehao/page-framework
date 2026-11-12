package com.usernav.page.internal;

import android.view.View;

public final class FinishPageRequest extends TransitionRequest {
    public final PageIdentity pageIdentity;
    public View view;

    public FinishPageRequest(PageIdentity pageIdentity, PageAnimationOptions animationOptions) {
        super(animationOptions);
        this.pageIdentity = pageIdentity;
    }
}
