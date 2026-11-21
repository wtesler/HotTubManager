package tesler.will.hottubmanager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tesler.will.hottubmanager.data.db.dao.FillCycleDao
import tesler.will.hottubmanager.data.db.dao.MaintenanceDao
import tesler.will.hottubmanager.data.db.dao.StartupDao
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceLogEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceTaskEntity
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity
import tesler.will.hottubmanager.data.repository.MaintenanceScheduleAnchorer
import tesler.will.hottubmanager.data.repository.StartupRepository
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.scheduler.AlarmScheduler
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StartupRepositoryTest {

    private val fixedInstant = Instant.parse("2026-11-21T14:00:00Z")
    private val calculator = ScheduleCalculator(Clock.fixed(fixedInstant, ZoneOffset.UTC))

    @Test
    fun `step progression and step 6 schedules 24h filter clean and anchors schedules`() = runTest {
        val startupDao = FakeStartupDao()
        val fillCycleDao = FakeFillCycleDao()
        val maintenanceDao = FakeMaintenanceDao()
        val alarmScheduler = FakeAlarmScheduler()
        var anchoredDate: LocalDate? = null
        val anchorer = MaintenanceScheduleAnchorer { anchoredDate = it }

        val repo = StartupRepository(
            startupDao = startupDao,
            fillCycleDao = fillCycleDao,
            maintenanceDao = maintenanceDao,
            maintenanceAnchorer = anchorer,
            scheduleCalculator = calculator,
            alarmScheduler = alarmScheduler
        )

        val session = repo.startNewStartupSession("Test Startup")
        assertEquals(1, session.currentStep)
        assertFalse(session.isCompleted)

        repo.setStepFlag("s1_bypass", true)
        repo.advanceFromStep(1)
        assertEquals(2, repo.getActiveSession()?.currentStep)

        val step6Result = repo.completeStep6AndSchedule24hFilterClean(resetMaintenanceAnchorsToToday = true)
        assertNotNull(step6Result)
        assertEquals(7, step6Result?.currentStep)
        assertEquals(fixedInstant, step6Result?.step6CompletedAt)
        assertEquals(fixedInstant.plus(Duration.ofHours(24)), step6Result?.filterCleanDueAt)
        assertEquals(fixedInstant.plus(Duration.ofHours(24)), alarmScheduler.scheduled24hAt)
        assertEquals(LocalDate.of(2026, 11, 21), anchoredDate)

        val step7Result = repo.completeStep7FilterClean()
        assertNotNull(step7Result)
        assertTrue(step7Result!!.isCompleted)
        assertTrue(alarmScheduler.cancelled24h)
        assertTrue(maintenanceDao.logs.any { it.taskType == TaskType.CLEAN_FILTER })
    }

    private class FakeAlarmScheduler : AlarmScheduler {
        var scheduled24hAt: Instant? = null
        var cancelled24h = false

        override fun scheduleMaintenanceResync() {}
        override fun schedule24HourFilterClean(triggerAt: Instant) {
            scheduled24hAt = triggerAt
        }
        override fun cancel24HourFilterClean() {
            cancelled24h = true
        }
        override fun scheduleTimerCompletion(timerId: String, triggerAt: Instant) {}
        override fun cancelTimerCompletion() {}
    }

    private class FakeStartupDao : StartupDao {
        private val items = MutableStateFlow<List<StartupSessionEntity>>(emptyList())
        private var nextId = 1L

        override fun observeActiveSession(): Flow<StartupSessionEntity?> =
            items.map { list -> list.lastOrNull { !it.isCompleted } }

        override suspend fun getActiveSession(): StartupSessionEntity? =
            items.value.lastOrNull { !it.isCompleted }

        override fun observeLatestSession(): Flow<StartupSessionEntity?> =
            items.map { it.lastOrNull() }

        override suspend fun insert(session: StartupSessionEntity): Long {
            val id = nextId++
            items.value = items.value + session.copy(id = id)
            return id
        }

        override suspend fun update(session: StartupSessionEntity) {
            items.value = items.value.map { if (it.id == session.id) session else it }
        }
    }

    private class FakeFillCycleDao : FillCycleDao {
        private val items = MutableStateFlow<List<FillCycleEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAllFillCycles(): Flow<List<FillCycleEntity>> = items
        override fun observeActiveFillCycle(): Flow<FillCycleEntity?> =
            items.map { list -> list.lastOrNull { it.completedAt == null } }

        override suspend fun getActiveFillCycle(): FillCycleEntity? =
            items.value.lastOrNull { it.completedAt == null }

        override suspend fun getMaxCycleNumber(): Int =
            items.value.maxOfOrNull { it.cycleNumber } ?: 0

        override suspend fun insert(fillCycle: FillCycleEntity): Long {
            val id = nextId++
            items.value = items.value + fillCycle.copy(id = id)
            return id
        }

        override suspend fun update(fillCycle: FillCycleEntity) {
            items.value = items.value.map { if (it.id == fillCycle.id) fillCycle else it }
        }
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val logs = mutableListOf<MaintenanceLogEntity>()
        private val tasks = mutableListOf<MaintenanceTaskEntity>()

        override fun observeAllTasks(): Flow<List<MaintenanceTaskEntity>> = MutableStateFlow(tasks)
        override suspend fun getAllTasks(): List<MaintenanceTaskEntity> = tasks
        override suspend fun getTaskByType(taskType: TaskType): MaintenanceTaskEntity? =
            tasks.firstOrNull { it.taskType == taskType }
        override suspend fun insertTaskIgnore(task: MaintenanceTaskEntity): Long = 1L
        override suspend fun insertTasksIgnore(tasks: List<MaintenanceTaskEntity>) {}
        override suspend fun updateTask(task: MaintenanceTaskEntity) {}
        override suspend fun insertLog(log: MaintenanceLogEntity): Long {
            logs.add(log)
            return logs.size.toLong()
        }
        override fun observeAllLogs(): Flow<List<MaintenanceLogEntity>> = MutableStateFlow(logs)
        override fun observeRecentLogs(limit: Int): Flow<List<MaintenanceLogEntity>> =
            MutableStateFlow(logs.take(limit))
    }
}
