package tesler.will.hottubmanager.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tesler.will.hottubmanager.data.db.entity.WaterTestEntity

@Dao
interface WaterTestDao {

    @Query("SELECT * FROM water_tests ORDER BY testedAt DESC")
    fun observeAllTests(): Flow<List<WaterTestEntity>>

    @Query("SELECT * FROM water_tests ORDER BY testedAt DESC LIMIT 1")
    fun observeLatestTest(): Flow<WaterTestEntity?>

    @Insert
    suspend fun insert(test: WaterTestEntity): Long
}
