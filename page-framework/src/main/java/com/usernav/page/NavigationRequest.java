package com.usernav.page;

/** Read-only request snapshot for policy and diagnostic callbacks; arguments are shallow copied. */
public final class NavigationRequest {
    public enum Operation { START, FINISH }
    private final Operation operation;
    private final ViewPage source;
    private final Class<? extends ViewPage> target;
    private final PageBundle arguments;
    private final Integer requestCode;
    NavigationRequest(Operation operation, ViewPage source, Class<? extends ViewPage> target,
                      PageBundle arguments, Integer requestCode) {
        this.operation = operation;
        this.source = source;
        this.target = target;
        this.arguments = arguments == null ? new PageBundle() : PageNavigator.copy(arguments);
        this.requestCode = requestCode;
    }
    public Operation operation() { return operation; }
    public ViewPage source() { return source; }
    public Class<? extends ViewPage> target() { return target; }
    public PageBundle arguments() { return PageNavigator.copy(arguments); }
    public Integer requestCode() { return requestCode; }
}
