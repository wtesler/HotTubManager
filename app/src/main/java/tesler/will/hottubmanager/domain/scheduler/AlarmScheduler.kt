package tesler.will.hottubmanager.domain.scheduler

import java.time.Instant

interface AlarmScheduler {
    fun scheduleMaintenanceResync()
    fun schedule24HourFilterClean(triggerAt: Instant)
    fun cancel24HourFilterClean()
    fun scheduleTimerCompletion(timerId: String, triggerAt: Instant)
    fun cancelTimerCompletion()
}
