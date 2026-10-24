# DataGuard 🛡️

**A private, on-device Android app that tracks your mobile data, Wi-Fi and hotspot usage, forecasts when your plan will run out, and warns you before it does.**

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Privacy](https://img.shields.io/badge/data-100%25%20on--device-success)

---

## Table of contents

- [Download](#download)
- [Overview](#overview)
- [Features](#features)
- [How it works](#how-it-works)
- [Permissions](#permissions)
- [Privacy](#privacy)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Install the APK](#install-the-apk)
- [Build from source](#build-from-source)
- [Default settings](#default-settings)
- [Known limitations](#known-limitations)
- [License](#license)

---

## Download

| | |
|---|---|
| **Latest release** | [github.com/percentageup-oss/DataGuard/releases/latest](https://github.com/percentageup-oss/DataGuard/releases/latest) |
| **Direct APK (v0.1.0)** | [app-debug.apk](https://github.com/percentageup-oss/DataGuard/releases/download/v0.1.0/app-debug.apk) |
| **Size** | 16.7 MB |
| **SHA-256 (v0.1.0)** | `6a320afc052d79dc42eb59557e26925c14dc5b9ad123e798ba085efbccdb8e35` |

> The published file is a **debug build**, signed with a debug key. It is fine for testing and personal use, but it is not a Play Store release build. See [Install the APK](#install-the-apk) for setup steps.

---

## Overview

Mobile plans have a monthly allowance, a billing day, and sometimes surprise overages. DataGuard reads the counters Android already keeps about network traffic, saves a daily history of them on your phone, and turns that into:

- a live view of how much of your plan you've used this billing cycle,
- a forecast of the day you'll run out at your current pace,
- alerts at 50 / 75 / 90 / 100 % of your allowance and when usage suddenly spikes,
- a per-app breakdown (foreground vs. background),
- a hotspot (tethering) counter, and
- a home-screen widget plus an optional live speed meter in the notification shade.

Everything runs locally. There are no accounts, no servers, no ads and no analytics code in the app.

---

## Features

### Dashboard
- **Progress ring**: percentage of your monthly allowance used, with animated sweep and status colour (green below 75 %, orange from 75 %, red from 90 %). Shows used / total and remaining data.
- **Usage forecast**: "At this pace you'll run out on *date*", or a "safe" message if your current pace covers the whole cycle. Also shows your average daily pace and days until reset.
- **Metric tiles**: today's mobile usage, today's Wi-Fi usage, daily budget, and a SIM status tile.
- **Hotspot & tethering card**: data shared through the hotspot this cycle (optionally shown against a hotspot cap).
- **Live speed switch** and a pull-to-refresh / refresh button.
- **Onboarding banners** for the Usage Access permission and, on some phone brands, battery optimisation.

### Apps (per-app usage)
- Per-app mobile usage for the current billing cycle, split into **foreground** and **background** with a coloured bar.
- Filter by **Mobile**, **Wi-Fi** or **Combined**; sort by **Total**, **Foreground** or **Background**.
- Search by app name or package name.
- One-tap shortcut to an app's system *App info* page, where you can restrict its background data.

### History
- Daily snapshots stored in a local Room database: mobile, Wi-Fi and hotspot totals plus the day's top mobile-data app.
- Bar chart comparing mobile vs. Wi-Fi, with **Daily** and **Monthly** views.
- **Export CSV** report (see [Known limitations](#known-limitations)) for checking your carrier's bill.

### Plan & alerts
- Monthly allowance in **GB or MB**.
- **Billing reset day** (1st to 31st). Usage for the cycle is counted from 00:00 on that day, and short months are handled by clamping to the last day.
- Optional **daily budget** (MB). Leave it empty and DataGuard calculates one from the remaining allowance and remaining days.
- Optional **hotspot cap** (MB).
- **Spike alert threshold** (MB since the last background check).
- Notification-permission prompt on Android 13+.

### Background monitoring
- A periodic WorkManager job runs about every 15 minutes: it saves today's snapshot, checks alert conditions and refreshes the widget.
- A boot receiver re-schedules monitoring after a reboot or app update and restarts the live speed meter if it was on.

### Live speed meter
- Optional ongoing notification showing real-time download and upload speed, for example `↓ 2.4 MB/s  ↑ 180 KB/s`, sampled every 1.5 seconds.
- Includes a **Turn Off** action in the notification.

### Home-screen widget
- 3×2 resizable widget showing data left, data used today, days until reset and a progress bar. Tapping it opens the app.
- Refreshes every 30 minutes and after each background check.

### Phone-brand battery protection
- On Xiaomi, Redmi, Poco, Oppo, Vivo, Realme, Samsung, Huawei and OnePlus devices, the dashboard shows a banner asking you to set DataGuard to **Unrestricted** battery use so background alerts aren't killed.

### Theme
- Light and dark themes that follow the system setting.

---

## How it works

### Where the numbers come from
DataGuard uses Android's `NetworkStatsManager`, the same counters the system Settings app reads. This needs the **Usage Access** special permission (`PACKAGE_USAGE_STATS`), which you grant manually in system settings. No root is required.

| Metric | Source |
|---|---|
| Mobile and Wi-Fi totals | `querySummary` for mobile and Wi-Fi networks over a time range |
| Per-app usage, foreground vs. background | `queryDetails`, split on the bucket state |
| Hotspot / tethering | `queryDetailsForUid` using `NetworkStats.Bucket.UID_TETHERING` on the mobile network |
| Live speed | `TrafficStats` total received / sent bytes (all networks, so mobile and Wi-Fi combined) |

### Billing cycle
The cycle starts at midnight on your reset day. If today is on or after that day, the cycle began this month; otherwise it began last month. Reset days beyond a month's length (for example 31 in February) are clamped.

### Forecast
```
average pace   = mobile bytes used this cycle ÷ days elapsed
days until dry = remaining allowance ÷ average pace
```
If that is shorter than the days left in the cycle, DataGuard shows the projected run-out date. Otherwise it reports that you are safe for the cycle.

### Daily budget
Your own daily target if you set one, otherwise `remaining allowance ÷ days left in the cycle`.

### Alerts

| Alert | Trigger | Notification channel |
|---|---|---|
| **Plan threshold** | Cycle mobile usage reaches 50 %, 75 %, 90 % or 100 % of the allowance. Each level is sent once per month. | *Usage & Spike Alerts* (high importance) |
| **Spike** | Mobile usage grew by at least your spike threshold since the previous background check. The alert names the top app in that window. | *Usage & Spike Alerts* (high importance) |
| **Hotspot active** | Tethered data grew by more than 5 MB since the previous check. Includes a button that opens your phone's wireless settings. | *Hotspot Reminders* (default importance) |
| **Live speed** | Ongoing while the meter is switched on. | *Real-time Speed Monitor* (low importance) |

Background checks are scheduled with WorkManager at a 15-minute interval with a 5-minute flex window. Android, Doze mode and some phone brands can delay them.

### Long-term history
Android only keeps network counters for a limited time. DataGuard writes one row per day (`yyyy-MM-dd`) to its own Room database (`dataguard.db`) and updates today's row on every background check and refresh, so your history outlives the system counters. Rows are not deleted automatically.

---

## Permissions

| Permission | Used for |
|---|---|
| `PACKAGE_USAGE_STATS` | Reading network statistics. Granted through **Usage Access** in system settings. |
| `POST_NOTIFICATIONS` | Plan, spike and hotspot alerts, and the live speed meter (runtime prompt on Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | Restoring background monitoring after reboot or app update. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | The live speed notification service. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Letting you exempt DataGuard from battery optimisation. |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `READ_PHONE_STATE` | Declared in the manifest, but the current source code does not use them (it makes no network requests and does not read SIM or phone state). |

---

## Privacy

- **On-device only.** Usage history, plan settings and alert state are stored locally (Room database and `SharedPreferences`).
- **No accounts, no cloud backend, no analytics, no ads.** The source contains no networking code.
- **CSV exports** are written to the app's cache folder and only leave the phone when you choose to share them.
- The app has Android's standard Auto Backup enabled (`allowBackup="true"`), so if device backup is turned on, your phone's OS may include DataGuard's local data in its backup.

---

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2024.09.00), Material 3, Material Icons Extended |
| Architecture | Single-activity, MVVM (`AndroidViewModel` + `StateFlow`), repository layer |
| Database | Room 2.7.0 with KSP |
| Background work | WorkManager 2.9.1 (`CoroutineWorker`), foreground service, `BroadcastReceiver`, `AppWidgetProvider` |
| Settings storage | `SharedPreferences` (the DataStore dependency is declared but not currently used) |
| System APIs | `NetworkStatsManager`, `TrafficStats`, `AppOpsManager`, `PowerManager`, `FileProvider` |
| Build | Gradle 9.3.1, Android Gradle Plugin 9.1.1, JDK 21, version catalog (`gradle/libs.versions.toml`) |
| SDK levels | `minSdk` 26 (Android 8.0), `targetSdk` / `compileSdk` 36 |

App identity: namespace `com.example.dataguard`, application ID `com.aistudio.dataguard.kxmpzq`, version `1.0` (code 1).

---

## Project structure

```
DataGuard/
├── build.gradle.kts            # Root build file (plugin declarations)
├── settings.gradle.kts         # Repositories and module list (:app)
├── gradle/libs.versions.toml   # Dependency and plugin versions
└── app/
    ├── build.gradle.kts        # Android module config, signing, dependencies
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                # Icons, widget layout and info, FileProvider paths, themes
        └── java/com/example/dataguard/
            ├── DataGuardApp.kt         # Application: DB, prefs, repository, channels, schedules worker
            ├── MainActivity.kt         # Compose host with bottom navigation
            ├── data/
            │   ├── local/              # AppDatabase, DailySnapshotDao, PlanPreferences
            │   ├── model/              # DailySnapshot, DataPlan, AppUsageItem
            │   ├── network/            # NetworkStatsHelper (all system-counter queries)
            │   └── repository/         # DataGuardRepository (cycle stats, forecast, CSV export)
            ├── receiver/               # BootReceiver
            ├── service/                # SpeedMonitorService (live speed notification)
            ├── widget/                 # DataGuardWidgetProvider
            ├── worker/                 # UsageMonitorWorker (periodic checks and alerts)
            └── ui/
                ├── components/         # ProgressRing, UsageBarChart, cards, banners
                ├── screens/            # Dashboard, TopApps, History, PlanSetup
                ├── theme/              # Colours and light/dark theme
                └── viewmodel/          # DataGuardViewModel
```

---

## Install the APK

1. On your phone, download [`app-debug.apk`](https://github.com/percentageup-oss/DataGuard/releases/download/v0.1.0/app-debug.apk).
2. Open the file. If Android asks, allow **Install unknown apps** for the browser or file manager you used. Google Play Protect may also show a warning for sideloaded debug builds.
3. Open **DataGuard**.
4. Tap **Enable Usage Access in Settings** on the Dashboard, choose **DataGuard**, and switch on **Allow usage tracking**. Return to the app and your mobile, Wi-Fi and hotspot counters will fill in.
5. Open the **Plan** tab, enter your monthly allowance and billing reset day, and tap **Save Plan Settings**. On Android 13+, also tap **Enable Notifications**.
6. If you see a battery banner on the Dashboard, tap **Allow Unrestricted Battery**.
7. Optional: switch on **Live Speed Notification**, and add the DataGuard widget from your home screen's widget picker.

---

## Build from source

**Requirements:** a recent Android Studio release that supports AGP 9.1.1, JDK 21, and Android SDK Platform 36.

Two things are not included in the repository and are needed before the first build:

- **A signing keystore.** `app/build.gradle.kts` signs both build types with `debug.keystore` in the project root (alias `androiddebugkey`, password `android`). Create one in the project root:
  ```bash
  keytool -genkeypair -v -keystore debug.keystore -storepass android \
    -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  ```
- **The Gradle wrapper.** Android Studio can set up Gradle 9.3.1 for you. From the command line, run `gradle wrapper --gradle-version 9.3.1` once.

**In Android Studio:** open the project folder, wait for Gradle sync, choose a device or emulator running Android 8.0 or newer, and press **Run ▶**.

**From the command line:**
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Release builds use the same debug keystore, so they are not suitable for publishing to the Play Store. Create a proper release key before distributing widely.

---

## Default settings

| Setting | Default |
|---|---|
| Monthly allowance | 10 GB (10,240 MB) |
| Billing reset day | 1st of the month |
| Daily budget | Automatic |
| Hotspot cap | Off (2,048 MB suggested when you switch it on) |
| Spike alert threshold | 100 MB |
| Live speed meter | Off |

---

## Known limitations

- **CSV export may fail.** The `FileProvider` authority in the manifest is `com.example.dataguard.fileprovider`, but the code asks for `<applicationId>.fileprovider` (`com.aistudio.dataguard.kxmpzq.fileprovider`). Until the manifest uses `${applicationId}.fileprovider`, tapping **Export CSV** can show an "Export failed" message.
- **Hotspot cap is informational.** It is shown on the hotspot card, but no alert fires when it is exceeded.
- **SIM status tile is static.** The "Dual SIM Status" tile is a fixed label. Mobile usage is queried without a per-SIM filter, so there is no per-SIM breakdown.
- **Hotspot support message.** "Hotspot stats not supported on this phone" appears when your phone doesn't record tethering counters, and also when no tethering traffic has been recorded yet.
- **Alert resets follow the calendar month.** The 50 / 75 / 90 / 100 % alert flags reset on the 1st of each month, not on your billing reset day.
- **Daily chart ordering.** The daily chart takes the last 14 entries of a newest-first list, so with more than 14 days of history it may show older days instead of the most recent 14.
- **Live speed counts all networks.** The meter shows combined mobile and Wi-Fi throughput, not mobile only.
- **Background timing.** Alerts depend on WorkManager, so they can be delayed by Doze mode or aggressive battery management.
- **Debug-signed build.** The published APK uses a debug signature.

---

## License

No license has been specified yet.
