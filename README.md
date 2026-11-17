# FocusLog

**FocusLog** is a minimalist, offline-first native Android app built with Kotlin, Jetpack Compose (Material 3), and MVVM architecture.

- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)
- **Architecture**: MVVM with Room Database, Kotlin Coroutines, and Flow
- **100% Offline**: Zero internet permission, no login, no third-party tracking or analytics.

---

## Core Flow

1. **Custom Focus Timer**:
   - Set custom minutes and optional seconds.
   - Quick preset chips (15m, 25m, 45m, 60m) or direct numeric input.
   - Can be repeated throughout the day.

2. **Unstoppable Alarm**:
   - When the timer ends, an alarm rings continuously (even if the app is closed or the screen is locked).
   - Utilizes `AlarmManager.setExactAndAllowWhileIdle()`, foreground `AlarmService` (`USAGE_ALARM` audio loop + wake lock), and full-screen intent `AlarmActivity`.

3. **Mandatory Activity Logging**:
   - The alarm **cannot be dismissed** until you type what you accomplished during the session and submit it.
   - Back gestures and outside touch are blocked until submitted.

4. **Visual Card Stack**:
   - Submitted entries appear as a stacked deck of cards on the dashboard.
   - **Newest card on top**: Fully colored with vibrant Material 3 accents, full size, and high elevation.
   - **Older cards**: Greyed out, slightly smaller, offset, and layered behind.
   - Expandable full list view to review earlier sessions in detail.

5. **Daily 2:00 AM IST Reset & Markdown Export**:
   - Every day at **2:00 AM IST (`Asia/Kolkata`)**, all completed sessions are exported into a clean Markdown file named `YYYY-MM-DD.md`.
   - Export Location:
     `Android/data/com.focuslog.app/files/Documents/FocusLog/{YYYY-MM-DD}.md`
   - After successful export, active entries are safely purged from the local Room database to prepare for a fresh day.
   - Includes automatic catch-up export if the device was turned off at 2:00 AM.

---

## Project Structure

```
WhatUdoin/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/focuslog/app/
│   │   │   ├── FocusLogApplication.kt
│   │   │   ├── data/
│   │   │   │   ├── export/
│   │   │   │   │   └── MarkdownExporter.kt
│   │   │   │   ├── local/
│   │   │   │   │   ├── FocusLogDao.kt
│   │   │   │   │   ├── FocusLogDatabase.kt
│   │   │   │   │   └── FocusLogEntry.kt
│   │   │   │   └── repository/
│   │   │   │       └── FocusLogRepository.kt
│   │   │   ├── service/
│   │   │   │   ├── AlarmReceiver.kt
│   │   │   │   ├── AlarmService.kt
│   │   │   │   └── BootReceiver.kt
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   │   ├── CardStack.kt
│   │   │   │   │   └── TimerPicker.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── AlarmActivity.kt
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   └── MainScreen.kt
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   └── viewmodel/
│   │   │   │       ├── FocusLogUiState.kt
│   │   │   │       └── FocusLogViewModel.kt
│   │   │   ├── util/
│   │   │   │   └── TimeUtils.kt
│   │   │   └── worker/
│   │   │       └── DailyExportWorker.kt
│   │   └── res/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

---

## How to Build & Run

### In Android Studio
1. Open Android Studio -> **Open...** -> select `c:\WhatUdoin`.
2. Let Gradle sync project dependencies.
3. Connect a physical Android device or start an Android Emulator (API 26 to 35).
4. Click **Run** (`Shift + F10`).

### Via Command Line
```powershell
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
