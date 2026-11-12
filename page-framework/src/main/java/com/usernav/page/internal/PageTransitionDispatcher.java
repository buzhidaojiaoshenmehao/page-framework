package com.usernav.page.internal;

import android.os.Handler;
import android.view.ViewGroup;
import android.view.animation.Animation;
import com.usernav.page.engine.IPageAnimationListener;

public final class PageTransitionDispatcher {
    public final ViewGroup container;
    public final Handler handler = new Handler();

    public static final class AnimationCallback implements Animation.AnimationListener {
        public final IPageAnimationListener internalListener;
        public final IPageAnimationListener externalListener;

        public AnimationCallback(CompletionCallback completionCallback,
                IPageAnimationListener externalListener) {
            this.internalListener = completionCallback;
            this.externalListener = externalListener;
        }

        @Override
        public final void onAnimationEnd(Animation animation) {
            try {
                if (externalListener != null) {
                    externalListener.onFinished();
                }
            } catch (Throwable error) {
                error.printStackTrace();
            }
            if (internalListener != null) {
                internalListener.onFinished();
            }
        }

        @Override
        public final void onAnimationRepeat(Animation animation) {
        }

        @Override
        public final void onAnimationStart(Animation animation) {
            if (internalListener != null) {
                internalListener.onStarted();
            }
            try {
                if (externalListener != null) {
                    externalListener.onStarted();
                }
            } catch (Throwable error) {
                error.printStackTrace();
            }
        }
    }

    public static final class CompletionCallback implements IPageAnimationListener, Runnable {
        public final Runnable completion;
        public final PageTransitionDispatcher dispatcher;

        public CompletionCallback(PageTransitionDispatcher dispatcher, Runnable completion) {
            this.dispatcher = dispatcher;
            this.completion = completion;
        }

        @Override
        public final void onFinished() {
            dispatcher.handler.post(this);
        }

        @Override
        public final void onStarted() {
        }

        @Override
        public final void run() {
            if (completion != null) {
                completion.run();
            }
        }
    }

    public PageTransitionDispatcher(ViewGroup container) {
        this.container = container;
    }
}
