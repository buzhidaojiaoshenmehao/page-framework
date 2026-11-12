package com.usernav.page;

import com.usernav.page.engine.IPageAnimationProvider;

/** Defaults for a page class. Explicit bundle flags/animation take precedence. */
public final class PagePolicy {
    final int flags;
    final IPageAnimationProvider animation;
    public PagePolicy(int flags, IPageAnimationProvider animation) {
        PageNavigator.coreFlags(flags);
        this.flags = flags;
        this.animation = animation;
    }
    public static PagePolicy standard() { return new PagePolicy(PageBundle.FLAG_LAUNCH_MODE_STANDARD, null); }
    public static PagePolicy transparent(IPageAnimationProvider animation) {
        return new PagePolicy(PageBundle.FLAG_LAUNCH_MODE_TRANSPARENT, animation);
    }
}
