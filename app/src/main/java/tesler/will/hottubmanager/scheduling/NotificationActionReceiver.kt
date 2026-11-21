package tesler.will.hottubmanager.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.HotTubApplication
import tesler.will.hottubmanager.domain.model.TaskType

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE_ONE_DAY = "tesler.will.hottubmanager.ACTION_SNOOZE_ONE_DAY"
        const val ACTION_DISMISS_NOTIFICATION = "tesler.will.hottubmanager.ACTION_DISMISS_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as HotTubApplication
        val container = app.container
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_SNOOZE_ONE_DAY -> {
                        val taskTypeName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TYPE)
                        val taskType = taskTypeName?.let {
                            runCatching { TaskType.valueOf(it) }.getOrNull()
                        }
                        if (taskType != null) {
                            container.maintenanceRepository.snoozeTask(taskType, 1L)
                        }
                        if (notifId != -1) {
                            container.notificationHelper.cancelNotification(notifId)
                        }
                    }

                    ACTION_DISMISS_NOTIFICATION -> {
                        if (notifId != -1) {
                            container.notificationHelper.cancelNotification(notifId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
