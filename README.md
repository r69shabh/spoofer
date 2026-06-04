<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Spoofer Logo" width="120" height="120" />
  <h1>Spoofer: Advanced Android Location Simulation Engine</h1>
  
  <p>
    <strong>A robust, open-source mock location provider built natively for Android using Kotlin, Jetpack Compose, and modern architecture principles.</strong>
  </p>

  <p>
    <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" /></a>
    <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
    <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
    <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge" alt="License" /></a>
  </p>
</div>

---

## 📖 Table of Contents
1. [Project Overview](#-project-overview)
2. [Deep Dive into Features](#-deep-dive-into-features)
3. [Software Architecture & Tech Stack](#-software-architecture--tech-stack)
4. [The Rubber-Banding Conundrum (Technical Challenge)](#-the-rubber-banding-conundrum-technical-challenge)
5. [Installation & Setup Guide](#-installation--setup-guide)
6. [Known Bugs & Limitations](#-known-bugs--limitations)
7. [Contributing & Community Help](#-contributing--community-help)
8. [License](#-license)

---

## 🌍 Project Overview

Location spoofing is a critical tool for developers, privacy advocates, and general power users. Whether you are QA testing a location-based social media application, playing a geo-fenced mobile game, or bypassing regional network locks, having precise, system-wide control over your device's telemetry is essential.

**Spoofer** is not just another fake GPS app. It is designed to be a highly resilient location injection engine. Unlike legacy spoofers that rely on outdated UI frameworks and simple GPS overrides, Spoofer is built entirely on **Material Design 3**, driven by a reactive **MVVM** architecture, and explicitly targets modern Android (API 26 to API 35) security paradigms. It intercepts the Android OS `LocationManager` pipeline and completely replaces hardware GPS telemetry with software-generated coordinates, speeds, and bearings.

---

## ✨ Deep Dive into Features

Spoofer is divided into three distinct location injection modalities, supported by a powerful search infrastructure.

### 1. Static Teleportation (Static Mode)
The simplest form of spoofing. By entering an address or dropping a pin on the map, Spoofer immediately teleports the device's system coordinates to that exact location. 
* **Implementation Details:** The engine injects coordinates with a randomized micro-jitter (changing by 0.000018 degrees) to simulate the natural satellite drift of real hardware, preventing anti-cheat systems from detecting a perfectly static, "impossible" GPS lock.

### 2. Manual Directional Control (Joystick Mode)
An on-screen, floating joystick overlay allows the user to manually "walk", "drive", or "fly" in any 360-degree direction.
* **Physics Simulation:** The joystick calculates vector magnitude and bearing. These vectors are passed through a Haversine physics algorithm (`METERS_PER_DEGREE_LAT`) to advance the coordinates mathematically based on the user's selected speed profile (e.g., walking at 5 km/h vs driving at 120 km/h).

### 3. Automated Route Navigation (Directions Mode)
The hallmark feature of Spoofer. Users can select an Origin and a Destination. The app queries the OpenRouteService (OSRM) API to generate a realistic road-geometry polyline.
* **Dynamic Interpolation:** The `SpeedSimulationUseCase` engine iterates over the polyline segments. It calculates the exact distance between nodes and mathematically interpolates your position along the curve of the road at a highly specific update rate (5Hz). It dynamically updates the device's `bearing` (compass heading) and `speed` metrics so that apps like Google Maps behave identically to being in a real moving vehicle.

### 4. Advanced Geocoding Infrastructure
Instead of relying on the restrictive and often rate-limited Android `Geocoder`, Spoofer utilizes the **Photon API** (backed by OpenStreetMap). This allows for rich Point of Interest (POI) searches. We apply a location-bias algorithm, meaning if your map is currently looking at New York, searching for "Starbucks" will prioritize results in New York rather than returning a Starbucks in London.

---

## 🏗 Software Architecture & Tech Stack

The codebase is structured around modern Android development standards to ensure maintainability, testability, and reactive UI updates.

- **UI Layer (Jetpack Compose):** 100% declarative UI. We utilize Material 3 components, Bottom Sheets, and custom composables (`LocationInputField`). State is strictly hoisted and collected from ViewModels.
- **Presentation Layer (MVVM):** `MapViewModel` manages the monolithic state of the map, location engines, and search results using Kotlin `StateFlow` and `viewModelScope` coroutines.
- **Dependency Injection (Hilt/Dagger):** All repositories, use cases, and location providers are scoped as singletons and injected via Hilt to ensure lifecycle safety across Foreground Services and Activities.
- **Data Layer (Room & Datastore):** Favorite locations are persisted using a Room SQLite database. User preferences (like dark mode and default speeds) are saved using Preferences DataStore.
- **Service Layer (Foreground Services):** Location injection happens inside a highly privileged Foreground Service (`MockLocationService`). This prevents the Android OS from killing the location loop when the app is minimized or the screen is turned off.

---

## 🚨 The Rubber-Banding Conundrum (Technical Challenge)

If you are an Android framework engineer, this section is for you. 

**Rubber-Banding** is the most notorious problem in location spoofing. Modern Android devices do not just use satellite GPS; they use the **Fused Location Provider (FLP)**. FLP aggressively scans local Wi-Fi MAC addresses, Bluetooth beacons, and cellular towers to triangulate your position. 

When you spoof the GPS, FLP detects a massive discrepancy between your mocked satellite location and the physical Wi-Fi routers around you. FLP often decides the Mock Location is "wrong" and forcefully snaps the device's location back to your real physical location for a split second, causing the marker to bounce rapidly back and forth (Rubber-Banding).

### Our 4-Pronged Mitigation Strategy
To suppress FLP and force it to accept our mock coordinates on strict operating systems like Android 14 and 15, we have implemented an extreme override loop in `MockLocationProvider.kt`:

1. **Mocking the Undocumented `fused` Provider:**
   By default, developers only mock `LocationManager.GPS_PROVIDER`. We explicitly inject mock locations into the hidden `"fused"` test provider as well. This injects our fake coordinates directly into the pipeline Google Play Services uses.
2. **Hyper-Aggressive 5Hz Tick Spamming:**
   Real hardware GPS chips update at 1Hz (1 update per second). Our coroutine loop fires at **5Hz (every 200ms)**. By flooding the `LocationManager` with 5 times the volume of data, we mathematically drown out the real hardware updates.
3. **Artificial Perfect Accuracy:**
   FLP evaluates incoming location data and trusts the source with the lowest error margin. We hardcode our mock location accuracy to `1.0f` (1 meter). Because real GPS is usually 3m-10m accurate, the FLP algorithm looks at our fake signal, deems it "mathematically superior," and prioritizes it.
4. **API 31+ Anti-Tamper Bypasses:**
   Android 15 requires strict flags. We utilize Java Reflection to invoke the hidden `Location.makeComplete()` method and explicitly set the `isMock = true` boolean to prevent the OS from discarding our packets as malformed.

### 🆘 We Need Your Help!
Despite this incredibly aggressive mitigation architecture, **micro-jumps still occasionally occur**. If the device detects a massive influx of strong Wi-Fi signals, FLP will still momentarily overpower our 5Hz spam. 

**We are actively seeking pull requests and architectural advice from Android security or framework experts to help us achieve a 100% airtight, zero-jump spoofing pipeline without requiring Magisk/Root.**

---

## ⚙️ Installation & Setup Guide

Since this app interfaces with system-level Developer Options, installation requires a few extra steps.

### Prerequisites
- A physical Android device or Emulator running Android 8.0 (API 26) or higher.
- Android Studio Ladybug (or newer).
- JDK 17.

### Build Instructions
1. **Clone the Source:**
   ```bash
   git clone https://github.com/yourusername/spoofer.git
   cd spoofer
   ```
2. **Open in Android Studio:**
   Allow Gradle to sync the dependencies.
3. **Compile the APK:**
   Click the **Run** button, or build via terminal:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Install to Device:**
   Ensure your device is connected via ADB and install the generated APK.

### Enabling Mock Locations (Crucial Step)
The app will not work unless you grant it Mock Location authority at the OS level.
1. Open your phone's **Settings**.
2. Scroll to **About Phone** -> **Software Information**.
3. Tap **Build Number** rapidly 7 times to unlock Developer Options.
4. Go back to the main Settings menu and open **Developer Options**.
5. Scroll down to the **Debugging** section.
6. Tap **Select mock location app** and choose **Spoofer** from the list.
7. *(Optional but Highly Recommended)*: Go to Settings -> Location -> Location Services and turn **OFF "Google Location Accuracy"** (Wi-Fi/Bluetooth scanning). This severely cripples Rubber-Banding.

---

## 🐛 Known Bugs & Limitations

- **Rubber-Banding:** As detailed above, micro-jumps back to the real hardware location still occasionally occur on devices with aggressive Wi-Fi scanning enabled.
- **Altitude/Elevation Simulation:** Currently, altitude is hardcoded to `0.0`. Advanced games/apps that cross-reference altitude with topographical maps may flag the spoofed location as suspicious.
- **Directions Route Limitations:** If an excessively long route (e.g., cross-country) is selected, the OSRM polyline response may be too large to parse efficiently on the main thread, causing temporary UI freezes.

---

## 🤝 Contributing & Community Help

We believe in the power of open-source collaboration. We are actively looking for contributors to help build new features, refine the Material 3 UI, and conquer the rubber-banding issue.

Please read our [**CONTRIBUTING.md**](CONTRIBUTING.md) for full details on how you can get involved, our code of conduct, and our pull request pipeline.

1. **Fork** the repository.
2. **Clone** your fork locally.
3. **Create a branch** for your feature or bugfix (`git checkout -b feature/advanced-physics`).
4. **Commit** your changes (`git commit -m 'Implement advanced physics model'`).
5. **Push** to your fork (`git push origin feature/advanced-physics`).
6. Open a **Pull Request** on this repository.

---

## 📜 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for complete details. You are free to modify, distribute, and use this code commercially and privately, provided proper attribution is given.
