package com.example.collisioncalc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.*
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

    // DOB display (MMDDYYYY typing) -> store ISO
    var dobDisplay by remember(unitId) { mutableStateOf(isoToMmDdYyyyDigits(unit.dobIso)) }

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

                    ClearableTextField(
                        value = dobDisplay,
                        onValueChange = { raw ->
                            // keep the UX simple: user types digits, we format MM/DD/YYYY display
                            val masked = applyMmDdYyyyMaskDigits(raw)
                            dobDisplay = masked

                            // store ISO only when valid, clear when empty
                            val iso = parseMmDdYyyyDigitsToIso(masked)
                            // we don't mutate unit here; we’ll apply on save
                            // (so no “cursor jumping” side effects)
                        },
                        label = "DOB (MM/DD/YYYY)",
                        keyboardType = KeyboardType.Number,
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
                    val iso = parseMmDdYyyyDigitsToIso(dobDisplay) ?: if (dobDisplay.filter(Char::isDigit).isEmpty()) "" else unit.dobIso

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
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier
    )
}

/* ---------------------------
   DOB helpers
   - UI: MM/DD/YYYY (digits + slashes)
   - Store: ISO YYYY-MM-DD
---------------------------- */

private fun isoToMmDdYyyyDigits(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val d = LocalDate.parse(iso)
        "%02d/%02d/%04d".format(d.monthValue, d.dayOfMonth, d.year)
    }.getOrElse { "" }
}

// Accept any user input, keep digits, format as MM/DD/YYYY as they type
private fun applyMmDdYyyyMaskDigits(input: String): String {
    val digits = input.filter(Char::isDigit).take(8)
    val mm = digits.take(2)
    val dd = digits.drop(2).take(2)
    val yyyy = digits.drop(4).take(4)

    return buildString {
        append(mm)
        if (digits.length >= 3) append("/")
        append(dd)
        if (digits.length >= 5) append("/")
        append(yyyy)
    }.trimEnd('/')
}

// Parse MM/DD/YYYY -> ISO if valid
private fun parseMmDdYyyyDigitsToIso(display: String): String? {
    val digits = display.filter(Char::isDigit)
    if (digits.length != 8) return null
    val mm = digits.substring(0, 2).toIntOrNull() ?: return null
    val dd = digits.substring(2, 4).toIntOrNull() ?: return null
    val yyyy = digits.substring(4, 8).toIntOrNull() ?: return null
    return runCatching { LocalDate.of(yyyy, mm, dd).toString() }.getOrNull()
}
