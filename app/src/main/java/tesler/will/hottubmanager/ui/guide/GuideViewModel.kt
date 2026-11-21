package tesler.will.hottubmanager.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceLogEntity
import tesler.will.hottubmanager.data.prefs.UserPreferences
import tesler.will.hottubmanager.data.prefs.UserPreferencesStore
import tesler.will.hottubmanager.data.repository.MaintenanceRepository
import tesler.will.hottubmanager.domain.model.OverdueFrequency
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.scheduling.MaintenanceAlarmScheduler
import tesler.will.hottubmanager.scheduling.NotificationHelper
import java.time.LocalTime

data class GuideUiState(
    val preferences: UserPreferences = UserPreferences(),
    val recentLogs: List<MaintenanceLogEntity> = emptyList(),
    val allLogs: List<MaintenanceLogEntity> = emptyList(),
    val fillCycles: List<FillCycleEntity> = emptyList(),
    val historySearchQuery: String = "",
    val historyFilterType: TaskType? = null,
    val testNotificationMessage: String? = null
)

class GuideViewModel(
    private val preferencesStore: UserPreferencesStore,
    private val maintenanceRepository: MaintenanceRepository,
    private val alarmScheduler: MaintenanceAlarmScheduler,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filterType = MutableStateFlow<TaskType?>(null)
    private val testFeedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GuideUiState> = combine(
        preferencesStore.preferencesFlow,
        maintenanceRepository.observeAllLogs(),
        maintenanceRepository.observeAllFillCycles(),
        searchQuery,
        filterType,
        testFeedback
    ) { values ->
        val prefs = values[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val logs = values[1] as List<MaintenanceLogEntity>
        @Suppress("UNCHECKED_CAST")
        val cycles = values[2] as List<FillCycleEntity>
        val query = values[3] as String
        val filter = values[4] as TaskType?
        val feedback = values[5] as String?

        GuideUiState(
            preferences = prefs,
            recentLogs = logs.take(3),
            allLogs = logs,
            fillCycles = cycles,
            historySearchQuery = query,
            historyFilterType = filter,
            testNotificationMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GuideUiState()
    )

    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            preferencesStore.setReminderTime(time)
            alarmScheduler.rescheduleAll()
        }
    }

    fun setAdvanceNoticeDays(days: Int) {
        viewModelScope.launch {
            preferencesStore.setAdvanceNoticeDays(days)
            alarmScheduler.rescheduleAll()
        }
    }

    fun setOverdueFrequency(frequency: OverdueFrequency) {
        viewModelScope.launch {
            preferencesStore.setOverdueFrequency(frequency)
            alarmScheduler.rescheduleAll()
        }
    }

    fun sendTestNotification() {
        val sent = notificationHelper.sendTestNotification()
        testFeedback.value = if (sent) {
            "Test notification sent!"
        } else {
            "Notification permission is not granted. Tap Grant Notifications above."
        }
    }

    fun clearTestFeedback() {
        testFeedback.value = null
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilterType(type: TaskType?) {
        filterType.value = type
    }
}
