# DataGuard 🛡️

**DataGuard** is a private, device-local native Android application built with Kotlin, Jetpack Compose, Material 3, and Room Database (minSdk 26). It tracks cellular mobile data and Wi-Fi usage, monitors tethering (hotspot) data, forecasts plan exhaustion dates, alerts on consumption spikes and plan limits via WorkManager, features a real-time status bar speed monitor, provides a home screen widget, and exports verification reports as CSV.

---

## 🔒 Privacy & Core Philosophy
- **100% On-Device**: No accounts, no cloud database, no tracking analytics, and no advertisements.
- **Hardware Telemetry**: Direct integration with Android's `NetworkStatsManager` using `PACKAGE_USAGE_STATS` (Usage Access).
- **Long-term Retention**: Daily snapshots persisted in local **Room Database** so your history survives even after Android OS purges system counters.

---

## 🌟 Key Features

### 1. Plan Dashboard & Progress Ring
- **Big Progress Ring**: Displays real-time data plan % used with animated sweeps and semantic status colors (Green < 75%, Orange 75–90%, Red > 90%).
- **Forecast Engine**: Dynamic prediction calculating exact depletion date (`"At this pace you'll run out on <date>"`) or indicating safe consumption.
- **Metric Tiles**: Today's mobile usage, today's Wi-Fi usage, daily recommended budget, and dual-SIM multi-slot aggregation.
- **Tethering / Hotspot Monitor**: Tracks total bytes shared via hotspot using Android's `NetworkStats.Bucket.UID_TETHERING`. If an OEM ROM does not record tethering UID counters, displays `"Hotspot stats not supported on this phone"`.

### 2. Plan Setup & Accurate Billing Reset
- **Custom Allowance**: Configure monthly data limit in MB or GB.
- **Exact Billing Reset Day**: Set cycle reset day (1st–31st); usage resets strictly at midnight on that day each month.
- **Daily Budget**: Set a fixed target or let the app auto-calculate from remaining allowance and remaining days.
- **Separate Hotspot Cap**: Optional standalone cap for tethered devices.
- **Spike Alert Threshold**: Configure instantaneous usage jump limit (default: 100 MB in 15 minutes).

### 3. Background Alerts (WorkManager & Notifications)
- **Threshold Alerts**: Periodic notifications at 50%, 75%, 90%, and 100% of your plan allowance.
- **Spike Detection**: WorkManager samples network deltas every 15 minutes; if a jump exceeds the threshold, identifies and names the top consuming app.
- **Hotspot Reminder**: Detects active tethering byte increases and alerts with a direct shortcut to Wi-Fi Hotspot settings.
- **Android 13+ Ready**: Requests runtime `POST_NOTIFICATIONS` permission with graceful fallbacks.

### 4. Per-App Usage Breakdown
- **Foreground vs. Background**: Visual progress bars breaking down active screen-time usage vs. background data drain.
- **Multi-Network Filters**: Filter by Mobile Data, Wi-Fi, or Combined.
- **Sorting**: Sort by Total Usage, Foreground, or Background.
- **Direct System Settings**: 1-click shortcut to open any app's App Info page to restrict background data or modify permissions.

### 5. Real-Time Speed Monitor & Home Screen Widget
- **Live Speed Notification**: Toggleable persistent notification showing real-time download and upload speeds (e.g., `↓ 2.4 MB/s ↑ 180 KB/s`) sampled via `TrafficStats`.
- **Home Screen Widget**: Custom AppWidget showing remaining data, daily usage, and billing reset countdown right on your home launcher.

### 6. History & Carrier Bill CSV Export
- **Daily & Monthly Charts**: Canvas-rendered bar charts comparing Mobile vs. Wi-Fi consumption over time.
- **Export CSV Report**: Generates timestamped `.csv` file via Android `FileProvider` so you can verify carrier bills and track discrepancies.

### 7. Edge Cases & OEM Battery Protection
- **Usage Access Guard**: Onboarding card explaining why Android requires Usage Access, launching `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- **OEM Battery Killers**: Detects Xiaomi, Redmi, Poco, Oppo, Vivo, Samsung, Huawei, and OnePlus devices and alerts the user to grant Unrestricted battery access so background alert workers aren't terminated.
- **Boot Persistence**: `BootReceiver` restores `WorkManager` monitoring and restarts the Live Speed service on device reboot (`ACTION_BOOT_COMPLETED`).

---

## 🛠️ Tech Stack & Architecture
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Local Database**: Room 2.7.0 with KSP (Kotlin Symbol Processing)
- **Background Jobs**: AndroidX WorkManager (CoroutineWorker)
- **Preferences**: SharedPreferences / DataStore
- **System APIs**: `NetworkStatsManager`, `TrafficStats`, `PowerManager`, `AppOpsManager`, `AppWidgetProvider`
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin 9.1.1 (JDK 21, minSdk 26, targetSdk 36)

---

## 🚀 Running in Android Studio

1. **Clone / Open the Project**:
   - Open Android Studio (Ladybug / Iguana or newer with JDK 21 support).
   - Select **File > Open** and select the root directory of this project.

2. **Gradle Sync**:
   - Android Studio will automatically sync Gradle dependencies using `libs.versions.toml`.

3. **Run on Device or Emulator**:
   - Select an emulator or physical device running Android 8.0 (API 26) or higher.
   - Click the green **Run (▶)** button (or press `Shift + F10`).

4. **Grant Usage Access**:
   - On first launch, tap **"Enable Usage Access in Settings"** on the Dashboard banner.
   - Select **DataGuard** in the system list and toggle **Allow usage tracking**.
   - Return to DataGuard — your actual mobile, Wi-Fi, and hotspot counters will populate automatically!