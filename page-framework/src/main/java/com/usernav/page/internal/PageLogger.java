package com.usernav.page.internal;

import android.util.Log;

/** Android logging bridge replacing the original business logging service. */
public final class PageLogger {
    private PageLogger() {}

    public static void error(String tag, String message) {
        Log.e(tag, message);
    }

    public static void errorWithCause(String message, Throwable error) {
        Log.e("PageFramework", message, error);
    }

    public static void debug(String tag, String message) {
        Log.d(tag, message);
    }

    public static void warn(String tag, String message) {
        Log.w(tag, message);
    }
}
