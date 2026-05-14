# Prescription Library

**An offline‑only Android application for managing personal prescription records.**

---

## 🎯 Purpose

The app lets users store, view, and back up their medication prescriptions securely on their device.  It is designed for:
* High privacy – no network calls or external dependencies.
* Simple, high‑contrast UI suitable for elderly users.
* Easy backup/restore using AES‑256 encrypted files.

---

## 📦 Repository Layout

```
├─ .gitignore            # excludes build artefacts, local configs, secrets, logs
├─ Dockerfile            # container image for reproducible Android builds
├─ build.gradle.kts      # top‑level Gradle script (plugins only)
├─ gradle/               # Gradle wrapper files
├─ gradle.properties    # JVM/Gradle settings
├─ local.properties     # **DO NOT COMMIT** – contains your SDK path (ignored)
├─ settings.gradle.kts  # includes the `app` module
└─ app/                  # Android module
   ├─ src/main/java/com/aushadh/app/      # Kotlin source code
   │   ├─ MainActivity.kt                # entry point UI
   │   ├─ data/AppDatabase.kt            # Room DB definition
   │   ├─ ui/…                           # Compose UI screens
   │   ├─ util/BackupUtility.kt          # encrypted backup logic
   │   └─ util/FileStorageManager.kt   # file‑I/O helpers
   └─ src/main/res/                     # resources (icons, layouts)
```

---

## 🛠️ Prerequisites

* **Docker** (recommended) – the provided `Dockerfile` contains everything needed (Ubuntu 22.04, JDK 17, Android SDK, Gradle).
* If you prefer a native build, install:
  * Java 17 JDK
  * Android SDK command‑line tools (`cmdline-tools/latest`)
  * Gradle 8.x (wrapper is included, so you can just run `./gradlew`)

---

## 🚀 Building the App

### Using Docker (recommended for reproducibility)
```bash
# Build the Docker image
docker build -t prescription-builder .

# Run a container that builds the APK and writes it to a local folder
docker run --rm -v "$(pwd)/output:/project/app/build/outputs/apk/debug" \
    prescription-builder \
    ./gradlew assembleDebug
```
The generated `app-debug.apk` will appear in the `output/` directory on the host.

### Native build (Linux/macOS/Windows with WSL)
```bash
# Ensure SDK path is set in local.properties (this file is ignored by Git)
./gradlew clean assembleDebug
```
The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔐 Security & Privacy

* No remote services are used – all data stays on‑device.
* Backups are encrypted with AES‑256 (see `BackupUtility.kt`).
* `local.properties` (SDK path) and any keystore files are excluded via `.gitignore`.

---

## 📚 Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/awesome‑feature`).
3. Make sure the code compiles (`./gradlew test` for unit tests, if any).
4. Submit a Pull Request.

Please keep the `.gitignore` up‑to‑date with any new local files you add.

---

## 📜 License

This project is released under the **MIT License** – see the `LICENSE` file for details.

---

*Happy coding!*
