package com.mabbology.aurajournal.ui.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun parseDate(isoString: String): LocalDate {
    return try {
        OffsetDateTime.parse(isoString).toLocalDate()
    } catch (_: Exception) {
        LocalDate.now()
    }
}

fun formatDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return when {
        date.isEqual(today) -> "Today"
        date.isEqual(yesterday) -> "Yesterday"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            date.format(formatter)
        }
    }
}

fun formatTime(isoString: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        odt.format(formatter)
    } catch (_: Exception) {
        "Invalid time"
    }
}
