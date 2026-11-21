package tesler.will.hottubmanager.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.data.db.dao.MaintenanceDao
import tesler.will.hottubmanager.data.db.dao.StartupDao
import tesler.will.hottubmanager.data.prefs.UserPreferencesStore
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.scheduler.AlarmScheduler
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import java.time.Instant

class MaintenanceAlarmScheduler(
    private val context: Context,
    private val maintenanceDao: MaintenanceDao,
    private val startupDao: StartupDao,
    private val preferencesStore: UserPreferencesStore,
    private val scheduleCalculator: ScheduleCalculator
) : AlarmScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val REQUEST_CODE_TASK_BASE = 4100
        private const val REQUEST_CODE_FILTER_24H = 4200
        private const val REQUEST_CODE_TIMER = 4300

        const val EXTRA_TIMER_ID = "extra_timer_id"
    }

    fun canScheduleExactAlarms(): Boolean {
        return alarmManager?.canScheduleExactAlarms() == true
    }

    override fun scheduleMaintenanceResync() {
        scope.launch {
            rescheduleAll()
        }
    }

    suspend fun rescheduleAll() {
        val prefs = preferencesStore.getPreferences()
        val tasks = maintenanceDao.getAllTasks()

        for (task in tasks) {
            if (!task.isEnabled) {
                cancelTaskAlarm(task.taskType)
                continue
            }
            val effectiveDue = scheduleCalculator.effectiveDueDate(
                task.nextDueDate,
                task.snoozedUntilDate
            )
            val lastNotified = preferencesStore.getLastNotifiedDate(task.taskType)
            val triggerAt = scheduleCalculator.computeNextReminderTrigger(
                effectiveDueDate = effectiveDue,
                reminderTime = prefs.reminderTime,
                advanceNoticeDays = prefs.advanceNoticeDays,
                overdueFrequency = prefs.overdueFrequency,
                lastOverdueNotifiedDate = lastNotified
            )
            if (triggerAt != null) {
                scheduleTaskAlarm(task.taskType, triggerAt)
            } else {
                cancelTaskAlarm(task.taskType)
            }
        }

        // Re-register 24h post-startup filter clean if pending
        val activeStartup = startupDao.getActiveSession()
        val filterDueAt = activeStartup?.filterCleanDueAt
        val flags = activeStartup?.completedStepFlagsJson.orEmpty()
        val step7Done = flags.contains("\"step_7_done\":true")
        if (activeStartup != null && filterDueAt != null && !step7Done) {
            if (filterDueAt.isAfter(scheduleCalculator.nowInstant())) {
                schedule24HourFilterClean(filterDueAt)
            }
        } else {
            cancel24HourFilterClean()
        }

        // Re-register active countdown timer if still in future
        val activeTimer = prefs.activeTimer
        if (activeTimer != null && activeTimer.endEpochMillis > scheduleCalculator.nowInstant().toEpochMilli()) {
            scheduleTimerCompletion(activeTimer.timerId, Instant.ofEpochMilli(activeTimer.endEpochMillis))
        }
    }

    private fun scheduleTaskAlarm(taskType: TaskType, triggerAt: Instant) {
        val pendingIntent = taskAlarmPendingIntent(taskType)
        setAlarmAllowWhileIdle(triggerAt.toEpochMilli(), pendingIntent)
    }

    private fun cancelTaskAlarm(taskType: TaskType) {
        alarmManager?.cancel(taskAlarmPendingIntent(taskType))
    }

    override fun schedule24HourFilterClean(triggerAt: Instant) {
        setAlarmAllowWhileIdle(triggerAt.toEpochMilli(), filter24hPendingIntent())
    }

    override fun cancel24HourFilterClean() {
        alarmManager?.cancel(filter24hPendingIntent())
    }

    override fun scheduleTimerCompletion(timerId: String, triggerAt: Instant) {
        val intent = Intent(context, TimerCompleteReceiver::class.java).apply {
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarmAllowWhileIdle(triggerAt.toEpochMilli(), pi)
    }

    override fun cancelTimerCompletion() {
        val intent = Intent(context, TimerCompleteReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pi)
    }

    private fun setAlarmAllowWhileIdle(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager ?: return
        if (manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun taskAlarmPendingIntent(taskType: TaskType): PendingIntent {
        val intent = Intent(context, MaintenanceAlarmReceiver::class.java).apply {
            action = MaintenanceAlarmReceiver.ACTION_TASK_REMINDER
            putExtra(NotificationHelper.EXTRA_TASK_TYPE, taskType.name)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TASK_BASE + taskType.stableId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun filter24hPendingIntent(): PendingIntent {
        val intent = Intent(context, MaintenanceAlarmReceiver::class.java).apply {
            action = MaintenanceAlarmReceiver.ACTION_FILTER_24H_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FILTER_24H,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
