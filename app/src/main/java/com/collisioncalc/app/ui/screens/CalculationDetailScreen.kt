package com.collisioncalc.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcId
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.SavedCalculation
import kotlin.math.abs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val calcTimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d/yy h:mm a", Locale.US)

private fun formatLocalTime(epochMs: Long): String {
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(calcTimeFmt)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationDetailScreen(
    caseFile: CaseFile,
    calcId: CalcId,
    onBack: () -> Unit
) {
    val calc = caseFile.calculations.firstOrNull { it.calcId == calcId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculation") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        if (calc == null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Calculation not found.", style = MaterialTheme.typography.titleMedium)
                Text("It may have been deleted or the case data didn’t load correctly.")
                Button(onClick = onBack) { Text("Back") }
            }
            return@Scaffold
        }

        val attributionLabel = buildAttributionLabel(caseFile, calc)

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(calc.title, style = MaterialTheme.typography.titleLarge)

            if (calc.equationText.isNotBlank()) {
                Text(calc.equationText, style = MaterialTheme.typography.bodyMedium)
            }

            AssistChip(
                onClick = { },
                enabled = false,
                label = { Text("Attributed: $attributionLabel") }
            )

            // Outputs (highlight)
            if (calc.outputs.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Result", style = MaterialTheme.typography.titleSmall)
                        calc.outputs.forEach { v ->
                            Text("${v.name}: ${formatNum(v.value)} ${v.unit}".trim())
                        }
                    }
                }
            }

            // Inputs
            if (calc.inputs.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Inputs", style = MaterialTheme.typography.titleSmall)
                        calc.inputs.forEach { v ->
                            Text("${v.name}: ${formatNum(v.value)} ${v.unit}".trim())
                        }
                    }
                }
            }

            // Work shown
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Work Shown", style = MaterialTheme.typography.titleSmall)
                    if (calc.steps.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodySmall)
                    } else {
                        calc.steps.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Notes (optional)
            if (calc.notes.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Notes", style = MaterialTheme.typography.titleSmall)
                        Text(calc.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun formatNum(v: Double): String {
    if (!v.isFinite()) return "—"
    val s = "%.3f".format(abs(v))
    return s.trimEnd('0').trimEnd('.')
}

private fun buildAttributionLabel(caseFile: CaseFile, calc: SavedCalculation): String {
    // Prefer Units if present
    if (calc.attributedUnitIds.isNotEmpty()) {
        val labels = caseFile.units
            .filter { calc.attributedUnitIds.contains(it.unitId) }
            .map { it.label.ifBlank { it.kind.name } }
        return if (labels.isEmpty()) "Unassigned" else labels.joinToString(", ")
    }

    // Fall back to Vehicles (legacy)
    if (calc.attributedVehicleIds.isNotEmpty()) {
        val labels = caseFile.vehicles
            .filter { calc.attributedVehicleIds.contains(it.vehicleId) }
            .map { it.label.ifBlank { "Vehicle" } }
        return if (labels.isEmpty()) "Unassigned" else labels.joinToString(", ")
    }

    return "Unassigned"
}
