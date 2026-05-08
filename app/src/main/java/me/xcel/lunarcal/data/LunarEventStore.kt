package me.xcel.lunarcal.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LunarEventStore(context: Context) {
    private val preferences = context.getSharedPreferences("lunar_events", Context.MODE_PRIVATE)

    fun loadEvents(): List<LunarEvent> {
        val raw = preferences.getString(KEY_EVENTS, "[]").orEmpty()
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toEvent())
            }
        }.sortedWith(compareBy<LunarEvent> { it.startDateIso }.thenBy { it.title })
    }

    fun saveEvents(events: List<LunarEvent>) {
        val array = JSONArray()
        events.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private fun JSONObject.toEvent(): LunarEvent {
        val providerIds = optJSONArray("calendarProviderIds") ?: JSONArray()
        val reminders = optJSONArray("reminders")
        val occurrenceSyncs = optJSONArray("occurrenceSyncs") ?: JSONArray()
        return LunarEvent(
            id = getString("id"),
            title = getString("title"),
            recurrence = LunarRecurrence.valueOf(getString("recurrence")),
            lunarMonthNumber = getInt("lunarMonthNumber"),
            lunarMonthName = getString("lunarMonthName"),
            lunarDayNumber = getInt("lunarDayNumber"),
            lunarDayName = getString("lunarDayName"),
            isLeapMonth = getBoolean("isLeapMonth"),
            startDateIso = getString("startDateIso"),
            reminders = reminders?.let(::readReminders) ?: LunarReminder.defaultReminders(),
            occurrenceSyncs = readOccurrenceSyncs(occurrenceSyncs),
            calendarProviderIds = buildList {
                for (index in 0 until providerIds.length()) {
                    add(providerIds.getLong(index))
                }
            },
        )
    }

    private fun LunarEvent.toJson(): JSONObject {
        val providerIds = JSONArray()
        calendarProviderIds.forEach { providerIds.put(it) }
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("recurrence", recurrence.name)
            .put("lunarMonthNumber", lunarMonthNumber)
            .put("lunarMonthName", lunarMonthName)
            .put("lunarDayNumber", lunarDayNumber)
            .put("lunarDayName", lunarDayName)
            .put("isLeapMonth", isLeapMonth)
            .put("startDateIso", startDateIso)
            .put("reminders", remindersToJson(reminders))
            .put("occurrenceSyncs", occurrenceSyncsToJson(occurrenceSyncs))
            .put("calendarProviderIds", providerIds)
    }

    private fun readReminders(array: JSONArray): List<LunarReminder> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    LunarReminder(
                        amount = item.optInt("amount", 1).coerceAtLeast(1),
                        unit = runCatching {
                            ReminderOffsetUnit.valueOf(item.optString("unit", ReminderOffsetUnit.DAYS.name))
                        }.getOrDefault(ReminderOffsetUnit.DAYS),
                        time = item.optString("time", "09:00").ifBlank { "09:00" },
                    ),
                )
            }
        }.take(MAX_REMINDERS_PER_EVENT).ifEmpty { LunarReminder.defaultReminders() }
    }

    private fun readOccurrenceSyncs(array: JSONArray): List<EventOccurrenceSync> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val overrideReminders = item.optJSONArray("remindersOverride")?.takeIf { it.length() > 0 }
                add(
                    EventOccurrenceSync(
                        dateIso = item.getString("dateIso"),
                        providerEventId = item.optLong("providerEventId", -1L),
                        calendarId = item.optLong("calendarId", -1L),
                        uid = item.optString("uid"),
                        fingerprint = item.optString("fingerprint"),
                        titleOverride = item.optString("titleOverride").ifBlank { null },
                        remindersOverride = overrideReminders?.let(::readReminders),
                        externallyDeleted = item.optBoolean("externallyDeleted", false),
                        deletionDecisionPending = item.optBoolean("deletionDecisionPending", false),
                    ),
                )
            }
        }
    }

    private fun remindersToJson(reminders: List<LunarReminder>): JSONArray {
        val array = JSONArray()
        reminders.take(MAX_REMINDERS_PER_EVENT).forEach { reminder ->
            array.put(
                JSONObject()
                    .put("amount", reminder.amount)
                    .put("unit", reminder.unit.name)
                    .put("time", reminder.time),
            )
        }
        return array
    }

    private fun occurrenceSyncsToJson(syncs: List<EventOccurrenceSync>): JSONArray {
        val array = JSONArray()
        syncs.forEach { sync ->
            array.put(
                JSONObject()
                    .put("dateIso", sync.dateIso)
                    .put("providerEventId", sync.providerEventId)
                    .put("calendarId", sync.calendarId)
                    .put("uid", sync.uid)
                    .put("fingerprint", sync.fingerprint)
                    .put("titleOverride", sync.titleOverride ?: "")
                    .put("remindersOverride", sync.remindersOverride?.let(::remindersToJson) ?: JSONArray())
                    .put("externallyDeleted", sync.externallyDeleted)
                    .put("deletionDecisionPending", sync.deletionDecisionPending),
            )
        }
        return array
    }

    companion object {
        private const val KEY_EVENTS = "events"
    }
}
