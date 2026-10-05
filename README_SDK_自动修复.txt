如果 Android Studio 报错：
SDK location not found

不要手动修改代码。

1. 关闭正在运行的 Gradle Build。
2. 在项目根目录双击：
   双击我_自动修复SDK.bat
3. 脚本会自动寻找 Android SDK，并创建 local.properties。
4. 回 Android Studio：
   File -> Sync Project with Gradle Files

如果脚本提示找不到 SDK：
Android Studio -> Tools -> SDK Manager
把窗口顶部的 “Android SDK Location” 路径截图或复制给 ChatGPT。
