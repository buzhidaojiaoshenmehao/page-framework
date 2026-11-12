package com.usernav.page.internal;

public final class PageResultRequest {
    public final int requestCode;
    public final PageIdentity callerIdentity;

    public PageResultRequest(int requestCode, PageIdentity callerIdentity) {
        this.requestCode = requestCode;
        this.callerIdentity = callerIdentity;
    }
}
