# 🔥 EVA-Android - Face Detection Application

![GitHub stars](https://img.shields.io/github/stars/EVA-Android/EVA-Android.svg?style=social)
![GitHub forks](https://img.shields.io/github/forks/EVA-Android/EVA-Android.svg?style=social)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)

---

## 📱 About EVA-Android

**EVA-Android** is an advanced real-time face detection application built on Google's MediaPipe framework. It provides high-performance face detection capabilities with support for multiple inference modes and hardware accelerators.

### ✨ Key Features

| Feature | Description |
|---------|-------------|
| 🎥 **Real-time Camera Detection** | Detect faces in live camera streams with low latency |
| 🖼️ **Image Detection** | Analyze static images from gallery |
| 🎬 **Video Detection** | Process video files frame by frame |
| ⚡ **GPU Acceleration** | Leverage GPU for ultra-fast inference |
| 🧠 **CPU Support** | Fallback to CPU for wider device compatibility |
| 📊 **Real-time Metrics** | Display inference time and FPS |
| 🎨 **Visual Feedback** | Draw bounding boxes around detected faces |

### 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                            │
│  ┌─────────────────┐  ┌─────────────────────────────────┐  │
│  │   CameraFragment │  │        GalleryFragment          │  │
│  │  (Live Stream)   │  │  (Image/Video Processing)      │  │
│  └────────┬────────┘  └──────────────────┬──────────────┘  │
│           │                              │                  │
│           └──────────────┬───────────────┘                  │
│                          ▼                                  │
│           ┌───────────────────────────────┐                │
│           │      FaceDetectorHelper       │                │
│           │  (MediaPipe Face Detector)    │                │
│           └──────────────┬───────────────┘                │
│                          ▼                                  │
│           ┌───────────────────────────────┐                │
│           │  face_detection_short_range   │                │
│           │        .tflite Model          │                │
│           └───────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- 🛠️ **Android Studio** Arctic Fox or higher
- 📱 **Android SDK** API Level 24+
- 🤖 **Gradle** 7.0+
- 💻 **Java Development Kit** 11+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/EVA-Android/EVA-Android.git
   cd EVA-Android
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned repository

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio

---

## 🎮 Usage

### Live Camera Mode

1. Launch the application
2. The camera will automatically start
3. Face detection runs in real-time
4. Bounding boxes are drawn around detected faces
5. Inference time is displayed at the bottom

### Gallery Mode

1. Tap the gallery icon in the navigation bar
2. Select an image or video from your gallery
3. The app will process the media and display results

### Settings

- **Confidence Threshold**: Adjust detection sensitivity (0.0-1.0)
- **Delegate**: Switch between CPU and GPU
- **Running Mode**: Image, Video, or Live Stream

---

## 🧠 Technical Details

### Model Specifications

| Property | Value |
|----------|-------|
| **Model Type** | TensorFlow Lite |
| **Model Name** | `face_detection_short_range.tflite` |
| **Input Size** | 256x256 pixels |
| **Quantization** | FP16 |

### Performance Metrics

| Metric | CPU | GPU |
|--------|-----|-----|
| Inference Time | ~30ms | ~10ms |
| FPS | ~30 | ~60 |

### Supported Running Modes

1. **IMAGE Mode** - Process single static images
2. **VIDEO Mode** - Process video files sequentially
3. **LIVE_STREAM Mode** - Real-time camera processing

---

## 📁 Project Structure

```
EVA-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/google/mediapipe/examples/facedetection/
│   │   │   ├── MainActivity.kt       # Main activity with navigation
│   │   │   ├── MainViewModel.kt      # ViewModel for settings
│   │   │   ├── FaceDetectorHelper.kt # Core detection logic
│   │   │   └── OverlayView.kt        # Custom view for drawing
│   │   ├── assets/
│   │   │   └── face_detection_short_range.tflite
│   │   ├── res/                      # UI resources
│   │   └── AndroidManifest.xml       # App configuration
│   └── build.gradle                  # Module build config
├── gradle/                           # Gradle wrapper
├── build.gradle                      # Project build config
└── README.md                         # This file
```

---

## 🔧 Configuration

### Changing Detection Threshold

The default confidence threshold is set to **0.9** (90%). To change this:

```kotlin
// In FaceDetectorHelper.kt
const val THRESHOLD_DEFAULT = 0.9F  // Adjust this value
```

### Switching Delegate

The app supports both CPU and GPU delegates:

```kotlin
// CPU (default)
FaceDetectorHelper.DELEGATE_CPU = 0

// GPU (faster inference)
FaceDetectorHelper.DELEGATE_GPU = 1
```

---

## 📝 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to:

1. 🍴 Fork the repository
2. 🌿 Create a feature branch
3. ✏️ Make your changes
4. 📤 Submit a pull request

---

## 📧 Contact

For questions or support, please reach out to the development team.

---

⭐ **If you find this project useful, please give it a star!**

---

*Built with ❤️ using MediaPipe & TensorFlow Lite*