package tesler.will.hottubmanager.scheduling

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import tesler.will.hottubmanager.HotTubApplication
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MaintenanceReconcileWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val UNIQUE_WORK_NAME = "ls450dx_maintenance_reconcile"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceReconcileWorker>(
                12,
                TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? HotTubApplication ?: return Result.success()
        val container = app.container
        container.notificationHelper.ensureChannelsCreated()

        val prefs = container.preferencesStore.getPreferences()
        val today = container.scheduleCalculator.today()
        val now = container.scheduleCalculator.nowInstant()
        val tasks = container.maintenanceRepository.getAllTasks()

        for (task in tasks) {
            if (!task.isEnabled) continue
            val effectiveDue = container.scheduleCalculator.effectiveDueDate(
                task.nextDueDate,
                task.snoozedUntilDate
            )
            val lastNotified = container.preferencesStore.getLastNotifiedDate(task.taskType)
            if (container.scheduleCalculator.shouldNotifyDuringReconcile(
                    effectiveDueDate = effectiveDue,
                    reminderTime = prefs.reminderTime,
                    advanceNoticeDays = prefs.advanceNoticeDays,
                    overdueFrequency = prefs.overdueFrequency,
                    lastNotifiedDate = lastNotified
                )
            ) {
                val daysLeft = container.scheduleCalculator.daysUntilDue(effectiveDue, today)
                container.notificationHelper.showMaintenanceReminder(task.taskType, daysLeft)
                container.preferencesStore.setLastNotifiedDate(task.taskType, today)
            }
        }

        // Check if 24h post-startup filter clean was missed
        val activeStartup = container.startupRepository.getActiveSession()
        val filterDueAt = activeStartup?.filterCleanDueAt
        val step7Done = activeStartup?.completedStepFlagsJson?.contains("\"step_7_done\":true") == true
        if (activeStartup != null && filterDueAt != null && !step7Done && !now.isBefore(filterDueAt)) {
            val dueDate = filterDueAt.atZone(ZoneId.systemDefault()).toLocalDate()
            if (!today.isAfter(dueDate.plusDays(1))) {
                container.notificationHelper.show24HourFilterCleanReminder()
            }
        }

        container.timerManager.restoreOngoingNotificationIfRunning()
        container.alarmScheduler.rescheduleAll()
        return Result.success()
    }
}
