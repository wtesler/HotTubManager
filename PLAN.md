# HotTubManager — Implementation Plan

A bespoke, 100% offline Android application tailored specifically for managing the **LS450DX 7-Person 22-Jet 110V Plug-and-Play Spa with Waterfall**.

---

## 1. Project Overview & Goals

- **Hardware-Specific Accuracy**: Every checklist, chemical target range, nudge dose, cartridge setting, filter cycle sequence, and safety rule is built specifically for the LS450DX workflow defined in [`app/AGENTS.md`](app/AGENTS.md).
- **Reliable Local Scheduling & Push Notifications**: Calendar-aware recurring intervals (4 months, 4 weeks, 1 month) and one-off timed follow-ups (24-hour post-startup filter cleaning, 15/20/30-minute treatment timers) run completely on-device and persist across reboots, timezone changes, and Doze mode.
- **Zero Cloud Dependencies**: All state, schedules, chemistry logs, and history are stored locally using Room and DataStore.
- **Outdoor / Wet-Hand Friendly UX**: Clean Material 3 UI with large touch targets, clear safety callout badges (`JETS OFF`, `COVER OPEN`, `AIR VALVES CLOSED`), and resumable step-by-step checklists.

---

## 2. Preserved LS450DX Domain Specifications

### 2.1 Recurring Maintenance Tasks

| Task | Interval | Calendar Math | Settings & Safety Callouts | Built-in Action |
| :--- | :--- | :--- | :--- | :--- |
| **Blue Mineral Cartridge** | Every 4 months | `LocalDate.plusMonths(4)` | Keep on **setting 6**. **Ensure jets are NOT running.** | Safety checklist + mark done |
| **Silver Chlorine Cartridge** | Every 4 weeks | `LocalDate.plusWeeks(4)` (28 days) | Keep on **setting 2**. **Ensure jets are NOT running.** | Safety checklist + mark done |
| **Monthly Shock Packet** | Every 1 month | `LocalDate.plusMonths(1)` | **Jets running** and **cover open** for **20 minutes**. | Integrated **20-min timer** |
| **Clean Filter** | Every 1 month | `LocalDate.plusMonths(1)` | **Garden hose**. **Ensure jets are NOT running.** | Safety checklist + mark done |
| **Refill Hot Tub** | Every 4 months | `LocalDate.plusMonths(4)` | **Unplug**, **empty with pump**, **wipe down shell & underside of cover with microfiber cloth**. | Drain checklist → launches **Startup Sequence** |

### 2.2 Startup / Fresh-Fill Sequence (7 Steps)

1. **Bypass Water Softener**
   - Verify water softener bypass is active before filling.
2. **Fill Water Directly into Filter Well**
   - Pull out top basket and push garden hose straight down into the open filter well.
   - Stop filling **halfway between the top rim and the grey filter ring**, above all jets.
3. **Plug In, Set Temperature & Configure Filter Cycles**
   - Plug in spa and set target water temperature.
   - Change factory defaults (`F1 = 1 hr`, `F2 = 30 min`) to **2 hours each**:
     1. Press and hold **Light** for **5 seconds** until `F1` appears.
     2. Press **Up / Down** to set `F1` to **2** (0.5–6.0 hr range, 30-min steps).
     3. Press **Light** once to save `F1` and switch to `F2`.
     4. Press **Up / Down** to set `F2` to **2**.
     5. Wait **15 seconds** without touching any buttons to save and return to water temp *(includes optional 15-second hands-off countdown button)*.
4. **Balance Water in Strict Order** *(enforced sequential flow)*
   - Dip test strip and adjust **one chemical at a time**.
   - Between every chemical addition: wait **15 minutes** with **jets running high** and **air valves closed** *(built-in 15-min circulation timer)*.
   - **Order 1 — Total Alkalinity (`80–120 ppm`)**:
     - Low → **Alkalinity Increaser** (nudge dose: **3 tablespoons**)
     - High → **pH Decreaser** (nudge dose: **1–2 teaspoons**)
   - **Order 2 — pH (`7.2–7.8`)**:
     - High → **pH Decreaser** (nudge dose: **1–2 teaspoons**)
     - Low → **Alkalinity Increaser** (nudge dose: **3 tablespoons**)
   - **Order 3 — Calcium Hardness (`150–250 ppm`)**:
     - Low → **Calcium Hardness Increaser** (nudge dose: **3 tablespoons**)
