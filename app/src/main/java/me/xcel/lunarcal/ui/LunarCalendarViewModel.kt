package me.xcel.lunarcal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.xcel.lunarcal.data.AppDestination
import me.xcel.lunarcal.data.AppLanguage
import me.xcel.lunarcal.data.CalendarBounds
import me.xcel.lunarcal.data.CalendarDisplayMode
import me.xcel.lunarcal.data.CalendarSyncManager
import me.xcel.lunarcal.data.EventOccurrenceSync
import me.xcel.lunarcal.data.ExternalDeletionDecision
import me.xcel.lunarcal.data.LunarDataSource
import me.xcel.lunarcal.data.LunarDay
import me.xcel.lunarcal.data.LunarEvent
import me.xcel.lunarcal.data.LunarEventStore
import me.xcel.lunarcal.data.LunarReminder
import me.xcel.lunarcal.data.LunarRecurrence
import me.xcel.lunarcal.data.MAX_REMINDERS_PER_EVENT
import me.xcel.lunarcal.data.PhoneCalendar
import me.xcel.lunarcal.data.SyncScanRange
import me.xcel.lunarcal.data.ThemePreference
import me.xcel.lunarcal.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

data class LunarUiState(
    val isLoadingData: Boolean = true,
    val dataError: String? = null,
    val today: LocalDate = LocalDate.now(CalendarSyncManager.CHINA_ZONE),
    val visibleMonth: YearMonth = YearMonth.now(CalendarSyncManager.CHINA_ZONE),
    val visibleYear: Int = LocalDate.now(CalendarSyncManager.CHINA_ZONE).year,
    val displayMode: CalendarDisplayMode = CalendarDisplayMode.MONTH,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val destination: AppDestination = AppDestination.CALENDAR,
    val lunarDays: Map<LocalDate, LunarDay> = emptyMap(),
    val events: List<LunarEvent> = emptyList(),
    val phoneCalendars: List<PhoneCalendar> = emptyList(),
    val selectedExternalCalendarIds: Set<Long> = emptySet(),
    val isSyncScanning: Boolean = false,
    val scannedSyncRange: SyncScanRange? = null,
    val pendingExternalDeletion: ExternalDeletionDecision? = null,
)

class LunarCalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = UserPreferences(application)
    private val eventStore = LunarEventStore(application)
    private val dataSource = LunarDataSource(application)
    private val syncManager = CalendarSyncManager(application)

    private val _uiState = MutableStateFlow(
        LunarUiState(
            displayMode = preferences.calendarDisplayMode(),
            themePreference = preferences.themePreference(),
            appLanguage = preferences.appLanguage(),
            events = eventStore.loadEvents(),
            selectedExternalCalendarIds = preferences.selectedExternalCalendarIds(),
        ),
    )
    val uiState: StateFlow<LunarUiState> = _uiState

    init {
        loadLunarData()
        refreshPhoneCalendars()
    }

    fun setDestination(destination: AppDestination) {
        _uiState.update { it.copy(destination = destination) }
    }

    fun setThemePreference(preference: ThemePreference) {
        preferences.saveThemePreference(preference)
        _uiState.update { it.copy(themePreference = preference) }
    }

    fun setAppLanguage(language: AppLanguage) {
        preferences.saveAppLanguage(language)
        _uiState.update { it.copy(appLanguage = language) }
    }

    fun toggleDrawerTheme() {
        val current = _uiState.value.themePreference
        val next = when (current) {
            ThemePreference.DARK -> ThemePreference.LIGHT
            ThemePreference.LIGHT -> ThemePreference.DARK
            ThemePreference.SYSTEM -> ThemePreference.DARK
        }
        setThemePreference(next)
    }

    fun toggleDisplayMode() {
        val next = when (_uiState.value.displayMode) {
            CalendarDisplayMode.MONTH -> CalendarDisplayMode.YEAR
            CalendarDisplayMode.YEAR -> CalendarDisplayMode.MONTH
        }
        preferences.saveCalendarDisplayMode(next)
        _uiState.update { it.copy(displayMode = next) }
    }

    fun previousPage() {
        _uiState.update { state ->
            when (state.displayMode) {
                CalendarDisplayMode.MONTH -> {
                    val previous = CalendarBounds.clamp(state.visibleMonth.minusMonths(1))
                    state.copy(visibleMonth = previous, visibleYear = previous.year)
                }
                CalendarDisplayMode.YEAR -> state.copy(visibleYear = CalendarBounds.clampYear(state.visibleYear - 1))
            }
        }
    }

    fun nextPage() {
        _uiState.update { state ->
            when (state.displayMode) {
                CalendarDisplayMode.MONTH -> {
                    val next = CalendarBounds.clamp(state.visibleMonth.plusMonths(1))
                    state.copy(visibleMonth = next, visibleYear = next.year)
                }
                CalendarDisplayMode.YEAR -> state.copy(visibleYear = CalendarBounds.clampYear(state.visibleYear + 1))
            }
        }
    }

    fun goToday() {
        val today = CalendarBounds.clamp(LocalDate.now(CalendarSyncManager.CHINA_ZONE))
        _uiState.update {
            it.copy(
                today = today,
                visibleMonth = YearMonth.from(today),
                visibleYear = today.year,
            )
        }
    }

    fun setVisibleMonth(month: YearMonth) {
        val clamped = CalendarBounds.clamp(month)
        _uiState.update { it.copy(visibleMonth = clamped, visibleYear = clamped.year) }
    }

    fun setVisibleYear(year: Int) {
        _uiState.update { it.copy(visibleYear = CalendarBounds.clampYear(year)) }
    }

    fun jumpTo(date: LocalDate) {
        val clamped = CalendarBounds.clamp(date)
        _uiState.update {
            it.copy(
                visibleMonth = YearMonth.from(clamped),
                visibleYear = clamped.year,
                destination = AppDestination.CALENDAR,
            )
        }
    }

    fun upsertEvent(event: LunarEvent): LunarEvent {
        val updated = _uiState.value.events.filterNot { it.id == event.id } + event
        val sorted = updated.sortedWith(compareBy<LunarEvent> { it.startDateIso }.thenBy { it.title })
        eventStore.saveEvents(sorted)
        _uiState.update { it.copy(events = sorted) }
        return event
    }

    fun deleteEvent(event: LunarEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            syncManager.deleteProviderEvents(event.providerEventIds())
            val updated = _uiState.value.events.filterNot { it.id == event.id }
            eventStore.saveEvents(updated)
            _uiState.update { it.copy(events = updated) }
        }
    }

    fun syncEvent(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val event = state.events.firstOrNull { it.id == eventId } ?: return@launch
            val synced = syncManager.syncEventWindow(event, state.lunarDays, state.today)
            val updated = state.events.map { if (it.id == synced.id) synced else it }
            eventStore.saveEvents(updated)
            _uiState.update {
                it.copy(
                    events = updated,
                    pendingExternalDeletion = nextPendingExternalDeletion(updated),
                )
            }
        }
    }

    fun scanVisibleWindow(force: Boolean = false) {
        val state = _uiState.value
        if (state.isLoadingData || state.lunarDays.isEmpty() || state.isSyncScanning || !syncManager.hasCalendarPermission()) return
        val range = if (state.events.isEmpty()) {
            SyncScanRange(CalendarBounds.startDate, CalendarBounds.endDate)
        } else {
            visibleScanRange(state)
        }
        if (!force && state.scannedSyncRange?.contains(range) == true) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncScanning = true) }
            val providerOccurrences = withContext(Dispatchers.IO) {
                syncManager.scanAppOccurrences(range.startDate, range.endDate)
            }
            val merged = mergeProviderScan(_uiState.value, providerOccurrences, range)
            val toppedUpEvents = withContext(Dispatchers.IO) {
                merged.events.map { event ->
                    syncManager.ensureFutureEventWindow(event, merged.lunarDays, merged.today)
                }.sortedWith(compareBy<LunarEvent> { it.startDateIso }.thenBy { it.title })
            }
            val finalState = merged.copy(
                events = toppedUpEvents,
                pendingExternalDeletion = merged.pendingExternalDeletion ?: nextPendingExternalDeletion(toppedUpEvents),
            )
            withContext(Dispatchers.IO) {
                eventStore.saveEvents(finalState.events)
            }
            _uiState.update { finalState }
        }
    }

    fun resolveExternalDeletion(decision: ExternalDeletionDecision, regenerateFuture: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val fromDate = runCatching { LocalDate.parse(decision.fromDateIso) }.getOrNull() ?: return@launch
            val event = state.events.firstOrNull { it.id == decision.eventId } ?: return@launch
            val adjusted = event.copy(
                occurrenceSyncs = event.occurrenceSyncs.map { sync ->
                    val date = runCatching { LocalDate.parse(sync.dateIso) }.getOrNull()
                    if (date != null && !date.isBefore(fromDate)) {
                        if (regenerateFuture) {
                            sync.copy(externallyDeleted = false, deletionDecisionPending = false)
                        } else {
                            sync.copy(
                                externallyDeleted = sync.externallyDeleted || sync.deletionDecisionPending,
                                deletionDecisionPending = false,
                            )
                        }
                    } else {
                        sync
                    }
                },
            )
            val finalEvent = if (regenerateFuture) {
                syncManager.syncEventWindow(adjusted, state.lunarDays, state.today)
            } else {
                adjusted
            }
            val updated = state.events.map { if (it.id == finalEvent.id) finalEvent else it }
            eventStore.saveEvents(updated)
            _uiState.update {
                it.copy(
                    events = updated,
                    pendingExternalDeletion = nextPendingExternalDeletion(updated),
                    scannedSyncRange = null,
                )
            }
        }
    }

    fun refreshPhoneCalendars() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendars = syncManager.listVisibleCalendars()
            _uiState.update { it.copy(phoneCalendars = calendars) }
        }
    }

    fun setExternalCalendarSelected(calendarId: Long, selected: Boolean) {
        val next = _uiState.value.selectedExternalCalendarIds.toMutableSet().apply {
            if (selected) add(calendarId) else remove(calendarId)
        }
        preferences.saveSelectedExternalCalendarIds(next)
        _uiState.update { it.copy(selectedExternalCalendarIds = next) }
    }

    private fun loadLunarData() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { dataSource.loadDays() }
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { days -> state.copy(isLoadingData = false, lunarDays = days, dataError = null) },
                    onFailure = { error -> state.copy(isLoadingData = false, dataError = error.message ?: error.toString()) },
                )
            }
            scanVisibleWindow(force = true)
        }
    }

    private fun visibleScanRange(state: LunarUiState): SyncScanRange {
        return when (state.displayMode) {
            CalendarDisplayMode.MONTH -> {
                val start = state.visibleMonth.minusMonths(12).atDay(1)
                val end = state.visibleMonth.plusMonths(12).atEndOfMonth()
                SyncScanRange(maxOf(start, CalendarBounds.startDate), minOf(end, CalendarBounds.endDate))
            }
            CalendarDisplayMode.YEAR -> {
                val startYear = CalendarBounds.clampYear(state.visibleYear - 1)
                val endYear = CalendarBounds.clampYear(state.visibleYear + 1)
                SyncScanRange(
                    maxOf(LocalDate.of(startYear, 1, 1), CalendarBounds.startDate),
                    minOf(LocalDate.of(endYear, 12, 31), CalendarBounds.endDate),
                )
            }
        }
    }

    private fun mergeProviderScan(
        state: LunarUiState,
        providerOccurrences: List<CalendarSyncManager.ProviderOccurrence>,
        range: SyncScanRange,
    ): LunarUiState {
        val providersByEventId = providerOccurrences.groupBy { it.eventId }
        val eventsById = state.events.associateBy { it.id }.toMutableMap()
        providersByEventId.forEach { (eventId, providers) ->
            if (eventId !in eventsById) {
                rebuildEventFromProvider(providers, state.lunarDays)?.let { eventsById[eventId] = it }
            }
        }
        val mergedEvents = eventsById.values.map { event ->
            mergeEventScan(event, providersByEventId[event.id].orEmpty(), state.lunarDays, range)
        }.sortedWith(compareBy<LunarEvent> { it.startDateIso }.thenBy { it.title })
        val previousRange = state.scannedSyncRange
        val mergedRange = if (previousRange == null) {
            range
        } else {
            SyncScanRange(
                startDate = minOf(previousRange.startDate, range.startDate),
                endDate = maxOf(previousRange.endDate, range.endDate),
            )
        }
        return state.copy(
            events = mergedEvents,
            isSyncScanning = false,
            scannedSyncRange = mergedRange,
            pendingExternalDeletion = state.pendingExternalDeletion ?: nextPendingExternalDeletion(mergedEvents),
        )
    }

    private fun mergeEventScan(
        event: LunarEvent,
        providerOccurrences: List<CalendarSyncManager.ProviderOccurrence>,
        lunarDays: Map<LocalDate, LunarDay>,
        range: SyncScanRange,
    ): LunarEvent {
        val records = event.occurrenceSyncs.associateBy { it.dateIso }.toMutableMap()
        val providerDates = providerOccurrences.map { it.dateIso }.toSet()
        providerOccurrences.forEach { occurrence ->
            records[occurrence.dateIso] = EventOccurrenceSync(
                dateIso = occurrence.dateIso,
                providerEventId = occurrence.providerEventId,
                calendarId = occurrence.calendarId,
                uid = occurrence.uid,
                fingerprint = occurrence.fingerprint,
                titleOverride = occurrence.title.takeIf { it != event.title },
                remindersOverride = occurrence.reminders.takeIf { it != event.reminders },
                externallyDeleted = false,
                deletionDecisionPending = false,
            )
        }
        syncManager.occurrenceDates(event, lunarDays, range.startDate, range.endDate).forEach { date ->
            val dateIso = date.toString()
            val record = records[dateIso]
            if (
                record != null &&
                record.providerEventId > 0L &&
                !record.externallyDeleted &&
                !record.deletionDecisionPending &&
                dateIso !in providerDates
            ) {
                records[dateIso] = record.copy(
                    providerEventId = -1L,
                    externallyDeleted = true,
                    deletionDecisionPending = true,
                )
            }
        }
        return event.copy(occurrenceSyncs = records.values.sortedBy { it.dateIso })
    }

    private fun rebuildEventFromProvider(
        providers: List<CalendarSyncManager.ProviderOccurrence>,
        lunarDays: Map<LocalDate, LunarDay>,
    ): LunarEvent? {
        val first = providers.minByOrNull { it.occurrenceDate } ?: return null
        val firstLunar = lunarDays[first.occurrenceDate]
        val recurrence = first.recurrence ?: return null
        val lunarMonthNumber = first.lunarMonthNumber ?: firstLunar?.monthNumber ?: return null
        val lunarDayNumber = first.lunarDayNumber ?: firstLunar?.dayNumber ?: return null
        val isLeapMonth = first.isLeapMonth ?: firstLunar?.isLeapMonth ?: false
        val lunarMonthName = first.lunarMonthName ?: firstLunar?.monthName ?: lunarMonthNumber.toString()
        val lunarDayName = first.lunarDayName ?: firstLunar?.dayName ?: lunarDayNumber.toString()
        val reminders = first.reminders.take(MAX_REMINDERS_PER_EVENT).ifEmpty { LunarReminder.defaultReminders() }
        val event = LunarEvent(
            id = first.eventId,
            title = first.title,
            recurrence = recurrence,
            lunarMonthNumber = lunarMonthNumber,
            lunarMonthName = lunarMonthName,
            lunarDayNumber = lunarDayNumber,
            lunarDayName = lunarDayName,
            isLeapMonth = isLeapMonth,
            startDateIso = first.startDateIso ?: first.dateIso,
            reminders = reminders,
        )
        return event.copy(
            occurrenceSyncs = providers.map { occurrence ->
                EventOccurrenceSync(
                    dateIso = occurrence.dateIso,
                    providerEventId = occurrence.providerEventId,
                    calendarId = occurrence.calendarId,
                    uid = occurrence.uid,
                    fingerprint = occurrence.fingerprint,
                    titleOverride = occurrence.title.takeIf { it != event.title },
                    remindersOverride = occurrence.reminders.take(MAX_REMINDERS_PER_EVENT).takeIf { it != reminders },
                )
            }.sortedBy { it.dateIso },
        )
    }

    private fun nextPendingExternalDeletion(events: List<LunarEvent>): ExternalDeletionDecision? {
        return events.firstNotNullOfOrNull { event ->
            val pending = event.occurrenceSyncs
                .filter { it.deletionDecisionPending }
                .sortedBy { it.dateIso }
            val first = pending.firstOrNull() ?: return@firstNotNullOfOrNull null
            ExternalDeletionDecision(
                eventId = event.id,
                eventTitle = event.title,
                fromDateIso = first.dateIso,
                deletedCount = pending.count { it.dateIso >= first.dateIso },
            )
        }
    }

    private fun LunarEvent.providerEventIds(): List<Long> {
        return calendarProviderIds + occurrenceSyncs.map { it.providerEventId }
    }
}
