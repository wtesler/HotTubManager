package tesler.will.hottubmanager.domain.chemistry

import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs

enum class ParameterStatus {
    UNKNOWN,
    LOW,
    IN_RANGE,
    HIGH
}

enum class BalanceStep {
    TOTAL_ALKALINITY,
    PH,
    CALCIUM_HARDNESS,
    ALL_BALANCED
}

data class WaterReadingInput(
    val totalAlkalinityPpm: Int? = null,
    val ph: Double? = null,
    val calciumHardnessPpm: Int? = null,
    val freeChlorinePpm: Double? = null,
    val waterTempF: Int? = null
)

data class WaterBalanceAdvice(
    val activeStep: BalanceStep,
    val taStatus: ParameterStatus,
    val phStatus: ParameterStatus,
    val chStatus: ParameterStatus,
    val chlorineStatus: ParameterStatus,
    val isPhLocked: Boolean,
    val isChLocked: Boolean,
    val headline: String,
    val chemicalName: String?,
    val doseText: String?,
    val instructions: String,
    val needs15MinWaitTimer: Boolean,
    val isFullyBalanced: Boolean
)

class WaterBalanceAdvisor {

    fun evaluate(
        input: WaterReadingInput,
        manualOverrideOrder: Boolean = false
    ): WaterBalanceAdvice {
        val taStatus = when (val ta = input.totalAlkalinityPpm) {
            null -> ParameterStatus.UNKNOWN
            in Ls450dxSpecs.TA_MIN_PPM..Ls450dxSpecs.TA_MAX_PPM -> ParameterStatus.IN_RANGE
            else -> if (ta < Ls450dxSpecs.TA_MIN_PPM) ParameterStatus.LOW else ParameterStatus.HIGH
        }

        val phStatus = when (val ph = input.ph) {
            null -> ParameterStatus.UNKNOWN
            in Ls450dxSpecs.PH_MIN..Ls450dxSpecs.PH_MAX -> ParameterStatus.IN_RANGE
            else -> if (ph < Ls450dxSpecs.PH_MIN) ParameterStatus.LOW else ParameterStatus.HIGH
        }

        val chStatus = when (val ch = input.calciumHardnessPpm) {
            null -> ParameterStatus.UNKNOWN
            in Ls450dxSpecs.CH_MIN_PPM..Ls450dxSpecs.CH_MAX_PPM -> ParameterStatus.IN_RANGE
            else -> if (ch < Ls450dxSpecs.CH_MIN_PPM) ParameterStatus.LOW else ParameterStatus.HIGH
        }

        val chlorineStatus = when (val fc = input.freeChlorinePpm) {
            null -> ParameterStatus.UNKNOWN
            in Ls450dxSpecs.FREE_CHLORINE_MIN_PPM..Ls450dxSpecs.FREE_CHLORINE_MAX_PPM -> ParameterStatus.IN_RANGE
            else -> if (fc < Ls450dxSpecs.FREE_CHLORINE_MIN_PPM) ParameterStatus.LOW else ParameterStatus.HIGH
        }

        val isPhLocked = !manualOverrideOrder && taStatus != ParameterStatus.IN_RANGE
        val isChLocked = !manualOverrideOrder &&
            (taStatus != ParameterStatus.IN_RANGE || phStatus != ParameterStatus.IN_RANGE)

        // When all three inputs are blank, always prompt to start with TA first
        if (taStatus == ParameterStatus.UNKNOWN &&
            phStatus == ParameterStatus.UNKNOWN &&
            chStatus == ParameterStatus.UNKNOWN
        ) {
            return WaterBalanceAdvice(
                activeStep = BalanceStep.TOTAL_ALKALINITY,
                taStatus = taStatus,
                phStatus = phStatus,
                chStatus = chStatus,
                chlorineStatus = chlorineStatus,
                isPhLocked = isPhLocked,
                isChLocked = isChLocked,
                headline = "Step 1: Test Total Alkalinity First",
                chemicalName = null,
                doseText = null,
                instructions = "Dip a test strip and enter Total Alkalinity (target ${Ls450dxSpecs.TA_MIN_PPM}–${Ls450dxSpecs.TA_MAX_PPM} ppm). Adjust TA before touching pH or Calcium Hardness.",
                needs15MinWaitTimer = false,
                isFullyBalanced = false
            )
        }

        // Strict Order 1: Total Alkalinity (80–120 ppm)
        val shouldCheckTa = if (manualOverrideOrder) {
            taStatus == ParameterStatus.LOW || taStatus == ParameterStatus.HIGH
        } else {
            taStatus != ParameterStatus.IN_RANGE
        }
        if (shouldCheckTa) {
            return when (taStatus) {
                ParameterStatus.UNKNOWN -> WaterBalanceAdvice(
                    activeStep = BalanceStep.TOTAL_ALKALINITY,
                    taStatus = taStatus,
                    phStatus = phStatus,
                    chStatus = chStatus,
                    chlorineStatus = chlorineStatus,
                    isPhLocked = isPhLocked,
                    isChLocked = isChLocked,
                    headline = "Step 1: Test Total Alkalinity First",
                    chemicalName = null,
                    doseText = null,
                    instructions = "Dip a test strip and enter Total Alkalinity (target ${Ls450dxSpecs.TA_MIN_PPM}–${Ls450dxSpecs.TA_MAX_PPM} ppm). Adjust TA before touching pH or Calcium Hardness.",
                    needs15MinWaitTimer = false,
                    isFullyBalanced = false
                )

                ParameterStatus.LOW -> WaterBalanceAdvice(
                    activeStep = BalanceStep.TOTAL_ALKALINITY,
                    taStatus = taStatus,
                    phStatus = phStatus,
                    chStatus = chStatus,
                    chlorineStatus = chlorineStatus,
                    isPhLocked = isPhLocked,
                    isChLocked = isChLocked,
                    headline = "Step 1: Total Alkalinity is Low (${input.totalAlkalinityPpm} ppm)",
                    chemicalName = "Alkalinity Increaser",
                    doseText = Ls450dxSpecs.DOSE_ALKALINITY_INCREASER,
                    instructions = "Add ${Ls450dxSpecs.DOSE_ALKALINITY_INCREASER} of Alkalinity Increaser. Run jets on HIGH with air valves CLOSED for 15 minutes, then re-test before adjusting pH.",
                    needs15MinWaitTimer = true,
                    isFullyBalanced = false
                )

                ParameterStatus.HIGH -> WaterBalanceAdvice(
                    activeStep = BalanceStep.TOTAL_ALKALINITY,
                    taStatus = taStatus,
                    phStatus = phStatus,
                    chStatus = chStatus,
                    chlorineStatus = chlorineStatus,
                    isPhLocked = isPhLocked,
                    isChLocked = isChLocked,
                    headline = "Step 1: Total Alkalinity is High (${input.totalAlkalinityPpm} ppm)",
                    chemicalName = "pH Decreaser",
                    doseText = Ls450dxSpecs.DOSE_PH_DECREASER,
                    instructions = "Add ${Ls450dxSpecs.DOSE_PH_DECREASER} of pH Decreaser. Run jets on HIGH with air valves CLOSED for 15 minutes, then re-test before adjusting pH.",
                    needs15MinWaitTimer = true,
                    isFullyBalanced = false
                )

                ParameterStatus.IN_RANGE -> error("Handled above")
            }
        }

        // Strict Order 2: pH (7.2–7.8)
        val shouldCheckPh = if (manualOverrideOrder) {
            phStatus == ParameterStatus.LOW || phStatus == ParameterStatus.HIGH
        } else {
            phStatus != ParameterStatus.IN_RANGE
        }
        if (shouldCheckPh) {
            return when (phStatus) {
                ParameterStatus.UNKNOWN -> WaterBalanceAdvice(
                    activeStep = BalanceStep.PH,
                    taStatus = taStatus,
                    phStatus = phStatus,
                    chStatus = chStatus,
                    chlorineStatus = chlorineStatus,
                    isPhLocked = isPhLocked,
                    isChLocked = isChLocked,
                    headline = "Step 2: Enter pH Reading",
                    chemicalName = null,
                    doseText = null,
                    instructions = "Total Alkalinity is in range (${Ls450dxSpecs.TA_MIN_PPM}–${Ls450dxSpecs.TA_MAX_PPM} ppm). Enter pH (target ${Ls450dxSpecs.PH_MIN}–${Ls450dxSpecs.PH_MAX}).",
                    needs15MinWaitTimer = false,
                    isFullyBalanced = false
                )

                ParameterStatus.HIGH -> WaterBalanceAdvice(
                    activeStep = BalanceStep.PH,
                    taStatus = taStatus,
                    phStatus = phStatus,
                    chStatus = chStatus,
                    chlorineStatus = chlorineStatus,
                    isPhLocked = isPhLocked,
                    isChLocked = isChLocked,
                    headline = "Step 2: pH is High (${input.ph})",
                    chemicalName = "pH Decreaser",
                    doseText = Ls450dxSpecs.DOSE_PH_DECREASER,
                    instructions = "Add ${Ls450dxSpecs.DOSE_PH_DECREASER} of pH Decreaser. Run jets on HIGH with air valves CLOSED for 15 minutes, then re-test before adjusting Calcium Hardness.",
                    needs15MinWaitTimer = true,
                    isFullyBalanced = false
                )

                ParameterStatus.LOW -> WaterBalanceAdvice(
                    activeStep = BalanceStep.PH,
                    taStatus = taStatus,
                    phStatus = phStatus,
                    chStatus = chStatus,
                    chlorineStatus = chlorineStatus,
                    isPhLocked = isPhLocked,
                    isChLocked = isChLocked,
                    headline = "Step 2: pH is Low (${input.ph})",
                    chemicalName = "Alkalinity Increaser",
                    doseText = Ls450dxSpecs.DOSE_ALKALINITY_INCREASER,
                    instructions = "Add ${Ls450dxSpecs.DOSE_ALKALINITY_INCREASER} of Alkalinity Increaser. Run jets on HIGH with air valves CLOSED for 15 minutes, then re-test before adjusting Calcium Hardness.",
                    needs15MinWaitTimer = true,
                    isFullyBalanced = false
                )

                ParameterStatus.IN_RANGE -> error("Handled above")
            }
        }

        // Strict Order 3: Calcium Hardness (150–250 ppm)
        return when (chStatus) {
            ParameterStatus.UNKNOWN -> WaterBalanceAdvice(
                activeStep = BalanceStep.CALCIUM_HARDNESS,
                taStatus = taStatus,
                phStatus = phStatus,
                chStatus = chStatus,
                chlorineStatus = chlorineStatus,
                isPhLocked = isPhLocked,
                isChLocked = isChLocked,
                headline = "Step 3: Enter Calcium Hardness Reading",
                chemicalName = null,
                doseText = null,
                instructions = "TA and pH are in range. Enter Calcium Hardness (target ${Ls450dxSpecs.CH_MIN_PPM}–${Ls450dxSpecs.CH_MAX_PPM} ppm).",
                needs15MinWaitTimer = false,
                isFullyBalanced = false
            )

            ParameterStatus.LOW -> WaterBalanceAdvice(
                activeStep = BalanceStep.CALCIUM_HARDNESS,
                taStatus = taStatus,
                phStatus = phStatus,
                chStatus = chStatus,
                chlorineStatus = chlorineStatus,
                isPhLocked = isPhLocked,
                isChLocked = isChLocked,
                headline = "Step 3: Calcium Hardness is Low (${input.calciumHardnessPpm} ppm)",
                chemicalName = "Calcium Hardness Increaser",
                doseText = Ls450dxSpecs.DOSE_CALCIUM_INCREASER,
                instructions = "Add ${Ls450dxSpecs.DOSE_CALCIUM_INCREASER} of Calcium Hardness Increaser. Run jets on HIGH with air valves CLOSED for 15 minutes, then re-test.",
                needs15MinWaitTimer = true,
                isFullyBalanced = false
            )

            ParameterStatus.HIGH -> WaterBalanceAdvice(
                activeStep = BalanceStep.CALCIUM_HARDNESS,
                taStatus = taStatus,
                phStatus = phStatus,
                chStatus = chStatus,
                chlorineStatus = chlorineStatus,
                isPhLocked = isPhLocked,
                isChLocked = isChLocked,
                headline = "Calcium Hardness is High (${input.calciumHardnessPpm} ppm)",
                chemicalName = null,
                doseText = null,
                instructions = "Do not add Calcium Hardness Increaser. If scale forms or hardness is very high, partially drain and top off with soft/balanced water.",
                needs15MinWaitTimer = false,
                isFullyBalanced = false
            )

            ParameterStatus.IN_RANGE -> WaterBalanceAdvice(
                activeStep = BalanceStep.ALL_BALANCED,
                taStatus = taStatus,
                phStatus = phStatus,
                chStatus = chStatus,
                chlorineStatus = chlorineStatus,
                isPhLocked = false,
                isChLocked = false,
                headline = "Water Chemistry Balanced",
                chemicalName = null,
                doseText = null,
                instructions = "Total Alkalinity (${Ls450dxSpecs.TA_MIN_PPM}–${Ls450dxSpecs.TA_MAX_PPM} ppm), pH (${Ls450dxSpecs.PH_MIN}–${Ls450dxSpecs.PH_MAX}), and Calcium Hardness (${Ls450dxSpecs.CH_MIN_PPM}–${Ls450dxSpecs.CH_MAX_PPM} ppm) are all within target range.",
                needs15MinWaitTimer = false,
                isFullyBalanced = true
            )
        }
    }
}
