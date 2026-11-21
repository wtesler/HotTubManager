package tesler.will.hottubmanager.domain.ls450dx

import tesler.will.hottubmanager.domain.model.ChecklistStepSpec
import tesler.will.hottubmanager.domain.model.IntervalUnit
import tesler.will.hottubmanager.domain.model.SafetyBadge
import tesler.will.hottubmanager.domain.model.TaskSpec
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.model.TimerPreset

object Ls450dxSpecs {

    const val MODEL_NAME = "LS450DX 7-Person 22-Jet 110V Plug-and-Play Spa"
    const val TYPICAL_FILL_CYCLE_DAYS = 120

    // Water Chemistry Target Ranges
    const val TA_MIN_PPM = 80
    const val TA_MAX_PPM = 120
    const val PH_MIN = 7.2
    const val PH_MAX = 7.8
    const val CH_MIN_PPM = 150
    const val CH_MAX_PPM = 250
    const val FREE_CHLORINE_MIN_PPM = 1.0
    const val FREE_CHLORINE_MAX_PPM = 5.0

    // Nudge Doses
    const val DOSE_ALKALINITY_INCREASER = "3 tablespoons"
    const val DOSE_PH_DECREASER = "1–2 teaspoons"
    const val DOSE_CALCIUM_INCREASER = "3 tablespoons"

    // Cartridge Settings
    const val BLUE_MINERAL_SETTING = 6
    const val SILVER_CHLORINE_SETTING = 2

    val recurringTasks: List<TaskSpec> = listOf(
        TaskSpec(
            taskType = TaskType.BLUE_MINERAL,
            title = "Replace Blue Mineral Cartridge",
            shortTitle = "Blue Mineral",
            intervalLabel = "Every 4 months",
            intervalAmount = 4,
            intervalUnit = IntervalUnit.MONTHS,
            summary = "Keep on setting 6. Ensure jets are NOT running while replacing.",
            badges = listOf(SafetyBadge.SETTING_6, SafetyBadge.JETS_OFF),
            checklistSteps = listOf(
                ChecklistStepSpec(
                    id = "blue_jets_off",
                    label = "Confirm jets are OFF (NOT running)",
                    detail = "Never remove or insert cartridges while jets are running.",
                    critical = true
                ),
                ChecklistStepSpec(
                    id = "blue_remove_old",
                    label = "Remove old Blue Mineral Cartridge from filter compartment"
                ),
                ChecklistStepSpec(
                    id = "blue_setting_6",
                    label = "Set new Blue Mineral Cartridge dial to setting 6",
                    detail = "LS450DX requires setting 6 for the Blue Mineral Cartridge.",
                    critical = true
                ),
                ChecklistStepSpec(
                    id = "blue_insert",
                    label = "Insert cartridge securely into holder"
                )
            )
        ),
        TaskSpec(
            taskType = TaskType.SILVER_CHLORINE,
            title = "Replace Silver Chlorine Cartridge",
            shortTitle = "Silver Chlorine",
            intervalLabel = "Every 4 weeks",
            intervalAmount = 4,
            intervalUnit = IntervalUnit.WEEKS,
            summary = "Keep on setting 2. Ensure jets are NOT running while replacing.",
            badges = listOf(SafetyBadge.SETTING_2, SafetyBadge.JETS_OFF),
            checklistSteps = listOf(
                ChecklistStepSpec(
                    id = "silver_jets_off",
                    label = "Confirm jets are OFF (NOT running)",
                    detail = "Never remove or insert cartridges while jets are running.",
                    critical = true
                ),
                ChecklistStepSpec(
                    id = "silver_remove_old",
                    label = "Remove spent Silver Chlorine Cartridge"
                ),
                ChecklistStepSpec(
                    id = "silver_setting_2",
                    label = "Set new Silver Chlorine Cartridge dial to setting 2",
                    detail = "LS450DX requires setting 2 for the Silver Chlorine Cartridge.",
                    critical = true
                ),
                ChecklistStepSpec(
                    id = "silver_insert",
                    label = "Insert cartridge securely into holder"
                )
            )
        ),
        TaskSpec(
            taskType = TaskType.MONTHLY_SHOCK,
            title = "Monthly Shock Packet",
            shortTitle = "Monthly Shock",
            intervalLabel = "Every 1 month",
            intervalAmount = 1,
            intervalUnit = IntervalUnit.MONTHS,
            summary = "Run with jets running and cover open for 20 minutes.",
            badges = listOf(SafetyBadge.JETS_HIGH, SafetyBadge.COVER_OPEN),
            checklistSteps = listOf(
                ChecklistStepSpec(
                    id = "shock_cover_open",
                    label = "Open spa cover completely",
                    detail = "Keep cover open for the entire 20-minute treatment to allow off-gassing and protect the cover.",
                    critical = true
                ),
                ChecklistStepSpec(
                    id = "shock_add_packet",
                    label = "Add monthly shock packet to water with jets running"
                ),
                ChecklistStepSpec(
                    id = "shock_run_20m",
                    label = "Circulate with jets running and cover open for 20 minutes",
                    detail = "Use the 20-minute timer below."
                )
            ),
            linkedTimer = TimerPreset.MONTHLY_SHOCK_20M
        ),
        TaskSpec(
            taskType = TaskType.CLEAN_FILTER,
            title = "Clean Filter",
            shortTitle = "Clean Filter",
            intervalLabel = "Every 1 month",
            intervalAmount = 1,
            intervalUnit = IntervalUnit.MONTHS,
            summary = "Rinse thoroughly with garden hose. Ensure jets are NOT running.",
            badges = listOf(SafetyBadge.GARDEN_HOSE, SafetyBadge.JETS_OFF),
            checklistSteps = listOf(
                ChecklistStepSpec(
                    id = "filter_jets_off",
                    label = "Confirm jets are OFF (NOT running)",
                    detail = "Never remove the filter while pumps/jets are running.",
                    critical = true
                ),
                ChecklistStepSpec(
                    id = "filter_remove",
                    label = "Lift top basket and remove filter cartridge from filter well"
                ),
                ChecklistStepSpec(
                    id = "filter_rinse",
                    label = "Rinse pleats thoroughly using a garden hose",
                    detail = "Spray between all pleats until water runs clear."
                ),
                ChecklistStepSpec(
                    id = "filter_reinstall",
                    label = "Reinstall filter cartridge and top basket"
                )
            )
        ),
        TaskSpec(
            taskType = TaskType.REFILL_TUB,
            title = "Drain & Refill Hot Tub",
            shortTitle = "Refill Tub",
            intervalLabel = "Every 4 months",
            intervalAmount = 4,
            intervalUnit = IntervalUnit.MONTHS,
            summary = "Unplug spa, empty with pump, wipe shell & underside of cover with microfiber cloth, then run Startup Sequence.",
            badges = listOf(SafetyBadge.UNPLUG_FIRST, SafetyBadge.MICROFIBER_WIPE),
            checklistSteps = listOf(
                ChecklistStepSpec(
                    id = "refill_unplug",
                    label = "UNPLUG the hot tub from the 110V outlet first",
                    detail = "Safety critical: disconnect power completely before draining.",
                    critical = true
                ),
                ChecklistStepSpec(
                    id = "refill_pump_empty",
                    label = "Empty tub completely using sump/utility pump"
                ),
                ChecklistStepSpec(
                    id = "refill_wipe_shell",
                    label = "Wipe down entire acrylic shell with a clean microfiber cloth"
                ),
                ChecklistStepSpec(
                    id = "refill_wipe_cover",
                    label = "Wipe down the underside of the cover with a microfiber cloth"
                )
            )
        )
    )

