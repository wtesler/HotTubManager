package tesler.will.hottubmanager.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tesler.will.hottubmanager.domain.model.OverdueFrequency
import tesler.will.hottubmanager.domain.model.TaskType
import java.time.LocalDate
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hot_tub_prefs")

@Serializable
data class ActiveTimerState(
    val timerId: String,
    val title: String,
    val instructions: String,
    val completionTitle: String,
    val completionBody: String,
    val endEpochMillis: Long,
    val totalDurationMillis: Long
)

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val advanceNoticeDays: Int = 1,
    val overdueFrequency: OverdueFrequency = OverdueFrequency.DAILY,
    val activeTimer: ActiveTimerState? = null
)

class UserPreferencesStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val ADVANCE_NOTICE_DAYS = intPreferencesKey("advance_notice_days")
        val OVERDUE_FREQUENCY = stringPreferencesKey("overdue_frequency")
        val ACTIVE_TIMER_JSON = stringPreferencesKey("active_timer_json")
        fun lastNotifiedEpochDayKey(taskType: TaskType) =
            longPreferencesKey("last_notified_day_${taskType.name}")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val hour = prefs[Keys.REMINDER_HOUR] ?: 9
        val minute = prefs[Keys.REMINDER_MINUTE] ?: 0
        val freqName = prefs[Keys.OVERDUE_FREQUENCY]
        val overdueFreq = freqName?.let {
            runCatching { OverdueFrequency.valueOf(it) }.getOrNull()
        } ?: OverdueFrequency.DAILY
        val timerJson = prefs[Keys.ACTIVE_TIMER_JSON]
        val activeTimer = timerJson?.let {
            runCatching { json.decodeFromString<ActiveTimerState>(it) }.getOrNull()
        }

        UserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            reminderTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)),
            advanceNoticeDays = prefs[Keys.ADVANCE_NOTICE_DAYS] ?: 1,
            overdueFrequency = overdueFreq,
            activeTimer = activeTimer
        )
    }

    suspend fun getPreferences(): UserPreferences = preferencesFlow.first()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setReminderTime(time: LocalTime) {
        context.dataStore.edit {
            it[Keys.REMINDER_HOUR] = time.hour
            it[Keys.REMINDER_MINUTE] = time.minute
        }
    }

    suspend fun setAdvanceNoticeDays(days: Int) {
        context.dataStore.edit { it[Keys.ADVANCE_NOTICE_DAYS] = days }
    }

    suspend fun setOverdueFrequency(frequency: OverdueFrequency) {
        context.dataStore.edit { it[Keys.OVERDUE_FREQUENCY] = frequency.name }
    }

    suspend fun setActiveTimer(timer: ActiveTimerState?) {
        context.dataStore.edit { prefs ->
            if (timer == null) {
                prefs.remove(Keys.ACTIVE_TIMER_JSON)
            } else {
                prefs[Keys.ACTIVE_TIMER_JSON] = json.encodeToString(timer)
            }
        }
    }

    suspend fun getLastNotifiedDate(taskType: TaskType): LocalDate? {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.lastNotifiedEpochDayKey(taskType)]?.let(LocalDate::ofEpochDay)
    }

    suspend fun setLastNotifiedDate(taskType: TaskType, date: LocalDate) {
        context.dataStore.edit {
            it[Keys.lastNotifiedEpochDayKey(taskType)] = date.toEpochDay()
        }
    }

    suspend fun clearLastNotifiedDate(taskType: TaskType) {
        context.dataStore.edit {
            it.remove(Keys.lastNotifiedEpochDayKey(taskType))
        }
    }
}
