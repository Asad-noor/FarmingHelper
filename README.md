# 🌿 AgriLens

**Offline-first crop diagnostic tool for farmers — powered by on-device AI.**

AgriLens helps farmers identify leaf diseases and crop pests directly in the field, with zero internet dependency. Point the camera at a leaf or an insect, get an instant analysis. No data leaves the device. No connectivity required. Built for the conditions farmers actually work in.

---

## 📱 Screenshots

> _Coming soon — screenshots will be added as screens are completed._

---

## 🎯 Problem Statement

Farmers in agricultural regions frequently work in areas with no reliable internet access. When a crop shows signs of disease or pest damage, every hour matters — delayed diagnosis means wider spread and greater yield loss. Existing solutions require cloud connectivity, making them impractical in the field.

AgriLens solves this by running all analysis fully on-device using a quantized TFLite model, providing instant, private, offline-capable diagnostics directly from the farmer's phone.

---

## ✨ Features

- 📷 **Live camera capture** via CameraX with flash support
- 🍃 **Leaf disease classification** — detects diseases like blight, rust, mildew, and healthy state across major crops (tomato, potato, apple, grape, corn, pepper)
- 🐛 **Pest / worm identification** — classifies common agricultural pests and insects from a photo
- 📊 **Top-3 predictions** with confidence score bars per result
- 🗂️ **Local history** — past scans saved and filterable by analysis type
- 🔒 **Fully offline** — no internet permission declared in the manifest
- 🔐 **Encrypted local storage** — history protected via Android Keystore-backed keys
- ✅ **Model integrity verification** — SHA-256 hash check at load time for both models

---

## 🏗️ Architecture

AgriLens follows **Clean Architecture** principles combined with **MVVM**, structured in strict package separation within a single Gradle module. Dependency direction is enforced: `presentation → domain ← data`. The domain layer has zero Android dependencies.

```
:app
 ├─ presentation/        # Compose UI, ViewModels, Navigation
 │   ├─ home/
 │   ├─ camera/
 │   ├─ result/
 │   ├─ history/
 │   └─ navigation/
 │
 ├─ domain/              # Pure Kotlin — models, use-cases, repository interfaces
 │   ├─ model/
 │   ├─ repository/
 │   └─ usecase/
 │
 └─ data/                # Repository implementations, TFLite classifier, DataStore
     ├─ classifier/
     ├─ repository/
     ├─ local/
     └─ security/
```

> At production scale this would be split into separate Gradle modules (`:domain`, `:data`, `:presentation`) for strict build-time boundary enforcement and faster incremental builds. The package structure mirrors that split intentionally.

---

## 🔄 Screen Flow

```
┌─────────────────────┐
│   Permission Gate    │  Camera permission check + model integrity verification
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│        Home          │  Two entry points
│  [Leaf Disease]      │──────────────────────────┐
│  [Identify Pest]     │──────────────────────┐   │
└─────────────────────┘                       │   │
                                              ▼   ▼
                                   ┌─────────────────────┐
                                   │       Camera         │  Carries AnalysisType
                                   │  CameraX preview     │  through navigation
                                   │  Capture button      │
                                   └────────┬────────────┘
                                            │ capture
                                            ▼
                                   ┌─────────────────────┐
                                   │       Result         │  On-device inference
                                   │  Top-3 predictions   │  Confidence bars
                                   │  Save to history     │
                                   └────────┬────────────┘
                                            │ save
                                            ▼
                                   ┌─────────────────────┐
                                   │       History        │  Filterable by type
                                   │  Past scans          │  Encrypted local store
                                   └─────────────────────┘
```

---

## 🧩 Key Design Decisions

### 1. One config-driven classifier, not two separate classes

Both analysis modes share a single `TFLiteClassifier` driven by a `ModelConfig`. Swapping models means swapping config — not duplicating infrastructure.

```kotlin
data class ModelConfig(
    val assetPath: String,
    val labelsPath: String,
    val inputSize: Int,
    val mean: Float,
    val std: Float
)
```

### 2. AnalysisType flows through the navigation graph

The selected mode (`LEAF_DISEASE` or `PEST`) is passed as a type-safe navigation argument from Home → Camera → Result. The repository picks the correct `ModelConfig` based on it. No global state, no flag-checking buried in the classifier.

### 3. ViewModels never touch TFLite

ViewModels call use-cases only. The domain use-case calls the repository interface. The repository implementation holds the classifier. This boundary means the inference engine can be swapped, mocked for tests, or upgraded without touching a single ViewModel.

```
ViewModel → ClassifyImageUseCase → ClassifierRepository (interface)
                                          ↓
                                  ClassifierRepositoryImpl
                                          ↓
                                   TFLiteClassifier
```

### 4. UiState as a sealed interface

```kotlin
sealed interface UiState {
    data object Loading : UiState
    data class Success(val predictions: List<Prediction>) : UiState
    data class Error(val message: String) : UiState
}
```

Every screen observes a `StateFlow<UiState>` — the UI is always a pure function of state.

---

## 🔐 Security

