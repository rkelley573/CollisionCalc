package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.VehicleId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributePromptScreen(
    caseFile: CaseFile,

    // Optional default selection (so callers don't have to pass anything)
    defaultVehicleId: VehicleId? = null,

    onChoose: (VehicleId) -> Unit,
    onSkip: () -> Unit
) {
    val v1 = caseFile.vehicles.getOrNull(0)
    val v2 = caseFile.vehicles.getOrNull(1)

    var selectedVehicleId by remember(caseFile.caseId, defaultVehicleId) {
        mutableStateOf(
            defaultVehicleId
                ?: v1?.vehicleId
                ?: v2?.vehicleId
        )
    }

    val selectedLabel = when (selectedVehicleId) {
        v1?.vehicleId -> v1?.label ?: "Vehicle 1"
        v2?.vehicleId -> v2?.label ?: "Vehicle 2"
        else -> "None"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attribute Calculation") },
                navigationIcon = { IconButton(onClick = onSkip) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Choose which vehicle this calculation belongs to.",
                style = MaterialTheme.typography.bodyMedium
            )

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Vehicle", style = MaterialTheme.typography.titleSmall)

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

                    Text(
                        "Selected: $selectedLabel",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = {
                    val chosen = selectedVehicleId ?: return@Button
                    onChoose(chosen)
                },
                enabled = selectedVehicleId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assign")
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip")
            }
        }
    }
}
