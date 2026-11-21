# AGENTS.md — HotTubManager

Operational handbook for AI coding agents working on **HotTubManager**, a 100% offline Android app tailored to the **LS450DX 7-Person 22-Jet 110V Plug-and-Play Spa with Waterfall**. See [PLAN.md](PLAN.md) for the full original design specification.

---

## 1. Environment & Build Configuration

- **Package / App ID**: `tesler.will.hottubmanager`
- **SDK Levels**: `minSdk = 33` (Android 13+), `compileSdk = 37`, `targetSdk = 37` ([app/build.gradle.kts](app/build.gradle.kts))
- **Tooling**: AGP `9.5.2` (built-in Kotlin — do **not** apply `org.jetbrains.kotlin.android`), Gradle `9.6.0` (configuration cache enabled in [gradle.properties](gradle.properties)), Kotlin Compose & Serialization plugins `2.4.21`, KSP `2.3.14` ([gradle/libs.versions.toml](gradle/libs.versions.toml))
- **DI**: Manual `AppContainer` instantiated in `HotTubApplication` (no Hilt/Dagger)

### Build & Test Commands
Run from `W:\AndroidProjects\HotTubManager` (or via Android Studio Gradle tools):
```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleRelease
# WARNING: Do NOT run :app:connectedDebugAndroidTest against a user's physical device
# with live app data — AGP uninstalls `tesler.will.hottubmanager` after instrumentation
# tests finish, wiping Room DB & DataStore data! Only run on a disposable emulator or
# when explicitly requested.
.\gradlew.bat :app:connectedDebugAndroidTest
```

### Known Tooling & Codebase Gotchas
0. **Preserving User Data on Device**: Installing/deploying the debug app in-place (`studio_deploy` / Android Studio Run) preserves Room and DataStore data. Running `:app:connectedDebugAndroidTest` uninstalls `tesler.will.hottubmanager` after completion and wipes all local data — prefer JVM unit tests (`:app:testDebugUnitTest`) when verifying changes on a connected personal device.
1. **Compose BOM / `compileSdk 37` Constraint**: Host SDK has `android-37.0` (not `37.1`). Keep `composeBom = "2026.08.00"`; newer BOMs (e.g., `2026.11.01`) pull `foundation-android:1.13.0` which requires `compileSdk >= 37.1`.
2. **Material Icons Extended**: Not included in recent Compose BOMs; declared explicitly in `libs.versions.toml` under group `androidx.compose.material` (`material-icons-extended:1.7.8`).
3. **Room Schema Exports**: KSP exports schemas to `app/schemas/` (`room.schemaLocation`). Update schema/migrations carefully if modifying Room entities.
4. **Navigation 3 & TopAppBar Insets**: `HotTubMainShell` applies `Modifier.consumeWindowInsets(innerPadding)` on `NavDisplay`. Any destination that renders its own `TopAppBar` (such as `HistoryRouteScreen` in `ui/guide/HistoryScreen.kt`) **must** pass `windowInsets = WindowInsets(0)` and apply `Modifier.padding(top = contentPadding.calculateTopPadding())`; otherwise `TopAppBar`'s internal `windowInsetsPadding` subtracts the consumed status-bar inset and draws under the status bar.
5. **Chemistry Empty State**: `WaterBalanceAdvisor.evaluate()` checks whether all three of TA, pH, and CH are `UNKNOWN` first and returns `Step 1: Test Total Alkalinity First` even when `manualOverrideOrder = true` (used by `ChemistryViewModel`).
6. **v1 Scope Exclusions**: Zero cloud/network features, no Supplies Inventory Tracker, and no JSON Export/Import Backup.

---

## 2. LS450DX Hardware & Domain Rules (Source of Truth: `domain/ls450dx/Ls450dxSpecs.kt`)

