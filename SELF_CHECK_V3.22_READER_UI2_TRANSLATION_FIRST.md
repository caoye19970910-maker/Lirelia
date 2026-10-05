# V3.22 self-check

- [x] V3.21 源码为基础，未更改 applicationId。
- [x] versionCode 112 -> 113；versionName -> 3.22-reader-ui2-translation-first。
- [x] 普通点词从完整 DictionarySheet 改为 QuickTranslationDock。
- [x] 完整词典保留，并改为二级显式入口。
- [x] 快译 Dock 同时提供：离线词义、离线句子辅助、自然整句翻译、单词发音、阅读整句。
- [x] 翻译请求仍复用 SentenceTranslationManager 缓存。
- [x] Reader chrome 为 overlay，不参与正文/分页布局。
- [x] 顶部和底部 chrome 使用统一短动画。
- [x] EPUB/TXT 长按 Popup 增加 window-space anchor，尽量靠近被选词。
- [x] 颜色点击立即写入标注的 V3.21 修复保留。
- [x] Lirelia 名字与图标保留。
- [x] Kotlin 文件粗略分隔符检查与 kotlinc parser 检查未发现新增 “expecting …” 语法错误。
- [ ] 当前容器缺少 gradle-wrapper.jar 且无网络，无法执行完整 Android Gradle assemble；需在 Android Studio/本机 Gradle 环境最终构建验证。
