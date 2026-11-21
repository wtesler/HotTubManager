package tesler.will.hottubmanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tesler.will.hottubmanager.data.db.dao.FillCycleDao
import tesler.will.hottubmanager.data.db.dao.MaintenanceDao
import tesler.will.hottubmanager.data.db.dao.StartupDao
import tesler.will.hottubmanager.data.db.dao.WaterTestDao
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceLogEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceTaskEntity
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity
import tesler.will.hottubmanager.data.db.entity.WaterTestEntity
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import java.time.LocalDate

@Database(
    entities = [
        MaintenanceTaskEntity::class,
        FillCycleEntity::class,
        MaintenanceLogEntity::class,
        StartupSessionEntity::class,
        WaterTestEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HotTubDatabase : RoomDatabase() {

    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun fillCycleDao(): FillCycleDao
    abstract fun startupDao(): StartupDao
    abstract fun waterTestDao(): WaterTestDao

    companion object {
        private const val DATABASE_NAME = "hot_tub_manager.db"

        fun create(context: Context): HotTubDatabase {
            lateinit var instance: HotTubDatabase
            instance = Room.databaseBuilder(
                context.applicationContext,
                HotTubDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDefaultTasks(instance.maintenanceDao(), LocalDate.now())
                        }
                    }
                })
                .build()
            return instance
        }

        suspend fun seedDefaultTasks(
            maintenanceDao: MaintenanceDao,
            baseDate: LocalDate = LocalDate.now()
        ) {
            val defaults = Ls450dxSpecs.recurringTasks.map { spec ->
                MaintenanceTaskEntity(
                    taskType = spec.taskType,
                    title = spec.title,
                    intervalAmount = spec.intervalAmount,
                    intervalUnit = spec.intervalUnit,
                    lastCompletedDate = null,
                    nextDueDate = baseDate,
                    snoozedUntilDate = null,
                    isEnabled = true
                )
            }
            maintenanceDao.insertTasksIgnore(defaults)
        }
    }
}
