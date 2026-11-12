package com.usernav.page.engine;

import com.usernav.page.internal.PageParameters;

public interface IPageStackActionListener {
    void onPageBack(IPageController departingPage, IPageController revealedPage,
            PageParameters parameters);
}
