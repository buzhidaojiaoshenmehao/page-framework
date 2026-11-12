# The restored engine creates controllers through Class.newInstance().
-keep public class * extends com.usernav.page.ViewPage { public <init>(); }
-keep public class * implements com.usernav.page.engine.IPageController { public <init>(); }
