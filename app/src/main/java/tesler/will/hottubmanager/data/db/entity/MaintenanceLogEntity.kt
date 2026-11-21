package tesler.will.hottubmanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import tesler.will.hottubmanager.domain.model.TaskType
import java.time.Instant

@Entity(tableName = "maintenance_logs")
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskType: TaskType,
    val completedAt: Instant,
    val fillCycleId: Long? = null,
    val notes: String = ""
)
