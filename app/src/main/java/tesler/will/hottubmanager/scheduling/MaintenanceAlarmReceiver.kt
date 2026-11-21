package tesler.will.hottubmanager.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.HotTubApplication
import tesler.will.hottubmanager.domain.model.TaskType

class MaintenanceAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "tesler.will.hottubmanager.ACTION_TASK_REMINDER"
        const val ACTION_FILTER_24H_REMINDER = "tesler.will.hottubmanager.ACTION_FILTER_24H_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as HotTubApplication
        val container = app.container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.notificationHelper.ensureChannelsCreated()
                when (intent.action) {
                    ACTION_TASK_REMINDER -> {
                        val taskTypeName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TYPE)
                        val taskType = taskTypeName?.let {
                            runCatching { TaskType.valueOf(it) }.getOrNull()
                        }
                        if (taskType != null) {
                            val task = container.database.maintenanceDao().getTaskByType(taskType)
                            if (task != null && task.isEnabled) {
                                val today = container.scheduleCalculator.today()
                                val effectiveDue = container.scheduleCalculator.effectiveDueDate(
                                    task.nextDueDate,
                                    task.snoozedUntilDate
                                )
                                val daysUntilDue = container.scheduleCalculator.daysUntilDue(effectiveDue, today)
                                container.notificationHelper.showMaintenanceReminder(taskType, daysUntilDue)
                                container.preferencesStore.setLastNotifiedDate(taskType, today)
                            }
                        }
                        container.alarmScheduler.rescheduleAll()
                    }

                    ACTION_FILTER_24H_REMINDER -> {
                        val active = container.startupRepository.getActiveSession()
                        val alreadyDone = active?.completedStepFlagsJson?.contains("\"step_7_done\":true") == true
                        if (active != null && !alreadyDone) {
                            container.notificationHelper.show24HourFilterCleanReminder()
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