5. **Startup Shock Packet**
   - Add shock packet.
   - Run **jets on high** with **air valves/filter closed** for **30 minutes**. Keep **cover open**.
   - *(Built-in 30-minute countdown timer with completion alert — distinct from the 20-minute monthly shock.)*
6. **Insert Cartridges**
   - **Ensure jets are NOT running.**
   - Insert Blue Mineral Cartridge (**setting 6**) and Silver Chlorine Cartridge (**setting 2**).
   - No need to run jets afterwards.
   - *Completing Step 6 automatically schedules the 24-hour filter rinse notification and offers to reset the 4-month / 4-week / 1-month maintenance anchors to today.*
7. **Clean Filter After 24 Hours**
   - Scheduled automatically for `Step 6 completion + 24 hours` with a dedicated push notification.
   - Rinse with garden hose while ensuring **jets are NOT running**.

---

## 3. Product Features & Screen Architecture

The app uses a **4-tab bottom navigation bar** plus a **first-run onboarding flow** and modal sheets/detail views:

### 3.1 First-Run Onboarding
- Requests Android `POST_NOTIFICATIONS` permission with a clear explanation of why notifications are core to the app.
- Prompts to grant Android's **Alarms & Reminders** (`SCHEDULE_EXACT_ALARM`) permission for minute-accurate scheduled reminders and timer alerts (with a graceful fallback if skipped).
- Lets the user configure their **preferred daily reminder time** (default `9:00 AM`) and **advance notice** (default `1 day before`).
- Offers two initialization paths:
  1. **Starting a Fresh Fill Today** → Launches directly into the 7-step Startup Sequence.
  2. **Hot Tub Is Already Running** → Quick date-picker screen to enter the last completed date (or next due date) for each of the 5 maintenance tasks so reminders start on the right cadence immediately.

### 3.2 Tab 1: Dashboard (Home)
- **Tub Status Banner**: Summarizes current health ("All Caught Up", "1 Task Due Soon", or "Overdue Action Needed") plus current Fill Cycle age (e.g., *"Fill Cycle #1 — Day 42 of ~120"*).
- **Active Timer Strip**: Persistent banner if a 15-min, 20-min, or 30-min countdown timer is currently running, with live remaining time and quick open/cancel controls.
- **Active Startup / 24-Hour Follow-up Card**: Shown whenever a Startup Sequence is in progress or waiting on the 24-hour post-startup filter clean.
- **Maintenance Schedule List**: The 5 recurring tasks sorted by urgency (Overdue → Due Today → Upcoming):
  - Shows task title, next due date, relative countdown (*"In 5 days"*, *"Due today"*, *"2 days overdue"*), and key setting/safety pills (e.g., `Setting 6`, `Jets OFF`, `Cover Open 20m`).
  - Tapping a task opens its **Interactive Maintenance Checklist Sheet**:
    - Step-by-step safety checkboxes (e.g., *"Confirm jets are OFF"*, *"Verify setting 6"*).
    - Embedded **20-minute timer** for Monthly Shock.
    - **"Complete Today"** or **"Log Past Date"** (to backdate if you completed it yesterday).
    - **"Snooze / Reschedule"** option (e.g., +1 day, +3 days, or pick a date).
    - Optional note field.
    - For **Refill Hot Tub**, completing the Drain/Prep checklist (Unplug → Pump empty → Wipe shell & cover underside with microfiber cloth) transitions seamlessly into starting a fresh **Startup Sequence**.

### 3.3 Tab 2: Startup & Refill Wizard
- Interactive, persistent, resumable 7-step stepper that saves progress after every tap (survives closing the app between chemical wait periods and the 24-hour filter clean).
- **Step 3** includes the collapsible/interactive 5-step **F1/F2 = 2 hr** control panel walkthrough + a **15-second save timer**.
- **Step 4** provides a guided **Strict-Order Chemical Balancer**:
  - Locks pH until Total Alkalinity is confirmed in range (`80–120 ppm`), and locks Calcium Hardness until pH is confirmed in range (`7.2–7.8`), with a manual override toggle if the user ever needs it.
  - Lets the user enter test strip values, shows exact nudge doses (`3 tbsp` or `1–2 tsp`), logs each addition, and starts the **15-minute jets-high / air-closed timer** with 1 tap.
