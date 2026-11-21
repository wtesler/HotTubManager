package tesler.will.hottubmanager.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tesler.will.hottubmanager.data.db.entity.MaintenanceLogEntity
import tesler.will.hottubmanager.data.db.entity.MaintenanceTaskEntity
import tesler.will.hottubmanager.domain.model.TaskType

@Dao
interface MaintenanceDao {

    @Query("SELECT * FROM maintenance_tasks ORDER BY id ASC")
    fun observeAllTasks(): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks ORDER BY id ASC")
    suspend fun getAllTasks(): List<MaintenanceTaskEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE taskType = :taskType LIMIT 1")
    suspend fun getTaskByType(taskType: TaskType): MaintenanceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskIgnore(task: MaintenanceTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTasksIgnore(tasks: List<MaintenanceTaskEntity>)

    @Update
    suspend fun updateTask(task: MaintenanceTaskEntity)

    @Insert
    suspend fun insertLog(log: MaintenanceLogEntity): Long

    @Query("SELECT * FROM maintenance_logs ORDER BY completedAt DESC")
    fun observeAllLogs(): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int): Flow<List<MaintenanceLogEntity>>
}
