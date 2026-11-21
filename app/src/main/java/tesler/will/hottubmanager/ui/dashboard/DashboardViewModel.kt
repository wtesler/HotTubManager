package tesler.will.hottubmanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity
import tesler.will.hottubmanager.data.prefs.ActiveTimerState
import tesler.will.hottubmanager.data.repository.MaintenanceRepository
import tesler.will.hottubmanager.data.repository.StartupRepository
import tesler.will.hottubmanager.domain.model.TaskStatus
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import tesler.will.hottubmanager.scheduling.TimerManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class DashboardUiState(
    val tasks: List<TaskStatus> = emptyList(),
    val activeFillCycle: FillCycleEntity? = null,
    val fillCycleDayCount: Long? = null,
    val activeStartupSession: StartupSessionEntity? = null,
    val activeTimer: ActiveTimerState? = null
)

class DashboardViewModel(
    private val maintenanceRepository: MaintenanceRepository,
    private val startupRepository: StartupRepository,
    private val timerManager: TimerManager,
    private val scheduleCalculator: ScheduleCalculator
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        maintenanceRepository.observeTaskStatuses(),
        maintenanceRepository.observeActiveFillCycle(),
        startupRepository.observeActiveSession(),
        timerManager.activeTimerFlow
    ) { tasks, fillCycle, startupSession, activeTimer ->
        val dayCount = fillCycle?.let { cycle ->
            val startDate = cycle.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
            ChronoUnit.DAYS.between(startDate, scheduleCalculator.today()).coerceAtLeast(0L) + 1L
        }
        DashboardUiState(
            tasks = tasks,
            activeFillCycle = fillCycle,
            fillCycleDayCount = dayCount,
            activeStartupSession = startupSession,
            activeTimer = activeTimer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun completeTask(taskType: TaskType, completedDate: LocalDate, notes: String) {
        viewModelScope.launch {
            maintenanceRepository.completeTask(taskType, completedDate, notes)
        }
    }

    fun completeRefillAndStartStartup(notes: String, onStarted: () -> Unit) {
        viewModelScope.launch {
            maintenanceRepository.completeTask(
                taskType = TaskType.REFILL_TUB,
                completedDate = scheduleCalculator.today(),
                notes = notes.ifBlank { "Drained & wiped tub; started Startup Sequence" }
            )
            startupRepository.startNewStartupSession("Refill & Fresh Startup")
            onStarted()
        }
    }

    fun snoozeTask(taskType: TaskType, days: Long) {
        viewModelScope.launch {
            maintenanceRepository.snoozeTask(taskType, days)
        }
    }

    fun snoozeTaskUntil(taskType: TaskType, targetDate: LocalDate) {
        viewModelScope.launch {
            maintenanceRepository.snoozeTaskUntil(taskType, targetDate)
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
