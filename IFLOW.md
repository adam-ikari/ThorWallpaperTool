# 索尔掌机壁纸工具 (ThorWallpaperTool) - 项目说明

## 项目概述

这是一个 Android 应用，旨在帮助用户将图片裁切到适合索尔双屏掌机的分辨率。该应用会将用户选择的原始图片处理成两个独立的壁纸：一个用于上屏（主屏），一个用于下屏（副屏），以适配索尔掌机的双屏特性。

- **上屏（主屏）分辨率**: 1920×1080
- **下屏（副屏）分辨率**: 1240×1080

应用采用 Kotlin 编写，使用 Android Studio 进行开发，基于 AndroidX 库构建。

## 项目结构

```
ThorWallpaperTool/
├── app/
│   ├── src/main/java/com/adam/thorwallpapertool/
│   │   ├── MainActivity.kt          # 主界面逻辑
│   │   └── ImageProcessor.kt        # 图片处理核心逻辑
│   ├── src/main/res/layout/
│   │   └── activity_main.xml        # 主界面布局
│   ├── build.gradle.kts             # 模块级构建配置
├── build.gradle.kts                 # 项目级构建配置
├── gradle.properties                # Gradle 项目属性
├── settings.gradle.kts              # 项目设置
└── README.md                        # 项目说明文档
```

## 核心功能

1. **图片选择**：用户可以从设备相册中选择一张图片作为原始壁纸
2. **图片预览**：显示选中的图片及其尺寸信息
3. **间隔设置**：用户可以自定义上下屏之间的间隔像素数
4. **壁纸生成**：根据索尔掌机的屏幕参数处理图片，生成适配上下屏的两个壁纸（以上下拼接方式：上屏在上，下屏在下）
5. **图片保存**：将处理后的两个壁纸保存到设备相册

## 技术实现

### 图片处理逻辑 (ImageProcessor.kt)

- **缩放计算**：根据原始图片尺寸和上下拼接的总尺寸（含间隔）计算合适的缩放比例
- **内容保持**：缩放过程中保持原始内容的宽高比例不变
- **上下拼接**：以上下方式拼接（上屏在上，下屏在下）
- **组合画布**：创建上下拼接的组合画布，将缩放后的图片居中放置
- **精确裁切**：从组合画布中精确裁切上下屏壁纸
- **PPI 优化**：考虑上下屏 PPI 差异，确保内容在不同屏幕上视觉大小一致
- **间隔支持**：支持自定义上屏与下屏壁纸之间的间隔

### 用户界面 (MainActivity.kt)

- 使用 ConstraintLayout 布局实现响应式界面
- 集成 Android 系统图片选择器
- 提供间隔输入框，支持用户自定义屏幕间隔
- 在后台线程处理图片以避免 UI 阻塞
- 提供进度指示器和操作反馈

## 构建和运行

### 环境要求
- Android Studio
- Android SDK (compileSdk 36, minSdk 33, targetSdk 36)
- Java 11
- Kotlin (版本根据 gradle/libs.versions.toml 中的配置)

### 构建命令
```bash
# 使用 Gradle Wrapper 构建项目
./gradlew build

# 构建调试 APK
./gradlew assembleDebug

# 安装调试 APK 到连接的设备
./gradlew installDebug
```

### 依赖库
- androidx.core.ktx
- androidx.appcompat
- material
- androidx.activity
- androidx.constraintlayout
- JUnit (测试)
- AndroidX JUnit (Android 测试)
- AndroidX Espresso (UI 测试)

## 开发约定

- 代码采用 Kotlin 官方风格 (kotlin.code.style=official)
- 使用 AndroidX 库 (android.useAndroidX=true)
- 项目使用非传递 R 类以减小资源类大小 (android.nonTransitiveRClass=true)

## 使用方法

1. 启动应用后点击"选择图片"按钮
2. 从相册中选择一张图片
3. 点击"生成壁纸"按钮开始处理
4. 处理完成后，生成的上下屏壁纸会自动保存到相册的 ThorWallpaperTool 文件夹中