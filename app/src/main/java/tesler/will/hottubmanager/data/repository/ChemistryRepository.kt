package tesler.will.hottubmanager.data.repository

import kotlinx.coroutines.flow.Flow
import tesler.will.hottubmanager.data.db.dao.FillCycleDao
import tesler.will.hottubmanager.data.db.dao.WaterTestDao
import tesler.will.hottubmanager.data.db.entity.WaterTestEntity
import tesler.will.hottubmanager.domain.chemistry.WaterReadingInput
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator

class ChemistryRepository(
    private val waterTestDao: WaterTestDao,
    private val fillCycleDao: FillCycleDao,
    private val scheduleCalculator: ScheduleCalculator
) {
    fun observeAllTests(): Flow<List<WaterTestEntity>> = waterTestDao.observeAllTests()

    fun observeLatestTest(): Flow<WaterTestEntity?> = waterTestDao.observeLatestTest()

    suspend fun logWaterTest(
        input: WaterReadingInput,
        chemicalsAddedSummary: String = "",
        notes: String = ""
    ): Long {
        val activeCycle = fillCycleDao.getActiveFillCycle()
        val entity = WaterTestEntity(
            testedAt = scheduleCalculator.nowInstant(),
            fillCycleId = activeCycle?.id,
            totalAlkalinityPpm = input.totalAlkalinityPpm,
            ph = input.ph,
            calciumHardnessPpm = input.calciumHardnessPpm,
            freeChlorinePpm = input.freeChlorinePpm,
            waterTempF = input.waterTempF,
            chemicalsAddedSummary = chemicalsAddedSummary.trim(),
            notes = notes.trim()
        )
        return waterTestDao.insert(entity)
    }
}
