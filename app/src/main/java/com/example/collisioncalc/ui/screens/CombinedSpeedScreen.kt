package com.example.collisioncalc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.CalcType
import com.example.collisioncalc.data.CalcValue
import com.example.collisioncalc.data.CaseFile
import com.example.collisioncalc.data.SavedCalculation
import com.example.collisioncalc.ui.components.AttributionPicker
import com.example.collisioncalc.ui.components.AttributionSelection
import kotlin.math.sqrt

private data class SpeedRow(
    val id: String,
    val label: String,
    val valueText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinedSpeedScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    var attribution by remember(caseFile.caseId) { mutableStateOf(AttributionSelection()) }

    var rows by remember {
        mutableStateOf(
            listOf(
                SpeedRow(id = "1", label = "Component 1", valueText = ""),
                SpeedRow(id = "2", label = "Component 2", valueText = "")
            )
        )
    }

    fun parsedValues(): List<Double> =
        rows.mapNotNull { it.valueText.trim().takeIf { t -> t.isNotEmpty() }?.toDoubleOrNull() }

    val values = parsedValues()
    val sumSquares = values.sumOf { it * it }
    val combined = if (values.size >= 2) sqrt(sumSquares) else null

    val equationText = "S = √(Σv²)"
    val steps = buildList {
        add("Equation: $equationText")
        if (values.isNotEmpty()) {
            val termStr = values.joinToString(" + ") { v -> "${v}²" }
            add("Substitute: S = √($termStr)")
            val expanded = values.joinToString(" + ") { v -> (v * v).toString() }
            add("Compute: S = √($expanded)")
        }
        if (combined != null) add("Result: S = ${"%.2f".format(combined)} mph")
        else add("Result: Enter at least two speeds.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Combined Speed") },
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

            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Attribution", style = MaterialTheme.typography.titleSmall)

                    AttributionPicker(
                        caseFile = caseFile,
                        selection = attribution,
                        onSelectionChange = { attribution = it },
                        title = "Assign to"
                    )

                    Text(
                        "Select the Unit/Vehicle this belongs to before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Inputs", style = MaterialTheme.typography.titleSmall)
                    Text("Combined = √(sum of each speed²)", style = MaterialTheme.typography.bodySmall)

                    rows.forEachIndexed { idx, r ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ClearableOutlinedTextField(
                                    value = r.label,
                                    onValueChange = { newLabel ->
                                        rows = rows.toMutableList().apply {
                                            this[idx] = this[idx].copy(label = newLabel)
                                        }
                                    },
                                    label = "Label",
                                    keyboardType = KeyboardType.Text,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                ClearableOutlinedTextField(
                                    value = r.valueText,
                                    onValueChange = { newVal ->
                                        rows = rows.toMutableList().apply {
                                            this[idx] = this[idx].copy(valueText = newVal)
                                        }
                                    },
                                    label = "Speed (mph)",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (rows.size > 2) {
                                    TextButton(
                                        onClick = {
                                            rows = rows.toMutableList().apply { removeAt(idx) }
                                        }
                                    ) { Text("Remove") }
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val nextId = (rows.size + 1).toString()
                                rows = rows + SpeedRow(
                                    id = nextId,
                                    label = "Component ${rows.size + 1}",
                                    valueText = ""
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Add Row") }

                        OutlinedButton(
                            onClick = {
                                rows = listOf(
                                    SpeedRow(id = "1", label = "Component 1", valueText = ""),
                                    SpeedRow(id = "2", label = "Component 2", valueText = "")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reset") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Result", style = MaterialTheme.typography.titleSmall)

                    Text(
                        text = if (combined != null)
                            "Combined Speed: ${"%.2f".format(combined)} mph"
                        else
                            "Combined Speed: —",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Σv² = ${"%.2f".format(sumSquares)}", style = MaterialTheme.typography.bodySmall)
                    Text("n = ${values.size}", style = MaterialTheme.typography.bodySmall)

                    var showWork by remember { mutableStateOf(false) }
                    TextButton(onClick = { showWork = !showWork }) {
                        Text(if (showWork) "Hide Work" else "Show Work")
                    }

                    if (showWork) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            steps.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (values.size < 2) return@Button

                    val inputs = rows.mapNotNull { row ->
                        val v = row.valueText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                            ?: return@mapNotNull null
                        CalcValue(
                            name = row.label.ifBlank { "Component" },
                            value = v,
                            unit = "mph"
                        )
                    }

                    val outVal = combined ?: return@Button

                    val calc = SavedCalculation(
                        type = CalcType.COMBINED_SPEED,
                        title = "Combined Speed",
                        inputs = inputs,
                        outputs = listOf(CalcValue(name = "S", value = outVal, unit = "mph")),
                        equationText = equationText,
                        steps = steps,
                        // Leave unassigned unless user selects something
                        attributedUnitIds = attribution.unitIds,
                        attributedVehicleIds = attribution.vehicleIds
                    )

                    onSaveCalculation(calc)
                    onBack()
                },
                enabled = combined != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ClearableOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier
    )
}
