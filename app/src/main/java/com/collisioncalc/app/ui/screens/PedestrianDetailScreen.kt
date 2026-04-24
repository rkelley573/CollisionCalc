package com.collisioncalc.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedestrianDetailScreen(
    caseFile: CaseFile,
    unitId: UnitId,
    onBack: () -> Unit,
    onSavePedestrian: (PedestrianUnit) -> Unit
) {
    val unit = caseFile.units.firstOrNull { it.unitId == unitId } as? PedestrianUnit

    if (unit == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pedestrian") },
                    navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("Pedestrian not found.")
            }
        }
        return
    }

    var label by remember(unitId) { mutableStateOf(unit.label) }

    var last by remember(unitId) { mutableStateOf(unit.name.last) }
    var first by remember(unitId) { mutableStateOf(unit.name.first) }
    var middle by remember(unitId) { mutableStateOf(unit.name.middle) }
    var suffix by remember(unitId) { mutableStateOf(unit.name.suffix) }

    // DOB (store digits only: MMDDYYYY) -> store ISO
    var dobDigits by remember(unitId) { mutableStateOf(isoToMmDdYyyyDigits(unit.dobIso)) }

    var address by remember(unitId) { mutableStateOf(unit.address) }
    var phone by remember(unitId) { mutableStateOf(unit.phone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(label.ifBlank { "Pedestrian" }) },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pedestrian Info", style = MaterialTheme.typography.titleSmall)

                    ClearableTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = "Label",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Name", style = MaterialTheme.typography.labelLarge)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ClearableTextField(
                            value = last,
                            onValueChange = { last = it },
                            label = "Last",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                        ClearableTextField(
                            value = first,
                            onValueChange = { first = it },
                            label = "First",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ClearableTextField(
                            value = middle,
                            onValueChange = { middle = it },
                            label = "Middle",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                        ClearableTextField(
                            value = suffix,
                            onValueChange = { suffix = it },
                            label = "Suffix",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DobMmDdYyyyField(
                        digits = dobDigits,
                        onDigitsChange = { dobDigits = it },
                        label = "DOB (MM/DD/YYYY)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    ClearableTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Address",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )

                    ClearableTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone",
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    val iso =
                        parseMmDdYyyyDigitsToIso(dobDigits)
                            ?: if (dobDigits.filter(Char::isDigit).isEmpty()) "" else unit.dobIso

                    val updated = unit.copy(
                        label = label,
                        name = NameParts(last = last, first = first, middle = middle, suffix = suffix),
                        dobIso = iso,
                        address = address,
                        phone = phone
                    )
                    onSavePedestrian(updated)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Pedestrian") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------------------------
   Clearable input helper
---------------------------- */

@Composable
private fun ClearableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.focusProperties { canFocus = false }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier
    )
}

/* ---------------------------
   DOB helpers
   - UI: digits stored as MMDDYYYY, displayed as MM/DD/YYYY via VisualTransformation
   - Store: ISO YYYY-MM-DD
---------------------------- */

private fun isoToMmDdYyyyDigits(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val d = LocalDate.parse(iso)
        "%02d%02d%04d".format(d.monthValue, d.dayOfMonth, d.year)
    }.getOrElse { "" }
}

// Parse digits MMDDYYYY -> ISO if valid
private fun parseMmDdYyyyDigitsToIso(digitsOnly: String): String? {
    val digits = digitsOnly.filter(Char::isDigit)
    if (digits.length != 8) return null
    val mm = digits.substring(0, 2).toIntOrNull() ?: return null
    val dd = digits.substring(2, 4).toIntOrNull() ?: return null
    val yyyy = digits.substring(4, 8).toIntOrNull() ?: return null
    return runCatching { LocalDate.of(yyyy, mm, dd).toString() }.getOrNull()
}

@Composable
private fun DobMmDdYyyyField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = digits,
        onValueChange = { raw -> onDigitsChange(raw.filter(Char::isDigit).take(8)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = MmDdYyyyVisualTransformation(),
        trailingIcon = {
            if (digits.isNotEmpty()) {
                IconButton(onClick = { onDigitsChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier
    )
}

private class MmDdYyyyVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val digits = text.text.filter(Char::isDigit).take(8)

        val transformed = buildString {
            val mm = digits.take(2)
            val dd = digits.drop(2).take(2)
            val yyyy = digits.drop(4).take(4)

            append(mm)
            if (digits.length > 2) append("/")
            append(dd)
            if (digits.length > 4) append("/")
            append(yyyy)
        }

        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, digits.length)
                return when {
                    digits.length <= 2 -> o
                    digits.length <= 4 -> if (o <= 2) o else o + 1
                    else -> when {
                        o <= 2 -> o
                        o <= 4 -> o + 1
                        else -> o + 2
                    }
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val t = offset.coerceIn(0, transformed.length)
                return when {
                    digits.length <= 2 -> t.coerceIn(0, digits.length)
                    digits.length <= 4 -> {
                        // only first slash exists at index 2
                        when {
                            t <= 2 -> t
                            else -> (t - 1).coerceIn(0, digits.length)
                        }
                    }
                    else -> {
                        // slashes at indexes 2 and 5
                        when {
                            t <= 2 -> t
                            t <= 5 -> (t - 1).coerceIn(0, digits.length)
                            else -> (t - 2).coerceIn(0, digits.length)
                        }
                    }
                }
            }
        }

        return androidx.compose.ui.text.input.TransformedText(
            androidx.compose.ui.text.AnnotatedString(transformed),
            offsetMapping
        )
    }
}
