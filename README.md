# puzzle-pt

## 打包 APK

确保已安装 JDK 和 Android SDK，并在项目根目录执行：

```bash
./build-apk.sh
```

默认构建 release 版本，APK 输出到 `app/build/outputs/apk/release/`。当前项目未配置签名时，生成的是 `app-release-unsigned.apk`，安装前需要签名。

构建可直接安装的 debug 版本：

```bash
./build-apk.sh debug
```

debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。
