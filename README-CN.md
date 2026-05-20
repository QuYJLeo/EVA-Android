# 🔥 EVA-Android - 人脸检测应用

![GitHub stars](https://img.shields.io/github/stars/EVA-Android/EVA-Android.svg?style=social)
![GitHub forks](https://img.shields.io/github/forks/EVA-Android/EVA-Android.svg?style=social)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)

---

## 📱 关于 EVA-Android

**EVA-Android** 是一款基于 Google MediaPipe 框架构建的高级实时人脸检测应用。它提供高性能的人脸检测能力，支持多种推理模式和硬件加速器。

### ✨ 主要功能

| 功能 | 描述 |
|------|------|
| 🎥 **实时摄像头检测** | 低延迟实时人脸检测 |
| 🖼️ **图片检测** | 分析相册中的静态图片 |
| 🎬 **视频检测** | 逐帧处理视频文件 |
| ⚡ **GPU 加速** | 利用 GPU 实现超快速推理 |
| 🧠 **CPU 支持** | 兼容更多设备 |
| 📊 **实时指标** | 显示推理时间和帧率 |
| 🎨 **视觉反馈** | 在检测到的人脸周围绘制边界框 |

### 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                            │
│  ┌─────────────────┐  ┌─────────────────────────────────┐  │
│  │   CameraFragment │  │        GalleryFragment          │  │
│  │    (实时流模式)    │  │     (图片/视频处理)            │  │
│  └────────┬────────┘  └──────────────────┬──────────────┘  │
│           │                              │                  │
│           └──────────────┬───────────────┘                  │
│                          ▼                                  │
│           ┌───────────────────────────────┐                │
│           │      FaceDetectorHelper       │                │
│           │   (MediaPipe 人脸检测器)       │                │
│           └──────────────┬───────────────┘                │
│                          ▼                                  │
│           ┌───────────────────────────────┐                │
│           │  face_detection_short_range   │                │
│           │        .tflite 模型           │                │
│           └───────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

- 🛠️ **Android Studio** Arctic Fox 或更高版本
- 📱 **Android SDK** API Level 24+
- 🤖 **Gradle** 7.0+
- 💻 **Java Development Kit** 11+

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/EVA-Android/EVA-Android.git
   cd EVA-Android
   ```

2. **在 Android Studio 中打开**
   - 启动 Android Studio
   - 选择 "Open an existing project"
   - 导航到克隆的仓库

3. **构建项目**
   ```bash
   ./gradlew build
   ```

4. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 Android Studio 中的 "Run"

---

## 🎮 使用指南

### 实时摄像头模式

1. 启动应用
2. 摄像头自动启动
3. 实时进行人脸检测
4. 在检测到的人脸周围绘制边界框
5. 底部显示推理时间

### 相册模式

1. 点击导航栏中的相册图标
2. 从相册选择图片或视频
3. 应用将处理媒体文件并显示结果

### 设置选项

- **置信度阈值**: 调整检测灵敏度 (0.0-1.0)
- **推理设备**: 在 CPU 和 GPU 之间切换
- **运行模式**: 图片、视频或实时流

---

## 🧠 技术细节

### 模型规格

| 属性 | 值 |
|------|-----|
| **模型类型** | TensorFlow Lite |
| **模型名称** | `face_detection_short_range.tflite` |
| **输入尺寸** | 256x256 像素 |
| **量化类型** | FP16 |

### 性能指标

| 指标 | CPU | GPU |
|------|-----|-----|
| 推理时间 | ~30ms | ~10ms |
| 帧率 | ~30 | ~60 |

### 支持的运行模式

1. **IMAGE 模式** - 处理单张静态图片
2. **VIDEO 模式** - 顺序处理视频文件
3. **LIVE_STREAM 模式** - 实时摄像头处理

---

## 📁 项目结构

```
EVA-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/google/mediapipe/examples/facedetection/
│   │   │   ├── MainActivity.kt       # 主活动，包含导航
│   │   │   ├── MainViewModel.kt      # 设置的 ViewModel
│   │   │   ├── FaceDetectorHelper.kt # 核心检测逻辑
│   │   │   └── OverlayView.kt        # 自定义绘制视图
│   │   ├── assets/
│   │   │   └── face_detection_short_range.tflite
│   │   ├── res/                      # UI 资源文件
│   │   └── AndroidManifest.xml       # 应用配置
│   └── build.gradle                  # 模块构建配置
├── gradle/                           # Gradle 包装器
├── build.gradle                      # 项目构建配置
└── README-CN.md                      # 此文件
```

---

## 🔧 配置说明

### 修改检测阈值

默认置信度阈值设置为 **0.9** (90%)。如需修改：

```kotlin
// 在 FaceDetectorHelper.kt 中
const val THRESHOLD_DEFAULT = 0.9F  // 调整此值
```

### 切换推理设备

应用支持 CPU 和 GPU 两种推理设备：

```kotlin
// CPU (默认)
FaceDetectorHelper.DELEGATE_CPU = 0

// GPU (更快的推理速度)
FaceDetectorHelper.DELEGATE_GPU = 1
```

---

## 📝 许可证

本项目采用 Apache License 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件。

---

## 🤝 贡献指南

欢迎贡献代码！请按照以下步骤：

1. 🍴 Fork 仓库
2. 🌿 创建功能分支
3. ✏️ 进行修改
4. 📤 提交 Pull Request

---

## 📧 联系方式

如有问题或需要支持，请联系开发团队。

---

⭐ **如果您觉得这个项目有用，请给它一个星标！**

---

*使用 MediaPipe & TensorFlow Lite 构建 ❤️*