package tesler.will.hottubmanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import tesler.will.hottubmanager.data.db.dao.FillCycleDao
import tesler.will.hottubmanager.data.db.dao.MaintenanceDao
import tesler.will.hottubmanager.data.db.dao.StartupDao
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceLogEntity
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.scheduler.AlarmScheduler
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator

class StartupRepository(
    private val startupDao: StartupDao,
    private val fillCycleDao: FillCycleDao,
    private val maintenanceDao: MaintenanceDao,
    private val maintenanceAnchorer: MaintenanceScheduleAnchorer,
    private val scheduleCalculator: ScheduleCalculator,
    private val alarmScheduler: AlarmScheduler
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeActiveSession(): Flow<StartupSessionEntity?> = startupDao.observeActiveSession()

    fun observeLatestSession(): Flow<StartupSessionEntity?> = startupDao.observeLatestSession()

    suspend fun getActiveSession(): StartupSessionEntity? = startupDao.getActiveSession()

    suspend fun startNewStartupSession(notes: String = "Fresh Fill Startup"): StartupSessionEntity {
        val now = scheduleCalculator.nowInstant()

        // Close out any previous active fill cycle
        fillCycleDao.getActiveFillCycle()?.let { current ->
            fillCycleDao.update(current.copy(completedAt = now))
        }

        // Complete any dangling incomplete startup session
        startupDao.getActiveSession()?.let { previous ->
            startupDao.update(previous.copy(isCompleted = true))
        }

        val nextCycleNum = fillCycleDao.getMaxCycleNumber() + 1
        val fillCycleId = fillCycleDao.insert(
            FillCycleEntity(
                cycleNumber = nextCycleNum,
                startedAt = now,
                notes = notes
            )
        )

        val session = StartupSessionEntity(
            fillCycleId = fillCycleId,
            startedAt = now,
            currentStep = 1,
            completedStepFlagsJson = "{}",
            isCompleted = false
        )
        val id = startupDao.insert(session)
        return session.copy(id = id)
    }

    fun parseFlags(flagsJson: String): Map<String, Boolean> {
        return runCatching {
            json.decodeFromString<Map<String, Boolean>>(flagsJson)
        }.getOrDefault(emptyMap())
    }

    suspend fun setStepFlag(flagKey: String, checked: Boolean) {
        val session = startupDao.getActiveSession() ?: return
        val updatedFlags = parseFlags(session.completedStepFlagsJson).toMutableMap()
        updatedFlags[flagKey] = checked
        startupDao.update(
            session.copy(completedStepFlagsJson = json.encodeToString(updatedFlags))
        )
    }

    suspend fun setCurrentStep(step: Int) {
        val session = startupDao.getActiveSession() ?: return
        val clamped = step.coerceIn(1, 7)
        startupDao.update(session.copy(currentStep = clamped))
    }

    suspend fun advanceFromStep(completedStep: Int) {
        val session = startupDao.getActiveSession() ?: return
        val updatedFlags = parseFlags(session.completedStepFlagsJson).toMutableMap()
        updatedFlags["step_${completedStep}_done"] = true
        val nextStep = (completedStep + 1).coerceAtMost(7)
        startupDao.update(
            session.copy(
                currentStep = maxOf(session.currentStep, nextStep),
                completedStepFlagsJson = json.encodeToString(updatedFlags)
            )
        )
    }

    /**
     * Completes Step 6 (Insert Cartridges):
     * - Records step6CompletedAt and filterCleanDueAt (+24 hours)
     * - Schedules the 24-hour post-startup filter rinse alarm
     * - Optionally anchors all recurring maintenance tasks to today
     */
    suspend fun completeStep6AndSchedule24hFilterClean(
        resetMaintenanceAnchorsToToday: Boolean
    ): StartupSessionEntity? {
        val session = startupDao.getActiveSession() ?: return null
        val now = scheduleCalculator.nowInstant()
        val filterCleanDue = scheduleCalculator.compute24HourFilterCleanTime(now)

        val updatedFlags = parseFlags(session.completedStepFlagsJson).toMutableMap()
        updatedFlags["step_6_done"] = true

        val updated = session.copy(
            currentStep = 7,
            completedStepFlagsJson = json.encodeToString(updatedFlags),
            step6CompletedAt = now,
            filterCleanDueAt = filterCleanDue
        )
        startupDao.update(updated)

        alarmScheduler.schedule24HourFilterClean(filterCleanDue)

        if (resetMaintenanceAnchorsToToday) {
            maintenanceAnchorer.anchorMaintenanceSchedulesToDate(scheduleCalculator.today())
            maintenanceDao.insertLog(
                MaintenanceLogEntity(
                    taskType = TaskType.REFILL_TUB,
                    completedAt = now,
                    fillCycleId = session.fillCycleId,
                    notes = "Fresh fill startup Step 6 completed (schedules anchored)"
                )
            )
        }
        return updated
    }

    /**
     * Completes Step 7 (24-Hour Post-Startup Filter Clean) and marks Startup Session complete.
     */
    suspend fun completeStep7FilterClean(): StartupSessionEntity? {
        val session = startupDao.getActiveSession() ?: return null
        val now = scheduleCalculator.nowInstant()
        val updatedFlags = parseFlags(session.completedStepFlagsJson).toMutableMap()
        updatedFlags["step_7_done"] = true

        val completed = session.copy(
            currentStep = 7,
            completedStepFlagsJson = json.encodeToString(updatedFlags),
            isCompleted = true
        )
        startupDao.update(completed)
        alarmScheduler.cancel24HourFilterClean()

        maintenanceDao.insertLog(
            MaintenanceLogEntity(
                taskType = TaskType.CLEAN_FILTER,
                completedAt = now,
                fillCycleId = session.fillCycleId,
                notes = "24-hour post-startup filter rinse completed"
            )
        )
        return completed
    }
}
