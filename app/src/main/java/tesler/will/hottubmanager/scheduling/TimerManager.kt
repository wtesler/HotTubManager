package tesler.will.hottubmanager.scheduling

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tesler.will.hottubmanager.data.prefs.ActiveTimerState
import tesler.will.hottubmanager.data.prefs.UserPreferencesStore
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.domain.scheduler.AlarmScheduler
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import java.time.Instant

class TimerManager(
    private val preferencesStore: UserPreferencesStore,
    private val alarmScheduler: AlarmScheduler,
    private val notificationHelper: NotificationHelper,
    private val scheduleCalculator: ScheduleCalculator
) {
    val activeTimerFlow: Flow<ActiveTimerState?> = preferencesStore.preferencesFlow.map { it.activeTimer }

    suspend fun startPresetTimer(preset: TimerPreset) {
        val nowMillis = scheduleCalculator.nowInstant().toEpochMilli()
        val endMillis = nowMillis + preset.durationMillis
        val state = ActiveTimerState(
            timerId = preset.id,
            title = preset.title,
            instructions = preset.instructions,
            completionTitle = preset.completionTitle,
            completionBody = preset.completionBody,
            endEpochMillis = endMillis,
            totalDurationMillis = preset.durationMillis
        )
        preferencesStore.setActiveTimer(state)
        alarmScheduler.scheduleTimerCompletion(preset.id, Instant.ofEpochMilli(endMillis))
        notificationHelper.showActiveTimerNotification(state)
    }

    suspend fun cancelActiveTimer() {
        alarmScheduler.cancelTimerCompletion()
        notificationHelper.cancelActiveTimerNotification()
        preferencesStore.setActiveTimer(null)
    }

    suspend fun restoreOngoingNotificationIfRunning() {
        val timer = preferencesStore.getPreferences().activeTimer ?: return
        val nowMillis = scheduleCalculator.nowInstant().toEpochMilli()
        if (timer.endEpochMillis > nowMillis) {
            notificationHelper.showActiveTimerNotification(timer)
        } else {
            notificationHelper.cancelActiveTimerNotification()
            preferencesStore.setActiveTimer(null)
            notificationHelper.showTimerCompleteNotification(timer.completionTitle, timer.completionBody)
        }
    }
}