- **Step 5** embeds the **30-minute Startup Shock timer** (air valves closed, cover open).
- **Step 6** enforces the **Jets OFF** confirmation, reminds cartridge settings (`6` and `2`), schedules the **24-hour filter clean notification**, and syncs maintenance due dates.
- **Step 7** displays the countdown to the 24-hour mark and lets the user check off the post-startup filter clean to complete the Startup Sequence and archive the Fill Cycle start.

### 3.4 Tab 3: Water Chemistry & Timers
- **Quick-Launch Timers Panel**: One-tap timers for routine use outside the wizard:
  - `15 min` — Chemical Addition Wait (Jets high, air valves closed)
  - `20 min` — Monthly Shock (Jets running, cover open)
  - `30 min` — Startup Shock (Jets high, air valves closed, cover open)
  - `15 sec` — F1 / F2 Panel Save Wait
- **Water Test Strip Logger & Dosing Assistant**:
  - Input fields / quick sliders for **Total Alkalinity (ppm)**, **pH**, **Calcium Hardness (ppm)**, plus optional **Free Chlorine (ppm)** and **Water Temp (°F)**.
  - Visual target-band indicators (`80–120`, `7.2–7.8`, `150–250`).
  - Instant strict-order dosing recommendation ("Step 1: TA is 60 ppm (Low). Add 3 tbsp Alkalinity Increaser, run jets high with air valves closed for 15 min, then re-test before adjusting pH.").
  - Saves each reading to local history with timestamp and chemicals added.
- **Chemistry History List**: Reverse-chronological log grouped by fill cycle so trends are easy to spot.

### 3.5 Tab 4: Guide, History & Settings
- **LS450DX Quick Reference Card**:
  - Filter cycle setup (`F1/F2 = 2 hrs` via Light button sequence)
  - Cartridge settings (`Blue Mineral = 6`, `Silver Chlorine = 2`, `Jets OFF`)
  - Water chemistry target ranges & nudge doses
  - Safety checklist summary (Bypass softener, fill via filter well, jets off, cover open, unplug before draining)
- **Maintenance History Log**: Full searchable/filterable log of all completed maintenance tasks and past fill cycles, with notes.
- **Notification & App Settings**:
  - Preferred reminder time of day (TimePicker).
  - Advance notice selector (`None`, `1 day before`, `3 days before`, `1 week before`).
  - Overdue reminder frequency (`Daily`, `Every 2 days`, `Off`).
  - **"Send Test Notification"** button to verify notifications immediately.
  - Permission status indicators (`POST_NOTIFICATIONS` and `SCHEDULE_EXACT_ALARM` / "Alarms & Reminders") with one-tap jump to Android Settings to grant or adjust them.

---

## 4. Scheduling & Notification Architecture

Because reliable scheduling and notifications are an essential requirement, this subsystem will be built cleanly and defensively:

### 4.1 Calendar-Aware Date Engine
- Uses `java.time.LocalDate`, `LocalTime`, `ZonedDateTime`, `Period`, and `Duration` (natively supported on API 26+).
- True calendar arithmetic:
  - 4 months → `baseDate.plusMonths(4)` (handles 28/30/31-day months & leap years properly).
  - 1 month → `baseDate.plusMonths(1)`.
  - 4 weeks → `baseDate.plusWeeks(4)` (exact 28-day cycle for Silver Chlorine Cartridge).
  - 24-hour filter clean → `Instant.plus(Duration.ofHours(24))`.
- Injectable `Clock` interface for deterministic unit testing.

### 4.2 Notification Channels
1. `maintenance_reminders` (**Importance: HIGH**) — Due-today, advance-warning, and overdue maintenance alerts.
2. `startup_followups` (**Importance: HIGH**) — 24-hour post-startup filter clean reminder.
3. `active_timers` (**Importance: LOW / Silent**) — Ongoing countdown chronometer while a 15m/20m/30m timer is active.
4. `timer_alerts` (**Importance: HIGH** with sound & vibration) — Immediate alert when a chemical wait or shock timer finishes (e.g., *"30-min Startup Shock Complete — Safe to close cover!"*).

