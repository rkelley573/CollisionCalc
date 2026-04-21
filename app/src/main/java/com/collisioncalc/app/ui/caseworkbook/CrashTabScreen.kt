package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CollisionInfo
import com.collisioncalc.app.data.CrashLocation
import java.time.LocalDate

@Composable
fun CrashTabScreen(
    crashInfo: CollisionInfo,
    onCrashInfoChange: (CollisionInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Date/Time: keep full TextFieldValue so caret behaves correctly with masks ---
    var dateTf by remember { mutableStateOf(TextFieldValue(isoToDisplayDate(crashInfo.dateIso))) }
    var timeTf by remember { mutableStateOf(TextFieldValue(crashInfo.time24h)) }

    LaunchedEffect(crashInfo.dateIso) {
        val shown = isoToDisplayDate(crashInfo.dateIso)
        if (shown != dateTf.text) dateTf = dateTf.copy(text = shown, selection = TextRange(shown.length))
    }
    LaunchedEffect(crashInfo.time24h) {
        val shown = crashInfo.time24h
        if (shown != timeTf.text) timeTf = timeTf.copy(text = shown, selection = TextRange(shown.length))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Crash Info", style = MaterialTheme.typography.titleLarge)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ClearableOutlinedTextFieldValue(
                        value = dateTf,
                        onValueChange = { incoming ->
                            val masked = applyDateMaskPreserveCaret(incoming)
                            dateTf = masked

                            val iso = displayDateToIso(masked.text)
                            when {
                                iso != null -> onCrashInfoChange(crashInfo.copy(dateIso = iso))
                                masked.text.filter(Char::isDigit).isEmpty() -> onCrashInfoChange(crashInfo.copy(dateIso = ""))
                                else -> Unit
                            }
                        },
                        label = "Date (MM/DD/YYYY)",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    ClearableOutlinedTextFieldValue(
                        value = timeTf,
                        onValueChange = { incoming ->
                            val masked = applyTimeMaskPreserveCaret(incoming)
                            timeTf = masked

                            val t = parseHhMm(masked.text)
                            when {
                                t != null -> onCrashInfoChange(crashInfo.copy(time24h = t))
                                masked.text.filter(Char::isDigit).isEmpty() -> onCrashInfoChange(crashInfo.copy(time24h = ""))
                                else -> Unit
                            }
                        },
                        label = "Time (HH:MM)",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Primary location
                LocationEditorV2(
                    title = "Primary Location",
                    value = crashInfo.location,
                    onChange = { onCrashInfoChange(crashInfo.copy(location = it)) }
                )

                // NEW: Nearest intersecting road / reference marker
                LocationEditorV2(
                    title = "Nearest intersecting road / reference marker",
                    value = crashInfo.nearestReference,
                    onChange = { onCrashInfoChange(crashInfo.copy(nearestReference = it)) }
                )
            }
        }
    }
}

/* ---------------------------
   Location Editor (Non-Intersection default)
---------------------------- */