Security is a core concern, not an afterthought — particularly given that this app handles sensitive farm data and bundles proprietary ML models.

| Measure | Implementation |
|---|---|
| **On-device inference** | No network permission declared. Images never leave the device. |
| **Model integrity** | SHA-256 hash of each `.tflite` file verified at load time. Tampered or swapped models are rejected before any inference runs. |
| **Keystore-backed encryption** | AES key generated and stored in Android Keystore. Never hardcoded, never exported. |
| **Encrypted history** | Scan history stored via Jetpack Security `EncryptedFile`. Unreadable without the Keystore key. |
| **Scoped storage** | No broad storage permissions. All file access through scoped APIs. |
| **Release hardening** | R8 minification + resource shrinking enabled. All sensitive logging stripped in release builds via ProGuard rules. |
| **No unnecessary permissions** | Manifest declares only `CAMERA`. No `INTERNET`, no `READ_EXTERNAL_STORAGE`. |

> **Considered but out of scope for v1:** Certificate pinning (no network = no relevance), biometric lock for history access, root detection.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Camera | CameraX |
| ML Inference | TFLite (LiteRT) |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose (type-safe) |
| Image loading | Coil |
| Local storage | DataStore |
| Encrypted storage | Jetpack Security / EncryptedFile |
| Key management | Android Keystore |
| Build | Gradle Version Catalog (`libs.versions.toml`) |
| Min SDK | 26 |
| Target SDK | 35 |

---

## 🤖 ML Models

### Leaf Disease Model
- **Dataset:** PlantVillage
- **Classes:** ~38 (disease + healthy states across tomato, potato, apple, grape, corn, pepper, and others)
- **Input:** 224×224 RGB
- **Output:** Softmax over class labels

### Pest Identification Model
- **Dataset:** IP102 (102-class agricultural pest dataset)
- **Input:** TBD — locked after model acquisition
- **Output:** Softmax over pest class labels

> Both models are bundled in `assets/` and verified via SHA-256 at launch.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android device or emulator with API 26+

### Setup

```bash
git clone https://github.com/yourusername/AgriLens.git
cd AgriLens
```

Open in Android Studio, let Gradle sync, then run on a physical device (camera features require real hardware for best results).

> The `.tflite` model files are bundled in `app/src/main/assets/`. No additional download step required.

---

## 📁 Project Structure (detailed)

```
app/
 └─ src/main/
     ├─ assets/
     │   ├─ leaf_disease_model.tflite
     │   ├─ leaf_disease_labels.txt
     │   ├─ pest_model.tflite
     │   └─ pest_labels.txt
     │
     └─ kotlin/com/yourdomain/agrilens/
         │
         ├─ presentation/
         │   ├─ navigation/        # NavGraph, Routes, NavArgs
         │   ├─ home/              # HomeScreen, HomeViewModel
         │   ├─ camera/            # CameraScreen, CameraViewModel
         │   ├─ result/            # ResultScreen, ResultViewModel
         │   ├─ history/           # HistoryScreen, HistoryViewModel
         │   └─ component/         # Shared Composables (ConfidenceBar, etc.)
         │
         ├─ domain/
         │   ├─ model/
         │   │   ├─ Prediction.kt
         │   │   └─ AnalysisType.kt
         │   ├─ repository/
         │   │   └─ ClassifierRepository.kt
         │   └─ usecase/
         │       └─ ClassifyImageUseCase.kt
         │
         └─ data/
             ├─ classifier/
             │   ├─ TFLiteClassifier.kt
             │   └─ ModelConfig.kt
             ├─ repository/
             │   └─ ClassifierRepositoryImpl.kt
             ├─ local/
             │   ├─ HistoryDataSource.kt
             │   └─ entity/ScanRecord.kt
             └─ security/
                 ├─ ModelIntegrityChecker.kt
                 └─ KeystoreManager.kt
```

---

## 🗺️ Roadmap

- [x] Project architecture & planning
- [ ] Project skeleton + version catalog + Hilt setup
- [ ] Navigation graph with placeholder screens
- [ ] CameraX capture
- [ ] Domain layer (models, use-case, repository interface)
- [ ] ModelConfig + TFLiteClassifier (disease model, end-to-end)
- [ ] Result UI with confidence bars
- [ ] AnalysisType threading through navigation + Home two-button entry
- [ ] Pest model as second ModelConfig
- [ ] Security layer (Keystore + integrity check + encrypted history)
- [ ] History screen with filter
- [ ] UI polish + edge cases
- [ ] Screenshots + demo video

---

## 🤝 Contributing

This is a personal portfolio project. Issues and suggestions are welcome via GitHub Issues.

---

## 📄 License

```
MIT License

Copyright (c) 2025 [Your Name]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

---

## 👤 Author

**[Md. Asaduzzaman Noor]**
- GitHub: (https://github.com/Asad-noor)
- LinkedIn: (https://www.linkedin.com/in/md-asaduzzaman-noor/)

---

> _Built as a portfolio project to demonstrate Clean Architecture, offline-first design, on-device ML inference, and Android security best practices._
