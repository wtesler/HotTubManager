package tesler.will.hottubmanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "fill_cycles")
data class FillCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleNumber: Int,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val notes: String = ""
)
