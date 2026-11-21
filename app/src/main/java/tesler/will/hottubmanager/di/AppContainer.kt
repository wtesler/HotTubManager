package tesler.will.hottubmanager.di

import android.content.Context
import tesler.will.hottubmanager.data.db.HotTubDatabase
import tesler.will.hottubmanager.data.prefs.UserPreferencesStore
import tesler.will.hottubmanager.data.repository.ChemistryRepository
import tesler.will.hottubmanager.data.repository.MaintenanceRepository
import tesler.will.hottubmanager.data.repository.StartupRepository
import tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvisor
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import tesler.will.hottubmanager.scheduling.MaintenanceAlarmScheduler
import tesler.will.hottubmanager.scheduling.NotificationHelper
import tesler.will.hottubmanager.scheduling.TimerManager
import java.time.Clock

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()
    val scheduleCalculator: ScheduleCalculator = ScheduleCalculator(clock)
    val waterBalanceAdvisor: WaterBalanceAdvisor = WaterBalanceAdvisor()

    val database: HotTubDatabase = HotTubDatabase.create(appContext)
    val preferencesStore: UserPreferencesStore = UserPreferencesStore(appContext)

    val notificationHelper: NotificationHelper = NotificationHelper(appContext)

    val alarmScheduler: MaintenanceAlarmScheduler = MaintenanceAlarmScheduler(
        context = appContext,
        maintenanceDao = database.maintenanceDao(),
        startupDao = database.startupDao(),
        preferencesStore = preferencesStore,
        scheduleCalculator = scheduleCalculator
    )

    val timerManager: TimerManager = TimerManager(
        preferencesStore = preferencesStore,
        alarmScheduler = alarmScheduler,
        notificationHelper = notificationHelper,
        scheduleCalculator = scheduleCalculator
    )

    val maintenanceRepository: MaintenanceRepository = MaintenanceRepository(
        maintenanceDao = database.maintenanceDao(),
        fillCycleDao = database.fillCycleDao(),
        preferencesStore = preferencesStore,
        scheduleCalculator = scheduleCalculator,
        alarmScheduler = alarmScheduler
    )

    val startupRepository: StartupRepository = StartupRepository(
        startupDao = database.startupDao(),
        fillCycleDao = database.fillCycleDao(),
        maintenanceDao = database.maintenanceDao(),
        maintenanceAnchorer = maintenanceRepository,
        scheduleCalculator = scheduleCalculator,
        alarmScheduler = alarmScheduler
    )

    val chemistryRepository: ChemistryRepository = ChemistryRepository(
        waterTestDao = database.waterTestDao(),
        fillCycleDao = database.fillCycleDao(),
        scheduleCalculator = scheduleCalculator
    )
}
