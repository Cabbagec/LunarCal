package me.xcel.lunarcal.data

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class CalendarDisplayMode {
    MONTH,
    YEAR,
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage {
    ENGLISH,
    CHINESE,
}

enum class AppDestination {
    CALENDAR,
    ALL_EVENTS,
    SETTINGS,
}

enum class LunarRecurrence {
    YEARLY,
    MONTHLY,
    ONCE,
}

enum class ReminderOffsetUnit {
    DAYS,
    WEEKS,
}

const val MAX_REMINDERS_PER_EVENT: Int = 5

data class LunarReminder(
    val amount: Int,
    val unit: ReminderOffsetUnit,
    val time: String,
) {
    companion object {
        fun defaultReminders(): List<LunarReminder> {
            return listOf(LunarReminder(amount = 1, unit = ReminderOffsetUnit.DAYS, time = "09:00"))
        }
    }
}

data class EventOccurrenceSync(
    val dateIso: String,
    val providerEventId: Long,
    val calendarId: Long,
    val uid: String,
    val fingerprint: String,
    val titleOverride: String? = null,
    val remindersOverride: List<LunarReminder>? = null,
    val externallyDeleted: Boolean = false,
    val deletionDecisionPending: Boolean = false,
)

data class SyncScanRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun contains(other: SyncScanRange): Boolean {
        return !other.startDate.isBefore(startDate) && !other.endDate.isAfter(endDate)
    }
}

data class ExternalDeletionDecision(
    val eventId: String,
    val eventTitle: String,
    val fromDateIso: String,
    val deletedCount: Int,
)

data class LunarDay(
    val date: LocalDate,
    val lunarYear: Int,
    val ganzhi: String,
    val zodiac: String,
    val monthNumber: Int,
    val monthName: String,
    val isLeapMonth: Boolean,
    val dayNumber: Int,
    val dayName: String,
    val solarTerm: String?,
) {
    val yearLabel: String = "${ganzhi}年"
    val shortDayLabel: String = solarTerm ?: if (dayNumber == 1) monthName else dayName
    val fullLabel: String = "$yearLabel $monthName$dayName"
}

data class LunarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val recurrence: LunarRecurrence,
    val lunarMonthNumber: Int,
    val lunarMonthName: String,
    val lunarDayNumber: Int,
    val lunarDayName: String,
    val isLeapMonth: Boolean,
    val startDateIso: String,
    val reminders: List<LunarReminder> = LunarReminder.defaultReminders(),
    val occurrenceSyncs: List<EventOccurrenceSync> = emptyList(),
    val calendarProviderIds: List<Long> = emptyList(),
) {
    fun occursOn(day: LunarDay): Boolean {
        return when (recurrence) {
            LunarRecurrence.ONCE -> day.date.toString() == startDateIso
            LunarRecurrence.YEARLY ->
                day.monthNumber == lunarMonthNumber &&
                    day.dayNumber == lunarDayNumber &&
                    day.isLeapMonth == isLeapMonth
            LunarRecurrence.MONTHLY -> day.dayNumber == lunarDayNumber
        }
    }

    val lunarLabel: String
        get() = when (recurrence) {
            LunarRecurrence.MONTHLY -> lunarDayName
            else -> "$lunarMonthName$lunarDayName"
        }

    fun isHiddenOn(date: LocalDate): Boolean {
        return occurrenceSyncs.any { it.dateIso == date.toString() && it.externallyDeleted }
    }

    fun titleOn(date: LocalDate): String {
        return occurrenceSyncs.firstOrNull { it.dateIso == date.toString() }?.titleOverride ?: title
    }

    fun remindersOn(date: LocalDate): List<LunarReminder> {
        return (occurrenceSyncs.firstOrNull { it.dateIso == date.toString() }?.remindersOverride ?: reminders)
            .take(MAX_REMINDERS_PER_EVENT)
    }
}

data class PhoneCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
)

object CalendarBounds {
    val startDate: LocalDate = LocalDate.of(1901, 1, 1)
    val endDate: LocalDate = LocalDate.of(2100, 12, 31)
    val startMonth: YearMonth = YearMonth.from(startDate)
    val endMonth: YearMonth = YearMonth.from(endDate)
    const val startYear: Int = 1901
    const val endYear: Int = 2100
    const val monthPageCount: Int = (endYear - startYear + 1) * 12
    const val yearPageCount: Int = endYear - startYear + 1

    fun isSupported(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)

    fun clamp(date: LocalDate): LocalDate {
        return when {
            date.isBefore(startDate) -> startDate
            date.isAfter(endDate) -> endDate
            else -> date
        }
    }

    fun clamp(month: YearMonth): YearMonth {
        return when {
            month.isBefore(startMonth) -> startMonth
            month.isAfter(endMonth) -> endMonth
            else -> month
        }
    }

    fun clampYear(year: Int): Int = year.coerceIn(startYear, endYear)

    fun monthToPage(month: YearMonth): Int {
        val clamped = clamp(month)
        return (clamped.year - startYear) * 12 + clamped.monthValue - 1
    }

    fun pageToMonth(page: Int): YearMonth {
        val safePage = page.coerceIn(0, monthPageCount - 1)
        return YearMonth.of(startYear + safePage / 12, safePage % 12 + 1)
    }

    fun yearToPage(year: Int): Int = clampYear(year) - startYear

    fun pageToYear(page: Int): Int = startYear + page.coerceIn(0, yearPageCount - 1)
}