### 4.3 Reliable Delivery Strategy
- **Primary Trigger (`AlarmManager` with Exact Alarms)**:
  - Declares `android.permission.SCHEDULE_EXACT_ALARM` and prompts the user (in Onboarding and Settings via `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`) to allow exact alarms for minute-accurate calendar reminders and treatment timers.
  - Schedules wakeup alarms at the user's preferred `LocalTime` via `AlarmManager.setExactAndAllowWhileIdle` when `canScheduleExactAlarms()` is true, gracefully falling back to `setAndAllowWhileIdle` if permission is revoked or skipped.
  - Listens for `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` to automatically upgrade/reschedule alarms as soon as permission is granted.
- **Safety-Net Reconciliation (`WorkManager`)**:
  - A periodic `MaintenanceReconcileWorker` (every 12 hours) checks Room for any due/overdue tasks whose alarm may have been missed or dropped by aggressive OEM battery optimizers, posts any missed notification, and ensures the next alarm is scheduled.
- **System Event Resilience (`BroadcastReceiver`)**:
  - `ScheduleRescheduleReceiver` listens for:
    - `android.intent.action.BOOT_COMPLETED`
    - `android.intent.action.TIME_SET`
    - `android.intent.action.TIMEZONE_CHANGED`
    - `android.intent.action.MY_PACKAGE_REPLACED`
  - Re-reads Room & DataStore and re-registers all pending maintenance alarms, 24-hour startup follow-ups, and active timer completion alarms.
