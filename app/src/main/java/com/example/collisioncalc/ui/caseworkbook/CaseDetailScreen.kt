package com.example.collisioncalc.ui.caseworkbook

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.*
import com.example.collisioncalc.ui.components.CrashSummaryCard
import com.example.collisioncalc.ui.export.CaseExporter


import java.time.LocalDate
import kotlin.math.abs

enum class CaseTab { CRASH, UNITS, CALCS, NOTES, EXPORT }
enum class Cat { SPEED, LENGTH, MASS }

// Helper for dropdowns
data class UnitOpt(val key: String, val label: String)

// STD solver target
private enum class StdSolveFor { SPEED, DISTANCE, TIME }

// CALCS filter mode
private enum class CalcFilterMode { ALL, UNASSIGNED, UNIT }

/* ---------------------------
   KEYBOARD helper
---------------------------- */

private fun handleTabEnter(
    e: androidx.compose.ui.input.key.KeyEvent,
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
   CASE DETAIL SCREEN
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,

    // Phase 1
    onUpdateCrashInfo: (CollisionInfo) -> Unit,

    // Units
    onAddVehicleUnit: () -> Unit,
    onAddPedestrianUnit: () -> Unit,
    onRenameUnit: (UnitId, String) -> Unit,
    onRemoveUnit: (UnitId) -> Unit,

    // Notes / vehicle edit
    onAddNote: (String) -> Unit,
    onOpenVehicle: (vehicleId: VehicleId) -> Unit,
    onOpenPedestrian: (unitId: UnitId) -> Unit, // ✅ NEW

    // Tools
    onGoToCombinedSpeed: () -> Unit,
    onGoToMomentum: () -> Unit,

    // Calcs
    onOpenCalculation: (calcId: CalcId) -> Unit,

    // Save calc into this case (used by in-case Unit Tools)
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    var tab by remember { mutableStateOf(CaseTab.CRASH) }
    var showUnitTools by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Case: ${caseFile.serviceNumber}") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->

        if (showUnitTools) {
            CaseUnitToolsBottomSheet(
                caseFile = caseFile,
                onDismiss = { showUnitTools = false },
                onSaveCalculation = onSaveCalculation
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
                CaseTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = { Text(t.name) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    CaseTab.CRASH -> CrashTabScreen(
                        crashInfo = caseFile.crashInfo,
                        onCrashInfoChange = onUpdateCrashInfo,
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    )

                    CaseTab.UNITS -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        UnitsTabScreen(
                            caseFile = caseFile,
                            onOpenVehicle = onOpenVehicle,
                            onOpenPedestrian = onOpenPedestrian, // ✅ NEW
                            onAddVehicleUnit = onAddVehicleUnit,
                            onAddPedestrianUnit = onAddPedestrianUnit,
                            onRenameUnit = onRenameUnit,
                            onRemoveUnit = onRemoveUnit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CaseTab.CALCS -> CalculationsTab(
                        caseFile = caseFile,
                        onOpenCalculation = onOpenCalculation,
                        onGoToCombinedSpeed = onGoToCombinedSpeed,
                        onGoToMomentum = onGoToMomentum,
                        onOpenUnitTools = { showUnitTools = true }
                    )

                    CaseTab.NOTES -> NotesTab(caseFile = caseFile, onAddNote = onAddNote)

                    CaseTab.EXPORT -> ExportTab(caseFile = caseFile)
                }
            }
        }
    }
}

