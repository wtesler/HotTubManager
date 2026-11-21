package tesler.will.hottubmanager.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity

@Dao
interface FillCycleDao {

    @Query("SELECT * FROM fill_cycles ORDER BY cycleNumber DESC")
    fun observeAllFillCycles(): Flow<List<FillCycleEntity>>

    @Query("SELECT * FROM fill_cycles WHERE completedAt IS NULL ORDER BY cycleNumber DESC LIMIT 1")
    fun observeActiveFillCycle(): Flow<FillCycleEntity?>

    @Query("SELECT * FROM fill_cycles WHERE completedAt IS NULL ORDER BY cycleNumber DESC LIMIT 1")
    suspend fun getActiveFillCycle(): FillCycleEntity?

    @Query("SELECT COALESCE(MAX(cycleNumber), 0) FROM fill_cycles")
    suspend fun getMaxCycleNumber(): Int

    @Insert
    suspend fun insert(fillCycle: FillCycleEntity): Long

    @Update
    suspend fun update(fillCycle: FillCycleEntity)
}
