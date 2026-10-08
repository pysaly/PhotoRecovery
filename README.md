# 照片恢复（PhotoRecovery）

一款 Android 照片/视频恢复应用，用于找回误删的相册媒体文件。

## 功能

- **快速恢复（无需 root）**：读取系统相册回收站（MediaStore `IS_TRASHED`），直接找回约 30 天内误删并进入回收站的照片和视频。
- **深度扫描（需 root）**：以 root 身份读取整个外部存储（含被沙盒隔离的目录），枚举全部可访问的媒体文件，一键迁移/备份到「下载/PhotoRecovery」目录。
- **结果页**：缩略图网格、照片/视频筛选、全选、单选、批量恢复。

## 环境要求

| 项 | 要求 |
|---|---|
| Android Studio | 最新稳定版（内置 Gradle） |
| Android SDK | compileSdk 34 |
| JDK | 17 |
| 构建系统 | Gradle + AGP 8.2.2 + Kotlin 1.9.22（工程自带，打开即用） |

## 构建与安装

1. 用 **Android Studio** 打开本工程根目录（`PhotoRecovery/`）。
2. 等待 Gradle 同步完成（首次会下载依赖）。
3. 连接手机（开启 USB 调试）或在模拟器中运行：点击 **Run ▶**。
4. 也可通过菜单 **Build → Build APK(s)** 生成安装包，`app/build/outputs/apk/` 下的 APK 可安装到手机。

## 使用说明

1. **首次启动**：授予「照片和视频」读取权限（Android 13+ 为 `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`）。
2. **快速恢复**：直接点「快速恢复」，扫描系统回收站，勾选项目后点「恢复所选」。
   - 恢复结果会回到系统图库/回收站原位置。
3. **深度扫描**：需要设备已 root。未 root 时会提示。
   - 扫描全部外部存储媒体文件，恢复的项目会复制到「下载/PhotoRecovery」。

## 恢复原理与诚实说明

- **回收站恢复（推荐）**：这是 Android 11+ 上最可靠的真正恢复途径。误删的照片默认进入系统回收站（保留约 30 天），本应用通过 `is_trashed = 1` 找到它们，再用 `is_trashed = 0` 移回图库。
- **深度扫描**：现代 Android 文件系统在删除文件后，会释放 inode 并可能被 TRIM 清空底层数据块，因此「已删除文件」的物理恢复成功率低且随设备差异很大。本应用的深度扫描以 root 权限全盘枚举**仍可访问**的媒体文件用于迁移/备份，这是 root 模式下稳定、真实可运行的能力。
- 请勿对重要数据依赖此工具，关键照片建议开启云端备份。

## 目录结构

```
PhotoRecovery/
├── app/src/main/java/com/example/photorecovery/
│   ├── MainActivity.kt              # 单 Activity + Fragment 导航
│   ├── data/
│   │   ├── MediaItem.kt             # 数据模型
│   │   ├── MediaScanner.kt          # 回收站扫描（快速恢复）
│   │   ├── DeepRootScanner.kt       # root 全盘深度扫描
│   │   └── RecoveryRepository.kt    # 恢复逻辑
│   ├── ui/
│   │   ├── MediaViewModel.kt        # 共享状态
│   │   ├── HomeFragment.kt          # 主页：选模式 + 权限
│   │   ├── ResultsFragment.kt       # 结果页：筛选/全选/恢复
│   │   └── MediaAdapter.kt          # 缩略图网格适配器
│   └── util/
│       ├── RootUtils.kt             # root 检测 + su 执行
│       ├── PermissionUtils.kt       # 权限处理
│       └── ThumbnailLoader.kt       # 缩略图加载
└── app/src/main/res/                # 布局 / 资源
```