/* ---------------------------
   CALCS TAB
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationsTab(
    caseFile: CaseFile,
    onOpenCalculation: (CalcId) -> Unit,
    onGoToCombinedSpeed: () -> Unit,
    onGoToMomentum: () -> Unit,
    onOpenUnitTools: () -> Unit
) {
    fun formatCalcNumber(x: Double): String {
        if (!x.isFinite()) return "—"
        val s = "%.3f".format(x)
        return s.trimEnd('0').trimEnd('.')
    }

    fun formatLocalTime(epochMs: Long): String {
        val df = java.text.SimpleDateFormat("M/d/yy h:mm a", java.util.Locale.US)
        df.timeZone = java.util.TimeZone.getDefault()
        return df.format(java.util.Date(epochMs))
    }

    fun attributionLine(c: SavedCalculation): String {
        val unitLabels = c.attributedUnitIds
            .mapNotNull { id -> caseFile.units.firstOrNull { it.unitId == id } }
            .map { u -> u.label.ifBlank { u.kind.name } }
            .sorted()

        val vehicleLabels = c.attributedVehicleIds
            .mapNotNull { id -> caseFile.vehicles.firstOrNull { it.vehicleId == id } }
            .map { v -> v.label.ifBlank { "Vehicle" } }
            .sorted()

        if (unitLabels.isNotEmpty()) return unitLabels.joinToString(", ")
        if (vehicleLabels.isNotEmpty()) return "Vehicles: ${vehicleLabels.joinToString(", ")}"
        return "Unassigned"
    }

    fun outputsPreview(c: SavedCalculation): String {
        if (c.outputs.isEmpty()) return "No outputs"

        val multiAttrib = (c.attributedUnitIds.size + c.attributedVehicleIds.size) > 1
        val max = if (multiAttrib) 4 else 1

        val shown = c.outputs.take(max).joinToString(" • ") { o ->
            "${o.name} = ${formatCalcNumber(o.value)} ${o.unit}".trim()
        }

        val more = if (c.outputs.size > max) " …" else ""
        return shown + more
    }

    var filterMode by remember { mutableStateOf(CalcFilterMode.ALL) }
    var filterUnitId by remember { mutableStateOf<UnitId?>(null) }

    LaunchedEffect(caseFile.units) {
        if (caseFile.units.size == 1) {
            filterMode = CalcFilterMode.UNIT
            filterUnitId = caseFile.units.first().unitId
        }
        if (
            filterMode == CalcFilterMode.UNIT &&
            filterUnitId != null &&
            caseFile.units.none { it.unitId == filterUnitId }
        ) {
            filterMode = CalcFilterMode.ALL
            filterUnitId = null
        }
    }

    fun matchesFilter(c: SavedCalculation): Boolean = when (filterMode) {
        CalcFilterMode.ALL -> true
        CalcFilterMode.UNASSIGNED ->
            c.attributedUnitIds.isEmpty() && c.attributedVehicleIds.isEmpty()
        CalcFilterMode.UNIT ->
            filterUnitId != null && c.attributedUnitIds.contains(filterUnitId)
    }

    val filteredCalcs = remember(caseFile.calculations, filterMode, filterUnitId) {
        caseFile.calculations
            .asSequence()
            .filter { matchesFilter(it) }
            .sortedByDescending { it.createdAtEpochMs }
            .toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CrashSummaryCard(caseFile.crashInfo)

        HorizontalDivider()

        Text("Filter", style = MaterialTheme.typography.titleSmall)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = filterMode == CalcFilterMode.ALL,
                onClick = { filterMode = CalcFilterMode.ALL; filterUnitId = null },
                label = { Text("All") }
            )

            FilterChip(
                selected = filterMode == CalcFilterMode.UNASSIGNED,
                onClick = { filterMode = CalcFilterMode.UNASSIGNED; filterUnitId = null },
                label = { Text("Unassigned") }
            )

            caseFile.units.forEach { u ->
                val label = u.label.ifBlank { u.kind.name }
                FilterChip(
                    selected = filterMode == CalcFilterMode.UNIT && filterUnitId == u.unitId,
                    onClick = { filterMode = CalcFilterMode.UNIT; filterUnitId = u.unitId },
                    label = { Text(label) }
                )
            }
        }

        Text(
            text = "Showing ${filteredCalcs.size} of ${caseFile.calculations.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (filteredCalcs.isEmpty()) {
            Text("No saved calculations in this filter.")
            Text(
                "Run a tool and save a calc attributed to a Unit (or leave unassigned).",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onGoToCombinedSpeed, modifier = Modifier.weight(1f)) { Text("Combined Speed") }
                Button(onClick = onGoToMomentum, modifier = Modifier.weight(1f)) { Text("Momentum") }
            }
            Button(onClick = onOpenUnitTools, modifier = Modifier.fillMaxWidth()) { Text("Unit Tools") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onGoToCombinedSpeed, modifier = Modifier.weight(1f)) { Text("Combined Speed") }
                Button(onClick = onGoToMomentum, modifier = Modifier.weight(1f)) { Text("Momentum") }
            }
            Button(onClick = onOpenUnitTools, modifier = Modifier.fillMaxWidth()) { Text("Unit Tools") }

            Spacer(Modifier.height(6.dp))

            filteredCalcs.forEach { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenCalculation(c.calcId) }
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                c.title,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                formatLocalTime(c.createdAtEpochMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(outputsPreview(c), style = MaterialTheme.typography.bodySmall)

                        Text(
                            "Attributed to: ${attributionLine(c)}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "Tap to view work",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------------------
   IN-CASE UNIT TOOLS (Bottom Sheet)
---------------------------- */

