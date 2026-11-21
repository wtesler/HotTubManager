package tesler.will.hottubmanager.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.data.prefs.UserPreferencesStore
import tesler.will.hottubmanager.data.repository.MaintenanceRepository
import tesler.will.hottubmanager.data.repository.StartupRepository
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import java.time.LocalDate
import java.time.LocalTime

data class OnboardingUiState(
    val stepIndex: Int = 0, // 0 = permissions & preferences, 1 = setup path / existing dates
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val advanceNoticeDays: Int = 1,
    val selectedExistingTubMode: Boolean = false,
    val lastCompletedDates: Map<TaskType, LocalDate> = emptyMap(),
    val isSaving: Boolean = false
)

class OnboardingViewModel(
    private val preferencesStore: UserPreferencesStore,
    private val maintenanceRepository: MaintenanceRepository,
    private val startupRepository: StartupRepository,
    private val scheduleCalculator: ScheduleCalculator
) : ViewModel() {

    private val today = scheduleCalculator.today()

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            lastCompletedDates = Ls450dxSpecs.recurringTasks.associate { it.taskType to today }
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setStep(step: Int) {
        _uiState.update { it.copy(stepIndex = step) }
    }

    fun setReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    fun setAdvanceNoticeDays(days: Int) {
        _uiState.update { it.copy(advanceNoticeDays = days) }
    }

    fun setExistingTubMode(existing: Boolean) {
        _uiState.update { it.copy(selectedExistingTubMode = existing) }
    }

    fun setTaskLastCompletedDate(taskType: TaskType, date: LocalDate) {
        _uiState.update { state ->
            state.copy(lastCompletedDates = state.lastCompletedDates + (taskType to date))
        }
    }

    fun computePreviewNextDue(taskType: TaskType): LocalDate {
        val spec = Ls450dxSpecs.specFor(taskType)
        val lastDone = _uiState.value.lastCompletedDates[taskType] ?: today
        return scheduleCalculator.computeNextDueDate(
            completedDate = lastDone,
            intervalAmount = spec.intervalAmount,
            intervalUnit = spec.intervalUnit
        )
    }

    fun startFreshFillToday(onComplete: () -> Unit) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            preferencesStore.setReminderTime(state.reminderTime)
            preferencesStore.setAdvanceNoticeDays(state.advanceNoticeDays)
            maintenanceRepository.ensureDefaultTasksSeeded()
            startupRepository.startNewStartupSession("Initial Fresh Fill Startup")
            preferencesStore.setOnboardingCompleted(true)
            onComplete()
        }
    }

    fun saveExistingTubDates(onComplete: () -> Unit) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            preferencesStore.setReminderTime(state.reminderTime)
            preferencesStore.setAdvanceNoticeDays(state.advanceNoticeDays)
            maintenanceRepository.initializeFromExistingTub(state.lastCompletedDates)
            preferencesStore.setOnboardingCompleted(true)
            onComplete()
        }
    }
}
