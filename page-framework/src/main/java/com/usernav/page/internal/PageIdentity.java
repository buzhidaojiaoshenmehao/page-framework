package com.usernav.page.internal;

import android.view.View;
import com.usernav.page.engine.IPageController;
import java.lang.ref.WeakReference;

public final class PageIdentity {
    public WeakReference<IPageController> controllerReference;
    public WeakReference<View> viewReference;

    @Override
    public final String toString() {
        IPageController controller = controllerReference != null ? controllerReference.get() : null;
        View view = viewReference != null ? viewReference.get() : null;
        return "PageId {c=" + controller + " view=" + view + "}";
    }
}
