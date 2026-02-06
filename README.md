# BeatCalc

![App banner](media/banner.png)

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://developer.android.com/about/versions"><img alt="Android" src="https://img.shields.io/badge/Android-26%2B-brightgreen.svg?style=flat"/></a>
</p>

BeatCalc 是一个使用 **Jetpack Compose** 构建的计算器应用，支持 **计算历史**、**深色/浅色主题**，并提供更现代的界面与交互体验。

> 说明：本项目源自 SiliconeCalculator 的二次开发与重命名，用于发布与分发时请确保遵循开源许可及相关政策要求。

## 📷 预览
<p align="center">
  <img src="media/preview0.gif" alt="drawing" width="270" />
  <img src="media/preview1.png" alt="drawing" width="270" />
  <img src="media/preview2.png" alt="drawing" width="270" />
</p>

## ✨ 功能

- **基础运算**：加减乘除、百分比、正负号、小数等
- **计算历史**：本地保存表达式与结果，支持一键清空
- **主题**：深色/浅色主题切换
- **离线可用**：核心功能无需联网

## 🏛️ 架构

整体采用 Android 推荐的分层思路（UI + ViewModel + Data），并结合 Compose 的状态管理方式组织页面与交互。

## 🛠️ 技术栈

- **UI**
  - Jetpack Compose
  - Navigation Compose
  - ConstraintLayout (Compose)
- **依赖注入 / 数据**
  - Dagger Hilt
  - Room（本地数据库，用于历史记录）
- **其它**
  - Kotlin Coroutines
  - mXparser（表达式解析）
  - Baseline Profile / Benchmark（启动性能相关）

## 🚀 构建与运行

### 本地调试（Debug）

```bash
./gradlew :app:assembleDebug
```

### Release 签名构建（APK / AAB）

本项目 **不会**在仓库中提交 keystore 或密码。请使用以下任一方式提供签名信息：

- **方式 1：使用项目根目录的 `keystore.properties`（推荐）**
  - 复制 `keystore.properties.example` 为 `keystore.properties`
  - 填写你的真实参数（该文件已在 `.gitignore` 中忽略，不会提交）

```bash
cp keystore.properties.example keystore.properties
```

- **方式 2：使用环境变量**
  - `RELEASE_STORE_FILE_PATH`
  - `RELEASE_STORE_PASS`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASS`

然后执行：

```bash
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

产物路径：

- APK：`app/build/outputs/apk/release/app-release.apk`
- AAB：`app/build/outputs/bundle/release/app-release.aab`

## 🔒 隐私政策

- 中文（Markdown）：`PRIVACY_POLICY.md`
- 英文（Markdown）：`PRIVACY_POLICY_EN.md`
- 英文（HTML 页面）：`privacy-policy.html`

> 上架 Google Play 时，建议将 `privacy-policy.html` 部署到可公开访问的地址，并在 Play Console 的 Privacy Policy 字段填写该链接。

## 💯 MAD Score
![summary](media/summary.png)

## License
```
Copyright 2022 Erfan Sn
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
