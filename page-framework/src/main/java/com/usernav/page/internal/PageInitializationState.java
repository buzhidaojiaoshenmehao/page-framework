package com.usernav.page.internal;

/** Preserves the observed initialization marker without the original business service. */
public final class PageInitializationState {
    public static int initializationToken = 0;

    private PageInitializationState() {}
}
