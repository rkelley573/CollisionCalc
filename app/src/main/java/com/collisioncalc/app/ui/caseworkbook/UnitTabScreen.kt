package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.PedestrianUnit
import com.collisioncalc.app.data.UnitId
import com.collisioncalc.app.data.Vehicle
import com.collisioncalc.app.data.VehicleId
import com.collisioncalc.app.data.VehicleUnit

@Composable
fun UnitsTabScreen(
    caseFile: CaseFile,
    onOpenVehicle: (VehicleId) -> Unit,
    onOpenPedestrian: (UnitId) -> Unit,
    onAddVehicleUnit: () -> Unit,
    onAddPedestrianUnit: () -> Unit,
    onRenameUnit: (UnitId, String) -> Unit,
    onRemoveUnit: (UnitId) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<UnitId?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Units", style = MaterialTheme.typography.titleSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onAddVehicleUnit, modifier = Modifier.weight(1f)) { Text("Add Vehicle Unit") }
            Button(onClick = onAddPedestrianUnit, modifier = Modifier.weight(1f)) { Text("Add Pedestrian") }
        }

        HorizontalDivider()

        if (caseFile.units.isEmpty()) {
            Text("No units yet.")
        } else {
            caseFile.units.forEach { u ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (u) {
                            is VehicleUnit -> {
                                val v = caseFile.vehicles.firstOrNull { it.vehicleId == u.vehicleId }
                                Text(
                                    u.label.ifBlank { "Vehicle Unit" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    vehicleSummaryLine(v),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenVehicle(u.vehicleId) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Open Vehicle") }

                                    TextButton(onClick = { pendingDelete = u.unitId }) { Text("Remove") }
                                }
                            }

                            is PedestrianUnit -> {
                                Text(
                                    u.label.ifBlank { "Pedestrian" },
                                    style = MaterialTheme.typography.titleMedium
                                )

                                val nameLine = u.name.display().takeIf { it.isNotBlank() } ?: "—"
                                Text(
                                    "Name: $nameLine",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenPedestrian(u.unitId) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Open Pedestrian") }

                                    TextButton(onClick = { pendingDelete = u.unitId }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove unit?") },
            text = { Text("This will remove the unit and clear its attribution from saved calculations.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveUnit(pendingDelete!!)
                        pendingDelete = null
                    }
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

private fun vehicleSummaryLine(v: Vehicle?): String {
    if (v == null) return "Linked vehicle: —"

    val ymm = listOf(v.year.trim(), v.make.trim(), v.model.trim()).filter { it.isNotBlank() }.joinToString(" ")
    val color = v.color.trim().takeIf { it.isNotBlank() }

    val left = listOf(color, ymm).filterNotNull().joinToString(" • ").ifBlank { v.label.ifBlank { "Vehicle" } }
    val vin = v.vin.trim().takeIf { it.isNotBlank() }?.let { "VIN ${it.take(8)}…" }

    return listOf(left, vin).filterNotNull().joinToString("   |   ")
}