- **Interactive Notification Actions**:
  - Maintenance notification actions: **Open Checklist** (deep-links to the specific task's checklist sheet) and **Snooze 1 Day** (handled silently via `NotificationActionReceiver` without opening the UI).
  - Timer completion notification action: **Dismiss** / **Open App** (returns to active Startup step or Chemistry tab).

### 4.4 Background-Safe Countdown Timer Engine
- Stores active timer state (`timerId`, `label`, `instructions`, `endEpochMillis`, `totalDurationMillis`) in DataStore so timers survive process death.
- Posts an ongoing notification using `NotificationCompat.Builder.setUsesChronometer(true)` and `setChronometerCountDown(true)` so Android renders a smooth live countdown in the status bar with zero per-second wakeups.
- Schedules an `AlarmManager` completion trigger at `endEpochMillis` so the completion alert fires reliably even when the screen is off or the app is closed.

---

## 5. Technical Stack & Project Architecture

### 5.1 Current Project Configuration Notes
- **Namespace / App ID**: `tesler.will.hottubmanager`
- **Build Tooling**: AGP `9.5.2` (built-in Kotlin support — do **not** apply `org.jetbrains.kotlin.android`), Gradle `9.6.0`, Configuration Cache enabled.
- **SDK Levels**: `minSdk = 33` (Android 13+), `compileSdk = 37`, `targetSdk = 37` in [`app/build.gradle.kts`](app/build.gradle.kts). `java.time` and `POST_NOTIFICATIONS` are natively supported across all targeted versions.

### 5.2 Planned Dependencies (`gradle/libs.versions.toml`)
- **UI**: Jetpack Compose BOM (`2026.11.01`), `material3`, `material-icons-extended`, `ui-tooling-preview`, `activity-compose` (`1.13.0`), `lifecycle-viewmodel-compose` (`2.11.1`)
- **Plugins**: `org.jetbrains.kotlin.plugin.compose` (`2.4.21`), `org.jetbrains.kotlin.plugin.serialization` (`2.4.21`), `com.google.devtools.ksp` (`2.3.14`)
- **Navigation**: Jetpack Navigation 3 (`androidx.navigation3:navigation3-runtime:1.2.2`, `navigation3-ui:1.2.2`, `lifecycle-viewmodel-navigation3:2.11.1`)
- **Local Persistence**: Room (`2.8.5` — `room-runtime`, `room-ktx`, `room-compiler` via KSP), Preferences DataStore (`1.3.0`), `kotlinx-serialization-json` (`1.13.0`)
- **Scheduling**: WorkManager (`androidx.work:work-runtime-ktx:2.12.0`), `androidx.core:core-ktx` (`1.19.1`)
- **Testing**: JUnit 4, `kotlinx-coroutines-test` (`1.11.0`), `room-testing` (`2.8.5`), `work-testing` (`2.12.0`), Compose UI test (`ui-test-junit4`)
- **DI**: Lightweight manual `AppContainer` instantiated in `HotTubApplication` (avoids unnecessary Hilt/Dagger complexity for a single-module local app while keeping every dependency easily mockable/testable).

### 5.3 Package Structure (`tesler.will.hottubmanager`)

```text
tesler.will.hottubmanager/
├── HotTubApplication.kt          # Initializes AppContainer, NotificationChannels, & WorkManager
├── MainActivity.kt               # Edge-to-edge single-Activity host + deep-link intent handling
├── di/
│   └── AppContainer.kt           # Wires DB, Repositories, Clock, Scheduler, & TimerManager
├── data/
│   ├── db/
│   │   ├── HotTubDatabase.kt     # Room database + pre-population callback for the 5 LS450DX tasks
│   │   ├── Converters.kt         # LocalDate / Instant / Enum TypeConverters
│   │   ├── entity/               # MaintenanceTaskEntity, MaintenanceLogEntity, StartupSessionEntity,
│   │   │                         # WaterTestEntity, FillCycleEntity
│   │   └── dao/                  # DAOs exposing reactive Flow queries
│   ├── prefs/
│   │   └── UserPreferencesStore.kt  # DataStore for reminder time, advance days, active timer, onboarding
│   └── repository/
│       ├── MaintenanceRepository.kt
│       ├── StartupRepository.kt
│       └── ChemistryRepository.kt
├── domain/
│   ├── model/                    # Clean domain models & enums (TaskType, IntervalSpec, SafetyBadge)
│   ├── ls450dx/
│   │   └── Ls450dxSpecs.kt       # Single source of truth for LS450DX steps, ranges, doses, settings
│   ├── scheduler/
│   │   └── ScheduleCalculator.kt # Pure calendar interval math (4 mo, 4 wk, 1 mo, 24 hr)
│   └── chemistry/
│       └── WaterBalanceAdvisor.kt # Pure strict-order TA -> pH -> Calcium Hardness advisor
├── scheduling/
│   ├── NotificationHelper.kt     # Channel creation & NotificationCompat builders
│   ├── MaintenanceAlarmScheduler.kt  # AlarmManager scheduling & cancellation
│   ├── TimerManager.kt           # Active countdown state + chronometer notification + completion alarm
│   ├── MaintenanceAlarmReceiver.kt   # Fires due/advance/overdue & 24h startup notifications
│   ├── TimerCompleteReceiver.kt      # Fires timer finished alert
│   ├── NotificationActionReceiver.kt # Handles Snooze 1 Day / Dismiss actions
│   ├── ScheduleRescheduleReceiver.kt # Handles BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, UPDATE
│   └── MaintenanceReconcileWorker.kt # Periodic WorkManager safety net
└── ui/
    ├── theme/                    # Color, Typography, Shape, Theme (Spa Cedar & Aqua + Dynamic Color)
    ├── navigation/               # Navigation 3 routes, top-level tabs, deep links
    ├── components/               # Reusable SafetyBadge, CountdownPill, ChecklistItem, RangeMeter
    ├── onboarding/               # OnboardingScreen + ViewModel
    ├── dashboard/                # DashboardScreen, TaskChecklistSheet + ViewModel
    ├── startup/                  # StartupWizardScreen, DrainPrepSheet, Step cards + ViewModel
    ├── chemistry/                # ChemistryTimersScreen, TestStripSheet + ViewModel
    └── guide/                    # GuideAndSettingsScreen, HistoryScreen + ViewModel
```

### 5.4 Local Database Schema (Room)
1. **`maintenance_tasks`**: `id`, `taskType` (unique enum: `BLUE_MINERAL`, `SILVER_CHLORINE`, `MONTHLY_SHOCK`, `CLEAN_FILTER`, `REFILL_TUB`), `title`, `intervalAmount`, `intervalUnit` (`MONTHS`/`WEEKS`), `lastCompletedDate` (`LocalDate?`), `nextDueDate` (`LocalDate`), `snoozedUntilDate` (`LocalDate?`), `isEnabled`.
2. **`fill_cycles`**: `id`, `cycleNumber`, `startedAt` (`Instant`), `completedAt` (`Instant?`), `notes`.
3. **`maintenance_logs`**: `id`, `taskType`, `completedAt` (`Instant`), `fillCycleId` (`Long?`), `notes`.
4. **`startup_sessions`**: `id`, `fillCycleId`, `startedAt`, `currentStep` (`1..7`), `completedStepFlagsJson` (tracks every sub-checkbox for steps 1–7, including F1/F2 sub-steps and TA/pH/CH completion), `step6CompletedAt` (`Instant?`), `filterCleanDueAt` (`Instant?`), `isCompleted`.
5. **`water_tests`**: `id`, `testedAt`, `fillCycleId`, `totalAlkalinityPpm` (`Int?`), `ph` (`Double?`), `calciumHardnessPpm` (`Int?`), `freeChlorinePpm` (`Double?`), `waterTempF` (`Int?`), `chemicalsAddedSummary`, `notes`.

---

## 6. UI / UX Design Direction

- **Visual Theme**: Calm, tactile **"Thermal Spa"** Material 3 palette — deep slate/teal primary tones, warm cedar/amber accents for due-soon warnings, crisp coral/red for overdue & critical safety callouts, and calm aqua/emerald for "In Range" chemistry and completed tasks. Supports both **Light & Dark themes** plus optional Material You Dynamic Color.
- **Safety-First Callouts**: Critical hardware-protecting rules render as high-contrast icon chips directly on cards and checklists:
  - `JETS OFF` (Blue Mineral, Silver Chlorine, Filter Clean, Startup Step 6 & 7)
  - `COVER OPEN` (Startup Shock 30m, Monthly Shock 20m)
  - `AIR VALVES CLOSED` (Chemical Balancing 15m, Startup Shock 30m)
  - `UNPLUG FIRST` (Refill / Drain Prep)
  - `BYPASS SOFTENER` (Startup Step 1)
- **Edge-to-Edge & Accessibility**: Uses `enableEdgeToEdge()`, proper `Scaffold` padding and `WindowInsets`, minimum `48dp` touch targets, and clear content descriptions.

---

## 7. Phased Implementation Roadmap

### Phase 1: Project Setup, Dependencies, Theme & Navigation Shell
- Update [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`app/build.gradle.kts`](app/build.gradle.kts) with Compose, Material 3, Navigation 3, Room + KSP, WorkManager, DataStore, and Serialization.
- Update [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) with `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `MainActivity`, and Receivers.
- Create Material 3 theme (`ui/theme`), `HotTubApplication`, `MainActivity`, and 4-tab Navigation 3 scaffold.
- Verify clean Gradle build (`:app:assembleDebug`).

### Phase 2: Data Layer & Pure Domain Engines
- Implement `Ls450dxSpecs` containing all exact text, steps, ranges, doses, and settings from `AGENTS.md`.
- Implement `ScheduleCalculator` (calendar interval arithmetic) and `WaterBalanceAdvisor` (strict TA → pH → CH ordering and exact nudge doses).
- Implement Room entities, DAOs, `HotTubDatabase` (with pre-populated 5 recurring tasks), `UserPreferencesStore`, and Repositories.
- Write unit tests for `ScheduleCalculator` and `WaterBalanceAdvisor`.

### Phase 3: Scheduling, Notification & Timer Engine
- Implement `NotificationHelper` with the 4 notification channels and deep-link `PendingIntent`s.
- Implement `MaintenanceAlarmScheduler`, `MaintenanceAlarmReceiver`, `NotificationActionReceiver` (`Snooze 1 Day`), `ScheduleRescheduleReceiver` (`BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`), and `MaintenanceReconcileWorker`.
- Implement `TimerManager` and `TimerCompleteReceiver` with DataStore persistence and native countdown chronometer notifications.
- Write unit tests for alarm trigger timestamp calculations and snooze/reschedule behavior.

### Phase 4: Onboarding & Maintenance Dashboard
- Build `OnboardingScreen` (notification permission request, reminder time picker, Fresh Fill vs. Already Running setup).
- Build `DashboardScreen` (status banner, active timer strip, active 24h follow-up banner, sorted maintenance cards).
- Build `TaskChecklistSheet` (interactive safety checkboxes, embedded 20-min timer for Monthly Shock, Complete Today / Backdate / Snooze, Drain Prep transition for Refill).
- Render and inspect Compose Previews for Dashboard and Onboarding.

### Phase 5: Interactive 7-Step Startup & Refill Wizard
- Build `StartupWizardScreen` with resumable progress persisted in Room.
- Implement Step 3 interactive F1/F2 = 2 hr walkthrough + 15-second save timer.
- Implement Step 4 strict-order chemical balancer (TA → pH → CH) with inline test strip entry, exact nudge dose guidance, and 15-minute jets-high timer.
- Implement Step 5 (30-min shock timer, cover open, air valves closed) and Step 6 (cartridges setting 6 & 2, jets OFF check, auto-schedule 24-hour filter clean & optional maintenance anchor reset).
- Implement Step 7 (24-hour post-startup filter clean completion & Fill Cycle finalization).
- Render and inspect Compose Previews for the Wizard steps.

### Phase 6: Water Chemistry Logger & Quick Timers
- Build `ChemistryTimersScreen` with preset quick-launch timers (15m, 20m, 30m, 15s), active timer card, and water test strip logger with visual target-range meters and strict-order recommendation card.
- Build chemistry history list grouped by Fill Cycle.
- Render and inspect Compose Previews.

### Phase 7: Guide, History & Settings
- Build `GuideAndSettingsScreen` with LS450DX Quick Reference card, Maintenance History Log, and Notification Settings (time picker, advance notice, overdue frequency, exact alarm & notification permission controls, and **"Send Test Notification"**).
- Render and inspect Compose Previews.

### Phase 8: Testing, Verification & Polish
- Run full unit test suite (`:app:testDebugUnitTest`).
- Run Room migration/DAO and WorkManager instrumented tests (`:app:connectedDebugAndroidTest` if emulator/device is attached).
- Verify all Compose Previews render cleanly with zero warnings/clipping.
- End-to-end verification checklist on emulator/device:
  - Test notification fires immediately from Settings.
  - Timer counts down in notification shade while app is backgrounded and fires completion alert.
  - Completing Step 6 schedules the 24-hour filter clean reminder.
  - Completing each recurring task advances its next due date accurately (4 months, 4 weeks, 1 month).
  - Reschedule receiver preserves alarms across reboot (`adb shell am broadcast -a android.intent.action.BOOT_COMPLETED` / device reboot).

---

## 8. Verification & Testing Plan

- **Unit Tests (`app/src/test`)**:
  - `ScheduleCalculatorTest`: Month-end clamping (e.g., Oct 31 + 4 months → Feb 28/29), 4-week exact 28-day steps, advance-notice date math, overdue detection, 24-hour post-startup timestamp.
  - `WaterBalanceAdvisorTest`: Strict priority ordering (TA always first even if pH and CH are out of range; pH second only when TA is 80–120; CH third only when TA & pH are in range) and exact nudge doses (`3 tbsp`, `1–2 tsp`).
  - `StartupRepositoryTest`: Step progression, 24-hour filter clean scheduling upon Step 6 completion, maintenance schedule anchoring.
- **Instrumented / Integration Tests (`app/src/androidTest`)**:
  - Room DAO CRUD & pre-population verification (`HotTubDatabaseTest`).
  - `MaintenanceReconcileWorkerTest` using `TestListenableWorkerBuilder`.
- **UI Verification**:
  - Compose `@Preview` rendering across Light/Dark themes and compact/expanded phone sizes.

---

## 9. Confirmed Decisions

1. **SDK Levels**: `minSdk = 33` (Android 13+), `compileSdk = 37`, `targetSdk = 37` (applied in [`app/build.gradle.kts`](app/build.gradle.kts)).
2. **Exact Alarms Permission**: Request `SCHEDULE_EXACT_ALARM` ("Alarms & Reminders") for minute-accurate scheduled reminders and timer alerts, with automatic fallback to `setAndAllowWhileIdle` + `WorkManager` reconciliation if revoked or skipped.
3. **v1 Feature Scope**:
   - **Included**: Interactive Maintenance Dashboard & Checklists, 7-Step Startup & Refill Wizard, Water Chemistry Test-Strip Logger & Strict-Order Dosing Advisor, Background Countdown Timers, LS450DX Quick Reference Guide, Maintenance & Fill-Cycle History Log, and Notification/Alarm Settings.
   - **Excluded**: Supplies Inventory Tracker and Local JSON Export/Import Backup.