### Recurring Maintenance (`TaskType`)
| `TaskType` | Interval | Calendar Math | Required Settings & Safety Rules |
| :--- | :--- | :--- | :--- |
| `BLUE_MINERAL` | Every 4 months | `plusMonths(4)` | Dial **Setting 6**, **Jets OFF** (`JETS OFF`) |
| `SILVER_CHLORINE` | Every 4 weeks | `plusWeeks(4)` (28 days) | Dial **Setting 2**, **Jets OFF** (`JETS OFF`) |
| `MONTHLY_SHOCK` | Every 1 month | `plusMonths(1)` | **Jets RUNNING**, **Cover OPEN** (`COVER OPEN`), **20 min** timer |
| `CLEAN_FILTER` | Every 1 month | `plusMonths(1)` | Rinse with **Garden Hose**, **Jets OFF** (`JETS OFF`) |
| `REFILL_TUB` | Every 4 months | `plusMonths(4)` | **Unplug 110V**, empty with pump, wipe shell & cover underside with **microfiber cloth**, then launch 7-step Startup |
| `TEST_WATER_CHEMISTRY` | Every 1 week | `plusWeeks(1)` (7 days) | **Test Strip**, log readings in Chemistry tab, strict-order **TA -> pH -> CH**, **15 min** wait between additions |

### Strict-Order Water Balancing (`WaterBalanceAdvisor`)
1. **Total Alkalinity** (`80–120 ppm`) — Low: `3 tbsp` Alkalinity Increaser; High: `1–2 tsp` pH Decreaser.
2. **pH** (`7.2–7.8`) — Locked in Startup Step 4 until TA is in range. High: `1–2 tsp` pH Decreaser; Low: `3 tbsp` Alkalinity Increaser.
3. **Calcium Hardness** (`150–250 ppm`) — Locked in Startup Step 4 until TA & pH are in range. Low: `3 tbsp` Calcium Hardness Increaser.
- Wait **15 minutes** with **Jets HIGH** and **Air Valves CLOSED** between every chemical addition.

### 7-Step Startup Sequence (`StartupRepository` / `StartupWizardScreen`)
1. Engage home water softener bypass (`BYPASS SOFTENER`).
2. Pull top basket, push garden hose into open filter well, fill halfway between top rim and grey filter ring.
3. Plug in 110V spa, set temp, program `F1 = 2 hr` & `F2 = 2 hr` (hold Light 5s -> Up/Down to 2 -> Light once -> Up/Down to 2 -> wait **15s** hands-off save).
4. Strict-order TA -> pH -> CH balancing with inline test strip logger and 15m timer.
5. Startup Shock: **30 minutes** (`TimerPreset.STARTUP_SHOCK_30M`), **Jets HIGH**, **Air Valves/Filter CLOSED**, **Cover OPEN**.
6. Insert Blue Mineral (**Setting 6**) + Silver Chlorine (**Setting 2**) with **Jets OFF**. Completing Step 6 schedules the **24-hour** filter clean alarm (`ACTION_FILTER_24H_REMINDER`) and optionally resets recurring task anchors to today.
7. Clean filter after 24 hours with **Jets OFF** and garden hose; completes Startup Sequence.

---

## 3. Codebase Map (`app/src/main/java/tesler/will/hottubmanager/`)

- **App / DI**:
  - `HotTubApplication.kt` — Creates notification channels, schedules 12h `MaintenanceReconcileWorker`, seeds default tasks, reschedules alarms, restores running timer notification.
  - `MainActivity.kt` — `enableEdgeToEdge()`, dark/light scrim transparency, intent deep-link extraction (`EXTRA_NAV_TARGET`, `EXTRA_TASK_TYPE`).
  - `di/AppContainer.kt` — Dependency graph (`Clock`, `ScheduleCalculator`, `WaterBalanceAdvisor`, `HotTubDatabase`, `UserPreferencesStore`, `NotificationHelper`, `MaintenanceAlarmScheduler`, `TimerManager`, repositories).
