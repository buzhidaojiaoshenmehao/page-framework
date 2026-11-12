package com.usernav.page.internal;

import com.usernav.page.engine.IPageStackActionListener;
import java.util.HashMap;

public final class PageParameters {
    public int requestCode;
    public HashMap<String, Object> extras;
    public Class<?> identityClass;
    public String uniqueId;
    public int maxCount;
    public String maxCountId;
    public IPageStackActionListener stackActionListener;
}
