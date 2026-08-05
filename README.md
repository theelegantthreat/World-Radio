# 📻 World Radio

An interactive, feature-rich Android live radio streaming application built with **Kotlin** and **Jetpack Compose**. Stream live FM/AM stations from around the globe, record broadcasts in real-time, fine-tune stations using an interactive tuner dial, and save your favorite channels locally.

---

## ✨ Features

- 🌐 **Global Station Search**: Search thousands of live radio stations worldwide powered by the [Radio Browser API](https://www.radio-browser.info/). Filter easily by station name, country, or genre tags.
- 🎛️ **Retro FM Tuner Dial**: Interactive frequency tuner dial providing a tactile analog radio experience with frequency and band indicator calculations.
- 🎙️ **Live Audio Stream Recording**: Record live audio streams directly to local storage (`.mp3` / `.aac`). Features real-time duration tracking, built-in playback, and recording management.
- 🎵 **Live Track Title Metadata**: Automatic ICY stream metadata fetching displaying current playing track info in real time.
- 🎚️ **Embedded Playback Controller**: Sticky player widget with audio spectrum visualizers, volume controls, station switching, and play/pause controls.
- ⭐️ **Favorites & Listening History**: Save stations to favorites, track play counts, and access recently played stations backed by a local Room database.
- 💾 **Database Backup & Restore**: Export and import station library backups in JSON format.
- 🎨 **Cosmic Cyberpunk Theme**: Styled with Material 3 components, vibrant cyan accents, dynamic lighting effects, and dark cosmic aesthetics.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with **Material 3**
- **Architecture**: MVVM (Model-View-ViewModel) with `StateFlow` and Kotlin Coroutines
- **Local Database**: [Room Database](https://developer.android.com/training/data-storage/room) with KSP
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & [OkHttp 4](https://square.github.io/okhttp/)
- **JSON Serialization**: [Moshi](https://github.com/square/moshi) (Kotlin Code Gen)
- **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/)
- **Audio Engine**: Android `MediaPlayer` & OkHttp Byte Streaming engine

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug / Jellyfish or newer
- **JDK**: Version 11 or higher
- **Minimum SDK**: Android 7.0 (API level 24)
- **Target SDK**: Android 15 (API level 36)

### Installation & Build

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/world-radio.git
   cd world-radio
   ```

2. **Open in Android Studio**:
   Open Android Studio and select **Open**, then navigate to the cloned project folder.

3. **Build the Project**:
   Build the app using Gradle CLI or Android Studio:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on Device / Emulator**:
   Select your target emulator or connected device and press **Run** (`Shift + F10`).

---

## 📂 Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── api/          # RadioBrowser API & ICY Metadata fetcher
│   ├── db/           # Room Database, DAO, and Entities
│   ├── model/        # Data classes and Moshi DTO models
│   └── repository/   # Repository layer for API & local DB coordination
├── player/           # RadioPlaybackManager & RadioRecordingManager
├── ui/
│   ├── screens/      # Jetpack Compose UI screens & components
│   ├── theme/        # Material 3 Color palette, Typography & Theme
│   └── viewmodel/    # RadioViewModel managing reactive UI state
└── MainActivity.kt   # Entry point Activity
```

---

## 🧪 Testing

Run local unit and Robolectric tests with Gradle:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
