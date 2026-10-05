# SELF CHECK — V4.8_SCROLL_RULER_ANNOTATION_FIXES

## 对照用户反馈

- [x] 滚屏模式“下一页”不再只依赖 Readium 默认翻页；当前资源内滚动，到边缘显式进入相邻 spine 资源。
- [x] 章节末尾继续上滑可进入下一章；章节顶部继续下滑可返回上一章。
- [x] 阅读辅助支持直接拖动位置。
- [x] 阅读辅助支持调整高度，并持久保存位置/尺寸。
- [x] EPUB 标注颜色增强。
- [x] 选词工具条提供“取消标注”。
- [x] 选词工具条提供“＋生词”。
- [x] 单词颜色标注会自动加入生词本。
- [x] 生词在当前 EPUB 资源中后续出现时重复着色，章节切换会重刷。
- [x] “清除该词”会同时移除词汇状态和单词型标注。
- [x] 查词卡也提供已标词的“清除”。
- [x] 灰色 marker 可持久化。

## 静态检查

- Kotlin parser-level scan: PASS（Android/Compose/Readium classpath 不在当前 kotlinc 环境，因此存在预期 unresolved-reference 报错；未出现语法类报错）。
- INSTALL_TAP_SCRIPT node --check: PASS
- applyVocabularyWordHighlights JS node --check: PASS
- SQLite integrity: PASS / PASS

## 未完成项

- Android Gradle 完整编译：BLOCKED BY ENVIRONMENT
- 原因：当前沙箱 DNS/网络无法访问 `services.gradle.org`，Gradle 8.10.2 wrapper 无法下载。
- 本机验收建议：Android Studio 打开 V4.8 -> Gradle Sync -> Run -> EPUB 实机验证滚屏跨章、阅读尺拖动/缩放、标注/取消/重复词/清除流程。
