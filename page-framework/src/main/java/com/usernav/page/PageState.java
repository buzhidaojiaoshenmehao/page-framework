package com.usernav.page;

/** Business-facing state of a page. Enter/exit transitions count as STARTED. */
public enum PageState { INITIALIZED, CREATED, STARTED, RESUMED, DESTROYED }
