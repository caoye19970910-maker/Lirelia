# Self-check — V3.18 Reading Profile & Voice Route

- [x] versionCode 109 / versionName `3.18-reading-profile-voice-route`
- [x] 阅读控制中心加入：中国大陆 / Google 可用 / 离线
- [x] 模式切换为手动选择，不做地理位置或网络探测
- [x] 缓存优先；备用源只在失败时触发
- [x] 中国大陆模式拒绝主动选择 Google Android TTS
- [x] 离线模式拒绝需要网络的 Android voice
- [x] EPUB/TXT 实际段落暴露记录
- [x] PDF 文字层页面暴露记录
- [x] EPUB/TXT/PDF 点词记录
- [x] 30 天查词密度 / 不同查词 / 重复查询 / 顽固词
- [x] 最近 7 天 vs 前 7 天趋势
- [x] 每本书近 30 天查词密度
- [x] 阴阳性 / à-de / 变位 / 整句输出 / 个人生词训练画像
- [x] `reading_profile` 纳入数据备份
- [x] 词典完整释义保持不变
- [x] 轻量词法括号检查通过（修改的 4 个 Kotlin 文件括号/字符串状态正常）
- [ ] 完整 Gradle 编译：当前提供的源码包缺少 `gradle-wrapper.jar`，执行环境也无法联网补取 Gradle 8.10.2，因此本环境无法完成真正 Android 编译；项目原有 Windows `prepare_gradle_wrapper.ps1` 保留，可在联网的开发机自动补齐。
