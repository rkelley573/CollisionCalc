package com.collisioncalc.app.ui.components

import java.time.LocalDate

/**
 * Date display: "MM/DD/YYYY"
 * Stored: "YYYY-MM-DD" (ISO)
 */
fun isoToMmDdYyyyDisplay(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val d = LocalDate.parse(iso)
        "%02d/%02d/%04d".format(d.monthValue, d.dayOfMonth, d.year)
    }.getOrElse { "" }
}

fun applyMmDdYyyyMask(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(8)
    val mm = digits.take(2)
    val dd = digits.drop(2).take(2)
    val yyyy = digits.drop(4).take(4)

    return buildString {
        append(mm)
        if (digits.length >= 3) append("/")
        append(dd)
        if (digits.length >= 5) append("/")
        append(yyyy)
    }
}

fun parseMmDdYyyyToIso(display: String): String? {
    val digits = display.filter(Char::isDigit)
    if (digits.length != 8) return null
    val mm = digits.substring(0, 2).toIntOrNull() ?: return null
    val dd = digits.substring(2, 4).toIntOrNull() ?: return null
    val yyyy = digits.substring(4, 8).toIntOrNull() ?: return null
    return runCatching { LocalDate.of(yyyy, mm, dd).toString() }.getOrNull()
}

/**
 * Time display: "HH:MM"
 * Stored: "HH:mm"
 */
fun hhMmToDisplay(hhMm: String): String {
    if (hhMm.isBlank()) return ""
    val parts = hhMm.split(":")
    val hh = parts.getOrNull(0)?.toIntOrNull() ?: return ""
    val mm = parts.getOrNull(1)?.toIntOrNull() ?: return ""
    return "%02d:%02d".format(hh, mm)
}

fun applyHhMmMask(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(4)
    val hh = digits.take(2)
    val mm = digits.drop(2).take(2)
    return buildString {
        append(hh)
        if (digits.length >= 3) append(":")
        append(mm)
    }
}

fun parseDisplayToHhMm(display: String): String? {
    val digits = display.filter(Char::isDigit)
    if (digits.length != 4) return null
    val hh = digits.substring(0, 2).toIntOrNull() ?: return null
    val mm = digits.substring(2, 4).toIntOrNull() ?: return null
    if (hh !in 0..23 || mm !in 0..59) return null
    return "%02d:%02d".format(hh, mm)
}
