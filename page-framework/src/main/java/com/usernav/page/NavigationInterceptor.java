package com.usernav.page;

/** Return false to block a request. Callbacks must not initiate another navigation. */
public interface NavigationInterceptor { boolean allow(NavigationRequest request); }
