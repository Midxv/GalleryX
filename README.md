<p align="center">
  <img src="res/drawable/applogo.png" alt="GalleryX Logo" width="150"/>
</p>

<h1 align="center">GalleryX</h1>

<p align="center">
  <strong>A highly secure, privacy-first photo and video vault for Android.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Architecture-MVVM-blue" alt="Architecture">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
</p>

---

## 📖 About GalleryX
GalleryX is a robust Android application designed to keep your personal media truly private. Built with modern Android development practices, it encrypts your photos and videos locally, ensuring that no one can access your private moments without your permission.

Whether you need to hide sensitive documents, personal photos, or private videos, GalleryX provides bank-grade encryption paired with an intuitive, fluid user interface.

## ✨ Key Features
* **🔒 AES-256 Encryption:** Your media is fully encrypted on your local storage. No cloud leaks, no unauthorized access.
* **👻 Stealth Mode / App Hiding:** Disguise the app or hide it entirely from the launcher. Launch it via a secret dialer code.
* **👆 Biometric Unlock:** Quickly and securely access your vault using your device's fingerprint or face unlock.
* **📁 Smart Album Management:** Organize your encrypted photos and videos into custom albums.
* **☁️ Encrypted Backups:** Safely backup and restore your encrypted vault so you never lose your memories.
* **🎥 Built-in Media Player:** View your photos and stream your encrypted videos directly inside the app without decrypting them to public storage first.

## 📱 Screenshots

<p align="center">
  <img src="Public/assets/Screenshot_2026-02-28-15-42-15-461_com.app.galleryx.foss.jpg" width="24%" />
  <img src="Public/assets/Screenshot_2026-02-28-15-42-17-547_com.app.galleryx.foss.jpg" width="24%" />
  <img src="Public/assets/Screenshot_2026-02-28-15-42-22-624_com.app.galleryx.foss.jpg" width="24%" />
  <img src="Public/assets/Screenshot_2026-02-28-15-42-30-951_com.app.galleryx.foss.jpg" width="24%" />
</p>

## 🛠️ Tech Stack & Architecture
GalleryX is built using the latest Android development standards:
* **Language:** [Kotlin](https://kotlinlang.org/) (100%)
* **UI Toolkit:** Jetpack Compose & XML (Hybrid approach)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Local Database:** Room Database
* **Concurrency:** Kotlin Coroutines & Flow
* **Dependency Injection:** Dagger Hilt
* **Security:** Android Keystore System, BiometricPrompt API, AES Encryption

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest Version)
* JDK 17+
* Minimum SDK: 24 (Android 7.0)

### Building the Project
1. Clone the repository:
   ```bash
   git clone [https://github.com/Midxv/GalleryX.git](https://github.com/Midxv/GalleryX.git)