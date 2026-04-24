package com.collisioncalc.app.ui.screens

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
import com.collisioncalc.app.data.CalcType
import com.collisioncalc.app.data.CalcValue
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.ui.components.AttributionPicker
import com.collisioncalc.app.ui.components.AttributionSelection
import com.collisioncalc.app.ui.components.SpeedInputState
import com.collisioncalc.app.ui.components.SpeedInputWithFormulaPicker
import kotlin.math.sqrt

private data class SpeedRow(
    val id: String,
    val label: String,
    val input: SpeedInputState = SpeedInputState()
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
                SpeedRow(id = "1", label = "Component 1"),
                SpeedRow(id = "2", label = "Component 2")
            )
        )
    }

    fun parsedValues(): List<Pair<SpeedRow, Double>> =
        rows.mapNotNull { r -> r.input.effectiveValue?.let { r to it } }

    val valuePairs = parsedValues()
    val values = valuePairs.map { it.second }
    val sumSquares = values.sumOf { it * it }
    val combined = if (values.size >= 2) sqrt(sumSquares) else null

    val equationText = "S = √(Σv²)"

    // Collect derivation blocks for every row that was computed via a formula
    // (regardless of whether its current effectiveValue is usable right now).
    val derivationBlocks: List<List<String>> = rows.mapIndexedNotNull { idx, row ->
        val sectionLabel = row.label.ifBlank { "Segment ${idx + 1}" }
        row.input.derivationBlock(sectionLabel).takeIf { it.isNotEmpty() }
    }

    val steps = buildList {
        if (derivationBlocks.isNotEmpty()) {
            add("=== Segment derivations ===")
            derivationBlocks.forEachIndexed { i, block ->
                addAll(block)
                if (i < derivationBlocks.size - 1) add("")
            }
            add("")
        }

        add("=== Combined speed ===")
        add("Formula:    $equationText")
        if (values.isNotEmpty()) {
            val termStr = values.joinToString(" + ") { v -> "${"%.2f".format(v)}²" }
            add("Substitute: S = √($termStr)")
            val expanded = values.joinToString(" + ") { v -> "%.4f".format(v * v) }
            add("Compute:    S = √($expanded)")
            add("            S = √(${"%.4f".format(sumSquares)})")
        }
        if (combined != null) add("Result:     S = ${"%.2f".format(combined)} mph")
        else add("Result:     Enter at least two speeds.")
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
                    Text(
                        "For each segment, either enter a speed directly or calculate it from a formula.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    rows.forEachIndexed { idx, r ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = r.label,
                                onValueChange = { newLabel ->
                                    rows = rows.toMutableList().apply {
                                        this[idx] = this[idx].copy(label = newLabel)
                                    }
                                },
                                label = { Text("Label") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                trailingIcon = {
                                    if (r.label.isNotEmpty()) {
                                        IconButton(onClick = {
                                            rows = rows.toMutableList().apply {
                                                this[idx] = this[idx].copy(label = "")
                                            }
                                        }) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            SpeedInputWithFormulaPicker(
                                label = r.label.ifBlank { "Segment ${idx + 1}" },
                                state = r.input,
                                onStateChange = { newState ->
                                    rows = rows.toMutableList().apply {
                                        this[idx] = this[idx].copy(input = newState)
                                    }
                                }
                            )

                            if (rows.size > 2) {
                                TextButton(
                                    onClick = {
                                        rows = rows.toMutableList().apply { removeAt(idx) }
                                    }
                                ) { Text("Remove segment") }
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
                                    label = "Component ${rows.size + 1}"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Add Row") }

                        OutlinedButton(
                            onClick = {
                                rows = listOf(
                                    SpeedRow(id = "1", label = "Component 1"),
                                    SpeedRow(id = "2", label = "Component 2")
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
                        val v = row.input.effectiveValue ?: return@mapNotNull null
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
fun CombinedSpeedContent(
    allFormulas: List<UnitToolCatalog.Formula>,
    attributedUnitIds: Set<String>,
    attributedVehicleIds: Set<String>,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    var rows by remember {
        mutableStateOf(
            listOf(
                SpeedRow(id = "1", label = "Component 1"),
                SpeedRow(id = "2", label = "Component 2")
            )
        )
    }

    fun parsedValues(): List<Pair<SpeedRow, Double>> =
        rows.mapNotNull { r -> r.input.effectiveValue?.let { r to it } }

    val valuePairs = parsedValues()
    val values = valuePairs.map { it.second }
    val sumSquares = values.sumOf { it * it }
    val combined = if (values.size >= 2) sqrt(sumSquares) else null

    val equationText = "S = √(Σv²)"

    val derivationBlocks: List<List<String>> = rows.mapIndexedNotNull { idx, row ->
        val sectionLabel = row.label.ifBlank { "Segment ${idx + 1}" }
        row.input.derivationBlock(sectionLabel).takeIf { it.isNotEmpty() }
    }

    val steps = buildList {
        if (derivationBlocks.isNotEmpty()) {
            add("=== Segment derivations ===")
            derivationBlocks.forEachIndexed { i, block ->
                addAll(block)
                if (i < derivationBlocks.size - 1) add("")
            }
            add("")
        }
        add("=== Combined speed ===")
        add("Formula:    $equationText")
        if (values.isNotEmpty()) {
            val termStr = values.joinToString(" + ") { v -> "${"%.2f".format(v)}²" }
            add("Substitute: S = √($termStr)")
            val expanded = values.joinToString(" + ") { v -> "%.4f".format(v * v) }
            add("Compute:    S = √($expanded)")
            add("            S = √(${"%.4f".format(sumSquares)})")
        }
        if (combined != null) add("Result:     S = ${"%.2f".format(combined)} mph")
        else add("Result:     Enter at least two speeds.")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Inputs", style = MaterialTheme.typography.titleSmall)
                Text("Combined = √(sum of each speed²)", style = MaterialTheme.typography.bodySmall)
                Text(
                    "For each segment, either enter a speed directly or calculate it from a formula.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                rows.forEachIndexed { idx, r ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = r.label,
                            onValueChange = { newLabel ->
                                rows = rows.toMutableList().apply {
                                    this[idx] = this[idx].copy(label = newLabel)
                                }
                            },
                            label = { Text("Label") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            trailingIcon = {
                                if (r.label.isNotEmpty()) {
                                    IconButton(onClick = {
                                        rows = rows.toMutableList().apply {
                                            this[idx] = this[idx].copy(label = "")
                                        }
                                    }) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        SpeedInputWithFormulaPicker(
                            label = r.label.ifBlank { "Segment ${idx + 1}" },
                            state = r.input,
                            onStateChange = { newState ->
                                rows = rows.toMutableList().apply {
                                    this[idx] = this[idx].copy(input = newState)
                                }
                            }
                        )

                        if (rows.size > 2) {
                            TextButton(
                                onClick = {
                                    rows = rows.toMutableList().apply { removeAt(idx) }
                                }
                            ) { Text("Remove segment") }
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
                            rows = rows + SpeedRow(id = nextId, label = "Component ${rows.size + 1}")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add Row") }

                    OutlinedButton(
                        onClick = {
                            rows = listOf(
                                SpeedRow(id = "1", label = "Component 1"),
                                SpeedRow(id = "2", label = "Component 2")
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
                    val v = row.input.effectiveValue ?: return@mapNotNull null
                    CalcValue(name = row.label.ifBlank { "Component" }, value = v, unit = "mph")
                }
                val outVal = combined ?: return@Button
                onSaveCalculation(
                    SavedCalculation(
                        type = CalcType.COMBINED_SPEED,
                        title = "Combined Speed",
                        inputs = inputs,
                        outputs = listOf(CalcValue(name = "S", value = outVal, unit = "mph")),
                        equationText = equationText,
                        steps = steps,
                        attributedUnitIds = attributedUnitIds,
                        attributedVehicleIds = attributedVehicleIds
                    )
                )
            },
            enabled = combined != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}
