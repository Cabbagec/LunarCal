package me.xcel.lunarcal.data

import android.content.Context
import java.time.LocalDate

class LunarDataSource(private val context: Context) {
    fun loadDays(): Map<LocalDate, LunarDay> {
        return context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1)
                .filter { it.isNotBlank() }
                .map(::parseLine)
                .associateBy { it.date }
        }
    }

    private fun parseLine(line: String): LunarDay {
        val parts = line.trimEnd('\r').split(",", limit = 10)
        require(parts.size == 10) { "Malformed lunar CSV line: $line" }
        val solarTerm = parts[9].ifBlank { null }
        return LunarDay(
            date = LocalDate.parse(parts[0]),
            lunarYear = parts[1].toInt(),
            ganzhi = parts[2],
            zodiac = parts[3],
            monthNumber = parts[4].toInt(),
            monthName = parts[5],
            isLeapMonth = parts[6] == "1",
            dayNumber = parts[7].toInt(),
            dayName = parts[8],
            solarTerm = solarTerm,
        )
    }

    companion object {
        private const val ASSET_PATH = "lunar/hko_lunar_1901_2100.csv"
    }
}