private enum class UnitToolRoute { LIST, CONVERTER, STD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseUnitToolsBottomSheet(
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
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                FilterChip(selected = cat == Cat.SPEED, onClick = { cat = Cat.SPEED; result = null }, label = { Text("Speed") })
                FilterChip(selected = cat == Cat.LENGTH, onClick = { cat = Cat.LENGTH; result = null }, label = { Text("Length") })
                FilterChip(selected = cat == Cat.MASS, onClick = { cat = Cat.MASS; result = null }, label = { Text("Mass") })
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
                FilterChip(selected = solveFor == StdSolveFor.SPEED, onClick = { solveFor = StdSolveFor.SPEED; result = null }, label = { Text("Speed") })
                FilterChip(selected = solveFor == StdSolveFor.DISTANCE, onClick = { solveFor = StdSolveFor.DISTANCE; result = null }, label = { Text("Distance") })
                FilterChip(selected = solveFor == StdSolveFor.TIME, onClick = { solveFor = StdSolveFor.TIME; result = null }, label = { Text("Time") })
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
                        Text(u.kind.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            val summary = if (selectedUnitIds.isEmpty()) "Unassigned" else {
                caseFile.units
                    .filter { selectedUnitIds.contains(it.unitId) }
                    .joinToString(", ") { it.label.ifBlank { it.kind.name } }
            }
            Text("Selected: $summary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ---------------------------
   NOTES TAB
---------------------------- */

@Composable
private fun NotesTab(caseFile: CaseFile, onAddNote: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Notes are append-only and exported with this case.", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("New note") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = { onAddNote(text); text = "" },
            enabled = text.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add Note") }

        HorizontalDivider()

        if (caseFile.notes.isEmpty()) {
            Text("No notes entered.")
        } else {
            caseFile.notes.forEachIndexed { idx, n ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Note ${idx + 1} — ${formatLocalNoteTime(n.createdAtEpochMs)}", style = MaterialTheme.typography.bodySmall)
                        Text(n.text)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun formatLocalNoteTime(epochMs: Long): String {
    val df = java.text.SimpleDateFormat("M/d/yy h:mm a", java.util.Locale.US)
    df.timeZone = java.util.TimeZone.getDefault()
    return df.format(java.util.Date(epochMs))
}

/* ---------------------------
   EXPORT TAB (DOCX + PDF)
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportTab(caseFile: CaseFile) {
    val context = LocalContext.current

    var agency by rememberSaveable { mutableStateOf("Grand Prairie Police Department") }
    var preparedBy by rememberSaveable { mutableStateOf("") }
    var reviewedBy by rememberSaveable { mutableStateOf("") }
    var reportDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) } // YYYY-MM-DD
    var showFullWork by rememberSaveable { mutableStateOf(true) }
    var status by remember { mutableStateOf<String?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val meta = CaseExporter.ExportMeta(
            agencyName = agency,
            reportTitle = "CollisionCalc Case Report",
            preparedBy = preparedBy,
            reviewedBy = reviewedBy,
            reportDateIso = reportDateIso,
            showFullWork = showFullWork
        )

        runCatching {
            CaseExporter.exportPdf(context, caseFile, uri, meta)
        }.onSuccess {
            status = "PDF exported."
        }.onFailure { e ->
            status = "PDF export failed: ${e.message ?: "Unknown error"}"
        }
    }

    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val meta = CaseExporter.ExportMeta(
            agencyName = agency,
            reportTitle = "CollisionCalc Case Report",
            preparedBy = preparedBy,
            reviewedBy = reviewedBy,
            reportDateIso = reportDateIso,
            showFullWork = showFullWork
        )

        runCatching {
            CaseExporter.exportDocx(context, caseFile, uri, meta)
        }.onSuccess {
            status = "Word (.docx) exported."
        }.onFailure { e ->
            status = "Word export failed: ${e.message ?: "Unknown error"}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Export", style = MaterialTheme.typography.titleLarge)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = agency,
                    onValueChange = { agency = it },
                    label = { Text("Agency") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = preparedBy,
                    onValueChange = { preparedBy = it },
                    label = { Text("Prepared by") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reviewedBy,
                    onValueChange = { reviewedBy = it },
                    label = { Text("Reviewed by") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reportDateIso,
                    onValueChange = { reportDateIso = it },
                    label = { Text("Report date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Include work shown", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showFullWork, onCheckedChange = { showFullWork = it })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { docxLauncher.launch("CollisionCalc_${caseFile.serviceNumber}.docx") },
                modifier = Modifier.weight(1f)
            ) { Text("Export Word") }

            Button(
                onClick = { pdfLauncher.launch("CollisionCalc_${caseFile.serviceNumber}.pdf") },
                modifier = Modifier.weight(1f)
            ) { Text("Export PDF") }
        }

        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
