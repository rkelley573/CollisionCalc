package com.example.collisioncalc.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd
private val UI_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")

fun isoToUiDate(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        LocalDate.parse(iso, ISO_DATE).format(UI_DATE)
    } catch (_: DateTimeParseException) {
        ""
    }
}

fun uiToIsoDate(mmddyyyy: String): String {
    if (mmddyyyy.isBlank()) return ""
    return try {
        LocalDate.parse(mmddyyyy, UI_DATE).format(ISO_DATE)
    } catch (_: DateTimeParseException) {
        ""
    }
}

fun isValidUiDate(mmddyyyy: String): Boolean = uiToIsoDate(mmddyyyy).isNotBlank()

fun isValidTime24h(hhmm: String): Boolean {
    val r = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")
    return hhmm.isBlank() || r.matches(hhmm)
}