# Contributing to Spoofer

First off, thank you for considering contributing to Spoofer! It's people like you that make Spoofer such a powerful and resilient tool for Android development and privacy.

We welcome all contributions, from simple typo fixes in the documentation to major architectural refactors and new features.

---

## 🛠 How Can I Contribute?

### 1. Reporting Bugs
If you find a bug (such as a specific device experiencing extreme rubber-banding, or a UI glitch), please open an Issue. Include:
- Your Android version and device model.
- Steps to reproduce the bug.
- Logcat output (if applicable).

### 2. Suggesting Enhancements
Have an idea for a cool new feature? Maybe a GPX route importer or advanced altitude simulation?
- Open an Issue labeled `enhancement`.
- Describe the feature in detail.
- Explain why this enhancement would be useful to the majority of users.

### 3. Code Contributions (Pull Requests)
We love Pull Requests! Whether it is fixing a known bug or implementing a new feature, please follow the Pull Request process outlined below.

---

## 💻 Development Setup

To work on Spoofer locally, you will need the following setup:

1. **Android Studio:** Ladybug or newer.
2. **Java Development Kit (JDK):** JDK 17 is required.
3. **Android SDK:** Compile SDK is set to 35, Minimum SDK is 26.

### Architectural Guidelines for Contributors
When writing code for Spoofer, please adhere to our architectural standards:
- **UI:** All new UI must be written in **Jetpack Compose** (Material Design 3). Do not use XML layouts.
- **State Management:** Keep UI state hoisted. ViewModels should expose data strictly via `StateFlow`.
- **Dependency Injection:** Use **Hilt** for all dependency injection.
- **Coroutines:** Use Kotlin Coroutines for all asynchronous work. Avoid `Thread` or `AsyncTask`.

---

## 🧪 The "Holy Grail": Fixing Rubber-Banding

The most critical area where we need help is defeating Android's **Fused Location Provider (FLP)** rubber-banding without requiring the user to root their phone.

If you are contributing a fix to the location injection engine (`MockLocationProvider.kt` or `MockLocationService.kt`), please ensure you test your changes on an **Android 14 or 15 device with Wi-Fi and Bluetooth scanning enabled**.

Any PR that significantly reduces or eliminates micro-jumps will be reviewed and merged with the highest priority!

---

## 🔄 Pull Request Process

1. **Fork the repo** and create your branch from `main`.
2. **Name your branch** descriptively (e.g., `feature/import-gpx`, `bugfix/crash-on-search`).
3. **Write clean code** that adheres to the existing style (we follow standard Kotlin styling guidelines).
4. **Update documentation:** If you added a feature, please update `README.md` and any relevant code comments.
5. **Open the Pull Request:** Ensure your PR description clearly describes the problem you are solving and the technical approach you took.

---

## 📜 Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. We are committed to providing a welcoming and inspiring community for all. Harassment or abusive behavior of any kind will not be tolerated.

---

Thank you again for your interest in making Spoofer better! We look forward to reviewing your contributions.
