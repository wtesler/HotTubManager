package tesler.will.hottubmanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tesler.will.hottubmanager.data.db.HotTubDatabase
import tesler.will.hottubmanager.data.db.entity.WaterTestEntity
import tesler.will.hottubmanager.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HotTubDatabaseTest {

    private lateinit var db: HotTubDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HotTubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun seedDefaultTasksInsertsAll6Ls450dxTasksIdempotently() = runTest {
        val dao = db.maintenanceDao()
        val base = LocalDate.of(2026, 11, 21)
        HotTubDatabase.seedDefaultTasks(dao, base)
        HotTubDatabase.seedDefaultTasks(dao, base)

        val tasks = dao.getAllTasks()
        assertEquals(6, tasks.size)
        assertNotNull(dao.getTaskByType(TaskType.BLUE_MINERAL))
        assertNotNull(dao.getTaskByType(TaskType.SILVER_CHLORINE))
        assertNotNull(dao.getTaskByType(TaskType.MONTHLY_SHOCK))
        assertNotNull(dao.getTaskByType(TaskType.CLEAN_FILTER))
        assertNotNull(dao.getTaskByType(TaskType.REFILL_TUB))
        assertNotNull(dao.getTaskByType(TaskType.TEST_WATER_CHEMISTRY))
    }

    @Test
    fun waterTestInsertAndObserveLatestWorks() = runTest {
        val dao = db.waterTestDao()
        dao.insert(
            WaterTestEntity(
                testedAt = Instant.parse("2026-11-21T10:00:00Z"),
                totalAlkalinityPpm = 100,
                ph = 7.4,
                calciumHardnessPpm = 200
            )
        )
        val latest = dao.observeLatestTest().first()
        assertNotNull(latest)
        assertEquals(100, latest?.totalAlkalinityPpm)
    }
}
