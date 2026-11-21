package tesler.will.hottubmanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "water_tests")
data class WaterTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testedAt: Instant,
    val fillCycleId: Long? = null,
    val totalAlkalinityPpm: Int? = null,
    val ph: Double? = null,
    val calciumHardnessPpm: Int? = null,
    val freeChlorinePpm: Double? = null,
    val waterTempF: Int? = null,
    val chemicalsAddedSummary: String = "",
    val notes: String = ""
)
