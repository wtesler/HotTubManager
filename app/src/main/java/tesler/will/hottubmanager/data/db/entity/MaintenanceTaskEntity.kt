package tesler.will.hottubmanager.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import tesler.will.hottubmanager.domain.model.IntervalUnit
import tesler.will.hottubmanager.domain.model.TaskType
import java.time.LocalDate

@Entity(
    tableName = "maintenance_tasks",
    indices = [Index(value = ["taskType"], unique = true)]
)
data class MaintenanceTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskType: TaskType,
    val title: String,
    val intervalAmount: Int,
    val intervalUnit: IntervalUnit,
    val lastCompletedDate: LocalDate?,
    val nextDueDate: LocalDate,
    val snoozedUntilDate: LocalDate? = null,
    val isEnabled: Boolean = true
)
