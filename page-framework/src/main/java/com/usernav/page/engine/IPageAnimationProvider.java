package com.usernav.page.engine;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

public interface IPageAnimationProvider {
    Animation enterForAdd(ViewGroup container, View pageView);

    Animation enterForRemove(ViewGroup container, View pageView);

    Animation leaveForAdd(ViewGroup container, View pageView);

    Animation leaveForRemove(ViewGroup container, View pageView);
}
