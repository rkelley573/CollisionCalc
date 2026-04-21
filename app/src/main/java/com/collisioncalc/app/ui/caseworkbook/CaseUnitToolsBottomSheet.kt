package com.collisioncalc.app.ui.caseworkbook

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcType
import com.collisioncalc.app.data.CalcValue
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.UnitId
import kotlin.math.abs

enum class Cat { SPEED, LENGTH, MASS }

// Helper for dropdowns
data class UnitOpt(val key: String, val label: String)

// STD solver target
private enum class StdSolveFor { SPEED, DISTANCE, TIME }

/* ---------------------------
   KEYBOARD helper
---------------------------- */

private fun handleTabEnter(
    e: KeyEvent,
    onTab: () -> Unit,
    onEnter: () -> Unit
): Boolean {
    // Works on older Compose versions (no e.type)
    if (e.nativeKeyEvent.action != AndroidKeyEvent.ACTION_DOWN) return false

    return when (e.nativeKeyEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_TAB -> {
            onTab()
            true
        }
        AndroidKeyEvent.KEYCODE_ENTER,
        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
            onEnter()
            true
        }
        else -> false
    }
}

/* ---------------------------
   IN-CASE UNIT TOOLS (Bottom Sheet)
---------------------------- */

private enum class UnitToolRoute { LIST, CONVERTER, STD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseUnitToolsBottomSheet(
    caseFile: CaseFile,
    onDismiss: () -> Unit,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    var route by remember { mutableStateOf(UnitToolRoute.LIST) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = when (route) {
                        UnitToolRoute.LIST -> "Unit Tools (This Case)"
                        UnitToolRoute.CONVERTER -> "Unit Converter"
                        UnitToolRoute.STD -> "Speed / Time / Distance"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onDismiss) { Text("Close") }
            }

            when (route) {
                UnitToolRoute.LIST -> {
                    ToolRow(
                        title = "Unit Converter",
                        subtitle = "Convert values, attribute to Units, save into this case",
                        onClick = { route = UnitToolRoute.CONVERTER }
                    )
                    ToolRow(
                        title = "Speed / Time / Distance",
                        subtitle = "Solve v = d / t, attribute to Units, save into this case",
                        onClick = { route = UnitToolRoute.STD }
                    )
                }

                UnitToolRoute.CONVERTER -> {
                    TextButton(onClick = { route = UnitToolRoute.LIST }) { Text("← Back") }
                    UnitConverterInCase(caseFile = caseFile, onSaveCalculation = onSaveCalculation)
                }

                UnitToolRoute.STD -> {
                    TextButton(onClick = { route = UnitToolRoute.LIST }) { Text("← Back") }
                    SpeedTimeDistanceInCase(caseFile = caseFile, onSaveCalculation = onSaveCalculation)
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ToolRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ---------------------------
   UNIT CONVERTER (in-case)
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitConverterInCase(
    caseFile: CaseFile,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    fun toBase(cat: Cat, v: Double, u: String): Double = when (cat) {
        Cat.SPEED -> when (u) {
            "m/s" -> v
            "ft/s" -> v * 0.3048
            "mph" -> v * 0.44704
            else -> v
        }
        Cat.LENGTH -> when (u) {
            "m" -> v
            "km" -> v * 1000.0
            "ft" -> v * 0.3048
            "in" -> v * 0.0254
            else -> v
        }
        Cat.MASS -> when (u) {
            "kg" -> v
            "lb" -> v * 0.45359237
            else -> v
        }
    }

    fun fromBase(cat: Cat, v: Double, u: String): Double = when (cat) {
        Cat.SPEED -> when (u) {
            "m/s" -> v
            "ft/s" -> v / 0.3048
            "mph" -> v / 0.44704
            else -> v
        }
        Cat.LENGTH -> when (u) {
            "m" -> v
            "km" -> v / 1000.0
            "ft" -> v / 0.3048
            "in" -> v / 0.0254
            else -> v
        }
        Cat.MASS -> when (u) {
            "kg" -> v
            "lb" -> v / 0.45359237
            else -> v
        }
    }

    fun fmt(x: Double): String = "%.6f".format(abs(x)).trimEnd('0').trimEnd('.')

    val focusManager = LocalFocusManager.current
    val frValue = remember { FocusRequester() }
    val frCalc = remember { FocusRequester() }
    val frSave = remember { FocusRequester() }

    var cat by remember { mutableStateOf(Cat.SPEED) }
    val unitOptions = remember(cat) {
        when (cat) {
            Cat.SPEED -> listOf(UnitOpt("mph", "mph"), UnitOpt("ft/s", "ft/s"), UnitOpt("m/s", "m/s"))
            Cat.LENGTH -> listOf(UnitOpt("ft", "ft"), UnitOpt("in", "in"), UnitOpt("m", "m"), UnitOpt("km", "km"))
            Cat.MASS -> listOf(UnitOpt("lb", "lb"), UnitOpt("kg", "kg"))
        }
    }

    var fromUnit by remember(cat) { mutableStateOf(unitOptions.first().key) }
    var toUnit by remember(cat) { mutableStateOf(unitOptions.getOrNull(1)?.key ?: unitOptions.first().key) }

    var valueText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Double?>(null) }
    var attributedUnitIds by remember { mutableStateOf<Set<UnitId>>(emptySet()) }

    fun calculate() {
        val v = valueText.trim().toDoubleOrNull()
        if (v == null) {
            result = null
            return
        }
        val base = toBase(cat, v, fromUnit)
        result = fromBase(cat, base, toUnit)
    }

    val steps = remember(cat, valueText, fromUnit, toUnit, result) {
        buildList {
            add("Unit Conversion")
            add("Category: ${cat.name}")
            val v = valueText.trim().toDoubleOrNull()
            add("Input: ${v?.let { fmt(it) } ?: "—"} $fromUnit")
            add("To: $toUnit")
            add("Result: ${result?.let { fmt(it) } ?: "—"} $toUnit")
        }
    }

    LaunchedEffect(caseFile.units.size) {
        if (caseFile.units.size == 1 && attributedUnitIds.isEmpty()) {
            attributedUnitIds = setOf(caseFile.units.first().unitId)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Text("Category", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = cat == Cat.SPEED,
                    onClick = { cat = Cat.SPEED; result = null },
                    label = { Text("Speed") }
                )
                FilterChip(
                    selected = cat == Cat.LENGTH,
                    onClick = { cat = Cat.LENGTH; result = null },
                    label = { Text("Length") }
                )
                FilterChip(
                    selected = cat == Cat.MASS,
                    onClick = { cat = Cat.MASS; result = null },
                    label = { Text("Mass") }
                )
            }

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("Value") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { frCalc.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(frValue)
                    .onPreviewKeyEvent { e ->
                        handleTabEnter(
                            e = e,
                            onTab = { frCalc.requestFocus() },
                            onEnter = { calculate(); frSave.requestFocus() }
                        )
                    }
            )

            UnitDropdown(label = "From", options = unitOptions, selectedKey = fromUnit) {
                fromUnit = it
                result = null
            }
            UnitDropdown(label = "To", options = unitOptions, selectedKey = toUnit) {
                toUnit = it
                result = null
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { calculate() },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(frCalc)
                        .onPreviewKeyEvent { e ->
                            handleTabEnter(
                                e = e,
                                onTab = { frSave.requestFocus() },
                                onEnter = { calculate(); frSave.requestFocus() }
                            )
                        }
                ) { Text("Calculate") }

                OutlinedButton(
                    onClick = { valueText = ""; result = null; focusManager.clearFocus() },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }
            }

            Text(
                text = "Result: ${result?.let { "${fmt(it)} $toUnit" } ?: "—"}",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            UnitsOnlyAttributionPicker(
                caseFile = caseFile,
                selectedUnitIds = attributedUnitIds,
                onChange = { attributedUnitIds = it }
            )

            Button(
                onClick = {
                    val v = valueText.trim().toDoubleOrNull() ?: return@Button
                    val r = result ?: return@Button

                    val calc = SavedCalculation(
                        type = CalcType.UNIT_CONVERTER,
                        title = "Unit Conversion",
                        inputs = listOf(CalcValue("Value", v, fromUnit)),
                        outputs = listOf(CalcValue("Result", r, toUnit)),
                        equationText = "$fromUnit → $toUnit",
                        steps = steps,
                        attributedUnitIds = attributedUnitIds
                    )
                    onSaveCalculation(calc)
                },
                enabled = valueText.trim().toDoubleOrNull() != null && result != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(frSave)
            ) { Text("Save to Case") }

            LaunchedEffect(Unit) { frValue.requestFocus() }
        }
    }
}

/* ---------------------------
   SPEED / TIME / DISTANCE (in-case)
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedTimeDistanceInCase(
    caseFile: CaseFile,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    val distUnits = listOf(UnitOpt("ft", "ft"), UnitOpt("mi", "mi"), UnitOpt("m", "m"), UnitOpt("km", "km"))
    val timeUnits = listOf(UnitOpt("s", "s"), UnitOpt("min", "min"), UnitOpt("hr", "hr"))
    val speedUnits = listOf(UnitOpt("mph", "mph"), UnitOpt("ft/s", "ft/s"), UnitOpt("m/s", "m/s"))

    fun distToMeters(v: Double, u: String) = when (u) {
        "m" -> v
        "km" -> v * 1000.0
        "ft" -> v * 0.3048
        "mi" -> v * 1609.344
        else -> v
    }

    fun metersToDist(v: Double, u: String) = when (u) {
        "m" -> v
        "km" -> v / 1000.0
        "ft" -> v / 0.3048
        "mi" -> v / 1609.344
        else -> v
    }

    fun timeToSeconds(v: Double, u: String) = when (u) {
        "s" -> v
        "min" -> v * 60.0
        "hr" -> v * 3600.0
        else -> v
    }

    fun secondsToTime(v: Double, u: String) = when (u) {
        "s" -> v
        "min" -> v / 60.0
        "hr" -> v / 3600.0
        else -> v
    }

    fun speedToMps(v: Double, u: String) = when (u) {
        "m/s" -> v
        "ft/s" -> v * 0.3048
        "mph" -> v * 0.44704
        else -> v
    }

    fun mpsToSpeed(v: Double, u: String) = when (u) {
        "m/s" -> v
        "ft/s" -> v / 0.3048
        "mph" -> v / 0.44704
        else -> v
    }

    fun fmt(x: Double): String = "%.6f".format(abs(x)).trimEnd('0').trimEnd('.')

    val fr1 = remember { FocusRequester() }
    val fr2 = remember { FocusRequester() }
    val frCalc = remember { FocusRequester() }
    val frSave = remember { FocusRequester() }

    var solveFor by remember { mutableStateOf(StdSolveFor.SPEED) }

    var dText by remember { mutableStateOf("") }
    var tText by remember { mutableStateOf("") }
    var vText by remember { mutableStateOf("") }

    var dUnit by remember { mutableStateOf("ft") }
    var tUnit by remember { mutableStateOf("s") }
    var vUnit by remember { mutableStateOf("mph") }

    var result by remember { mutableStateOf<Double?>(null) }
    var attributedUnitIds by remember { mutableStateOf<Set<UnitId>>(emptySet()) }

    fun calculate() {
        result = null

        val d = dText.trim().toDoubleOrNull()
        val t = tText.trim().toDoubleOrNull()
        val v = vText.trim().toDoubleOrNull()

        when (solveFor) {
            StdSolveFor.SPEED -> {
                if (d == null || t == null || t == 0.0) return
                val m = distToMeters(d, dUnit)
                val s = timeToSeconds(t, tUnit)
                val mps = m / s
                result = mpsToSpeed(mps, vUnit)
            }
            StdSolveFor.DISTANCE -> {
                if (v == null || t == null) return
                val mps = speedToMps(v, vUnit)
                val s = timeToSeconds(t, tUnit)
                val m = mps * s
                result = metersToDist(m, dUnit)
            }
            StdSolveFor.TIME -> {
                if (d == null || v == null) return
                val m = distToMeters(d, dUnit)
                val mps = speedToMps(v, vUnit)
                if (mps == 0.0) return
                val s = m / mps
                result = secondsToTime(s, tUnit)
            }
        }
    }

    val steps = remember(solveFor, dText, tText, vText, dUnit, tUnit, vUnit, result) {
        buildList {
            add("Speed / Time / Distance")
            add("Solve: ${solveFor.name}")
            add("v = d / t")
            add("d = v × t")
            add("t = d / v")
            add("")
            add("Distance: ${dText.ifBlank { "—" }} $dUnit")
            add("Time: ${tText.ifBlank { "—" }} $tUnit")
            add("Speed: ${vText.ifBlank { "—" }} $vUnit")
            add("")
            add("Result: ${result?.let { fmt(it) } ?: "—"}")
        }
    }

    LaunchedEffect(caseFile.units.size) {
        if (caseFile.units.size == 1 && attributedUnitIds.isEmpty()) {
            attributedUnitIds = setOf(caseFile.units.first().unitId)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Text("Solve for", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = solveFor == StdSolveFor.SPEED,
                    onClick = { solveFor = StdSolveFor.SPEED; result = null },
                    label = { Text("Speed") }
                )
                FilterChip(
                    selected = solveFor == StdSolveFor.DISTANCE,
                    onClick = { solveFor = StdSolveFor.DISTANCE; result = null },
                    label = { Text("Distance") }
                )
                FilterChip(
                    selected = solveFor == StdSolveFor.TIME,
                    onClick = { solveFor = StdSolveFor.TIME; result = null },
                    label = { Text("Time") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dText,
                    onValueChange = { dText = it },
                    label = { Text("Distance") },
                    enabled = solveFor != StdSolveFor.DISTANCE,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { fr2.requestFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(fr1)
                        .onPreviewKeyEvent { e ->
                            handleTabEnter(
                                e = e,
                                onTab = { fr2.requestFocus() },
                                onEnter = { calculate(); frSave.requestFocus() }
                            )
                        }
                )
                UnitDropdown("Unit", distUnits, dUnit) { dUnit = it; result = null }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = tText,
                    onValueChange = { tText = it },
                    label = { Text("Time") },
                    enabled = solveFor != StdSolveFor.TIME,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(fr2)
                        .onPreviewKeyEvent { e ->
                            handleTabEnter(
                                e = e,
                                onTab = { frCalc.requestFocus() },
                                onEnter = { calculate(); frSave.requestFocus() }
                            )
                        }
                )
                UnitDropdown("Unit", timeUnits, tUnit) { tUnit = it; result = null }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = vText,
                    onValueChange = { vText = it },
                    label = { Text("Speed") },
                    enabled = solveFor != StdSolveFor.SPEED,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { e ->
                            handleTabEnter(
                                e = e,
                                onTab = { frCalc.requestFocus() },
                                onEnter = { calculate(); frSave.requestFocus() }
                            )
                        }
                )
                UnitDropdown("Unit", speedUnits, vUnit) { vUnit = it; result = null }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { calculate() },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(frCalc)
                        .onPreviewKeyEvent { e ->
                            handleTabEnter(
                                e = e,
                                onTab = { frSave.requestFocus() },
                                onEnter = { calculate(); frSave.requestFocus() }
                            )
                        }
                ) { Text("Calculate") }

                OutlinedButton(
                    onClick = { dText = ""; tText = ""; vText = ""; result = null },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }
            }

            Text(
                text = "Result: ${result?.let { fmt(it) } ?: "—"}",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            UnitsOnlyAttributionPicker(
                caseFile = caseFile,
                selectedUnitIds = attributedUnitIds,
                onChange = { attributedUnitIds = it }
            )

            Button(
                onClick = {
                    val calc = SavedCalculation(
                        type = CalcType.SPEED_TIME_DISTANCE,
                        title = "Speed / Time / Distance",
                        inputs = listOf(
                            CalcValue("Distance", dText.trim().toDoubleOrNull() ?: 0.0, dUnit),
                            CalcValue("Time", tText.trim().toDoubleOrNull() ?: 0.0, tUnit),
                            CalcValue("Speed", vText.trim().toDoubleOrNull() ?: 0.0, vUnit)
                        ),
                        outputs = listOf(
                            CalcValue(
                                name = when (solveFor) {
                                    StdSolveFor.SPEED -> "Speed"
                                    StdSolveFor.DISTANCE -> "Distance"
                                    StdSolveFor.TIME -> "Time"
                                },
                                value = result ?: 0.0,
                                unit = when (solveFor) {
                                    StdSolveFor.SPEED -> vUnit
                                    StdSolveFor.DISTANCE -> dUnit
                                    StdSolveFor.TIME -> tUnit
                                }
                            )
                        ),
                        equationText = "v = d / t, d = v × t, t = d / v",
                        steps = steps,
                        attributedUnitIds = attributedUnitIds
                    )
                    onSaveCalculation(calc)
                },
                enabled = result != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(frSave)
            ) { Text("Save to Case") }

            LaunchedEffect(Unit) { fr1.requestFocus() }
        }
    }
}

/* ---------------------------
   SHARED: Unit dropdown + Units-only attribution picker
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    label: String,
    options: List<UnitOpt>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.key == selectedKey }?.label ?: selectedKey

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label) },
                    onClick = {
                        onSelect(opt.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UnitsOnlyAttributionPicker(
    caseFile: CaseFile,
    selectedUnitIds: Set<UnitId>,
    onChange: (Set<UnitId>) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Attribution (Units)", style = MaterialTheme.typography.titleSmall)
            if (caseFile.units.isEmpty()) {
                Text("No units in this case yet.", style = MaterialTheme.typography.bodySmall)
                return@Column
            }

            caseFile.units.forEach { u ->
                val checked = selectedUnitIds.contains(u.unitId)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onChange(if (checked) selectedUnitIds - u.unitId else selectedUnitIds + u.unitId)
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            onChange(if (checked) selectedUnitIds - u.unitId else selectedUnitIds + u.unitId)
                        }
                    )
                    Column {
                        Text(u.label.ifBlank { u.kind.name }, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            u.kind.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val summary = if (selectedUnitIds.isEmpty()) "Unassigned" else {
                caseFile.units
                    .filter { selectedUnitIds.contains(it.unitId) }
                    .joinToString(", ") { it.label.ifBlank { it.kind.name } }
            }
            Text(
                "Selected: $summary",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
