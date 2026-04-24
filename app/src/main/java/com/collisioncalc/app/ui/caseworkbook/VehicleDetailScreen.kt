package com.collisioncalc.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcType
import com.collisioncalc.app.data.CalcValue
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.InsuranceInfo
import com.collisioncalc.app.data.Occupant
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.Vehicle
import com.collisioncalc.app.data.VehicleId
import com.collisioncalc.app.data.lookups.VinDecoder
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.util.Log
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    caseFile: CaseFile,
    vehicleId: VehicleId,
    onBack: () -> Unit,
    onSaveVehicle: (Vehicle) -> Unit,
    onSaveCalculation: (SavedCalculation) -> Unit,
    vinDecoderProvider: VinDecoder
) {
    val vehicle = caseFile.vehicles.firstOrNull { it.vehicleId == vehicleId }
    if (vehicle == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Vehicle") },
                    navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("Vehicle not found.")
            }
        }
        return
    }

    fun toD(s: String): Double? = s.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    fun fmt(x: Double, d: Int = 2) = "%.${d}f".format(x).trimEnd('0').trimEnd('.')

    // Editable fields
    var label by remember(vehicleId) { mutableStateOf(vehicle.label) }
    var color by remember(vehicleId) { mutableStateOf(vehicle.color) }
    var year by remember(vehicleId) { mutableStateOf(vehicle.year) }
    var make by remember(vehicleId) { mutableStateOf(vehicle.make) }
    var model by remember(vehicleId) { mutableStateOf(vehicle.model) }
    var vin by remember(vehicleId) { mutableStateOf(vehicle.vin) }
    var weightText by remember(vehicleId) { mutableStateOf(vehicle.weightLb?.toString() ?: "") }
    var notes by remember(vehicleId) { mutableStateOf(vehicle.notes) }

    // Insurance
    var insCompany by remember(vehicleId) { mutableStateOf(vehicle.insurance.company) }
    var insPolicy by remember(vehicleId) { mutableStateOf(vehicle.insurance.policyNumber) }
    var insPhone by remember(vehicleId) { mutableStateOf(vehicle.insurance.phone) }

    // Occupants
    var occupants by remember(vehicleId) { mutableStateOf(vehicle.occupants) }

    // Tires
    var stockWidth by remember(vehicleId) { mutableStateOf(vehicle.stockTireWidthMm?.toString() ?: "") }
    var stockAspect by remember(vehicleId) { mutableStateOf(vehicle.stockTireAspectPct?.toString() ?: "") }
    var stockWheel by remember(vehicleId) { mutableStateOf(vehicle.stockTireWheelIn?.toString() ?: "") }

    var curWidth by remember(vehicleId) { mutableStateOf(vehicle.currentTireWidthMm?.toString() ?: "") }
    var curAspect by remember(vehicleId) { mutableStateOf(vehicle.currentTireAspectPct?.toString() ?: "") }
    var curWheel by remember(vehicleId) { mutableStateOf(vehicle.currentTireWheelIn?.toString() ?: "") }

    // VIN decode
    val scope = rememberCoroutineScope()
    val vinDecoder: VinDecoder = remember { vinDecoderProvider }
    var vinDecodeStatus by remember(vehicleId) { mutableStateOf<String?>(null) }
    var isDecodingVin by remember(vehicleId) { mutableStateOf(false) }

    // ---- Find “known speed” for this vehicle (exclude tire corrections) ----
    data class KnownSpeed(val label: String, val mph: Double, val fromTitle: String)

    val knownSpeed: KnownSpeed? = remember(caseFile.calculations, vehicleId) {
        val preferredNames = listOf("S1", "S1′", "S2", "S2′")

        caseFile.calculations
            .asSequence()
            .filter { it.attributedVehicleIds.contains(vehicleId) }
            .filter { it.type != CalcType.TIRE_SPEED_CORRECTION }
            .sortedBy { it.createdAtEpochMs }
            .mapNotNull { calc ->
                val mphOutputs = calc.outputs.filter { it.unit.equals("mph", ignoreCase = true) }
                if (mphOutputs.isEmpty()) return@mapNotNull null

                val preferred = preferredNames.firstNotNullOfOrNull { name ->
                    mphOutputs.firstOrNull { it.name.trim() == name }
                } ?: mphOutputs.first()

                KnownSpeed(
                    label = preferred.name.trim().ifBlank { "Speed" },
                    mph = preferred.value,
                    fromTitle = calc.title
                )
            }
            .lastOrNull()
    }

    // ---- Tire correction math ----
    val sw = toD(stockWidth); val sa = toD(stockAspect); val sr = toD(stockWheel)
    val cw = toD(curWidth); val ca = toD(curAspect); val cr = toD(curWheel)

    val stockOk = (sw != null && sa != null && sr != null && sw > 0 && sa > 0 && sr > 0)
    val curOk = (cw != null && ca != null && cr != null && cw > 0 && ca > 0 && cr > 0)

    val sizesDifferent = stockOk && curOk && (
            abs(sw - cw) > 1e-9 || abs(sa - ca) > 1e-9 || abs(sr - cr) > 1e-9
            )

    val stockDiaIn = if (stockOk) tireDiameterIn(sw!!, sa!!, sr!!) else null
    val curDiaIn = if (curOk) tireDiameterIn(cw!!, ca!!, cr!!) else null

    val ratio = if (stockDiaIn != null && curDiaIn != null && stockDiaIn > 0.0) curDiaIn / stockDiaIn else null
    val correctedSpeed = if (knownSpeed != null && ratio != null) knownSpeed.mph * ratio else null

    val titleText = run {
        val parts = listOf(
            color.trim().takeIf { it.isNotBlank() },
            year.trim().takeIf { it.isNotBlank() },
            make.trim().takeIf { it.isNotBlank() },
            model.trim().takeIf { it.isNotBlank() }
        ).filterNotNull()
        if (parts.isEmpty()) label.ifBlank { "Vehicle" } else parts.joinToString("-")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
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

            // Vehicle Info
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vehicle Info", style = MaterialTheme.typography.titleSmall)

                    ClearableTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = "Label (internal)",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ClearableTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = "Color",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                        ClearableTextField(
                            value = year,
                            onValueChange = { year = it.filter(Char::isDigit).take(4) },
                            label = "Year",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ClearableTextField(
                            value = make,
                            onValueChange = { make = it },
                            label = "Make",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                        ClearableTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = "Model",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // VIN + Decode button
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {

                            ClearableTextField(
                                value = vin,
                                onValueChange = { vin = it; vinDecodeStatus = null },
                                label = "VIN",
                                keyboardType = KeyboardType.Ascii,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val v = vin.trim()
                                    vinDecodeStatus = null
                                    if (v.length < 11) {
                                        vinDecodeStatus = "VIN must be at least 11 characters."
                                    } else {
                                        scope.launch {
                                            isDecodingVin = true
                                            try {
                                                val decoded = vinDecoder.decode(v)
                                                if (decoded == null) {
                                                    vinDecodeStatus = "No decode data — check VIN and network."
                                                } else {
                                                    var applied = false
                                                    if (year.isBlank() && decoded.year.isNotBlank()) { year = decoded.year; applied = true }
                                                    if (make.isBlank() && decoded.make.isNotBlank()) { make = decoded.make; applied = true }
                                                    if (model.isBlank() && decoded.model.isNotBlank()) { model = decoded.model; applied = true }
                                                    vinDecodeStatus = if (applied) "Applied decoded fields." else "Nothing to fill (fields already set or decode empty)."
                                                }
                                            } catch (e: Exception) {
                                                Log.e("VehicleDetailScreen", "Decode error", e)
                                                vinDecodeStatus = "Decode failed: ${e.message}"
                                            } finally {
                                                isDecodingVin = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isDecodingVin && vin.trim().length >= 11
                            ) { Text(if (isDecodingVin) "..." else "Decode") }
                        }

                        if (!vinDecodeStatus.isNullOrBlank()) {
                            Text(
                                vinDecodeStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ClearableTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = "Weight (lb)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Insurance
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Insurance", style = MaterialTheme.typography.titleSmall)

                    ClearableTextField(
                        value = insCompany,
                        onValueChange = { insCompany = it },
                        label = "Insurance company",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ClearableTextField(
                            value = insPolicy,
                            onValueChange = { insPolicy = it },
                            label = "Policy #",
                            keyboardType = KeyboardType.Ascii,
                            modifier = Modifier.weight(1f)
                        )
                        ClearableTextField(
                            value = insPhone,
                            onValueChange = { insPhone = it },
                            label = "Phone",
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Occupants
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Occupants", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { occupants = occupants + Occupant() }) { Text("Add") }
                    }

                    if (occupants.isEmpty()) {
                        Text("No occupants entered.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        occupants.forEachIndexed { idx, o ->
                            val isDriver = o.seatingPosition.equals("Driver", ignoreCase = true)

                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Occupant ${idx + 1}", style = MaterialTheme.typography.labelLarge)
                                        TextButton(
                                            onClick = {
                                                occupants = occupants.toMutableList().also { list ->
                                                    if (idx in list.indices) list.removeAt(idx)
                                                }
                                            }
                                        ) { Text("Remove") }
                                    }

                                    Text("Name", style = MaterialTheme.typography.labelLarge)

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        ClearableTextField(
                                            value = o.name.last,
                                            onValueChange = { new ->
                                                occupants = occupants.toMutableList().also { list ->
                                                    if (idx in list.indices) list[idx] = list[idx].copy(name = list[idx].name.copy(last = new))
                                                }
                                            },
                                            label = "Last",
                                            keyboardType = KeyboardType.Text,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ClearableTextField(
                                            value = o.name.first,
                                            onValueChange = { new ->
                                                occupants = occupants.toMutableList().also { list ->
                                                    if (idx in list.indices) list[idx] = list[idx].copy(name = list[idx].name.copy(first = new))
                                                }
                                            },
                                            label = "First",
                                            keyboardType = KeyboardType.Text,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        ClearableTextField(
                                            value = o.name.middle,
                                            onValueChange = { new ->
                                                occupants = occupants.toMutableList().also { list ->
                                                    if (idx in list.indices) list[idx] = list[idx].copy(name = list[idx].name.copy(middle = new))
                                                }
                                            },
                                            label = "Middle",
                                            keyboardType = KeyboardType.Text,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ClearableTextField(
                                            value = o.name.suffix,
                                            onValueChange = { new ->
                                                occupants = occupants.toMutableList().also { list ->
                                                    if (idx in list.indices) list[idx] = list[idx].copy(name = list[idx].name.copy(suffix = new))
                                                }
                                            },
                                            label = "Suffix",
                                            keyboardType = KeyboardType.Text,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // DOB: user types ddmmyyyy, store ISO
                                    // --- DOB: allow typing freely (ddmmyyyy), commit to ISO only when valid ---
                                    val dobTextKey = "${vehicleId}_${idx}_dob"
                                    var dobText by remember(dobTextKey) { mutableStateOf(isoToMmDdYyyyDisplay(o.dobIso)) }

// Keep the editable text in sync if DOB is changed elsewhere (or loaded)
                                    LaunchedEffect(o.dobIso) {
                                        val formatted = isoToMmDdYyyyDisplay(o.dobIso)
                                        if (formatted != dobText) dobText = formatted
                                    }

                                    ClearableTextField(
                                        value = dobText,
                                        onValueChange = { raw ->
                                            // keep only digits, max 8
                                            val cleaned = raw.filter(Char::isDigit).take(8)
                                            dobText = cleaned

                                            // Only commit to model when it's a valid full date
                                            val iso = parseMmDdYyyyToIso(cleaned)

                                            occupants = occupants.toMutableList().also { list ->
                                                if (idx !in list.indices) return@also
                                                list[idx] = list[idx].copy(
                                                    dobIso = iso ?: if (cleaned.isEmpty()) "" else list[idx].dobIso
                                                )
                                            }
                                        },
                                        label = "DOB (mmddyyyy)",
                                        keyboardType = KeyboardType.Number,
                                        modifier = Modifier.fillMaxWidth()
                                    )


                                    SeatingDropdown(
                                        value = o.seatingPosition,
                                        onChange = { new ->
                                            occupants = occupants.toMutableList().also { list ->
                                                if (idx in list.indices) list[idx] = list[idx].copy(seatingPosition = new)
                                            }
                                        }
                                    )

                                    SeatbeltToggleRow(
                                        value = o.seatbeltWorn,
                                        onChange = { new ->
                                            occupants = occupants.toMutableList().also { list ->
                                                if (idx in list.indices) list[idx] = list[idx].copy(seatbeltWorn = new)
                                            }
                                        }
                                    )

                                    if (isDriver) {
                                        HorizontalDivider()
                                        Text("Driver ID Info", style = MaterialTheme.typography.labelLarge)

                                        ClearableTextField(
                                            value = o.idNumber,
                                            onValueChange = { new ->
                                                occupants = occupants.toMutableList().also { list ->
                                                    if (idx in list.indices) list[idx] = list[idx].copy(idNumber = new)
                                                }
                                            },
                                            label = "DL / ID #",
                                            keyboardType = KeyboardType.Ascii,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            ClearableTextField(
                                                value = o.idClass,
                                                onValueChange = { new ->
                                                    occupants = occupants.toMutableList().also { list ->
                                                        if (idx in list.indices) list[idx] = list[idx].copy(idClass = new)
                                                    }
                                                },
                                                label = "Class",
                                                keyboardType = KeyboardType.Ascii,
                                                modifier = Modifier.weight(1f)
                                            )
                                            ClearableTextField(
                                                value = o.phone,
                                                onValueChange = { new ->
                                                    occupants = occupants.toMutableList().also { list ->
                                                        if (idx in list.indices) list[idx] = list[idx].copy(phone = new)
                                                    }
                                                },
                                                label = "Phone",
                                                keyboardType = KeyboardType.Phone,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        ClearableTextField(
                                            value = o.idRestrictions,
                                            onValueChange = { new ->
                                                occupants = occupants.toMutableList().also { list ->
                                                    if (idx in list.indices) list[idx] = list[idx].copy(idRestrictions = new)
                                                }
                                            },
                                            label = "Restrictions",
                                            keyboardType = KeyboardType.Text,
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = false,
                                            minLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tires
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tires", style = MaterialTheme.typography.titleSmall)
                    Text("Stock = speedometer calibration. Current = what’s on the vehicle.", style = MaterialTheme.typography.bodySmall)

                    Text("Stock tire", style = MaterialTheme.typography.labelLarge)
                    TireTripleRowClearable(
                        width = stockWidth, onWidth = { stockWidth = it },
                        aspect = stockAspect, onAspect = { stockAspect = it },
                        wheel = stockWheel, onWheel = { stockWheel = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Current tire", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = {
                                curWidth = stockWidth
                                curAspect = stockAspect
                                curWheel = stockWheel
                            },
                            enabled = stockWidth.isNotBlank() || stockAspect.isNotBlank() || stockWheel.isNotBlank()
                        ) { Text("Copy stock → current") }
                    }

                    TireTripleRowClearable(
                        width = curWidth, onWidth = { curWidth = it },
                        aspect = curAspect, onAspect = { curAspect = it },
                        wheel = curWheel, onWheel = { curWheel = it }
                    )

                    if ((stockWidth.isNotBlank() || stockAspect.isNotBlank() || stockWheel.isNotBlank()) && !stockOk) {
                        Text("Stock tire: enter valid width/aspect/wheel (all > 0).", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if ((curWidth.isNotBlank() || curAspect.isNotBlank() || curWheel.isNotBlank()) && !curOk) {
                        Text("Current tire: enter valid width/aspect/wheel (all > 0).", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Speed correction preview + save (unchanged logic)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Speed Correction", style = MaterialTheme.typography.titleSmall)

                    if (knownSpeed == null) {
                        Text(
                            "No known speed yet for this vehicle. Once you save a speed (S1, S2, S1′, etc.) attributed to this vehicle, the correction will appear here.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (!sizesDifferent) {
                        Text("Known speed (${knownSpeed.label}): ${fmt(knownSpeed.mph)} mph.", style = MaterialTheme.typography.bodySmall)
                        Text("Stock/current tires match (or tire data incomplete), so no correction needed.", style = MaterialTheme.typography.bodySmall)
                    } else if (ratio == null || correctedSpeed == null || stockDiaIn == null || curDiaIn == null) {
                        Text("Known speed (${knownSpeed.label}): ${fmt(knownSpeed.mph)} mph.", style = MaterialTheme.typography.bodySmall)
                        Text("Enter valid stock + current tires to compute correction.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        val pctDiff = (ratio - 1.0) * 100.0
                        val sign = if (pctDiff >= 0) "+" else ""

                        Text("Known (indicated) ${knownSpeed.label}: ${fmt(knownSpeed.mph)} mph", style = MaterialTheme.typography.bodySmall)
                        Text("Source: ${knownSpeed.fromTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Text("Stock diameter: ${fmt(stockDiaIn, 2)} in", style = MaterialTheme.typography.bodySmall)
                        Text("Current diameter: ${fmt(curDiaIn, 2)} in", style = MaterialTheme.typography.bodySmall)

                        Text("Multiplier: × ${fmt(ratio, 4)}", style = MaterialTheme.typography.bodySmall)
                        Text("Difference: $sign${fmt(pctDiff, 2)}%", style = MaterialTheme.typography.bodySmall)
                        Text("Corrected: ${fmt(correctedSpeed)} mph", style = MaterialTheme.typography.titleMedium)

                        Spacer(Modifier.height(6.dp))

                        Button(
                            onClick = {
                                val calc = SavedCalculation(
                                    type = CalcType.TIRE_SPEED_CORRECTION,
                                    title = "Tire Speed Correction — ${titleText.ifBlank { "Vehicle" }}",
                                    inputs = listOf(
                                        CalcValue("Stock width", sw!!, "mm"),
                                        CalcValue("Stock aspect", sa!!, "%"),
                                        CalcValue("Stock wheel", sr!!, "in"),
                                        CalcValue("Current width", cw!!, "mm"),
                                        CalcValue("Current aspect", ca!!, "%"),
                                        CalcValue("Current wheel", cr!!, "in"),
                                        CalcValue(
                                            "Indicated (${knownSpeed.label})",
                                            abs(knownSpeed.mph),
                                            "mph"
                                        ),
                                        CalcValue("Multiplier", ratio, "x")
                                    ),
                                    outputs = listOf(
                                        CalcValue("Corrected speed", abs(correctedSpeed), "mph")
                                    ),
                                    equationText = "corrected = indicated × (D_current / D_stock)",
                                    steps = buildList {
                                        add("Tire Speed Correction")
                                        add(
                                            "Indicated (${knownSpeed.label}) = ${
                                                fmt(
                                                    knownSpeed.mph,
                                                    4
                                                )
                                            } mph"
                                        )
                                        add("Stock diameter = ${fmt(stockDiaIn, 6)} in")
                                        add("Current diameter = ${fmt(curDiaIn, 6)} in")
                                        add("Multiplier = D_current / D_stock = ${fmt(ratio, 6)}")
                                        add(
                                            "Corrected = ${fmt(knownSpeed.mph, 6)} × ${
                                                fmt(
                                                    ratio,
                                                    6
                                                )
                                            } = ${fmt(correctedSpeed, 6)} mph"
                                        )
                                    },
                                    attributedVehicleIds = setOf(vehicleId)
                                )
                                onSaveCalculation(calc)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save Tire Speed Correction") }
                    }
                }
            }

            // Notes
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notes", style = MaterialTheme.typography.titleSmall)
                    ClearableTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Vehicle notes",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 3
                    )
                }
            }

            Button(
                onClick = {
                    val updated = vehicle.copy(
                        label = label,
                        color = color,
                        year = year,
                        make = make,
                        model = model,
                        vin = vin,
                        weightLb = toD(weightText),
                        occupants = occupants,
                        insurance = InsuranceInfo(
                            company = insCompany,
                            policyNumber = insPolicy,
                            phone = insPhone
                        ),
                        stockTireWidthMm = sw,
                        stockTireAspectPct = sa,
                        stockTireWheelIn = sr,
                        currentTireWidthMm = cw,
                        currentTireAspectPct = ca,
                        currentTireWheelIn = cr,
                        notes = notes
                    )
                    onSaveVehicle(updated)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Vehicle") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------------------------
   Clearable input
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
   Occupants UI helpers
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeatingDropdown(
    value: String,
    onChange: (String) -> Unit
) {
    val options = listOf(
        "Driver",
        "Front Passenger",
        "Rear Left",
        "Rear Center",
        "Rear Right",
        "Other"
    )

    var expanded by remember { mutableStateOf(false) }
    val shown = value.ifBlank { "Select seating" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = shown,
            onValueChange = {},
            readOnly = true,
            label = { Text("Seating position") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onChange(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SeatbeltToggleRow(
    value: Boolean?,
    onChange: (Boolean?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Seatbelt worn", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text("Yes") })
            FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text("No") })
            FilterChip(selected = value == null, onClick = { onChange(null) }, label = { Text("Unknown") })
        }
    }
}

/* ---------------------------
   Tire triple with clear buttons
---------------------------- */

@Composable
private fun TireTripleRowClearable(
    width: String, onWidth: (String) -> Unit,
    aspect: String, onAspect: (String) -> Unit,
    wheel: String, onWheel: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ClearableTextField(
            value = width,
            onValueChange = onWidth,
            label = "Width (mm)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
        ClearableTextField(
            value = aspect,
            onValueChange = onAspect,
            label = "Aspect (%)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
        ClearableTextField(
            value = wheel,
            onValueChange = onWheel,
            label = "Wheel (in)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
    }
}

/* ---------------------------
   Date helpers: ddmmyyyy <-> ISO
---------------------------- */

private fun parseMmDdYyyyToIso(input: String): String? {
    val digits = input.filter { it.isDigit() }
    if (digits.length != 8) return null
    val mm = digits.substring(0, 2).toIntOrNull() ?: return null
    val dd = digits.substring(2, 4).toIntOrNull() ?: return null
    val yyyy = digits.substring(4, 8).toIntOrNull() ?: return null
    return runCatching { LocalDate.of(yyyy, mm, dd).toString() }.getOrNull()
}

private fun isoToMmDdYyyyDisplay(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val d = LocalDate.parse(iso)
        "%02d%02d%04d".format(d.monthValue, d.dayOfMonth, d.year)
    }.getOrElse { "" }
}

/* ---------------------------
   Tire math
---------------------------- */

private fun tireDiameterIn(widthMm: Double, aspectPct: Double, wheelIn: Double): Double {
    val sidewallIn = (widthMm * (aspectPct / 100.0)) / 25.4
    return wheelIn + (2.0 * sidewallIn)
}
