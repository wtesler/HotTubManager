package tesler.will.hottubmanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tesler.will.hottubmanager.data.db.HotTubDatabase
import tesler.will.hottubmanager.data.db.dao.FillCycleDao
import tesler.will.hottubmanager.data.db.dao.MaintenanceDao
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceLogEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceTaskEntity
import tesler.will.hottubmanager.data.prefs.UserPreferencesStore
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import tesler.will.hottubmanager.domain.model.TaskStatus
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.scheduler.AlarmScheduler
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import java.time.LocalDate
import java.time.ZoneId

fun interface MaintenanceScheduleAnchorer {
    suspend fun anchorMaintenanceSchedulesToDate(anchorDate: LocalDate)
}

class MaintenanceRepository(
    private val maintenanceDao: MaintenanceDao,
    private val fillCycleDao: FillCycleDao,
    private val preferencesStore: UserPreferencesStore,
    private val scheduleCalculator: ScheduleCalculator,
    private val alarmScheduler: AlarmScheduler
) : MaintenanceScheduleAnchorer {

    fun observeTaskStatuses(): Flow<List<TaskStatus>> {
        return combine(
            maintenanceDao.observeAllTasks(),
            preferencesStore.preferencesFlow
        ) { tasks, prefs ->
            val today = scheduleCalculator.today()
            tasks.map { entity ->
                val spec = Ls450dxSpecs.specFor(entity.taskType)
                val effectiveDue = scheduleCalculator.effectiveDueDate(
                    entity.nextDueDate,
                    entity.snoozedUntilDate
                )
                val daysLeft = scheduleCalculator.daysUntilDue(effectiveDue, today)
                val urgency = scheduleCalculator.computeUrgency(daysLeft, prefs.advanceNoticeDays)
                TaskStatus(
                    id = entity.id,
                    spec = spec,
                    lastCompletedDate = entity.lastCompletedDate,
                    nextDueDate = entity.nextDueDate,
                    snoozedUntilDate = entity.snoozedUntilDate,
                    effectiveDueDate = effectiveDue,
                    daysUntilDue = daysLeft,
                    urgency = urgency,
                    isEnabled = entity.isEnabled
                )
            }.sortedWith(
                compareBy<TaskStatus> { it.urgency.priority }
                    .thenBy { it.daysUntilDue }
                    .thenBy { it.spec.taskType.stableId }
            )
        }
    }

    fun observeAllLogs(): Flow<List<MaintenanceLogEntity>> = maintenanceDao.observeAllLogs()

    fun observeActiveFillCycle(): Flow<FillCycleEntity?> = fillCycleDao.observeActiveFillCycle()

    fun observeAllFillCycles(): Flow<List<FillCycleEntity>> = fillCycleDao.observeAllFillCycles()

    suspend fun ensureDefaultTasksSeeded() {
        if (maintenanceDao.getAllTasks().isEmpty()) {
            HotTubDatabase.seedDefaultTasks(maintenanceDao, scheduleCalculator.today())
        }
    }

    suspend fun completeTask(
        taskType: TaskType,
        completedDate: LocalDate = scheduleCalculator.today(),
        notes: String = ""
    ) {
        ensureDefaultTasksSeeded()
        val task = maintenanceDao.getTaskByType(taskType) ?: return
        val nextDue = scheduleCalculator.computeNextDueDate(
            completedDate = completedDate,
            intervalAmount = task.intervalAmount,
            intervalUnit = task.intervalUnit
        )
        val updated = task.copy(
            lastCompletedDate = completedDate,
            nextDueDate = nextDue,
            snoozedUntilDate = null
        )
        maintenanceDao.updateTask(updated)

        val activeCycle = fillCycleDao.getActiveFillCycle()
        val completedInstant = completedDate
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .let { if (completedDate == scheduleCalculator.today()) scheduleCalculator.nowInstant() else it }

        maintenanceDao.insertLog(
            MaintenanceLogEntity(
                taskType = taskType,
                completedAt = completedInstant,
                fillCycleId = activeCycle?.id,
                notes = notes.trim()
            )
        )
        preferencesStore.clearLastNotifiedDate(taskType)
        alarmScheduler.scheduleMaintenanceResync()
    }

    suspend fun snoozeTask(taskType: TaskType, daysToAdd: Long) {
        val today = scheduleCalculator.today()
        val task = maintenanceDao.getTaskByType(taskType) ?: return
        val base = maxOf(today, scheduleCalculator.effectiveDueDate(task.nextDueDate, task.snoozedUntilDate))
        val targetDate = if (daysToAdd <= 1L && scheduleCalculator.effectiveDueDate(task.nextDueDate, task.snoozedUntilDate) < today) {
            today.plusDays(daysToAdd)
        } else {
            base.plusDays(daysToAdd)
        }
        snoozeTaskUntil(taskType, targetDate)
    }

    suspend fun snoozeTaskUntil(taskType: TaskType, targetDate: LocalDate) {
        val task = maintenanceDao.getTaskByType(taskType) ?: return
        maintenanceDao.updateTask(task.copy(snoozedUntilDate = targetDate))
        preferencesStore.clearLastNotifiedDate(taskType)
        alarmScheduler.scheduleMaintenanceResync()
    }

    suspend fun initializeFromExistingTub(
        lastCompletedByTask: Map<TaskType, LocalDate>
    ) {
        ensureDefaultTasksSeeded()
        val today = scheduleCalculator.today()
        val tasks = maintenanceDao.getAllTasks()
        for (task in tasks) {
            val lastDone = lastCompletedByTask[task.taskType] ?: today
            val nextDue = scheduleCalculator.computeNextDueDate(
                completedDate = lastDone,
                intervalAmount = task.intervalAmount,
                intervalUnit = task.intervalUnit
            )
            maintenanceDao.updateTask(
                task.copy(
                    lastCompletedDate = lastDone,
                    nextDueDate = nextDue,
                    snoozedUntilDate = null
                )
            )
            preferencesStore.clearLastNotifiedDate(task.taskType)
        }

        if (fillCycleDao.getActiveFillCycle() == null) {
            val refillDate = lastCompletedByTask[TaskType.REFILL_TUB] ?: today
            val nextNumber = fillCycleDao.getMaxCycleNumber() + 1
            fillCycleDao.insert(
                FillCycleEntity(
                    cycleNumber = nextNumber,
                    startedAt = refillDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    notes = "Existing spa setup"
                )
            )
        }
        alarmScheduler.scheduleMaintenanceResync()
    }

    override suspend fun anchorMaintenanceSchedulesToDate(anchorDate: LocalDate) {
        ensureDefaultTasksSeeded()
        val tasks = maintenanceDao.getAllTasks()
        for (task in tasks) {
            val nextDue = scheduleCalculator.computeNextDueDate(
                completedDate = anchorDate,
                intervalAmount = task.intervalAmount,
                intervalUnit = task.intervalUnit
            )
            maintenanceDao.updateTask(
                task.copy(
                    lastCompletedDate = anchorDate,
                    nextDueDate = nextDue,
                    snoozedUntilDate = null
                )
            )
            preferencesStore.clearLastNotifiedDate(task.taskType)
        }
        alarmScheduler.scheduleMaintenanceResync()
    }

    suspend fun getAllTasks(): List<MaintenanceTaskEntity> {
        ensureDefaultTasksSeeded()
        return maintenanceDao.getAllTasks()
    }
}
