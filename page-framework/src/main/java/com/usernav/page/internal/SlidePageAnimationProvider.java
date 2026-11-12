package com.usernav.page.internal;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import com.usernav.page.engine.IPageAnimationProvider;

public class SlidePageAnimationProvider implements IPageAnimationProvider {
    @Override
    public Animation enterForAdd(ViewGroup container, View pageView) {
        TranslateAnimation animation = new TranslateAnimation(
                container.getWidth(), 0.0f, 0.0f, 0.0f);
        animation.setDuration(300L);
        return animation;
    }

    @Override
    public Animation enterForRemove(ViewGroup container, View pageView) {
        TranslateAnimation animation = new TranslateAnimation(
                -container.getWidth(), 0.0f, 0.0f, 0.0f);
        animation.setDuration(300L);
        return animation;
    }

    @Override
    public Animation leaveForAdd(ViewGroup container, View pageView) {
        TranslateAnimation animation = new TranslateAnimation(
                0.0f, -container.getWidth(), 0.0f, 0.0f);
        animation.setDuration(300L);
        return animation;
    }

    @Override
    public Animation leaveForRemove(ViewGroup container, View pageView) {
        TranslateAnimation animation = new TranslateAnimation(
                0.0f, container.getWidth(), 0.0f, 0.0f);
        animation.setDuration(300L);
        return animation;
    }
}
