package com.usernav.page.engine;

import com.usernav.page.internal.PageIdentity;
import com.usernav.page.internal.PageParameters;

/** Assigns a result to the identified page when the stack action is dispatched. */
public final class SetPageResultAction implements PageStack.StackAction {
    public final PageIdentity pageIdentity;
    public final int resultCode;
    public final PageParameters resultData;
    public final PageStack pageStack;

    public SetPageResultAction(PageStack pageStack, PageIdentity pageIdentity,
            int resultCode, PageParameters resultData) {
        this.pageStack = pageStack;
        this.pageIdentity = pageIdentity;
        this.resultCode = resultCode;
        this.resultData = resultData;
    }

    @Override
    public final boolean handleAction() {
        pageStack.setPageResult(pageIdentity, resultCode, resultData);
        return true;
    }
}
