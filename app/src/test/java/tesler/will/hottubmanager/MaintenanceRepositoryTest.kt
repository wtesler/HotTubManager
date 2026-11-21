package tesler.will.hottubmanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tesler.will.hottubmanager.data.db.dao.FillCycleDao
import tesler.will.hottubmanager.data.db.dao.MaintenanceDao
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceLogEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceTaskEntity
import tesler.will.hottubmanager.data.prefs.MaintenancePreferences
import tesler.will.hottubmanager.data.prefs.UserPreferences
import tesler.will.hottubmanager.data.repository.MaintenanceRepository
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.scheduler.AlarmScheduler
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MaintenanceRepositoryTest {

    private val fixedInstant = Instant.parse("2026-11-21T12:00:00Z")
    private val calculator = ScheduleCalculator(Clock.fixed(fixedInstant, ZoneOffset.UTC))

    @Test
    fun `snoozeTaskUntil updates due date both earlier and later than nextDueDate`() = runTest {
        val maintenanceDao = FakeMaintenanceDao()
        val fillCycleDao = FakeFillCycleDao()
        val prefs = FakePreferences()
        val repo = MaintenanceRepository(
            maintenanceDao = maintenanceDao,
            fillCycleDao = fillCycleDao,
            preferencesStore = prefs,
            scheduleCalculator = calculator,
            alarmScheduler = NoOpAlarmScheduler
        )

        repo.completeTask(TaskType.SILVER_CHLORINE, LocalDate.of(2026, 11, 21))

        // Reschedule earlier than Dec 19 -> Dec 3
        repo.snoozeTaskUntil(TaskType.SILVER_CHLORINE, LocalDate.of(2026, 12, 3))
        var status = repo.observeTaskStatuses().first().first { it.spec.taskType == TaskType.SILVER_CHLORINE }
        assertEquals(LocalDate.of(2026, 12, 3), status.effectiveDueDate)

        // Reschedule later than Dec 3 -> Dec 10
        repo.snoozeTaskUntil(TaskType.SILVER_CHLORINE, LocalDate.of(2026, 12, 10))
        status = repo.observeTaskStatuses().first().first { it.spec.taskType == TaskType.SILVER_CHLORINE }
        assertEquals(LocalDate.of(2026, 12, 10), status.effectiveDueDate)
    }

    @Test
    fun `ensureDefaultTasksSeeded backfills missing task types without overwriting existing`() = runTest {
        val maintenanceDao = FakeMaintenanceDao()
        maintenanceDao.insertTaskIgnore(
            MaintenanceTaskEntity(
                id = 1,
                taskType = TaskType.BLUE_MINERAL,
                title = "Replace Blue Mineral Cartridge",
                intervalAmount = 4,
                intervalUnit = tesler.will.hottubmanager.domain.model.IntervalUnit.MONTHS,
                lastCompletedDate = LocalDate.of(2026, 11, 1),
                nextDueDate = LocalDate.of(2027, 3, 1)
            )
        )
        val repo = MaintenanceRepository(
            maintenanceDao = maintenanceDao,
            fillCycleDao = FakeFillCycleDao(),
            preferencesStore = FakePreferences(),
            scheduleCalculator = calculator,
            alarmScheduler = NoOpAlarmScheduler
        )

        repo.ensureDefaultTasksSeeded()

        val all = maintenanceDao.getAllTasks()
        assertEquals(6, all.size)
        assertEquals(LocalDate.of(2027, 3, 1), maintenanceDao.getTaskByType(TaskType.BLUE_MINERAL)?.nextDueDate)
    }

    private object NoOpAlarmScheduler : AlarmScheduler {
        override fun scheduleMaintenanceResync() {}
        override fun schedule24HourFilterClean(triggerAt: Instant) {}
        override fun cancel24HourFilterClean() {}
        override fun scheduleTimerCompletion(timerId: String, triggerAt: Instant) {}
        override fun cancelTimerCompletion() {}
    }

    private class FakePreferences : MaintenancePreferences {
        override val preferencesFlow: Flow<UserPreferences> = MutableStateFlow(UserPreferences())
        override suspend fun clearLastNotifiedDate(taskType: TaskType) {}
    }

    private class FakeFillCycleDao : FillCycleDao {
        private val items = MutableStateFlow<List<FillCycleEntity>>(emptyList())
        override fun observeAllFillCycles(): Flow<List<FillCycleEntity>> = items
        override fun observeActiveFillCycle(): Flow<FillCycleEntity?> = items.map { it.lastOrNull() }
        override suspend fun getActiveFillCycle(): FillCycleEntity? = null
        override suspend fun getMaxCycleNumber(): Int = 0
        override suspend fun insert(fillCycle: FillCycleEntity): Long = 1L
        override suspend fun update(fillCycle: FillCycleEntity) {}
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        private val tasksFlow = MutableStateFlow<List<MaintenanceTaskEntity>>(emptyList())
        private val logsFlow = MutableStateFlow<List<MaintenanceLogEntity>>(emptyList())
        private var nextTaskId = 1L

        override fun observeAllTasks(): Flow<List<MaintenanceTaskEntity>> = tasksFlow
        override suspend fun getAllTasks(): List<MaintenanceTaskEntity> = tasksFlow.value
        override suspend fun getTaskByType(taskType: TaskType): MaintenanceTaskEntity? =
            tasksFlow.value.firstOrNull { it.taskType == taskType }

        override suspend fun insertTaskIgnore(task: MaintenanceTaskEntity): Long {
            if (tasksFlow.value.any { it.taskType == task.taskType }) return -1L
            val assigned = if (task.id == 0L) task.copy(id = nextTaskId++) else {
                nextTaskId = maxOf(nextTaskId, task.id + 1)
                task
            }
            tasksFlow.value = tasksFlow.value + assigned
            return assigned.id
        }

        override suspend fun insertTasksIgnore(tasks: List<MaintenanceTaskEntity>) {
            tasks.forEach { insertTaskIgnore(it) }
        }

        override suspend fun updateTask(task: MaintenanceTaskEntity) {
            tasksFlow.value = tasksFlow.value.map { if (it.taskType == task.taskType) task else it }
        }

        override suspend fun insertLog(log: MaintenanceLogEntity): Long {
            val id = (logsFlow.value.size + 1).toLong()
            logsFlow.value = listOf(log.copy(id = id)) + logsFlow.value
            return id
        }

        override fun observeAllLogs(): Flow<List<MaintenanceLogEntity>> = logsFlow
        override fun observeRecentLogs(limit: Int): Flow<List<MaintenanceLogEntity>> =
            logsFlow.map { it.take(limit) }
    }
}
