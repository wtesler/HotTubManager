package tesler.will.hottubmanager.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity
import tesler.will.hottubmanager.data.prefs.ActiveTimerState
import tesler.will.hottubmanager.data.repository.ChemistryRepository
import tesler.will.hottubmanager.data.repository.StartupRepository
import tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvice
import tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvisor
import tesler.will.hottubmanager.domain.chemistry.WaterReadingInput
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.scheduling.TimerManager

data class Step4InputState(
    val taText: String = "",
    val phText: String = "",
    val chText: String = "",
    val manualOverrideOrder: Boolean = false,
    val lastLoggedMessage: String? = null
)

data class StartupUiState(
    val activeSession: StartupSessionEntity? = null,
    val flags: Map<String, Boolean> = emptyMap(),
    val selectedStep: Int = 1,
    val activeTimer: ActiveTimerState? = null,
    val step4Input: Step4InputState = Step4InputState(),
    val step4Advice: WaterBalanceAdvice = WaterBalanceAdvisor().evaluate(WaterReadingInput()),
    val resetAnchorsOnStep6: Boolean = true
)

class StartupViewModel(
    private val startupRepository: StartupRepository,
    private val chemistryRepository: ChemistryRepository,
    private val timerManager: TimerManager,
    private val waterBalanceAdvisor: WaterBalanceAdvisor
) : ViewModel() {

    private val selectedStepOverride = MutableStateFlow<Int?>(null)
    private val step4InputState = MutableStateFlow(Step4InputState())
    private val resetAnchorsState = MutableStateFlow(true)

    val uiState: StateFlow<StartupUiState> = combine(
        startupRepository.observeActiveSession(),
        timerManager.activeTimerFlow,
        selectedStepOverride,
        step4InputState,
        resetAnchorsState
    ) { session, activeTimer, stepOverride, step4Input, resetAnchors ->
        val flags = session?.let { startupRepository.parseFlags(it.completedStepFlagsJson) }.orEmpty()
        val displayStep = stepOverride ?: session?.currentStep ?: 1
        val reading = WaterReadingInput(
            totalAlkalinityPpm = step4Input.taText.trim().toIntOrNull(),
            ph = step4Input.phText.trim().toDoubleOrNull(),
            calciumHardnessPpm = step4Input.chText.trim().toIntOrNull()
        )
        val advice = waterBalanceAdvisor.evaluate(reading, step4Input.manualOverrideOrder)

        StartupUiState(
            activeSession = session,
            flags = flags,
            selectedStep = displayStep.coerceIn(1, 7),
            activeTimer = activeTimer,
            step4Input = step4Input,
            step4Advice = advice,
            resetAnchorsOnStep6 = resetAnchors
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StartupUiState()
    )

    fun startFreshStartupSession() {
        viewModelScope.launch {
            startupRepository.startNewStartupSession("Fresh Fill Startup")
            selectedStepOverride.value = 1
        }
    }

    fun selectStep(step: Int) {
        selectedStepOverride.value = step.coerceIn(1, 7)
        viewModelScope.launch {
            startupRepository.setCurrentStep(step)
        }
    }

    fun setFlag(flagKey: String, checked: Boolean) {
        viewModelScope.launch {
            startupRepository.setStepFlag(flagKey, checked)
        }
    }

    fun advanceFromStep(step: Int) {
        viewModelScope.launch {
            startupRepository.advanceFromStep(step)
            selectedStepOverride.value = (step + 1).coerceAtMost(7)
        }
    }

    fun updateTaText(text: String) {
        step4InputState.update { it.copy(taText = text.filter(Char::isDigit), lastLoggedMessage = null) }
    }

    fun updatePhText(text: String) {
        var dotSeen = false
        val sanitized = buildString {
            for (ch in text) {
                if (ch.isDigit()) append(ch)
                else if (ch == '.' && !dotSeen) {
                    dotSeen = true
                    append(ch)
                }
            }
        }
        step4InputState.update { it.copy(phText = sanitized, lastLoggedMessage = null) }
    }

    fun updateChText(text: String) {
        step4InputState.update { it.copy(chText = text.filter(Char::isDigit), lastLoggedMessage = null) }
    }

    fun setManualOverride(overrideOrder: Boolean) {
        step4InputState.update { it.copy(manualOverrideOrder = overrideOrder) }
    }

    fun logStep4Reading(startTimerAfter: Boolean) {
        val state = uiState.value
        val input = state.step4Input
        val advice = state.step4Advice
        val reading = WaterReadingInput(
            totalAlkalinityPpm = input.taText.trim().toIntOrNull(),
            ph = input.phText.trim().toDoubleOrNull(),
            calciumHardnessPpm = input.chText.trim().toIntOrNull()
        )
        val chemicalSummary = if (advice.chemicalName != null && advice.doseText != null) {
            "${advice.doseText} ${advice.chemicalName}"
        } else {
            "Test strip logged"
        }

        viewModelScope.launch {
            chemistryRepository.logWaterTest(
                input = reading,
                chemicalsAddedSummary = chemicalSummary,
                notes = "Startup Step 4 balancing"
            )
            if (startTimerAfter) {
                timerManager.startPresetTimer(TimerPreset.CHEMICAL_WAIT_15M)
            }
            step4InputState.update {
                it.copy(
                    lastLoggedMessage = if (startTimerAfter) {
                        "Logged ($chemicalSummary) & started 15-min timer"
                    } else {
                        "Water test reading saved"
                    }
                )
            }
        }
    }

    fun setResetAnchorsOnStep6(reset: Boolean) {
        resetAnchorsState.value = reset
    }

    fun completeStep6() {
        viewModelScope.launch {
            startupRepository.completeStep6AndSchedule24hFilterClean(
                resetMaintenanceAnchorsToToday = resetAnchorsState.value
            )
            selectedStepOverride.value = 7
        }
    }

    fun completeStep7() {
        viewModelScope.launch {
            startupRepository.completeStep7FilterClean()
            selectedStepOverride.value = null
        }
    }

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
}