@Composable
private fun LocationEditorV2(
    title: String,
    value: CrashLocation,
    onChange: (CrashLocation) -> Unit
) {
    // Normalize: default to NonIntersection (your preference)
    val loc = when (value) {
        is CrashLocation.NonIntersection -> value
        CrashLocation.Unspecified -> CrashLocation.NonIntersection()
        is CrashLocation.Intersection -> CrashLocation.NonIntersection(
            blockNumber = "",
            streetName = value.street1,
            city = value.city,
            state = value.state,
            zip = value.zip,
            speedLimitMph = value.speedLimitMph
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ClearableOutlinedTextField(
                    value = loc.blockNumber,
                    onValueChange = { onChange(loc.copy(blockNumber = it.filter(Char::isDigit))) },
                    label = "Block #",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )

                ClearableOutlinedTextField(
                    value = loc.streetName,
                    onValueChange = { onChange(loc.copy(streetName = it)) },
                    label = "Street name",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(2f)
                )
            }

            ClearableOutlinedTextField(
                value = loc.city,
                onValueChange = { onChange(loc.copy(city = it)) },
                label = "City",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ClearableOutlinedTextField(
                    value = loc.state,
                    onValueChange = { onChange(loc.copy(state = normalizeState(it))) },
                    label = "State (2-letter)",
                    keyboardType = KeyboardType.Ascii,
                    modifier = Modifier.weight(1f)
                )

                ClearableOutlinedTextField(
                    value = loc.zip,
                    onValueChange = { onChange(loc.copy(zip = normalizeZip(it))) },
                    label = "Zip",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            ClearableOutlinedTextField(
                value = loc.speedLimitMph,
                onValueChange = { onChange(loc.copy(speedLimitMph = it.filter(Char::isDigit))) },
                label = "Speed limit (mph)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/* ---------------------------
   Clearable helpers
---------------------------- */

@Composable
private fun ClearableOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ClearableOutlinedTextFieldValue(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = {
            if (value.text.isNotEmpty()) {
                IconButton(onClick = { onValueChange(TextFieldValue("")) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier
    )
}

private fun normalizeState(s: String) = s.trim().uppercase().take(2)

private fun normalizeZip(s: String): String {
    val digits = s.filter(Char::isDigit)
    return when {
        digits.length <= 5 -> digits
        else -> digits.substring(0, 5) + "-" + digits.substring(5, minOf(9, digits.length))
    }
}

/* ---------------------------
   Masking that preserves caret (fixes your bug)
---------------------------- */

private fun applyDateMaskPreserveCaret(incoming: TextFieldValue): TextFieldValue {
    val digits = incoming.text.filter(Char::isDigit).take(8)

    // count digits before the caret in the incoming text
    val caret = incoming.selection.start.coerceIn(0, incoming.text.length)
    val digitsBeforeCaret = incoming.text.take(caret).count(Char::isDigit)

    val mm = digits.take(2)
    val dd = digits.drop(2).take(2)
    val yyyy = digits.drop(4).take(4)

    val masked = buildString {
        append(mm)
        if (digits.length > 2) append("/")
        append(dd)
        if (digits.length > 4) append("/")
        append(yyyy)
    }

    // place caret after the same number of digits in the masked string
    val newCaret = indexAfterNthDigit(masked, digitsBeforeCaret)
    return TextFieldValue(masked, selection = TextRange(newCaret))
}

private fun applyTimeMaskPreserveCaret(incoming: TextFieldValue): TextFieldValue {
    val digits = incoming.text.filter(Char::isDigit).take(4)

    val caret = incoming.selection.start.coerceIn(0, incoming.text.length)
    val digitsBeforeCaret = incoming.text.take(caret).count(Char::isDigit)

    val hh = digits.take(2)
    val mm = digits.drop(2).take(2)

    val masked = buildString {
        append(hh)
        if (digits.length > 2) append(":")
        append(mm)
    }

    val newCaret = indexAfterNthDigit(masked, digitsBeforeCaret)
    return TextFieldValue(masked, selection = TextRange(newCaret))
}

/**
 * Returns the string index that lands immediately after the Nth digit in [s].
 * If N is 0 -> 0. If N is beyond digits count -> end of string.
 */
private fun indexAfterNthDigit(s: String, nDigits: Int): Int {
    if (nDigits <= 0) return 0
    var count = 0
    for (i in s.indices) {
        if (s[i].isDigit()) {
            count++
            if (count == nDigits) return (i + 1)
        }
    }
    return s.length
}

/* ---------------------------
   Date/time parsing
---------------------------- */

private fun isoToDisplayDate(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val d = LocalDate.parse(iso)
        "%02d/%02d/%04d".format(d.monthValue, d.dayOfMonth, d.year)
    }.getOrElse { "" }
}

private fun displayDateToIso(display: String): String? {
    val digits = display.filter(Char::isDigit)
    if (digits.length != 8) return null
    val mm = digits.substring(0, 2).toIntOrNull() ?: return null
    val dd = digits.substring(2, 4).toIntOrNull() ?: return null
    val yyyy = digits.substring(4, 8).toIntOrNull() ?: return null
    return runCatching { LocalDate.of(yyyy, mm, dd).toString() }.getOrNull()
}

private fun parseHhMm(display: String): String? {
    val digits = display.filter(Char::isDigit)
    if (digits.length != 4) return null
    val hh = digits.substring(0, 2).toIntOrNull() ?: return null
    val mm = digits.substring(2, 4).toIntOrNull() ?: return null
    if (hh !in 0..23) return null
    if (mm !in 0..59) return null
    return "%02d:%02d".format(hh, mm)
}
