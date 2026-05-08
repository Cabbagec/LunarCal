package me.xcel.lunarcal.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class CalendarSyncManager(private val context: Context) {
    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    fun listVisibleCalendars(): List<PhoneCalendar> {
        if (!hasCalendarPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
        )
        return context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE}=1",
            null,
            "${CalendarContract.Calendars.ACCOUNT_NAME} ASC",
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PhoneCalendar(
                            id = cursor.getLong(0),
                            name = cursor.getString(1).orEmpty(),
                            accountName = cursor.getString(2).orEmpty(),
                            accountType = cursor.getString(3).orEmpty(),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    fun syncEventWindow(
        event: LunarEvent,
        lunarDays: Map<LocalDate, LunarDay>,
        today: LocalDate = LocalDate.now(CHINA_ZONE),
    ): LunarEvent {
        if (!hasCalendarPermission()) return event
        deleteProviderEvents(event.providerEventIds())
        val calendarId = preferredWritableCalendarId() ?: return event.copy(calendarProviderIds = emptyList())
        val (rangeStart, rangeEnd) = recurringWindow(event, today)
        val deletedDates = event.occurrenceSyncs
            .filter { it.externallyDeleted }
            .map { it.dateIso }
            .toSet()
        val inserted = occurrenceDates(event, lunarDays, rangeStart, rangeEnd)
            .filterNot { it.toString() in deletedDates }
            .mapNotNull { insertPhoneCalendarEvent(calendarId, event, it) }
        val preservedDeleted = event.occurrenceSyncs
            .filter { it.externallyDeleted && runCatching { LocalDate.parse(it.dateIso) }.getOrNull()?.let { date -> date in rangeStart..rangeEnd } == true }
            .map { it.copy(providerEventId = -1L, calendarId = calendarId, deletionDecisionPending = false) }
        return event.copy(
            occurrenceSyncs = (inserted + preservedDeleted).sortedBy { it.dateIso },
            calendarProviderIds = emptyList(),
        )
    }

    fun ensureFutureEventWindow(
        event: LunarEvent,
        lunarDays: Map<LocalDate, LunarDay>,
        today: LocalDate = LocalDate.now(CHINA_ZONE),
    ): LunarEvent {
        if (!hasCalendarPermission() || event.recurrence == LunarRecurrence.ONCE) return event
        val calendarId = event.occurrenceSyncs.firstOrNull { it.providerEventId > 0L }?.calendarId
            ?: preferredWritableCalendarId()
            ?: return event
        val (rangeStart, rangeEnd) = recurringWindow(event, today)
        val blockedDates = event.occurrenceSyncs
            .filter { it.externallyDeleted || it.deletionDecisionPending }
            .map { it.dateIso }
            .toSet()
        val existingDates = event.occurrenceSyncs
            .filter { it.providerEventId > 0L && !it.externallyDeleted }
            .map { it.dateIso }
            .toSet()
        val inserted = occurrenceDates(event, lunarDays, rangeStart, rangeEnd)
            .filterNot { it.toString() in blockedDates }
            .filterNot { it.toString() in existingDates }
            .mapNotNull { insertPhoneCalendarEvent(calendarId, event, it) }
        if (inserted.isEmpty() && event.calendarProviderIds.isEmpty()) return event
        return event.copy(
            occurrenceSyncs = (event.occurrenceSyncs + inserted).distinctBy { it.dateIso }.sortedBy { it.dateIso },
            calendarProviderIds = emptyList(),
        )
    }

    fun occurrenceDates(
        event: LunarEvent,
        lunarDays: Map<LocalDate, LunarDay>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<LocalDate> {
        return when (event.recurrence) {
            LunarRecurrence.ONCE -> listOf(LocalDate.parse(event.startDateIso)).filter { it in rangeStart..rangeEnd }
            LunarRecurrence.YEARLY,
            LunarRecurrence.MONTHLY -> lunarDays.values.asSequence()
                .filter { it.date in rangeStart..rangeEnd }
                .filter(event::occursOn)
                .map { it.date }
                .sorted()
                .toList()
        }
    }

    fun scanAppOccurrences(rangeStart: LocalDate, rangeEnd: LocalDate): List<ProviderOccurrence> {
        if (!hasCalendarPermission()) return emptyList()
        val startMillis = rangeStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endMillis = rangeEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val selection = """
            ${CalendarContract.Events.DTSTART}>=? AND ${CalendarContract.Events.DTSTART}<? AND (
                ${CalendarContract.Events.CUSTOM_APP_PACKAGE}=?
                OR ${CalendarContract.Events.CUSTOM_APP_URI} LIKE ?
                OR ${CalendarContract.Events.UID_2445} LIKE ?
                OR ${CalendarContract.Events.DESCRIPTION} LIKE ?
            )
        """.trimIndent()
        val args = arrayOf(
            startMillis.toString(),
            endMillis.toString(),
            APP_PACKAGE,
            "$URI_PREFIX%",
            "$UID_PREFIX%",
            "$LEGACY_SYNC_TAG%",
        )
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            args,
            "${CalendarContract.Events.DTSTART} ASC",
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val providerEventId = cursor.getLong(EVENT_ID)
                    val title = cursor.getString(EVENT_TITLE).orEmpty()
                    val dtStart = cursor.getLong(EVENT_DTSTART)
                    val allDay = cursor.getInt(EVENT_ALL_DAY) == 1
                    val uid = cursor.getString(EVENT_UID).orEmpty()
                    val customUri = cursor.getString(EVENT_CUSTOM_URI).orEmpty()
                    val description = cursor.getString(EVENT_DESCRIPTION).orEmpty()
                    val providerDate = if (allDay) {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        Instant.ofEpochMilli(dtStart).atZone(CHINA_ZONE).toLocalDate()
                    }
                    val metadata = parseMetadata(customUri, uid, description, providerDate) ?: continue
                    add(
                        ProviderOccurrence(
                            eventId = metadata.eventId,
                            occurrenceDate = metadata.occurrenceDate,
                            providerEventId = providerEventId,
                            calendarId = cursor.getLong(EVENT_CALENDAR_ID),
                            uid = uid.ifBlank { uidFor(metadata.eventId, metadata.occurrenceDate) },
                            title = title,
                            reminders = readReminders(providerEventId, metadata.occurrenceDate),
                            recurrence = metadata.recurrence,
                            lunarMonthNumber = metadata.lunarMonthNumber,
                            lunarMonthName = metadata.lunarMonthName,
                            lunarDayNumber = metadata.lunarDayNumber,
                            lunarDayName = metadata.lunarDayName,
                            isLeapMonth = metadata.isLeapMonth,
                            startDateIso = metadata.startDateIso,
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    fun deleteProviderEvents(providerIds: List<Long>) {
        if (!hasCalendarPermission()) return
        providerIds.distinct().filter { it > 0L }.forEach { id ->
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
            context.contentResolver.delete(uri, null, null)
        }
    }

    private fun preferredWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val calendars = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE}=1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CalendarCandidate(
                            id = cursor.getLong(0),
                            accountType = cursor.getString(1).orEmpty(),
                            accessLevel = cursor.getInt(2),
                        ),
                    )
                }
            }
        }.orEmpty()

        return calendars.firstOrNull { it.accountType == "com.google" }?.id
            ?: calendars.maxByOrNull { it.accessLevel }?.id
    }

    private fun recurringWindow(event: LunarEvent, today: LocalDate): Pair<LocalDate, LocalDate> {
        val firstEventDate = runCatching { LocalDate.parse(event.startDateIso) }.getOrDefault(today)
        val anchorDate = if (firstEventDate.isAfter(today)) firstEventDate else today
        val rangeStart = LocalDate.of(anchorDate.year, 1, 1)
        val rangeEnd = rangeStart.plusYears(RECURRING_RANGE_YEARS.toLong()).minusDays(1)
        return rangeStart to rangeEnd
    }

    private fun insertPhoneCalendarEvent(calendarId: Long, event: LunarEvent, date: LocalDate): EventOccurrenceSync? {
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val uid = uidFor(event.id, date)
        val reminders = event.reminders.take(MAX_REMINDERS_PER_EVENT)
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, "")
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.ALL_DAY, 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneOffset.UTC.id)
            put(CalendarContract.Events.EVENT_END_TIMEZONE, ZoneOffset.UTC.id)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
            put(CalendarContract.Events.HAS_ALARM, if (reminders.isEmpty()) 0 else 1)
            put(CalendarContract.Events.UID_2445, uid)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, APP_PACKAGE)
            put(CalendarContract.Events.CUSTOM_APP_URI, customAppUri(event, date))
        }
        val uri = runCatching {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        }.getOrElse {
            values.remove(CalendarContract.Events.UID_2445)
            runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }.getOrNull()
        } ?: return null
        val eventId = ContentUris.parseId(uri)
        insertReminders(eventId, date, reminders)
        return EventOccurrenceSync(
            dateIso = date.toString(),
            providerEventId = eventId,
            calendarId = calendarId,
            uid = uid,
            fingerprint = fingerprint(event.title, reminders),
        )
    }

    private fun insertReminders(eventId: Long, date: LocalDate, reminders: List<LunarReminder>) {
        reminders.distinct().take(MAX_REMINDERS_PER_EVENT).forEach { reminder ->
            val minutes = minutesBeforeEvent(date, reminder) ?: return@forEach
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, minutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        }
    }

    private fun readReminders(eventId: Long, date: LocalDate): List<LunarReminder> {
        val projection = arrayOf(
            CalendarContract.Reminders.MINUTES,
            CalendarContract.Reminders.METHOD,
        )
        return context.contentResolver.query(
            CalendarContract.Reminders.CONTENT_URI,
            projection,
            "${CalendarContract.Reminders.EVENT_ID}=?",
            arrayOf(eventId.toString()),
            "${CalendarContract.Reminders.MINUTES} ASC",
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    if (cursor.getInt(1) == CalendarContract.Reminders.METHOD_ALERT) {
                        add(reminderFromMinutes(date, cursor.getInt(0)))
                    }
                }
            }
        }.orEmpty().take(MAX_REMINDERS_PER_EVENT)
    }

    private fun minutesBeforeEvent(date: LocalDate, reminder: LunarReminder): Int? {
        val time = runCatching { LocalTime.parse(reminder.time) }.getOrNull() ?: return null
        val daysBefore = when (reminder.unit) {
            ReminderOffsetUnit.DAYS -> reminder.amount
            ReminderOffsetUnit.WEEKS -> reminder.amount * 7
        }.coerceAtLeast(1)
        val eventStart = date.atStartOfDay(CHINA_ZONE).toInstant()
        val reminderInstant = date.minusDays(daysBefore.toLong()).atTime(time).atZone(CHINA_ZONE).toInstant()
        val minutes = Duration.between(reminderInstant, eventStart).toMinutes()
        return minutes.takeIf { it >= 0L && it <= Int.MAX_VALUE }?.toInt()
    }

    private fun reminderFromMinutes(date: LocalDate, minutes: Int): LunarReminder {
        val eventStart = date.atStartOfDay(CHINA_ZONE).toInstant()
        val reminderTime = eventStart.minus(Duration.ofMinutes(minutes.toLong())).atZone(CHINA_ZONE)
        val daysBefore = ChronoUnit.DAYS.between(reminderTime.toLocalDate(), date).toInt().coerceAtLeast(1)
        return if (daysBefore % 7 == 0) {
            LunarReminder(daysBefore / 7, ReminderOffsetUnit.WEEKS, reminderTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString())
        } else {
            LunarReminder(daysBefore, ReminderOffsetUnit.DAYS, reminderTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString())
        }
    }

    private fun parseMetadata(
        customUri: String,
        uid: String,
        description: String,
        providerDate: LocalDate,
    ): ProviderMetadata? {
        parseCustomUri(customUri)?.let { return it }
        parseUid(uid)?.let { return ProviderMetadata(eventId = it.first, occurrenceDate = it.second) }
        return parseLegacyDescription(description, providerDate)
    }

    private fun parseCustomUri(raw: String): ProviderMetadata? {
        if (raw.isBlank()) return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        if (uri.scheme != URI_SCHEME || uri.host != URI_HOST) return null
        val segments = uri.pathSegments
        if (segments.size < 3 || segments[1] != "occurrence") return null
        val eventId = segments[0]
        val occurrenceDate = runCatching { LocalDate.parse(segments[2]) }.getOrNull() ?: return null
        return ProviderMetadata(
            eventId = eventId,
            occurrenceDate = occurrenceDate,
            recurrence = uri.getQueryParameter("recurrence")?.let { runCatching { LunarRecurrence.valueOf(it) }.getOrNull() },
            lunarMonthNumber = uri.getQueryParameter("lunarMonth")?.toIntOrNull(),
            lunarMonthName = uri.getQueryParameter("lunarMonthName"),
            lunarDayNumber = uri.getQueryParameter("lunarDay")?.toIntOrNull(),
            lunarDayName = uri.getQueryParameter("lunarDayName"),
            isLeapMonth = uri.getQueryParameter("leap")?.toBooleanStrictOrNull(),
            startDateIso = uri.getQueryParameter("start"),
        )
    }

    private fun parseUid(uid: String): Pair<String, LocalDate>? {
        if (!uid.startsWith(UID_PREFIX)) return null
        val parts = uid.removePrefix(UID_PREFIX).split(":")
        if (parts.size != 2) return null
        val date = runCatching { LocalDate.parse(parts[1]) }.getOrNull() ?: return null
        return parts[0] to date
    }

    private fun parseLegacyDescription(description: String, providerDate: LocalDate): ProviderMetadata? {
        if (!description.startsWith(LEGACY_SYNC_TAG)) return null
        val parts = description.split(" ")
        val eventId = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
        return ProviderMetadata(eventId = eventId, occurrenceDate = providerDate)
    }

    private fun customAppUri(event: LunarEvent, date: LocalDate): String {
        return Uri.Builder()
            .scheme(URI_SCHEME)
            .authority(URI_HOST)
            .appendPath(event.id)
            .appendPath("occurrence")
            .appendPath(date.toString())
            .appendQueryParameter("v", "1")
            .appendQueryParameter("recurrence", event.recurrence.name)
            .appendQueryParameter("lunarMonth", event.lunarMonthNumber.toString())
            .appendQueryParameter("lunarMonthName", event.lunarMonthName)
            .appendQueryParameter("lunarDay", event.lunarDayNumber.toString())
            .appendQueryParameter("lunarDayName", event.lunarDayName)
            .appendQueryParameter("leap", event.isLeapMonth.toString())
            .appendQueryParameter("start", event.startDateIso)
            .build()
            .toString()
    }

    private fun LunarEvent.providerEventIds(): List<Long> {
        return calendarProviderIds + occurrenceSyncs.map { it.providerEventId }
    }

    data class ProviderOccurrence(
        val eventId: String,
        val occurrenceDate: LocalDate,
        val providerEventId: Long,
        val calendarId: Long,
        val uid: String,
        val title: String,
        val reminders: List<LunarReminder>,
        val recurrence: LunarRecurrence?,
        val lunarMonthNumber: Int?,
        val lunarMonthName: String?,
        val lunarDayNumber: Int?,
        val lunarDayName: String?,
        val isLeapMonth: Boolean?,
        val startDateIso: String?,
    ) {
        val dateIso: String = occurrenceDate.toString()
        val fingerprint: String = fingerprint(title, reminders)
    }

    private data class ProviderMetadata(
        val eventId: String,
        val occurrenceDate: LocalDate,
        val recurrence: LunarRecurrence? = null,
        val lunarMonthNumber: Int? = null,
        val lunarMonthName: String? = null,
        val lunarDayNumber: Int? = null,
        val lunarDayName: String? = null,
        val isLeapMonth: Boolean? = null,
        val startDateIso: String? = null,
    )

    private data class CalendarCandidate(
        val id: Long,
        val accountType: String,
        val accessLevel: Int,
    )

    companion object {
        val CHINA_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
        const val RECURRING_RANGE_YEARS: Int = 10
        private const val APP_PACKAGE = "me.xcel.lunarcal"
        private const val LEGACY_SYNC_TAG = "LunarCal"
        private const val URI_SCHEME = "lunarcal"
        private const val URI_HOST = "event"
        private const val URI_PREFIX = "$URI_SCHEME://$URI_HOST/"
        private const val UID_PREFIX = "$APP_PACKAGE:"

        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.UID_2445,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI,
        )
        private const val EVENT_ID = 0
        private const val EVENT_CALENDAR_ID = 1
        private const val EVENT_TITLE = 2
        private const val EVENT_DESCRIPTION = 3
        private const val EVENT_DTSTART = 4
        private const val EVENT_ALL_DAY = 5
        private const val EVENT_UID = 6
        private const val EVENT_CUSTOM_URI = 8

        fun uidFor(eventId: String, date: LocalDate): String {
            return "$UID_PREFIX$eventId:$date"
        }

        fun fingerprint(title: String, reminders: List<LunarReminder>): String {
            return title.trim() + "|" + reminders.take(MAX_REMINDERS_PER_EVENT).sortedWith(
                compareBy<LunarReminder> { it.unit.name }.thenBy { it.amount }.thenBy { it.time },
            ).joinToString(";") { "${it.amount}:${it.unit.name}:${it.time}" }
        }
    }
}
