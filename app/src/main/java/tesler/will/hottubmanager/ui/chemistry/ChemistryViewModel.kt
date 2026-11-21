package tesler.will.hottubmanager.ui.chemistry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.WaterTestEntity
import tesler.will.hottubmanager.data.prefs.ActiveTimerState
import tesler.will.hottubmanager.data.repository.ChemistryRepository
import tesler.will.hottubmanager.data.repository.MaintenanceRepository
import tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvice
import tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvisor
import tesler.will.hottubmanager.domain.chemistry.WaterReadingInput
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.scheduling.TimerManager

data class ChemistryInputState(
    val taText: String = "",
    val phText: String = "",
    val chText: String = "",
    val chlorineText: String = "",
    val tempText: String = "",
    val chemicalsAdded: String = "",
    val notes: String = "",
    val savedFeedback: String? = null
)

data class ChemistryUiState(
    val activeTimer: ActiveTimerState? = null,
    val input: ChemistryInputState = ChemistryInputState(),
    val advice: WaterBalanceAdvice = WaterBalanceAdvisor().evaluate(WaterReadingInput(), manualOverrideOrder = true),
    val tests: List<WaterTestEntity> = emptyList(),
    val fillCyclesById: Map<Long, FillCycleEntity> = emptyMap()
)

class ChemistryViewModel(
    private val chemistryRepository: ChemistryRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val timerManager: TimerManager,
    private val waterBalanceAdvisor: WaterBalanceAdvisor
) : ViewModel() {

    private val inputFlow = MutableStateFlow(ChemistryInputState())

    val uiState: StateFlow<ChemistryUiState> = combine(
        timerManager.activeTimerFlow,
        inputFlow,
        chemistryRepository.observeAllTests(),
        maintenanceRepository.observeAllFillCycles()
    ) { activeTimer, input, tests, cycles ->
        val reading = WaterReadingInput(
            totalAlkalinityPpm = input.taText.trim().toIntOrNull(),
            ph = input.phText.trim().toDoubleOrNull(),
            calciumHardnessPpm = input.chText.trim().toIntOrNull(),
            freeChlorinePpm = input.chlorineText.trim().toDoubleOrNull(),
            waterTempF = input.tempText.trim().toIntOrNull()
        )
        val advice = waterBalanceAdvisor.evaluate(reading, manualOverrideOrder = true)
        ChemistryUiState(
            activeTimer = activeTimer,
            input = input,
            advice = advice,
            tests = tests,
            fillCyclesById = cycles.associateBy { it.id }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChemistryUiState()
    )

    fun startTimer(preset: TimerPreset) {
        viewModelScope.launch {
            timerManager.startPresetTimer(preset)
        }
    }

    fun cancelTimer() {
        viewModelScope.launch {
            timerManager.cancelActiveTimer()
        }
    }

    fun updateTa(text: String) = inputFlow.update {
        it.copy(taText = text.filter(Char::isDigit), savedFeedback = null)
    }

    fun updatePh(text: String) = inputFlow.update {
        it.copy(phText = sanitizeDecimal(text), savedFeedback = null)
    }

    fun updateCh(text: String) = inputFlow.update {
        it.copy(chText = text.filter(Char::isDigit), savedFeedback = null)
    }

    fun updateChlorine(text: String) = inputFlow.update {
        it.copy(chlorineText = sanitizeDecimal(text), savedFeedback = null)
    }

    fun updateTemp(text: String) = inputFlow.update {
        it.copy(tempText = text.filter(Char::isDigit), savedFeedback = null)
    }

    fun updateChemicalsAdded(text: String) = inputFlow.update { it.copy(chemicalsAdded = text, savedFeedback = null) }
    fun updateNotes(text: String) = inputFlow.update { it.copy(notes = text, savedFeedback = null) }

    private fun sanitizeDecimal(input: String): String {
        var dotSeen = false
        return buildString {
            for (ch in input) {
                if (ch.isDigit()) {
                    append(ch)
                } else if (ch == '.' && !dotSeen) {
                    dotSeen = true
                    append(ch)
                }
            }
        }
    }

    fun saveWaterTest(start15MinTimer: Boolean = false) {
        val current = inputFlow.value
        val reading = WaterReadingInput(
            totalAlkalinityPpm = current.taText.trim().toIntOrNull(),
            ph = current.phText.trim().toDoubleOrNull(),
            calciumHardnessPpm = current.chText.trim().toIntOrNull(),
            freeChlorinePpm = current.chlorineText.trim().toDoubleOrNull(),
            waterTempF = current.tempText.trim().toIntOrNull()
        )
        if (reading.totalAlkalinityPpm == null &&
            reading.ph == null &&
            reading.calciumHardnessPpm == null &&
            reading.freeChlorinePpm == null &&
            reading.waterTempF == null
        ) {
            inputFlow.update { it.copy(savedFeedback = "Enter at least one test reading before saving.") }
            return
        }

        val advice = uiState.value.advice
        val autoChemical = if (current.chemicalsAdded.isBlank() && start15MinTimer &&
            advice.chemicalName != null && advice.doseText != null
        ) {
            "${advice.doseText} ${advice.chemicalName}"
        } else {
            current.chemicalsAdded
        }

        viewModelScope.launch {
            chemistryRepository.logWaterTest(
                input = reading,
                chemicalsAddedSummary = autoChemical,
                notes = current.notes
            )
            if (start15MinTimer) {
                timerManager.startPresetTimer(TimerPreset.CHEMICAL_WAIT_15M)
            }
            inputFlow.update {
                ChemistryInputState(
                    savedFeedback = if (start15MinTimer) {
                        "Water test saved & 15-min circulation timer started."
                    } else {
                        "Water test saved to history."
                    }
                )
            }
        }
    }
}
