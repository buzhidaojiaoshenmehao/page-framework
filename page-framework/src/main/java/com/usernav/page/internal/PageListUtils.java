package com.usernav.page.internal;

import java.util.ArrayList;

public final class PageListUtils {
    private PageListUtils() {}

    /** Uses a one-based offset: 1 selects the last element. */
    public static <T> T getFromEnd(int offsetFromEnd, ArrayList<T> pages) {
        return pages.get(pages.size() - offsetFromEnd);
    }
}
