package com.collisioncalc.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.PedestrianUnit
import com.collisioncalc.app.data.UnitEntity
import com.collisioncalc.app.data.UnitId
import com.collisioncalc.app.data.VehicleUnit

@Immutable
data class AttributionSelection(
    val unitIds: Set<UnitId> = emptySet(),
    val vehicleIds: Set<String> = emptySet() // legacy field retained but no longer used in UI
)

@Composable
fun AttributionPicker(
    caseFile: CaseFile,
    selection: AttributionSelection,
    onSelectionChange: (AttributionSelection) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Attribute calculation to"
) {
    val hasUnits = caseFile.units.isNotEmpty()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            Text(
                "Select one or more Units. Leave blank for unassigned.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onSelectionChange(
                            selection.copy(unitIds = caseFile.units.map { it.unitId }.toSet())
                        )
                    },
                    enabled = hasUnits
                ) { Text("Select all") }

                OutlinedButton(
                    onClick = { onSelectionChange(selection.copy(unitIds = emptySet(), vehicleIds = emptySet())) },
                    enabled = selection.unitIds.isNotEmpty() || selection.vehicleIds.isNotEmpty()
                ) { Text("Clear") }
            }

            if (!hasUnits) {
                Text("No Units in this case yet.", style = MaterialTheme.typography.bodySmall)
                return@Column
            }

            Text("Units", style = MaterialTheme.typography.labelLarge)
            caseFile.units.forEach { u ->
                UnitRow(
                    unit = u,
                    caseFile = caseFile,
                    checked = u.unitId in selection.unitIds,
                    onCheckedChange = { checked ->
                        val next = if (checked) selection.unitIds + u.unitId else selection.unitIds - u.unitId
                        onSelectionChange(selection.copy(unitIds = next))
                    }
                )
            }
        }
    }
}

@Composable
private fun UnitRow(
    unit: UnitEntity,
    caseFile: CaseFile,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    when (unit) {
        is VehicleUnit -> {
            val vehicleLabel = caseFile.vehicles.firstOrNull { it.vehicleId == unit.vehicleId }
                ?.label
                ?.takeIf { it.isNotBlank() }
                ?: "Vehicle"
            val label = unit.label.takeIf { it.isNotBlank() } ?: "Vehicle Unit"
            CheckRow(
                label = label,
                sublabel = "Linked to $vehicleLabel",
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }

        is PedestrianUnit -> {
            val label = unit.label.takeIf { it.isNotBlank() } ?: "Pedestrian"
            CheckRow(
                label = label,
                sublabel = "Pedestrian Unit",
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun CheckRow(
    label: String,
    sublabel: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(start = 6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (!sublabel.isNullOrBlank()) {
                Text(sublabel, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
