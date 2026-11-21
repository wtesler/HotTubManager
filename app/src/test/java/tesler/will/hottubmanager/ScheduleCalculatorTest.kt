package tesler.will.hottubmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tesler.will.hottubmanager.domain.model.IntervalUnit
import tesler.will.hottubmanager.domain.model.OverdueFrequency
import tesler.will.hottubmanager.domain.model.UrgencyLevel
import tesler.will.hottubmanager.domain.scheduler.ScheduleCalculator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleCalculatorTest {

    private val zone = ZoneId.of("America/Chicago")

    private fun calculatorAt(dateTime: ZonedDateTime): ScheduleCalculator {
        return ScheduleCalculator(Clock.fixed(dateTime.toInstant(), zone))
    }

    @Test
    fun `4 months calendar arithmetic clamps month-end properly`() {
        val calc = ScheduleCalculator()
        val oct31 = LocalDate.of(2026, 10, 31)
        assertEquals(LocalDate.of(2027, 2, 28), calc.computeNextDueDate(oct31, 4, IntervalUnit.MONTHS))

        val oct31Leap = LocalDate.of(2027, 10, 31)
        assertEquals(LocalDate.of(2028, 2, 29), calc.computeNextDueDate(oct31Leap, 4, IntervalUnit.MONTHS))
    }

    @Test
    fun `4 weeks equals exact 28 days for silver chlorine cartridge`() {
        val calc = ScheduleCalculator()
        val start = LocalDate.of(2026, 11, 21)
        val next = calc.computeNextDueDate(start, 4, IntervalUnit.WEEKS)
        assertEquals(LocalDate.of(2026, 12, 19), next)
    }

    @Test
    fun `1 month calendar arithmetic handles 30 and 31 day months`() {
        val calc = ScheduleCalculator()
        val jan31 = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), calc.computeNextDueDate(jan31, 1, IntervalUnit.MONTHS))
    }

    @Test
    fun `24 hour post startup filter clean adds exact 24 hours`() {
        val calc = ScheduleCalculator()
        val step6 = Instant.parse("2026-11-21T15:30:00Z")
        val due = calc.compute24HourFilterCleanTime(step6)
        assertEquals(Instant.parse("2026-11-22T15:30:00Z"), due)
    }

    @Test
    fun `snoozed date overrides earlier nextDueDate`() {
        val calc = ScheduleCalculator()
        val due = LocalDate.of(2026, 11, 20)
        val snoozed = LocalDate.of(2026, 11, 25)
        assertEquals(snoozed, calc.effectiveDueDate(due, snoozed))
        assertEquals(due, calc.effectiveDueDate(due, LocalDate.of(2026, 11, 18)))
    }

    @Test
    fun `urgency classification reflects overdue, due today, due soon, upcoming`() {
        val calc = ScheduleCalculator()
        assertEquals(UrgencyLevel.OVERDUE, calc.computeUrgency(-1, 1))
        assertEquals(UrgencyLevel.DUE_TODAY, calc.computeUrgency(0, 1))
        assertEquals(UrgencyLevel.DUE_SOON, calc.computeUrgency(1, 1))
        assertEquals(UrgencyLevel.DUE_SOON, calc.computeUrgency(3, 3))
        assertEquals(UrgencyLevel.UPCOMING, calc.computeUrgency(5, 3))
    }

    @Test
    fun `next reminder trigger picks advance notice before due date`() {
        val now = ZonedDateTime.of(2026, 11, 21, 8, 0, 0, 0, zone)
        val calc = calculatorAt(now)
        val due = LocalDate.of(2026, 11, 25)

        val trigger = calc.computeNextReminderTrigger(
            effectiveDueDate = due,
            reminderTime = LocalTime.of(9, 0),
            advanceNoticeDays = 1,
            overdueFrequency = OverdueFrequency.DAILY,
            lastOverdueNotifiedDate = null
        )

        val expected = ZonedDateTime.of(2026, 11, 24, 9, 0, 0, 0, zone).toInstant()
        assertEquals(expected, trigger)
    }

    @Test
    fun `overdue task respects OverdueFrequency OFF`() {
        val now = ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, zone)
        val calc = calculatorAt(now)
        val due = LocalDate.of(2026, 11, 20)

        val trigger = calc.computeNextReminderTrigger(
            effectiveDueDate = due,
            reminderTime = LocalTime.of(9, 0),
            advanceNoticeDays = 1,
            overdueFrequency = OverdueFrequency.OFF,
            lastOverdueNotifiedDate = null
        )
        assertNull(trigger)
    }

    @Test
    fun `overdue task schedules next day or 2 days based on frequency`() {
        val now = ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, zone)
        val calc = calculatorAt(now)
        val due = LocalDate.of(2026, 11, 20)

        val trigger = calc.computeNextReminderTrigger(
            effectiveDueDate = due,
            reminderTime = LocalTime.of(9, 0),
            advanceNoticeDays = 1,
            overdueFrequency = OverdueFrequency.EVERY_TWO_DAYS,
            lastOverdueNotifiedDate = LocalDate.of(2026, 11, 24)
        )
        assertNotNull(trigger)
        val expected = ZonedDateTime.of(2026, 11, 26, 9, 0, 0, 0, zone).toInstant()
        assertEquals(expected, trigger)
    }

    @Test
    fun `reconcile detects missed due-today notification after reminder time`() {
        val now = ZonedDateTime.of(2026, 11, 21, 11, 0, 0, 0, zone)
        val calc = calculatorAt(now)
        assertTrue(
            calc.shouldNotifyDuringReconcile(
                effectiveDueDate = LocalDate.of(2026, 11, 21),
                reminderTime = LocalTime.of(9, 0),
                advanceNoticeDays = 1,
                overdueFrequency = OverdueFrequency.DAILY,
                lastNotifiedDate = null
            )
        )
        assertFalse(
            calc.shouldNotifyDuringReconcile(
                effectiveDueDate = LocalDate.of(2026, 11, 21),
                reminderTime = LocalTime.of(9, 0),
                advanceNoticeDays = 1,
                overdueFrequency = OverdueFrequency.DAILY,
                lastNotifiedDate = LocalDate.of(2026, 11, 21)
            )
        )
    }
}
