package tesler.will.hottubmanager.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity

@Dao
interface StartupDao {

    @Query("SELECT * FROM startup_sessions WHERE isCompleted = 0 ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<StartupSessionEntity?>

    @Query("SELECT * FROM startup_sessions WHERE isCompleted = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): StartupSessionEntity?

    @Query("SELECT * FROM startup_sessions ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestSession(): Flow<StartupSessionEntity?>

    @Insert
    suspend fun insert(session: StartupSessionEntity): Long

    @Update
    suspend fun update(session: StartupSessionEntity)
}
