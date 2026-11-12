# Page Framework Demo

Page Framework 是一套面向 Android 原生 View 的页面栈。一个 Activity 提供页面容器，`ViewPage` 创建页面 View，`PageNavigator` 负责入栈、出栈、系统返回、生命周期和转场。业务页面通过 `PageBundle` 接收参数，也可以将选择结果交回打开它的页面。

适合在同一宿主内切换多张页面、又要保留当前界面或临时业务状态的场景，例如表单中的选择页、覆盖列表或地图的筛选面板，以及编辑和确认之间反复切换的多步流程。

## 在 Demo 里试用

| 场景 | 怎么操作 | 可以看到什么 |
| --- | --- | --- |
| 表单选择与结果回传 | 修改首页的草稿标题，点「选择分类」，选「开发」。 | 选择页显示从首页传入的当前分类。返回后分类更新，草稿标题仍保留。 |
| 透明面板 | 点「打开透明面板」，再按系统返回键或点「关闭面板」。 | 面板叠在首页上，下面的首页保持可见。关闭后回到原来的页面。 |
| 多步流程与页面复用 | 点「体验多步流程」，增加商品数量，进入确认页，再点「确认并复用编辑页」。 | 已有的编辑页保留修改，收到新的确认参数，确认页从栈中移除。也可点「返回编辑页」观察退场动画。 |

底部实时显示页面栈，顺序从栈底到栈顶。确认页使用 View 资源作用域计时，View 移除时自动取消任务。

## 运行

使用 JDK 17 和 Android SDK 34。用 Android Studio 打开仓库根目录并运行 `demo` 模块，或连接 Android 5.0（API 21）以上的模拟器或设备后运行：

```sh
./gradlew :demo:installDebug
```

Demo 使用本地示例数据，无需账号、API Key 或网络权限。首次构建需要联网下载 Gradle 依赖。

```sh
./gradlew :page-framework:testDebugUnitTest :page-framework:assembleRelease
./gradlew :demo:connectedDebugAndroidTest -Pandroid.experimental.androidTest.useUnifiedTestPlatform=false
```

## 接入自己的 Activity

在根工程的 `settings.gradle` 加入 `include ':page-framework'`，再在应用模块添加依赖：

```groovy
dependencies {
    implementation project(':page-framework')
}
```

1. 为页面栈准备一个专用 `FrameLayout`，用 `new PageNavigator(this, container)` 绑定到 Activity。
2. 新建继承 `ViewPage` 的公开页面类，保留公开的无参构造函数。在 `createView(inflater, container)` 返回未挂载的 View。XML 布局使用 `inflate(layoutId, container, false)`。
3. 用 `navigator.startPage(HomePage.class, args)` 打开首页。可用 `register(action, type)` 注册字符串路由。
4. 在对应的 Activity 回调中转发 `onStart`、`onResume`、`onPause`、`onStop`、`onDestroy`。系统返回先交给 `navigator.onBackPressed()`，返回 `false` 时由宿主处理。

[完整 Activity 宿主](demo/src/main/java/com/usernav/pagedemo/MainActivity.java) · [三个页面场景的代码](demo/src/main/java/com/usernav/pagedemo/DemoPages.java)

| 能力 | 入口 |
| --- | --- |
| 参数与结果 | 打开页调用 `startPageForResult(...)`。被打开的页调用 `setResult(...)` 和 `finish()`，退出后结果交给发起页的 `onResult(...)`。 |
| 透明层与复用 | 通过 `PagePolicy.transparent(...)` 或单个 `PageBundle` 启动标志配置。支持标准入栈、SingleTop、SingleTask 和 SingleInstance。复用时在 `onNewArguments(...)` 接收新参数。 |
| 动画与返回 | 用 `PageNavigator.useDefaultAnimation(args)` 启用默认转场，或配置自定义动画。页面也可先用 `addBackHandler(...)` 关闭自身的临时界面。 |
| 资源与观察 | 用 `viewResources()` 管理 View 相关任务，用 `resources()` 管理页面级资源。可注册 `PageObserver` 观察页面栈变化，或用拦截器控制跳转。 |

导航和页面状态查询在主线程执行。参数与页面栈保存在当前进程中。Activity 重建时，由应用恢复需要保留的业务数据并重新打开对应页面。

[来源及权利说明](NOTICE.md)
