package tesler.will.hottubmanager

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.di.AppContainer
import tesler.will.hottubmanager.scheduling.MaintenanceReconcileWorker

class HotTubApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannelsCreated()
        MaintenanceReconcileWorker.enqueuePeriodic(this)

        appScope.launch {
            container.maintenanceRepository.ensureDefaultTasksSeeded()
            container.alarmScheduler.rescheduleAll()
            container.timerManager.restoreOngoingNotificationIfRunning()
        }
    }
}
