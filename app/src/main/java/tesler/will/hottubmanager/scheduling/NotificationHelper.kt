package tesler.will.hottubmanager.scheduling

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import tesler.will.hottubmanager.MainActivity
import tesler.will.hottubmanager.R
import tesler.will.hottubmanager.data.prefs.ActiveTimerState
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import tesler.will.hottubmanager.domain.model.TaskType

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MAINTENANCE = "maintenance_reminders"
        const val CHANNEL_STARTUP_FOLLOWUPS = "startup_followups"
        const val CHANNEL_ACTIVE_TIMERS = "active_timers"
        const val CHANNEL_TIMER_ALERTS = "timer_alerts"

        const val EXTRA_NAV_TARGET = "extra_nav_target"
        const val EXTRA_TASK_TYPE = "extra_task_type"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val NAV_DASHBOARD = "dashboard"
        const val NAV_STARTUP = "startup"
        const val NAV_CHEMISTRY = "chemistry"
        const val NAV_GUIDE = "guide"

        const val NOTIF_ID_ACTIVE_TIMER = 2001
        const val NOTIF_ID_TIMER_COMPLETE = 2002
        const val NOTIF_ID_FILTER_24H = 2003
        const val NOTIF_ID_TEST = 2099

        fun notificationIdForTask(taskType: TaskType): Int = 1000 + taskType.stableId
    }

    fun ensureChannelsCreated() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val maintenanceChannel = NotificationChannel(
            CHANNEL_MAINTENANCE,
            "Maintenance Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders for cartridges, monthly shock, filter cleaning, and hot tub refills."
        }

        val startupChannel = NotificationChannel(
            CHANNEL_STARTUP_FOLLOWUPS,
            "24-Hour Startup Follow-ups",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "24-hour post-startup filter cleaning alerts."
        }

        val activeTimerChannel = NotificationChannel(
            CHANNEL_ACTIVE_TIMERS,
            "Active Treatment Timers",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing countdown display while a treatment or circulation timer is running."
            setSound(null, null)
            enableVibration(false)
        }

        val timerAlertChannel = NotificationChannel(
            CHANNEL_TIMER_ALERTS,
            "Timer Completion Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts when a 15-min, 20-min, 30-min, or 15-sec timer finishes."
            enableVibration(true)
        }

        manager.createNotificationChannels(
            listOf(maintenanceChannel, startupChannel, activeTimerChannel, timerAlertChannel)
        )
    }

    fun hasPostNotificationsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showMaintenanceReminder(
        taskType: TaskType,
        daysUntilDue: Long
    ) {
        if (!hasPostNotificationsPermission()) return
        val spec = Ls450dxSpecs.specFor(taskType)
        val notifId = notificationIdForTask(taskType)

        val title = when {
            daysUntilDue < 0 -> "Overdue: ${spec.shortTitle}"
            daysUntilDue == 0L -> "Due Today: ${spec.title}"
            daysUntilDue == 1L -> "Due Tomorrow: ${spec.shortTitle}"
            else -> "Due in $daysUntilDue Days: ${spec.shortTitle}"
        }
        val body = spec.summary

        val openChecklistIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAV_TARGET, NAV_DASHBOARD)
                putExtra(EXTRA_TASK_TYPE, taskType.name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            notifId + 500,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SNOOZE_ONE_DAY
                putExtra(EXTRA_TASK_TYPE, taskType.name)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(R.drawable.ic_notification_spa)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openChecklistIntent)
            .addAction(0, "Open Checklist", openChecklistIntent)
            .addAction(0, "Snooze 1 Day", snoozeIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    fun show24HourFilterCleanReminder() {
        if (!hasPostNotificationsPermission()) return
        val openStartupIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_FILTER_24H,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAV_TARGET, NAV_STARTUP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "It has been 24 hours since you inserted cartridges. Ensure JETS ARE OFF, then rinse filter with garden hose."
        val notification = NotificationCompat.Builder(context, CHANNEL_STARTUP_FOLLOWUPS)
            .setSmallIcon(R.drawable.ic_notification_spa)
            .setContentTitle("24-Hour Post-Startup Filter Clean")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openStartupIntent)
            .addAction(0, "Open Step 7", openStartupIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_FILTER_24H, notification)
    }

    fun showActiveTimerNotification(timer: ActiveTimerState) {
        if (!hasPostNotificationsPermission()) return
        val openAppIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_ACTIVE_TIMER,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAV_TARGET, NAV_CHEMISTRY)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE_TIMERS)
            .setSmallIcon(R.drawable.ic_notification_spa)
            .setContentTitle(timer.title)
            .setContentText(timer.instructions)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(timer.endEpochMillis)
            .setShowWhen(true)
            .setContentIntent(openAppIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_ACTIVE_TIMER, notification)
    }

    fun cancelActiveTimerNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_ACTIVE_TIMER)
    }

    fun showTimerCompleteNotification(
        title: String,
        body: String
    ) {
        if (!hasPostNotificationsPermission()) return
        val openAppIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_TIMER_COMPLETE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAV_TARGET, NAV_CHEMISTRY)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            context,
            NOTIF_ID_TIMER_COMPLETE + 1,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISS_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, NOTIF_ID_TIMER_COMPLETE)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER_ALERTS)
            .setSmallIcon(R.drawable.ic_notification_spa)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Open App", openAppIntent)
            .addAction(0, "Dismiss", dismissIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_TIMER_COMPLETE, notification)
    }

    fun sendTestNotification(): Boolean {
        if (!hasPostNotificationsPermission()) return false
        ensureChannelsCreated()
        val openDashboardIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_TEST,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAV_TARGET, NAV_DASHBOARD)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(R.drawable.ic_notification_spa)
            .setContentTitle("LS450DX Notifications Active")
            .setContentText("Your hot tub reminders and treatment timer alerts are working properly.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openDashboardIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_TEST, notification)
        return true
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
