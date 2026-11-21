package tesler.will.hottubmanager.domain.scheduler

import tesler.will.hottubmanager.domain.model.IntervalUnit
import tesler.will.hottubmanager.domain.model.OverdueFrequency
import tesler.will.hottubmanager.domain.model.UrgencyLevel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ScheduleCalculator(
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun today(): LocalDate = LocalDate.now(clock)

    fun nowInstant(): Instant = Instant.now(clock)

    fun computeNextDueDate(
        completedDate: LocalDate,
        intervalAmount: Int,
        intervalUnit: IntervalUnit
    ): LocalDate = when (intervalUnit) {
        IntervalUnit.MONTHS -> completedDate.plusMonths(intervalAmount.toLong())
        IntervalUnit.WEEKS -> completedDate.plusWeeks(intervalAmount.toLong())
    }

    fun effectiveDueDate(
        nextDueDate: LocalDate,
        snoozedUntilDate: LocalDate?
    ): LocalDate {
        return if (snoozedUntilDate != null && snoozedUntilDate.isAfter(nextDueDate)) {
            snoozedUntilDate
        } else {
            nextDueDate
        }
    }

    fun daysUntilDue(effectiveDueDate: LocalDate, today: LocalDate = today()): Long {
        return ChronoUnit.DAYS.between(today, effectiveDueDate)
    }

    fun computeUrgency(
        daysUntilDue: Long,
        advanceNoticeDays: Int
    ): UrgencyLevel = when {
        daysUntilDue < 0 -> UrgencyLevel.OVERDUE
        daysUntilDue == 0L -> UrgencyLevel.DUE_TODAY
        daysUntilDue <= advanceNoticeDays.coerceAtLeast(1) -> UrgencyLevel.DUE_SOON
        else -> UrgencyLevel.UPCOMING
    }

    fun computeAdvanceReminderDate(effectiveDueDate: LocalDate, advanceDays: Int): LocalDate {
        return effectiveDueDate.minusDays(advanceDays.coerceAtLeast(0).toLong())
    }

    fun compute24HourFilterCleanTime(step6CompletedAt: Instant): Instant {
        return step6CompletedAt.plus(Duration.ofHours(24))
    }

    /**
     * Determines the next Instant when an alarm should fire for a task,
     * taking into account advance notice, due date, and overdue repeat frequency.
     */
    fun computeNextReminderTrigger(
        effectiveDueDate: LocalDate,
        reminderTime: LocalTime,
        advanceNoticeDays: Int,
        overdueFrequency: OverdueFrequency,
        lastOverdueNotifiedDate: LocalDate?
    ): Instant? {
        val nowZdt = ZonedDateTime.now(clock)
        val zone = nowZdt.zone
        val today = nowZdt.toLocalDate()

        val candidateDates = buildList {
            if (advanceNoticeDays > 0) {
                add(computeAdvanceReminderDate(effectiveDueDate, advanceNoticeDays))
            }
            add(effectiveDueDate)
        }.distinct().sorted()

        for (date in candidateDates) {
            val triggerZdt = ZonedDateTime.of(date, reminderTime, zone)
            if (triggerZdt.isAfter(nowZdt)) {
                return triggerZdt.toInstant()
            }
        }

        // We are on or past effectiveDueDate and today's trigger window may have passed, or task is overdue.
        val intervalDays = overdueFrequency.intervalDays ?: return null

        val nextOverdueDate = when {
            today.isBefore(effectiveDueDate) -> effectiveDueDate
            lastOverdueNotifiedDate == null -> {
                val todayTrigger = ZonedDateTime.of(today, reminderTime, zone)
                if (todayTrigger.isAfter(nowZdt)) today else today.plusDays(intervalDays.toLong())
            }
            else -> {
                val candidate = lastOverdueNotifiedDate.plusDays(intervalDays.toLong())
                val candidateZdt = ZonedDateTime.of(candidate, reminderTime, zone)
                if (candidateZdt.isAfter(nowZdt)) candidate else today.plusDays(intervalDays.toLong())
            }
        }

        return ZonedDateTime.of(nextOverdueDate, reminderTime, zone).toInstant()
    }

    /**
     * Checks whether a task currently warrants a reconciliation notification (e.g. missed alarm).
     */
    fun shouldNotifyDuringReconcile(
        effectiveDueDate: LocalDate,
        reminderTime: LocalTime,
        advanceNoticeDays: Int,
        overdueFrequency: OverdueFrequency,
        lastNotifiedDate: LocalDate?
    ): Boolean {
        val nowZdt = ZonedDateTime.now(clock)
        val today = nowZdt.toLocalDate()
        val todayTrigger = ZonedDateTime.of(today, reminderTime, nowZdt.zone)

        if (nowZdt.isBefore(todayTrigger)) return false
        if (lastNotifiedDate == today) return false

        val daysLeft = daysUntilDue(effectiveDueDate, today)
        if (daysLeft < 0) {
            val interval = overdueFrequency.intervalDays ?: return false
            if (lastNotifiedDate == null) return true
            val daysSinceLastNotif = ChronoUnit.DAYS.between(lastNotifiedDate, today)
            return daysSinceLastNotif >= interval
        }
        if (daysLeft == 0L) return true
        return advanceNoticeDays > 0 && daysLeft == advanceNoticeDays.toLong()
    }
}
