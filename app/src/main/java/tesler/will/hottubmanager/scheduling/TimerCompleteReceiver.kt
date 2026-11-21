package tesler.will.hottubmanager.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.HotTubApplication
import tesler.will.hottubmanager.domain.model.TimerPreset

class TimerCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as HotTubApplication
        val container = app.container
        val timerId = intent.getStringExtra(MaintenanceAlarmScheduler.EXTRA_TIMER_ID)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.notificationHelper.ensureChannelsCreated()
                container.notificationHelper.cancelActiveTimerNotification()

                val activeTimer = container.preferencesStore.getPreferences().activeTimer
                val preset = TimerPreset.entries.firstOrNull { it.id == timerId }

                val title = activeTimer?.completionTitle ?: preset?.completionTitle ?: "Hot Tub Timer Complete"
                val body = activeTimer?.completionBody ?: preset?.completionBody ?: "Your scheduled timer has finished."

                if (activeTimer == null || timerId == null || activeTimer.timerId == timerId) {
                    container.preferencesStore.setActiveTimer(null)
                }

                container.notificationHelper.showTimerCompleteNotification(title, body)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
