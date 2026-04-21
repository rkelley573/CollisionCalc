package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcId
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.UnitId
import com.collisioncalc.app.ui.components.CrashSummaryCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class CalcFilterMode { ALL, UNASSIGNED, UNIT }
private enum class CalcSortMode { NEWEST, OLDEST, TITLE }
private enum class CalcGroupMode { NONE, TYPE, DATE }

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

private val calcsDateDf: SimpleDateFormat by lazy {
    SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
}

private fun groupDateLabel(epochMs: Long): String {
    val cal = Calendar.getInstance()
    val todayY = cal.get(Calendar.YEAR)
    val todayD = cal.get(Calendar.DAY_OF_YEAR)

    val target = Calendar.getInstance().apply { timeInMillis = epochMs }
    val y = target.get(Calendar.YEAR)
    val d = target.get(Calendar.DAY_OF_YEAR)

    return when {
        y == todayY && d == todayD -> "Today"
        y == todayY && d == todayD - 1 -> "Yesterday"
        else -> calcsDateDf.format(Date(epochMs))
    }
}

private fun groupDateKey(epochMs: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    return (cal.get(Calendar.YEAR) * 1000) + cal.get(Calendar.DAY_OF_YEAR)
}

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

private data class CalcGroup(val key: Long, val title: String, val items: List<SavedCalculation>)

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

    var sortMode by remember { mutableStateOf(CalcSortMode.NEWEST) }
    var groupMode by remember { mutableStateOf(CalcGroupMode.NONE) }
    var query by remember { mutableStateOf("") }

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

    val filteredCalcs by remember(caseFile.calculations, filterMode, filterUnitId, sortMode, query) {
        derivedStateOf {
            val q = query.trim().lowercase(Locale.US)

            val base = caseFile.calculations
                .asSequence()
                .filter { matchesFilter(it) }
                .filter { c ->
                    if (q.isBlank()) true
                    else {
                        c.title.lowercase(Locale.US).contains(q) ||
                                c.type.name.lowercase(Locale.US).replace('_', ' ').contains(q)
                    }
                }

            when (sortMode) {
                CalcSortMode.NEWEST -> base.sortedByDescending { it.createdAtEpochMs }
                CalcSortMode.OLDEST -> base.sortedBy { it.createdAtEpochMs }
                CalcSortMode.TITLE -> base.sortedBy { it.title.lowercase(Locale.US) }
            }.toList()
        }
    }

    val groupedCalcs by remember(filteredCalcs, groupMode) {
        derivedStateOf {
            when (groupMode) {
                CalcGroupMode.NONE -> listOf(CalcGroup(0L, "", filteredCalcs))

                CalcGroupMode.TYPE -> filteredCalcs
                    .groupBy { it.type }
                    .toList()
                    .sortedBy { it.first.name }
                    .map { (k, v) ->
                        CalcGroup(
                            key = k.ordinal.toLong(),
                            title = k.name.replace('_', ' '),
                            items = v
                        )
                    }

                CalcGroupMode.DATE -> filteredCalcs
                    .groupBy { groupDateKey(it.createdAtEpochMs) }
                    .toList()
                    .sortedByDescending { (k, _) -> k }
                    .map { (k, v) ->
                        val title = groupDateLabel(v.maxOf { it.createdAtEpochMs })
                        CalcGroup(
                            key = k.toLong(),
                            title = title,
                            items = v
                        )
                    }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CrashSummaryCard(caseFile.crashInfo) }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tools", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onGoToCombinedSpeed, modifier = Modifier.weight(1f)) { Text("Combined Speed") }
                        Button(onClick = onGoToMomentum, modifier = Modifier.weight(1f)) { Text("Momentum") }
                    }
                    OutlinedButton(onClick = onOpenUnitTools, modifier = Modifier.fillMaxWidth()) {
                        Text("Unit Tools")
                    }
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Saved calculations", style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search title / type") }
                    )

                    Text("Filter", style = MaterialTheme.typography.labelLarge)

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

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        var sortExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = sortExpanded,
                            onExpandedChange = { sortExpanded = !sortExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = when (sortMode) {
                                    CalcSortMode.NEWEST -> "Newest"
                                    CalcSortMode.OLDEST -> "Oldest"
                                    CalcSortMode.TITLE -> "Title"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sort") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                                DropdownMenuItem(text = { Text("Newest") }, onClick = { sortMode = CalcSortMode.NEWEST; sortExpanded = false })
                                DropdownMenuItem(text = { Text("Oldest") }, onClick = { sortMode = CalcSortMode.OLDEST; sortExpanded = false })
                                DropdownMenuItem(text = { Text("Title") }, onClick = { sortMode = CalcSortMode.TITLE; sortExpanded = false })
                            }
                        }

                        var groupExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = groupExpanded,
                            onExpandedChange = { groupExpanded = !groupExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = when (groupMode) {
                                    CalcGroupMode.NONE -> "None"
                                    CalcGroupMode.TYPE -> "Type"
                                    CalcGroupMode.DATE -> "Date"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Group") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { groupMode = CalcGroupMode.NONE; groupExpanded = false })
                                DropdownMenuItem(text = { Text("Type") }, onClick = { groupMode = CalcGroupMode.TYPE; groupExpanded = false })
                                DropdownMenuItem(text = { Text("Date") }, onClick = { groupMode = CalcGroupMode.DATE; groupExpanded = false })
                            }
                        }
                    }

                    Text(
                        text = "Showing ${filteredCalcs.size} of ${caseFile.calculations.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (filteredCalcs.isEmpty()) {
            item { Text("No saved calculations match your current view.", style = MaterialTheme.typography.titleSmall) }
            item {
                Text(
                    "Use Momentum / Combined Speed / Unit Tools above, then save the results to this case.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            groupedCalcs.forEach { group ->
                if (groupMode != CalcGroupMode.NONE) {
                    item(key = "hdr_${group.key}") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = group.items,
                    key = { _, c -> c.calcId }
                ) { _, c ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenCalculation(c.calcId) }
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        c.type.name.replace('_', ' '),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    formatLocalTime(c.createdAtEpochMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AssistChip(
                                onClick = { },
                                label = { Text("Attributed: ${attributionLine(caseFile, c)}") }
                            )

                            Text(outputsPreview(c), style = MaterialTheme.typography.bodySmall)

                            Text(
                                "Tap to view work",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
