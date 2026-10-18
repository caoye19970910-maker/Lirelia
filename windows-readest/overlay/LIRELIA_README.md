# Lirelia × Readest · Windows 整合开发版 0.1

基于完整 Readest v0.12.16，基线提交 `3cc39ad7a558e6612e1a8135058fbd6f3c90b3e3`。
保留原版书架、阅读器、格式支持、批注、词典、翻译、朗读和同步代码；新增 Lirelia 学习模块。

这是源码开发版，不是已经通过 Windows 实机验收的安装包。当前交付未包含 Windows EXE。

## 已接入的功能

- 正文划词工具栏增加 **Lirelia** 入口；原词典、翻译等按钮保留。
- 桌面右下角 **Lirelia** 按钮打开学习面板，包含查词翻译、生词本、今日复习、设置。
- 单词使用原 Lecture Mini 的 Gemini 客户端；句子使用 DeepL；详细解析按点击调用 Gemini。
- 单词沿用当前 V2.9.4 的 gTTS，句子使用 Deepgram；支持缓存、重听、停止和语速调整。
- 明确识别出原形时显示原形发音按钮；不猜测未识别的原形。
- 收藏保存完整 Gemini 释义、书名、正文定位及上下文；单词自动进入复习计划。
- 复习直接显示保存的完整释义，保留 FSRS 四档评分；不会再生成“简义”。
- 从书架首次打开书籍时选择法语/英语，按书保存；书架右键 **Lirelia · 阅读语言** 可修改。
- 设置内支持导入 Lecture Mini 的 `lecture.sqlite3`，保留收藏、评分日志、调度状态和到期时间。
- 新数据库位于 `%APPDATA%\LireliaDesktop\lirelia.sqlite3`；旧数据库只读导入。
- Gemini、DeepL 的 10 个密钥槽位和 Deepgram 凭据沿用旧系统凭据标识；可直接读取同一 Windows 账户已有的密钥。
- 新功能无需登录 Readest 账户；原版涉及账户或订阅的功能仍沿用其原有逻辑。

## 构建 Windows 安装包

推荐 Windows 10/11 x64，至少 16 GB 内存。需要网络下载构建依赖。
建议解压到较短的目录，例如 `C:\LireliaReadest`。

先安装：

1. Node.js 24 x64、pnpm 11。
2. Python 3.12 x64（命令行 `python` 能调用）。
3. Rust stable / MSVC 工具链。
4. Visual Studio Build Tools 的“使用 C++ 的桌面开发”工作负载及 Windows SDK。
5. Microsoft Edge WebView2 Runtime。
6. 如果从 Git 克隆，安装 Git；本交付源码 ZIP 已展开应用所需子模块。

然后双击根目录 **构建Windows安装包.bat**。

脚本会建立独立 Python 虚拟环境、打包学习后台、安装前端依赖、执行学习模块测试和全项目类型/静态检查，再调用原版 Tauri 构建。
首次构建可能需要较长时间。构建失败会停止并显示错误，不会把源码当成已生成的安装包。

成功后的安装程序位于：

```text
target/x86_64-pc-windows-msvc/release/bundle/nsis/
```

开发调试：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\lirelia\build-windows.ps1 -Dev
```

也附有手动触发的 GitHub Actions 工作流：`Build Lirelia Windows`。
将完整源码保存在自己的 GitHub 仓库后，可在 Actions 中运行并下载构建产物。
本次未向任何远程仓库上传代码，也未运行远程工作流。

## 使用顺序

1. 打开阅读器，导入 EPUB 或 TXT，选择书籍的法语/英语。
2. 在正文中选中单词或句子，点击工具栏的 Lirelia。
3. 若提示尚未配置密钥，打开学习面板“设置”，选择对应槽位保存密钥。
4. 查询后点击“收藏”；单词自动入队，句子单独收藏。
5. 进入“今日复习”，翻卡后按忘记、困难、记得、轻松评分。
6. 有来源定位的收藏可点击“返回原文”；旧版收藏没有来源时不会补造。

导入旧数据前先退出旧版 Lecture Mini 并备份。设置 → 导入 Lecture Mini → 选择：

```text
%APPDATA%\LectureMini\lecture.sqlite3
```

重复导入不会重复添加同一评分日志，也不会覆盖新版本已有卡片进度。
缓存音频不随旧数据库迁移，后续可按需重新生成。

## 实现边界与待验收项

- Readest 原应用是完整保留的；学习面板为额外接入的模块，底层书籍渲染未替换。
- Python 核心通过 Tauri sidecar 的标准输入/输出与界面通信，没有本地 HTTP 端口。
- 本次没有实机验证 Windows 安装、文件关联、新窗口行为、音频输出、代理和真实 API 请求。
- 书籍语言提示接在书架的打开流程；外部“打开方式”等原版入口保持原行为。
- 新增 Lirelia 数据当前保存在本机，不会自动同步至现有 Lirelia Android。
- PDF 保留原版阅读能力；精细文本选择与定位仍需真实 PDF 样本验收。
- 自动发音受系统音频及网络影响；关闭面板、换查询或切换学习页面时会停止和丢弃过期播放。
- 当前学习侧栏覆盖在原阅读界面右侧，可调宽度并记忆；不会重写原版页面布局。
- 真实 API 配额、模型可用性和语音服务可达性需使用用户自己的账户验证。

## 来源与许可证

Readest 上游：https://github.com/readest/readest ，原 AGPL-3.0 许可证及各子模块许可证保留。
`lirelia/core/` 内查询、密钥和复习基础模块来自本项目已交付的 Lecture Mini V2.9.4；新接入层为 `bridge.py`。
FSRS 来自 py-fsrs，使用其原有许可证。构建后台时包含包元数据。
