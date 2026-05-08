package me.xcel.lunarcal.data

import android.content.Context

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    fun calendarDisplayMode(): CalendarDisplayMode {
        return runCatching {
            CalendarDisplayMode.valueOf(preferences.getString(KEY_DISPLAY_MODE, CalendarDisplayMode.MONTH.name).orEmpty())
        }.getOrDefault(CalendarDisplayMode.MONTH)
    }

    fun saveCalendarDisplayMode(mode: CalendarDisplayMode) {
        preferences.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
    }

    fun themePreference(): ThemePreference {
        return runCatching {
            ThemePreference.valueOf(preferences.getString(KEY_THEME, ThemePreference.SYSTEM.name).orEmpty())
        }.getOrDefault(ThemePreference.SYSTEM)
    }

    fun saveThemePreference(preference: ThemePreference) {
        preferences.edit().putString(KEY_THEME, preference.name).apply()
    }

    fun appLanguage(): AppLanguage {
        return runCatching {
            AppLanguage.valueOf(preferences.getString(KEY_LANGUAGE, defaultLanguage().name).orEmpty())
        }.getOrDefault(defaultLanguage())
    }

    fun saveAppLanguage(language: AppLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    fun selectedExternalCalendarIds(): Set<Long> {
        return preferences.getStringSet(KEY_EXTERNAL_CALENDARS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun saveSelectedExternalCalendarIds(ids: Set<Long>) {
        preferences.edit().putStringSet(KEY_EXTERNAL_CALENDARS, ids.map { it.toString() }.toSet()).apply()
    }

    companion object {
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_EXTERNAL_CALENDARS = "external_calendars"

        private fun defaultLanguage(): AppLanguage {
            return if (java.util.Locale.getDefault().language == "zh") {
                AppLanguage.CHINESE
            } else {
                AppLanguage.ENGLISH
            }
        }
    }
}
