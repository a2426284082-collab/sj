# 此刻 V1

交付包含手机端 PWA 和一个 Android WebView 外壳。Android APK 将任务数据保存在 SQLite（单行 JSON 快照），PWA 预览版使用 IndexedDB；Android API Key 保存到 Android Keystore 加密的本地存储。AI 请求才需要网络。

## 云端构建 APK

仓库根目录应为本 `outputs/` 文件夹，其中 `.github/workflows/build-apk.yml` 会在推送到 `main` 或手动触发时构建 debug APK，并上传名为 `cike-debug-apk` 的 GitHub Actions artifact。构建产物路径为：

`android/app/build/outputs/apk/debug/app-debug.apk`

debug APK 可以安装到 Android 手机上测试。签名 release APK 通过 GitHub Actions 下载名为 `cike-release-apk` 的 artifact。Release 构建需要在 GitHub 仓库设置 Actions secrets：`CIKE_SIGNING_KEY_BASE64` 和 `CIKE_SIGNING_PASSWORD`。签名密钥不会提交到仓库；同一个应用后续更新必须继续使用同一把密钥，请妥善备份。

## 本机预览 PWA

需要 Python 3：

```powershell
cd outputs
python -m http.server 8765
```

浏览器打开 `http://localhost:8765`。在 Android 手机上安装独立 PWA，需要将静态文件部署到 HTTPS 站点；原生 APK 不需要这一步。

## 已实现

- 今天首页、快速记录、日期识别、任务编辑、完成/开始/暂停/延期/删除、项目、想法池、日历和复盘。
- 任务类型、优先级、预计时间、完成标准、未完成原因、提醒时间；Android 任务提醒使用系统 AlarmManager/通知通道。
- DeepSeek 当前任务推荐、今日分析、任务拆解、单任务检查、复盘及自由提问；建议由用户确认，不自动改任务。
- IndexedDB 本地保存，JSON 备份导入/导出和 CSV 导出；Android API Key 使用 Android Keystore 加密，备份不包含 API Key。
- Android Studio/Gradle 工程和 GitHub Actions 云端 debug APK 工作流。

## 已知限制

- Android 使用本地 SQLite 快照表，PWA 预览版使用 IndexedDB；首次安装与 WebView 升级的持久化行为需要真机验收。
- 日历为月视图；今日复盘、延期与 AI 建议可用，但完整的复盘历史筛选、AI 自动化权限开关、前置任务编辑、实际耗时计时和所有设置项仍可继续补齐。
- 云端 debug APK 构建已在 GitHub Actions 成功运行；打开仓库的 Actions 页面下载 `cike-debug-apk` artifact。
- 发布包通过 GitHub Actions 签名并上传为 release artifact；应用商店上架还需自行完成商店资料和隐私/合规信息。
