# 卡包 · Android

一个离线卡面收藏工具。点击「+」从相册选图，填写可选备注并保存。首页会叠放卡片；点可见部分即可看完整卡面。打开详情，可以编辑、换图或删除。

测试版支持 Android 8.0 及以上（API 26+）。数据保存在当前设备的应用私有目录，卸载或清除应用数据会移除收藏。App 无需联网、登录或广泛相册权限。

## 构建

使用 JDK 17 和 Android SDK Platform 35、Build Tools 35.0.0。可直接在 Android Studio 打开此目录，或设置 `ANDROID_HOME` 后运行：

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。这是用于安装体验的调试签名包。主要手工验收流程见 [QA.md](QA.md)。