- **Domain (`domain/`)** — Pure Kotlin, Android-free for fast JVM testing:
  - `model/DomainModels.kt` — `TaskType`, `SafetyBadge`, `IntervalUnit`, `TimerPreset`, `DueStatus`, `OverdueFrequency`.
  - `ls450dx/Ls450dxSpecs.kt` — Canonical specs for all recurring checklists, startup steps, F1/F2 walkthrough, chemistry targets/doses.
  - `scheduler/ScheduleCalculator.kt` & `AlarmScheduler.kt` — Calendar math (`plusMonths`/`plusWeeks`), advance-notice/overdue alarm calculation, scheduler interface.
  - `chemistry/WaterBalanceAdvisor.kt` — Strict-order dosing engine.
- **Data (`data/`)**:
  - `db/HotTubDatabase.kt`, `Converters.kt`, `dao/*`, `entity/*` — 5 Room tables (`maintenance_tasks`, `fill_cycles`, `maintenance_logs`, `startup_sessions`, `water_tests`).
  - `prefs/UserPreferencesStore.kt` — Preferences DataStore (onboarding flag, reminder `LocalTime`, advance notice days, overdue frequency, `ActiveTimerState`).
  - `repository/MaintenanceRepository.kt` (implements `MaintenanceScheduleAnchorer`), `StartupRepository.kt`, `ChemistryRepository.kt`.
- **Scheduling & Notifications (`scheduling/`)**:
  - `NotificationHelper.kt` — 4 channels (`maintenance_reminders`, `startup_followups`, `active_timers`, `timer_alerts`), deep-link intents, `Snooze 1 Day` / `Dismiss` actions, `sendTestNotification()`.
  - `MaintenanceAlarmScheduler.kt` — Uses `AlarmManager.setExactAndAllowWhileIdle` when `canScheduleExactAlarms()` is true, falling back to `setAndAllowWhileIdle`.
  - `TimerManager.kt` — Persists `ActiveTimerState` in DataStore, posts silent chronometer ongoing notification, schedules completion alarm.
  - `MaintenanceAlarmReceiver.kt`, `TimerCompleteReceiver.kt`, `NotificationActionReceiver.kt`, `ScheduleRescheduleReceiver.kt` (whitelists `BOOT_COMPLETED`, `TIME_CHANGED`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`), `MaintenanceReconcileWorker.kt`.
- **UI (`ui/`)**:
  - `navigation/NavRoutes.kt` & `HotTubNavigationScaffold.kt` — Navigation 3 multi-backstack tabs (`DashboardRoute`, `StartupRoute`, `ChemistryRoute`, `GuideRoute`) + `HistoryRoute` sub-route + first-run `OnboardingRouteScreen`.
  - `onboarding/`, `dashboard/`, `startup/`, `chemistry/`, `guide/`, `components/`, `theme/`.

---

## 4. Test Suite

- **JVM Unit Tests (`app/src/test/.../`)**:
  - `ScheduleCalculatorTest.kt` — Month-end clamping, 4-week 28-day cadence, advance notice, overdue checks, 24h follow-up timing.
  - `WaterBalanceAdvisorTest.kt` — Empty input start step, strict TA -> pH -> CH priority, manual override mode, exact nudge doses.
  - `StartupRepositoryTest.kt` — Step progression, Step 6 -> 24h alarm scheduling & optional anchor reset, Step 7 completion.
  - `MaintenanceRepositoryTest.kt` — `snoozeTaskUntil` earlier & later than `nextDueDate`, non-destructive backfill seeding.
- **Instrumented Tests (`app/src/androidTest/.../`)**:
  - `HotTubDatabaseTest.kt` — Pre-populated 6 LS450DX tasks, fill cycle & log CRUD, water test history queries.
  - `MaintenanceReconcileWorkerTest.kt` — WorkManager execution via `TestListenableWorkerBuilder`.
