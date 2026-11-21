package tesler.will.hottubmanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "startup_sessions")
data class StartupSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fillCycleId: Long,
    val startedAt: Instant,
    val currentStep: Int = 1,
    val completedStepFlagsJson: String = "{}",
    val step6CompletedAt: Instant? = null,
    val filterCleanDueAt: Instant? = null,
    val isCompleted: Boolean = false
)
