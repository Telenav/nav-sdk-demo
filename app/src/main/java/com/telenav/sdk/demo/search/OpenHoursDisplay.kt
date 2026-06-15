package com.telenav.sdk.demo.search

import com.telenav.sdk.entity.model.base.Entity
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class OpenHoursSummary(
    val statusLabel: String,
    val isOpen: Boolean?,
    /** e.g. "Closes 8:30 PM" (without leading dot). */
    val closingLabel: String,
    /** Extra schedule text for Overview tab when available. */
    val fullSchedule: String
)

object OpenHoursDisplay {

    fun summarize(entity: Entity): OpenHoursSummary? {
        val openHours = entity.facets?.openHours ?: return null
        val displayText = openHours.displayText?.trim().orEmpty()
        val timeZone = entity.place?.address?.timeZone?.trim()

        if (displayText.isNotEmpty()) {
            return summaryFromDisplayText(displayText)
        }

        if (openHours.isOpen24hours == true) {
            return OpenHoursSummary(
                statusLabel = "Open 24 hours",
                isOpen = true,
                closingLabel = "",
                fullSchedule = ""
            )
        }

        val openNow = readOpenNow(openHours) ?: return null
        val statusLabel = if (openNow) "Open" else "Closed"
        val closingLabel = closingLabelFromRegularHours(openHours, openNow, timeZone)
        return OpenHoursSummary(
            statusLabel = statusLabel,
            isOpen = openNow,
            closingLabel = closingLabel,
            fullSchedule = ""
        )
    }

    private fun summaryFromDisplayText(displayText: String): OpenHoursSummary {
        val parts = displayText.split("·").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            return OpenHoursSummary(displayText, null, "", displayText)
        }
        val statusLabel = parts.first()
        val isOpen = when {
            statusLabel.equals("Open", ignoreCase = true) -> true
            statusLabel.equals("Closed", ignoreCase = true) -> false
            statusLabel.contains("24", ignoreCase = true) -> true
            else -> null
        }
        val closingLabel = parts.drop(1).joinToString(" · ")
        return OpenHoursSummary(
            statusLabel = statusLabel,
            isOpen = isOpen,
            closingLabel = closingLabel,
            fullSchedule = if (parts.size > 2) displayText else ""
        )
    }

    private fun closingLabelFromRegularHours(
        openHours: com.telenav.sdk.entity.model.base.FacetOpenHours,
        isOpenNow: Boolean,
        timeZone: String?
    ): String {
        val periods = openHours.regularOpenHours.orEmpty()
        if (periods.isEmpty()) return ""

        val calendar = Calendar.getInstance(
            timeZone?.takeIf { it.isNotEmpty() }?.let { TimeZone.getTimeZone(it) }
                ?: TimeZone.getDefault()
        )
        val googleDay = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val todayPeriods = periods.filter { period ->
            periodDay(period) == googleDay
        }

        if (isOpenNow) {
            for (period in todayPeriods) {
                for (slot in period.openTime.orEmpty()) {
                    val from = parseTimeToMinutes(slot.from) ?: continue
                    val to = parseTimeToMinutes(slot.to) ?: continue
                    if (nowMinutes in from until to) {
                        return "Closes ${formatTime12(slot.to)}"
                    }
                }
            }
            return ""
        }

        for (period in todayPeriods) {
            for (slot in period.openTime.orEmpty()) {
                val from = parseTimeToMinutes(slot.from) ?: continue
                if (nowMinutes < from) {
                    return "Opens ${formatTime12(slot.from)}"
                }
            }
        }
        return ""
    }

    private fun periodDay(period: Any): Int? {
        return runCatching {
            period.javaClass.methods.firstOrNull {
                it.name.equals("getDay", true) || it.name == "day"
            }?.invoke(period)?.let { value ->
                when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull()
                    else -> value.toString().toIntOrNull()
                }
            }
        }.getOrNull()
    }

    private fun readOpenNow(openHours: Any): Boolean? =
        runCatching {
            openHours.javaClass.methods.firstOrNull {
                it.name.equals("isOpenNow", true) || it.name.equals("getOpenNow", true)
            }?.invoke(openHours) as? Boolean
        }.getOrNull()

    private fun parseTimeToMinutes(time: String?): Int? {
        if (time.isNullOrBlank()) return null
        val parts = time.trim().split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun formatTime12(time24: String?): String {
        val minutes = parseTimeToMinutes(time24) ?: return time24.orEmpty()
        val hour = minutes / 60
        val minute = minutes % 60
        val isPm = hour >= 12
        val hour12 = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        return if (minute == 0) {
            String.format(Locale.getDefault(), "%d %s", hour12, if (isPm) "PM" else "AM")
        } else {
            String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, if (isPm) "PM" else "AM")
        }
    }
}
