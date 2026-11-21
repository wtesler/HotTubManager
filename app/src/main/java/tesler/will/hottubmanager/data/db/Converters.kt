package tesler.will.hottubmanager.data.db

import androidx.room.TypeConverter
import tesler.will.hottubmanager.domain.model.IntervalUnit
import tesler.will.hottubmanager.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(epochMillis: Long?): Instant? = epochMillis?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromTaskType(type: TaskType?): String? = type?.name

    @TypeConverter
    fun toTaskType(value: String?): TaskType? = value?.let(TaskType::valueOf)

    @TypeConverter
    fun fromIntervalUnit(unit: IntervalUnit?): String? = unit?.name

    @TypeConverter
    fun toIntervalUnit(value: String?): IntervalUnit? = value?.let(IntervalUnit::valueOf)
}
