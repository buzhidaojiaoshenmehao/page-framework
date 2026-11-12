package com.usernav.page.internal;

import android.util.Log;

/** Host bridge for the telemetry service omitted from the extracted library. */
public final class PageTelemetry {
    private PageTelemetry() {}

    public static void recordEvent(int level, int type, String event,
            String primaryDetail, String secondaryDetail, int code) {
        // The standalone framework has no telemetry backend.
    }

    public static void warn(String tag, String message) {
        Log.w(tag, message);
    }
}
