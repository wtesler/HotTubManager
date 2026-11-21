package tesler.will.hottubmanager.domain.model

import java.time.LocalDate

enum class TaskType(val stableId: Int) {
    BLUE_MINERAL(1),
    SILVER_CHLORINE(2),
    MONTHLY_SHOCK(3),
    CLEAN_FILTER(4),
    REFILL_TUB(5),
    TEST_WATER_CHEMISTRY(6)
}

enum class IntervalUnit {
    MONTHS,
    WEEKS
}

enum class SafetyBadge(val label: String, val isWarning: Boolean = false) {
    JETS_OFF("JETS OFF", isWarning = true),
    JETS_HIGH("JETS HIGH", isWarning = false),
    COVER_OPEN("COVER OPEN", isWarning = true),
    AIR_VALVES_CLOSED("AIR VALVES CLOSED", isWarning = true),
    UNPLUG_FIRST("UNPLUG FIRST", isWarning = true),
    BYPASS_SOFTENER("BYPASS SOFTENER", isWarning = true),
    SETTING_6("SETTING 6", isWarning = false),
    SETTING_2("SETTING 2", isWarning = false),
    GARDEN_HOSE("GARDEN HOSE", isWarning = false),
    MICROFIBER_WIPE("MICROFIBER CLOTH", isWarning = false),
    TEST_STRIP("TEST STRIP", isWarning = false),
    STRICT_ORDER("TA -> pH -> CH", isWarning = false)
}

enum class OverdueFrequency(val label: String, val intervalDays: Int?) {
    DAILY("Daily", 1),
    EVERY_TWO_DAYS("Every 2 days", 2),
    OFF("Off", null)
}

enum class TimerPreset(
    val id: String,
    val title: String,
    val durationMillis: Long,
    val instructions: String,
    val completionTitle: String,
    val completionBody: String
) {
    CHEMICAL_WAIT_15M(
        id = "timer_15m_chemical",
        title = "15-Min Chemical Mix",
        durationMillis = 15 * 60 * 1000L,
        instructions = "Run jets on HIGH with air valves CLOSED for 15 minutes before re-testing.",
        completionTitle = "15-Minute Circulation Complete",
        completionBody = "Dip a fresh test strip to check your water before adding the next chemical."
    ),
    MONTHLY_SHOCK_20M(
        id = "timer_20m_monthly_shock",
        title = "20-Min Monthly Shock",
        durationMillis = 20 * 60 * 1000L,
        instructions = "Keep JETS RUNNING and COVER OPEN for 20 minutes.",
        completionTitle = "20-Minute Monthly Shock Complete",
        completionBody = "Monthly shock circulation finished. You may close the spa cover."
    ),
    STARTUP_SHOCK_30M(
        id = "timer_30m_startup_shock",
        title = "30-Min Startup Shock",
        durationMillis = 30 * 60 * 1000L,
        instructions = "Run JETS ON HIGH with AIR VALVES CLOSED and COVER OPEN for 30 minutes.",
        completionTitle = "30-Min Startup Shock Complete",
        completionBody = "Safe to close cover and proceed to Step 6 (turn jets OFF before inserting cartridges)."
    ),
    FILTER_SAVE_15S(
        id = "timer_15s_panel_save",
        title = "15-Sec F1/F2 Panel Save",
        durationMillis = 15 * 1000L,
        instructions = "Do NOT touch any control panel buttons for 15 seconds to save F2 and return to water temp.",
        completionTitle = "Filter Cycle Settings Saved",
        completionBody = "15-second hands-off wait complete. Panel should now display water temperature."
    )
}

enum class UrgencyLevel(val priority: Int) {
    OVERDUE(0),
    DUE_TODAY(1),
    DUE_SOON(2),
    UPCOMING(3)
}

data class ChecklistStepSpec(
    val id: String,
    val label: String,
    val detail: String? = null,
    val critical: Boolean = false
)

data class TaskSpec(
    val taskType: TaskType,
    val title: String,
    val shortTitle: String,
    val intervalLabel: String,
    val intervalAmount: Int,
    val intervalUnit: IntervalUnit,
    val summary: String,
    val badges: List<SafetyBadge>,
    val checklistSteps: List<ChecklistStepSpec>,
    val linkedTimer: TimerPreset? = null
)

data class TaskStatus(
    val id: Long,
    val spec: TaskSpec,
    val lastCompletedDate: LocalDate?,
    val nextDueDate: LocalDate,
    val snoozedUntilDate: LocalDate?,
    val effectiveDueDate: LocalDate,
    val daysUntilDue: Long,
    val urgency: UrgencyLevel,
    val isEnabled: Boolean
)
