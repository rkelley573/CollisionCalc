package com.example.collisioncalc.ui.caseworkbook

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.*
import com.example.collisioncalc.ui.components.CrashSummaryCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class CalcFilterMode { ALL, UNASSIGNED, UNIT }

/* ---------------------------
   Hoisted helpers (perf)
---------------------------- */

private fun formatCalcNumber(x: Double): String {
    if (!x.isFinite()) return "—"
    val s = "%.3f".format(x)
    return s.trimEnd('0').trimEnd('.')
}

private val calcsTimeDf: SimpleDateFormat by lazy {
    SimpleDateFormat("M/d/yy h:mm a", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
}

private fun formatLocalTime(epochMs: Long): String =
    calcsTimeDf.format(Date(epochMs))

private fun attributionLine(caseFile: CaseFile, c: SavedCalculation): String {
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

private fun outputsPreview(c: SavedCalculation): String {
    if (c.outputs.isEmpty()) return "No outputs"

    val multiAttrib = (c.attributedUnitIds.size + c.attributedVehicleIds.size) > 1
    val max = if (multiAttrib) 4 else 1

    val shown = c.outputs.take(max).joinToString(" • ") { o ->
        "${o.name} = ${formatCalcNumber(o.value)} ${o.unit}".trim()
    }

    val more = if (c.outputs.size > max) " …" else ""
    return shown + more
}

/* ---------------------------
   CALCS TAB
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcsTabContent(
    caseFile: CaseFile,
    onOpenCalculation: (CalcId) -> Unit,
    onGoToCombinedSpeed: () -> Unit,
    onGoToMomentum: () -> Unit,
    onOpenUnitTools: () -> Unit
) {
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

    val filteredCalcs by remember(caseFile.calculations, filterMode, filterUnitId) {
        derivedStateOf {
            caseFile.calculations
                .asSequence()
                .filter { matchesFilter(it) }
                .sortedByDescending { it.createdAtEpochMs }
                .toList()
        }
    }

    // ✅ Tools launcher UI (THIS is what was missing from your list)
    val toolsRow: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onGoToCombinedSpeed, modifier = Modifier.weight(1f)) { Text("Combined Speed") }
            Button(onClick = onGoToMomentum, modifier = Modifier.weight(1f)) { Text("Momentum") }
        }
        Button(onClick = onOpenUnitTools, modifier = Modifier.fillMaxWidth()) { Text("Unit Tools") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CrashSummaryCard(caseFile.crashInfo) }

        // ✅ Always show launch buttons so you can run tools even with 0 calcs
        item { toolsRow() }

        item { HorizontalDivider() }

        item { Text("Filter", style = MaterialTheme.typography.titleSmall) }

        item {
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
        }

        item {
            Text(
                text = "Showing ${filteredCalcs.size} of ${caseFile.calculations.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filteredCalcs.isEmpty()) {
            item {
                Text("No saved calculations in this filter.")
            }
            item {
                Text(
                    "Use Momentum / Combined Speed / Unit Tools above, then save the results to this case.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(
                items = filteredCalcs,
                key = { _, c -> c.calcId }
            ) { _, c ->
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
                            "Attributed to: ${attributionLine(caseFile, c)}",
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

        item { Spacer(Modifier.height(24.dp)) }
    }
}
