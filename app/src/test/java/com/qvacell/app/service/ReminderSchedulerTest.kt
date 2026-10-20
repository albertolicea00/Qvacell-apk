package com.qvacell.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.qvacell.app.model.Reminder
import com.qvacell.app.model.ReminderRecurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerTest {
    private val scheduler = ReminderScheduler(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun oneTimeReminderHasNoNextOccurrence() {
        assertNull(scheduler.nextOccurrence(reminder(ReminderRecurrence.NONE)))
    }

    @Test
    fun recurringReminderAdvancesByItsConfiguredCalendarInterval() {
        val date = Calendar.getInstance().apply {
            clear()
            set(2025, Calendar.JANUARY, 15, 12, 30)
        }.timeInMillis

        assertEquals(expected(date, Calendar.DAY_OF_YEAR, 1), scheduler.nextOccurrence(reminder(ReminderRecurrence.DAILY, date)))
        assertEquals(expected(date, Calendar.WEEK_OF_YEAR, 1), scheduler.nextOccurrence(reminder(ReminderRecurrence.WEEKLY, date)))
        assertEquals(expected(date, Calendar.MONTH, 1), scheduler.nextOccurrence(reminder(ReminderRecurrence.MONTHLY, date)))
    }

    @Test
    fun customIntervalIsAtLeastOneDay() {
        val date = 1_737_000_000_000L
        assertEquals(expected(date, Calendar.DAY_OF_YEAR, 1), scheduler.nextOccurrence(
            reminder(ReminderRecurrence.CUSTOM, date, intervalDays = 0)
        ))
        assertEquals(expected(date, Calendar.DAY_OF_YEAR, 5), scheduler.nextOccurrence(
            reminder(ReminderRecurrence.CUSTOM, date, intervalDays = 5)
        ))
    }

    private fun expected(date: Long, field: Int, amount: Int): Long = Calendar.getInstance().apply {
        timeInMillis = date
        add(field, amount)
    }.timeInMillis

    private fun reminder(
        recurrence: ReminderRecurrence,
        date: Long = 0L,
        intervalDays: Int = 1
    ) = Reminder(
        id = "test",
        title = "Test",
        message = "",
        iconName = "bell",
        ussdCodeId = null,
        phoneNumber = "",
        date = date,
        recurrence = recurrence,
        customIntervalDays = intervalDays,
        isEnabled = true,
        templateKey = null
    )
}
