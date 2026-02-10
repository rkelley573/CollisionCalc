package com.example.collisioncalc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireSizeCompareScreen(
    onBack: () -> Unit,

    // ✅ NEW: optional default vehicle selection (used when launched from a case)
    defaultVehicleId: VehicleId? = null,

    // OPTIONAL: when opened from inside a case
    caseFile: CaseFile? = null,
    onSaveCalculation: ((SavedCalculation) -> Unit)? = null,
    onPromptAttribution: ((calcId: CalcId) -> Unit)? = null
) {
    val v1 = caseFile?.vehicles?.getOrNull(0)
    val v2 = caseFile?.vehicles?.getOrNull(1)

    // Local (screen-only) selected vehicle for attribution + quick fill.
    // Prefer defaultVehicleId if provided, otherwise fall back to V1.
    var selectedVehicleId by remember(caseFile, defaultVehicleId) {
        mutableStateOf(defaultVehicleId ?: v1?.vehicleId)
    }
    val selectedVehicle = remember(caseFile, selectedVehicleId) {
        caseFile?.vehicles?.firstOrNull { it.vehicleId == selectedVehicleId }
    }

    // Inputs (3-box tire sizes)
    var stockWidth by remember { mutableStateOf("") }   // mm
    var stockAspect by remember { mutableStateOf("") }  // %
    var stockWheel by remember { mutableStateOf("") }   // in

    var actualWidth by remember { mutableStateOf("") }
    var actualAspect by remember { mutableStateOf("") }
    var actualWheel by remember { mutableStateOf("") }

    // Speed correction
    var indicatedMph by remember { mutableStateOf("60") }

    fun toD(s: String): Double? = s.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    fun fmt(x: Double, d: Int = 2) = "%.${d}f".format(x).trimEnd('0').trimEnd('.')

    fun tireDiameterIn(widthMm: Double, aspectPct: Double, wheelIn: Double): Double {
        val sidewallIn = (widthMm * (aspectPct / 100.0)) / 25.4
        return wheelIn + (2.0 * sidewallIn)
    }

    val sw = toD(stockWidth)
    val sa = toD(stockAspect)
    val sr = toD(stockWheel)

    val aw = toD(actualWidth)
    val aa = toD(actualAspect)
    val ar = toD(actualWheel)

    val ind = toD(indicatedMph)

    val stockOk = (sw != null && sa != null && sr != null && sw > 0 && sa > 0 && sr > 0)
    val actualOk = (aw != null && aa != null && ar != null && aw > 0 && aa > 0 && ar > 0)
    val indOk = (ind != null && ind >= 0)

    val stockDia = if (stockOk) tireDiameterIn(sw!!, sa!!, sr!!) else null
    val actualDia = if (actualOk) tireDiameterIn(aw!!, aa!!, ar!!) else null

    val ratio = if (stockDia != null && actualDia != null && stockDia > 0.0) actualDia / stockDia else null
    val pctDiff = if (ratio != null) (ratio - 1.0) * 100.0 else null

    val correctedSpeed = if (ratio != null && indOk) ind!! * ratio else null

    val openedFromCase = (caseFile != null && onSaveCalculation != null)
    val canSave = openedFromCase && stockOk && actualOk && indOk && correctedSpeed != null

    fun applyVehicleStock(v: Vehicle?) {
        if (v == null) return
        stockWidth = v.stockTireWidthMm?.let { fmt(it, 0) }.orEmpty()
        stockAspect = v.stockTireAspectPct?.let { fmt(it, 0) }.orEmpty()
        stockWheel = v.stockTireWheelIn?.let { fmt(it, 0) }.orEmpty()
    }

    fun applyVehicleCurrent(v: Vehicle?) {
        if (v == null) return
        actualWidth = v.currentTireWidthMm?.let { fmt(it, 0) }.orEmpty()
        actualAspect = v.currentTireAspectPct?.let { fmt(it, 0) }.orEmpty()
        actualWheel = v.currentTireWheelIn?.let { fmt(it, 0) }.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tire Size Compare") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // If opened from a case, show local selection + quick-fill helpers
            if (caseFile != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Case Helper", style = MaterialTheme.typography.titleSmall)

                        Text(
                            text = if (selectedVehicle != null) {
                                "Selected: ${selectedVehicle.label}"
                            } else {
                                "Select a vehicle to attribute this correction."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedVehicleId == v1?.vehicleId,
                                onClick = { selectedVehicleId = v1?.vehicleId },
                                enabled = v1 != null,
                                label = { Text("Vehicle 1") }
                            )
                            FilterChip(
                                selected = selectedVehicleId == v2?.vehicleId,
                                onClick = { selectedVehicleId = v2?.vehicleId },
                                enabled = v2 != null,
                                label = { Text("Vehicle 2") }
                            )
                            FilterChip(
                                selected = selectedVehicleId == null,
                                onClick = { selectedVehicleId = null },
                                label = { Text("None") }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { applyVehicleStock(selectedVehicle) },
                                enabled = selectedVehicle != null
                            ) { Text("Use Selected Stock") }

                            OutlinedButton(
                                onClick = { applyVehicleCurrent(selectedVehicle) },
                                enabled = selectedVehicle != null
                            ) { Text("Use Selected Current") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { applyVehicleStock(v1) },
                                enabled = v1 != null
                            ) { Text("V1 Stock") }

                            OutlinedButton(
                                onClick = { applyVehicleStock(v2) },
                                enabled = v2 != null
                            ) { Text("V2 Stock") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { applyVehicleCurrent(v1) },
                                enabled = v1 != null
                            ) { Text("V1 Current") }

                            OutlinedButton(
                                onClick = { applyVehicleCurrent(v2) },
                                enabled = v2 != null
                            ) { Text("V2 Current") }
                        }
                    }
                }
            }

            // Stock Tire
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Stock Tire (speedometer calibrated)", style = MaterialTheme.typography.titleSmall)

                    TireRow(
                        width = stockWidth, onWidth = { stockWidth = it },
                        aspect = stockAspect, onAspect = { stockAspect = it },
                        wheel = stockWheel, onWheel = { stockWheel = it }
                    )

                    if ((stockWidth.isNotBlank() || stockAspect.isNotBlank() || stockWheel.isNotBlank()) && !stockOk) {
                        Text(
                            "Enter valid stock tire values (width/aspect/wheel must all be > 0).",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Actual Tire
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Actual Tire (installed)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = {
                                actualWidth = stockWidth
                                actualAspect = stockAspect
                                actualWheel = stockWheel
                            },
                            enabled = (stockWidth.isNotBlank() || stockAspect.isNotBlank() || stockWheel.isNotBlank())
                        ) { Text("Copy Stock → Actual") }
                    }

                    TireRow(
                        width = actualWidth, onWidth = { actualWidth = it },
                        aspect = actualAspect, onAspect = { actualAspect = it },
                        wheel = actualWheel, onWheel = { actualWheel = it }
                    )

                    if ((actualWidth.isNotBlank() || actualAspect.isNotBlank() || actualWheel.isNotBlank()) && !actualOk) {
                        Text(
                            "Enter valid actual tire values (width/aspect/wheel must all be > 0).",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Speed correction
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Speed Correction", style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = indicatedMph,
                        onValueChange = { indicatedMph = it },
                        label = { Text("Indicated speed") },
                        suffix = { Text("mph") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        isError = indicatedMph.isNotBlank() && !indOk,
                        supportingText = {
                            if (indicatedMph.isNotBlank() && !indOk) Text("Enter 0 or greater.")
                        }
                    )

                    if (stockDia != null && actualDia != null && ratio != null && indOk) {
                        HorizontalDivider()

                        Text("Results", style = MaterialTheme.typography.titleSmall)
                        ResultRow("Stock diameter", "${fmt(stockDia, 2)} in")
                        ResultRow("Actual diameter", "${fmt(actualDia, 2)} in")

                        pctDiff?.let {
                            val sign = if (it >= 0) "+" else ""
                            ResultRow("Difference", "$sign${fmt(it, 2)}%")
                        }

                        ResultRow("Multiplier", "× ${fmt(ratio, 4)}")

                        correctedSpeed?.let {
                            ResultRow("Actual speed", "${fmt(it, 2)} mph")
                        }

                        val warning = pctDiff?.let { abs(it) } ?: 0.0
                        if (warning >= 3.0) {
                            Text(
                                "Note: ≥3% difference can meaningfully affect speed estimates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text(
                            "Enter stock + actual tire sizes (and an indicated speed) to see results.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Save to case (only if opened from a case)
            if (openedFromCase) {
                Button(
                    onClick = {
                        val save = onSaveCalculation ?: return@Button
                        val prompt = onPromptAttribution
                        val out = correctedSpeed ?: return@Button
                        val ratioVal = ratio ?: return@Button
                        val stockDiaVal = stockDia ?: return@Button
                        val actualDiaVal = actualDia ?: return@Button
                        val indVal = ind ?: return@Button

                        val steps = buildList {
                            add("Tire speed correction")
                            add("Actual speed = Indicated speed × (D_actual / D_stock)")
                            add("Stock diameter = ${fmt(stockDiaVal, 4)} in")
                            add("Actual diameter = ${fmt(actualDiaVal, 4)} in")
                            add("Multiplier = ${fmt(ratioVal, 6)}")
                            add("Actual speed = ${fmt(indVal, 3)} × ${fmt(ratioVal, 6)} = ${fmt(out, 3)} mph")
                        }

                        val autoAttrib: Set<VehicleId> = setOfNotNull(selectedVehicleId)

                        val calc = SavedCalculation(
                            type = CalcType.TIRE_SPEED_CORRECTION,
                            title = "Tire Speed Correction",
                            inputs = listOf(
                                CalcValue("Stock width", sw!!, "mm"),
                                CalcValue("Stock aspect", sa!!, "%"),
                                CalcValue("Stock wheel", sr!!, "in"),
                                CalcValue("Actual width", aw!!, "mm"),
                                CalcValue("Actual aspect", aa!!, "%"),
                                CalcValue("Actual wheel", ar!!, "in"),
                                CalcValue("Indicated speed", indVal, "mph"),
                                CalcValue("Multiplier", ratioVal, "×")
                            ),
                            outputs = listOf(
                                CalcValue("Actual speed", out, "mph")
                            ),
                            equationText = "Actual = Indicated × (D_actual / D_stock)",
                            steps = steps,
                            attributedVehicleIds = autoAttrib
                        )

                        save(calc)

                        if (autoAttrib.isEmpty()) {
                            prompt?.invoke(calc.calcId)
                        } else {
                            onBack()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            selectedVehicleId != null -> "Save to Case (Attributed)"
                            else -> "Save to Case"
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    stockWidth = ""; stockAspect = ""; stockWheel = ""
                    actualWidth = ""; actualAspect = ""; actualWheel = ""
                    indicatedMph = "60"
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear") }
        }
    }
}

@Composable
private fun TireRow(
    width: String, onWidth: (String) -> Unit,
    aspect: String, onAspect: (String) -> Unit,
    wheel: String, onWheel: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = width,
            onValueChange = onWidth,
            label = { Text("Width") },
            suffix = { Text("mm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = aspect,
            onValueChange = onAspect,
            label = { Text("Aspect") },
            suffix = { Text("%") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = wheel,
            onValueChange = onWheel,
            label = { Text("Wheel") },
            suffix = { Text("in") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