    val taskSpecByType: Map<TaskType, TaskSpec> = recurringTasks.associateBy { it.taskType }

    fun specFor(type: TaskType): TaskSpec = taskSpecByType.getValue(type)

    data class FilterCycleStep(
        val number: Int,
        val instruction: String
    )

    val filterCycleWalkthrough: List<FilterCycleStep> = listOf(
        FilterCycleStep(1, "Press and hold the Light button for 5 seconds until \"F1\" appears."),
        FilterCycleStep(2, "Press Up / Down to set F1 to 2 hours (range 0.5–6.0 hrs in 30-min steps; factory default is 1 hr)."),
        FilterCycleStep(3, "Press the Light button once to save F1 and switch to \"F2\"."),
        FilterCycleStep(4, "Press Up / Down to set F2 to 2 hours (factory default is 30 min)."),
        FilterCycleStep(5, "Wait 15 seconds without touching any buttons to save F2 and return to water temp.")
    )

    data class ReferenceSection(
        val title: String,
        val items: List<Pair<String, String>>
    )

    val quickReferenceSections: List<ReferenceSection> = listOf(
        ReferenceSection(
            title = "Filter Cycle Configuration (F1 & F2)",
            items = listOf(
                "Target Setting" to "F1 = 2 hrs, F2 = 2 hrs (override factory 1 hr / 30 min)",
                "Button Sequence" to "Hold Light 5s -> Up/Down to 2 -> Press Light once -> Up/Down to 2 -> Wait 15s",
                "Step Increment" to "0.5 to 6.0 hours in 30-minute steps"
            )
        ),
        ReferenceSection(
            title = "Cartridge Settings",
            items = listOf(
                "Blue Mineral Cartridge" to "Setting 6 • Replace every 4 months • Jets OFF",
                "Silver Chlorine Cartridge" to "Setting 2 • Replace every 4 weeks (28 days) • Jets OFF"
            )
        ),
        ReferenceSection(
            title = "Water Chemistry Targets & Nudge Doses",
            items = listOf(
                "1. Total Alkalinity (FIRST)" to "80–120 ppm • Low: 3 tbsp Alkalinity Increaser • High: 1–2 tsp pH Decreaser",
                "2. pH (SECOND)" to "7.2–7.8 • High: 1–2 tsp pH Decreaser • Low: 3 tbsp Alkalinity Increaser",
                "3. Calcium Hardness (THIRD)" to "150–250 ppm • Low: 3 tbsp Calcium Hardness Increaser",
                "Circulation Between Additions" to "Wait 15 min with Jets HIGH and Air Valves CLOSED before re-testing"
            )
        ),
        ReferenceSection(
            title = "Shock & Drain Safety Rules",
            items = listOf(
                "Startup Shock" to "30 minutes • Jets HIGH • Air Valves/Filter CLOSED • Cover OPEN",
                "Monthly Shock" to "20 minutes • Jets RUNNING • Cover OPEN",
                "Water Fill" to "Bypass softener • Hose straight down into open filter well • Halfway between rim & grey filter ring",
                "Drain & Clean" to "Unplug 110V first • Empty with pump • Wipe shell & cover underside with microfiber cloth"
            )
        )
    )
}
