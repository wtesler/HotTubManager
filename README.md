# HotTubManager

A **100% offline** Android application built specifically for managing the **LS450DX 7-Person 22-Jet 110V Plug-and-Play Spa with Waterfall**.

Every maintenance cadence, cartridge setting, filter-cycle programming sequence, water-chemistry dose, and safety rule is tailored to the LS450DX — with local Room/DataStore persistence, exact/fallback `AlarmManager` reminders, WorkManager safety-net reconciliation, and zero cloud dependencies.

---

## Key Features

- **First-Run Onboarding**
  - Guides setup for `POST_NOTIFICATIONS` and optional `SCHEDULE_EXACT_ALARM` ("Alarms & Reminders").
  - Configures preferred daily reminder time (default `9:00 AM`) and advance notice (`1 day` default).
  - Initializes either a **Fresh Fill Today** (launches the 7-step Startup Sequence) or an **Already Running Tub** (backdates each of the 6 recurring maintenance tasks).

- **Maintenance Dashboard & Interactive Checklists**
  - Displays overall tub status, active Fill Cycle day counter (e.g., `Day 1 of ~120`), active countdown timers, and active startup/24h follow-up banners.
  - Tracks the 6 recurring LS450DX schedules sorted by urgency:
    1. **Replace Blue Mineral Cartridge** — Every 4 months (`Setting 6`, `JETS OFF`)
    2. **Replace Silver Chlorine Cartridge** — Every 4 weeks / 28 days (`Setting 2`, `JETS OFF`)
    3. **Monthly Shock Packet** — Every 1 month (`JETS RUNNING`, `COVER OPEN`, embedded 20-min timer)
    4. **Clean Filter** — Every 1 month (`GARDEN HOSE`, `JETS OFF`)
    5. **Drain & Refill Hot Tub** — Every 4 months (`UNPLUG 110V`, empty with pump, microfiber shell & cover wipe -> transitions to Startup Wizard)
    6. **Test Water Chemistry** — Every 1 week (`TEST STRIP`, `TA -> pH -> CH`, Chemistry logger link + 15-min mix timer)
  - Supports gated safety checklists, **Complete Today**, **Past Date** backdating, and **Snooze / Reschedule**.

- **Resumable 7-Step Startup & Refill Wizard**
  - Persists step progress and sub-checkboxes in Room across app restarts.
  - Includes the **F1 = 2 hr / F2 = 2 hr** Light-button walkthrough + **15-second hands-off save timer**, strict sequential **TA -> pH -> Calcium Hardness** balancer, **30-minute Startup Shock** timer (`JETS HIGH`, `AIR VALVES CLOSED`, `COVER OPEN`), cartridge insertion check (`Setting 6` & `2`, `JETS OFF`), and automatic **24-hour post-startup filter clean** alarm scheduling.

- **Water Chemistry Logger & Quick Timers**
  - Quick-launch timers: `15m Chem Mix`, `20m Monthly Shock`, `30m Startup Shock`, and `15s F1/F2 Save` (backed by DataStore, a silent chronometer notification, and an `AlarmManager` completion alert).
  - Test strip logger for **TA (`80–120 ppm`)**, **pH (`7.2–7.8`)**, **CH (`150–250 ppm`)**, Free Chlorine, and Water Temp with live range meters, strict-order dosing guidance (`3 tbsp` / `1–2 tsp`), and Fill Cycle history.

- **LS450DX Guide, Searchable History & Notification Settings**
  - Offline hardware quick-reference card, searchable/filterable Maintenance History log, reminder/alarm settings, and a **Send Test Notification** button.

---

## Tech Stack & Architecture

- **Language & Build**: Kotlin (AGP `9.5.2` built-in Kotlin), Gradle `9.6.0`, KSP `2.3.14`, `minSdk = 33`, `compileSdk = 37`, `targetSdk = 37`
- **UI**: Jetpack Compose Material 3 (Edge-to-Edge), Jetpack Navigation 3 (`navigation3-runtime`, `navigation3-ui`) with 4 multi-backstack tabs + `HistoryRoute`
- **Data**: Room (`2.8.5`), Preferences DataStore (`1.3.0`), `kotlinx-serialization-json`, manual `AppContainer` DI
- **Background Scheduling**: `AlarmManager` (`setExactAndAllowWhileIdle` with `setAndAllowWhileIdle` fallback), `BroadcastReceiver`s (`BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, snooze/dismiss actions), and periodic 12-hour WorkManager reconciliation (`MaintenanceReconcileWorker`)

### Package Layout (`app/src/main/java/tesler/will/hottubmanager/`)
```text
├── HotTubApplication.kt          # Seeds tasks, creates channels, schedules WorkManager & alarms
├── MainActivity.kt               # Edge-to-edge host + notification deep-link routing
├── di/AppContainer.kt            # Manual dependency injection container
├── domain/                       # Pure Kotlin models, LS450DX specs, calendar & chemistry engines
├── data/                         # Room DB (5 entities, 4 DAOs), DataStore prefs, repositories
├── scheduling/                   # NotificationHelper, AlarmScheduler, TimerManager, Receivers, Worker
└── ui/                           # Theme, Navigation 3 shell, Onboarding, Dashboard, Startup, Chemistry, Guide
```

---

## Building & Testing

### Prerequisites
- Android Studio / JDK 17+
- Android SDK Platform `37` (`android-37.0`)
- Physical device or emulator running Android 13 (API 33) or higher for instrumented tests

### Commands
```powershell
# Run JVM unit tests
.\gradlew.bat :app:testDebugUnitTest

# Run instrumented Room & WorkManager tests on a connected device/emulator
.\gradlew.bat :app:connectedDebugAndroidTest

# Build Debug and R8-optimized Release APKs
.\gradlew.bat :app:assembleDebug :app:assembleRelease
```

---

## Further Documentation
- [PLAN.md](PLAN.md) — Detailed product & technical implementation plan
- [AGENTS.md](AGENTS.md) — Quick-start context, domain rules, and codebase gotchas for AI coding agents
