package tesler.will.hottubmanager.scheduling

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.HotTubApplication

class ScheduleRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val allowedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (intent.action !in allowedActions) return

        val pendingResult = goAsync()
        val app = context.applicationContext as HotTubApplication
        val container = app.container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.notificationHelper.ensureChannelsCreated()
                container.alarmScheduler.rescheduleAll()
                container.timerManager.restoreOngoingNotificationIfRunning()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
