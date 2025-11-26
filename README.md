# 索尔掌机壁纸工具 (ThorWallpaperTool)

本应用专为索尔双屏掌机设计，能够智能地将壁纸裁切至合适的分辨率和比例，分别适配上下双屏显示需求。

## 索尔掌机屏幕参数

| 屏幕位置     | 分辨率    | 尺寸（英寸） | PPI（像素密度） |
| ------------ | --------- | ------------ | --------------- |
| 上屏（主屏） | 1920×1080 | 6            | 约 367          |
| 下屏（副屏） | 1240×1080 | 3.92         | 约 297          |

## 应用功能

- **图片选择**：用户可以从设备相册中选择一张图片作为原始壁纸
- **图片预览**：显示选中的图片及其尺寸信息
- **间隔设置**：用户可以自定义上下屏之间的间隔像素数
- **壁纸生成**：根据索尔掌机的屏幕参数处理图片，生成适配上下屏的两个壁纸
- **图片保存**：将处理后的两个壁纸保存到设备相册的 ThorWallpaperTool 文件夹中

## 壁纸处理原则

- **保持比例**：在处理过程中保持原始图片的内容比例不变，确保视觉效果自然协调
- **上下拼接**：壁纸以上下拼接方式生成（上屏在上，下屏在下），考虑 PPI 差异
- **视野完整**：生成的上下屏壁纸可完美拼接，还原原图的完整视野
- **PPI 优化**：考虑上下屏 PPI（像素密度）差异，确保在不同屏幕上显示效果一致
- **灵活调节**：支持自定义上屏与下屏壁纸之间的间隔，满足个性化需求

## 技术实现

### 开发环境

- **语言**：Kotlin
- **最低 SDK**：33 (Android 13)
- **目标 SDK**：36 (Android 15)
- **编译 SDK**：36
- **Java 版本**：11

### 核心依赖

- androidx.core.ktx
- androidx.appcompat
- material
- androidx.activity
- androidx.constraintlayout

### 项目结构

```
ThorWallpaperTool/
├── app/
│   ├── src/main/java/com/adam/thorwallpapertool/
│   │   ├── MainActivity.kt          # 主界面逻辑
│   │   ├── ImageProcessor.kt        # 图片处理核心逻辑
│   │   └── DeviceConfig.kt          # 设备配置参数
│   ├── src/main/res/layout/
│   │   └── activity_main.xml        # 主界面布局
│   └── build.gradle.kts             # 模块级构建配置
├── build.gradle.kts                 # 项目级构建配置
├── gradle.properties                # Gradle 项目属性
└── settings.gradle.kts              # 项目设置
```

## 构建和运行

### 环境要求

- Android Studio
- Android SDK (compileSdk 36, minSdk 33, targetSdk 36)
- Java 11
- Kotlin 2.0.21

### 构建命令

```bash
# 使用 Gradle Wrapper 构建项目
./gradlew build

# 构建调试 APK
./gradlew assembleDebug

# 安装调试 APK 到连接的设备
./gradlew installDebug
```

## TODO

- [x] 优化 PPI 处理
- [x] 优化横屏 UI 布局
- [x] 预设 Thor 上下屏间隔
- [ ] 自动设置壁纸
