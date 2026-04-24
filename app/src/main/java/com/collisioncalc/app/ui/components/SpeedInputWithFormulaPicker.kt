package com.collisioncalc.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.ui.screens.UnitToolCatalog

/**
 * A speed input with two modes:
 *  - Manual: plain text field for mph.
 *  - Calculated: pick a speed-producing formula (skid, yaw, vault, fall, etc.),
 *    enter what you know, compute a speed. The derivation steps are exposed
 *    so the parent can include them in its own Show Work.
 */
@Immutable
data class SpeedInputState(
    val mode: Mode = Mode.Manual,
    val manualText: String = "",
    val calculatedValue: Double? = null,
    val calculatedSteps: List<String> = emptyList(),
    val calculatedFormulaLabel: String? = null,
    // Picker sub-state (hoisted so selections survive mode toggles)
    val pickerGoal: UnitToolCatalog.Goal? = UnitToolCatalog.Goal.SPEED_FROM_D_MU_E,
    val pickerKnown: Set<UnitToolCatalog.Var> = emptySet(),
    val pickerValues: Map<UnitToolCatalog.Var, String> = emptyMap(),
    val pickerDistUnit: UnitToolCatalog.DistanceUnit = UnitToolCatalog.DistanceUnit.FT,
    val pickerTimeUnit: UnitToolCatalog.TimeUnit = UnitToolCatalog.TimeUnit.SEC,
    val pickerSignMode: UnitToolCatalog.SignMode = UnitToolCatalog.SignMode.MINUS,
    val pickerError: String? = null
) {
    enum class Mode { Manual, Calculated }

    val effectiveValue: Double?
        get() = when (mode) {
            Mode.Manual -> manualText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            Mode.Calculated -> calculatedValue
        }

    /** Lines to merge into parent Show Work. Empty when manual. */
    fun derivationBlock(sectionLabel: String): List<String> {
        if (mode != Mode.Calculated || calculatedSteps.isEmpty()) return emptyList()
        val title = calculatedFormulaLabel?.let { "$sectionLabel — $it" } ?: sectionLabel
        return buildList {
            add("--- $title ---")
            calculatedSteps.forEach { add("  $it") }
        }
    }
}

private val SPEED_PRODUCING_GOALS = listOf(
    UnitToolCatalog.Goal.SPEED_FROM_VELOCITY,
    UnitToolCatalog.Goal.SPEED_FROM_D_MU_E,
    UnitToolCatalog.Goal.SPEED_FROM_D_MU_G_E,
    UnitToolCatalog.Goal.YAW_SPEED,
    UnitToolCatalog.Goal.FLIP_VAULT_SPEED,
    UnitToolCatalog.Goal.FALL_SPEED,
    UnitToolCatalog.Goal.LONG_VAULT_SPEED,
    UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_A_T,
    UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_MU_T,
    UnitToolCatalog.Goal.S0_FROM_A_D_STOP
)

private val GOALS_NEEDING_SIGN = setOf(
    UnitToolCatalog.Goal.SPEED_FROM_D_MU_G_E,
    UnitToolCatalog.Goal.FLIP_VAULT_SPEED,
    UnitToolCatalog.Goal.FALL_SPEED,
    UnitToolCatalog.Goal.LONG_VAULT_SPEED,
    UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_A_T,
    UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_MU_T
)

private fun goalUsesDistance(goal: UnitToolCatalog.Goal): Boolean =
    UnitToolCatalog.varsForGoal(goal).any {
        it == UnitToolCatalog.Var.DISTANCE || it == UnitToolCatalog.Var.HEIGHT
    }

private fun goalUsesTime(goal: UnitToolCatalog.Goal): Boolean =
    UnitToolCatalog.varsForGoal(goal).contains(UnitToolCatalog.Var.TIME)

/** Mutex groups: speed↔velocity, μ↔a, and initial-source group for goals that need it. */
private fun mutexGroupsForGoal(goal: UnitToolCatalog.Goal): List<Set<UnitToolCatalog.Var>> {
    val needsInitialSourceMutex = goal in setOf(
        UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_A_T,
        UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_MU_T
    )
    return buildList {
        add(setOf(UnitToolCatalog.Var.SPEED_MPH, UnitToolCatalog.Var.VELOCITY_FPS))
        add(setOf(UnitToolCatalog.Var.MU, UnitToolCatalog.Var.A_FTPS2))
        if (needsInitialSourceMutex) {
            add(
                setOf(
                    UnitToolCatalog.Var.V0_FPS,
                    UnitToolCatalog.Var.V0_SPEED_MPH,
                    UnitToolCatalog.Var.SPEED_MPH
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpeedInputWithFormulaPicker(
    label: String,
    state: SpeedInputState,
    onStateChange: (SpeedInputState) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false
) {
    val allFormulas = remember { UnitToolCatalog.allFormulas() }

    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.mode == SpeedInputState.Mode.Manual,
                    onClick = {
                        onStateChange(state.copy(mode = SpeedInputState.Mode.Manual))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Enter manually") }
                SegmentedButton(
                    selected = state.mode == SpeedInputState.Mode.Calculated,
                    onClick = {
                        onStateChange(state.copy(mode = SpeedInputState.Mode.Calculated))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Calculate from formula") }
            }

            when (state.mode) {
                SpeedInputState.Mode.Manual -> {
                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(
                        value = state.manualText,
                        onValueChange = {
                            onStateChange(state.copy(manualText = it))
                        },
                        label = { Text("Speed") },
                        trailingIcon = {
                            if (state.manualText.isNotEmpty()) {
                                IconButton(onClick = {
                                    onStateChange(state.copy(manualText = ""))
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            } else {
                                Text("mph")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        isError = isError,
                        supportingText = supportingText?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SpeedInputState.Mode.Calculated -> CalculatedModeUi(
                    state = state,
                    allFormulas = allFormulas,
                    onStateChange = onStateChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CalculatedModeUi(
    state: SpeedInputState,
    allFormulas: List<UnitToolCatalog.Formula>,
    onStateChange: (SpeedInputState) -> Unit
) {
    val goal = state.pickerGoal

    // Method picker
    GoalDropDown(
        label = "Method",
        selected = goal,
        onSelect = { newGoal ->
            val allowed = UnitToolCatalog.varsForGoal(newGoal).toSet()
            onStateChange(
                state.copy(
                    pickerGoal = newGoal,
                    pickerKnown = state.pickerKnown.intersect(allowed),
                    pickerValues = state.pickerValues.filterKeys { allowed.contains(it) },
                    calculatedValue = null,
                    calculatedSteps = emptyList(),
                    calculatedFormulaLabel = null,
                    pickerError = null
                )
            )
        }
    )

    if (goal == null) {
        Text(
            "Pick a method above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Sign / distance-unit / time-unit selectors (only when relevant)
    if (goalUsesDistance(goal) || goalUsesTime(goal) || goal in GOALS_NEEDING_SIGN) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (goalUsesDistance(goal)) {
                StringDropDown(
                    label = "Distance unit",
                    value = state.pickerDistUnit.label,
                    options = UnitToolCatalog.DistanceUnit.entries.map { it.label },
                    onSelect = { sel ->
                        val picked = UnitToolCatalog.DistanceUnit.entries.first { it.label == sel }
                        onStateChange(state.copy(pickerDistUnit = picked, pickerError = null))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (goalUsesTime(goal)) {
                StringDropDown(
                    label = "Time unit",
                    value = state.pickerTimeUnit.label,
                    options = UnitToolCatalog.TimeUnit.entries.map { it.label },
                    onSelect = { sel ->
                        val picked = UnitToolCatalog.TimeUnit.entries.first { it.label == sel }
                        onStateChange(state.copy(pickerTimeUnit = picked, pickerError = null))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (goal in GOALS_NEEDING_SIGN) {
            StringDropDown(
                label = "± choice",
                value = state.pickerSignMode.label,
                options = UnitToolCatalog.SignMode.entries.map { it.label },
                onSelect = { sel ->
                    val picked = UnitToolCatalog.SignMode.entries.first { it.label == sel }
                    onStateChange(state.copy(pickerSignMode = picked, pickerError = null))
                }
            )
        }
    }

    // If the goal has exactly one formula whose requires cover every available var,
    // there's no real choice to make — skip the "What do you know?" chip step.
    val autoSelectVars = UnitToolCatalog.autoSelectVarsForGoal(goal, allFormulas)

    if (autoSelectVars == null) {
        Text("What do you know?", style = MaterialTheme.typography.bodyMedium)
        val vars = UnitToolCatalog.varsForGoal(goal)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            vars.forEach { v ->
                val selected = state.pickerKnown.contains(v)
                FilterChip(
                    selected = selected,
                    onClick = {
                        onStateChange(state.toggleKnown(v, goal))
                    },
                    label = { Text(v.label) }
                )
            }
        }
    }

    // Effective "known" set — auto-selected vars override stored selection when applicable.
    val effectiveKnown = autoSelectVars ?: state.pickerKnown

    // Value entry for each selected var
    val varsToShow = effectiveKnown.toList().sortedBy { it.label }
    if (varsToShow.isNotEmpty()) {
        Spacer(Modifier.height(2.dp))
        val focusManager = LocalFocusManager.current
        varsToShow.forEachIndexed { i, v ->
            val isLast = i == varsToShow.lastIndex
            OutlinedTextField(
                value = state.pickerValues[v].orEmpty(),
                onValueChange = { text ->
                    onStateChange(
                        state.copy(
                            pickerValues = state.pickerValues + (v to text),
                            pickerError = null
                        )
                    )
                },
                label = { Text(v.label) },
                trailingIcon = { Text(v.unitHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Compute
    val ctx = UnitToolCatalog.Context(state.pickerDistUnit, state.pickerTimeUnit, state.pickerSignMode)
    val entered = state.pickerValues.mapNotNull { (k, text) ->
        val d = text.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (d != null) k to d else null
    }.toMap()
    val resolved = UnitToolCatalog.resolveDerivedInputs(ctx, entered)
    val formula = UnitToolCatalog.pickFormula(goal, effectiveKnown, allFormulas)
    val canCompute = formula != null && formula.requires.all { resolved.containsKey(it) }

    Button(
        onClick = {
            val f = formula ?: return@Button
            try {
                val computed = f.compute(ctx, resolved)
                val mph = computed.outputs.firstOrNull { it.unit == "mph" }?.value
                if (mph == null || !mph.isFinite()) {
                    onStateChange(
                        state.copy(
                            pickerError = "Formula did not produce a valid mph output.",
                            calculatedValue = null,
                            calculatedSteps = emptyList(),
                            calculatedFormulaLabel = null
                        )
                    )
                } else {
                    val formulaSteps = buildList {
                        add("Formula: ${computed.equationText}")
                        addAll(computed.steps)
                    }
                    onStateChange(
                        state.copy(
                            calculatedValue = mph,
                            calculatedSteps = formulaSteps,
                            calculatedFormulaLabel = goal.title,
                            pickerError = null
                        )
                    )
                }
            } catch (e: IllegalArgumentException) {
                onStateChange(
                    state.copy(
                        pickerError = e.message ?: "Invalid input.",
                        calculatedValue = null,
                        calculatedSteps = emptyList(),
                        calculatedFormulaLabel = null
                    )
                )
            } catch (_: Exception) {
                onStateChange(
                    state.copy(
                        pickerError = "Could not compute. Check inputs.",
                        calculatedValue = null,
                        calculatedSteps = emptyList(),
                        calculatedFormulaLabel = null
                    )
                )
            }
        },
        enabled = canCompute,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Compute") }

    state.pickerError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }

    state.calculatedValue?.let { v ->
        var showSteps by remember(state.calculatedValue, state.calculatedFormulaLabel) {
            mutableStateOf(false)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Computed speed: ${"%.2f".format(v)} mph",
                    style = MaterialTheme.typography.titleMedium
                )
                state.calculatedFormulaLabel?.let {
                    Text("Method: $it", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { showSteps = !showSteps }) {
                    Text(if (showSteps) "Hide derivation" else "Show derivation")
                }
                if (showSteps) {
                    state.calculatedSteps.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun SpeedInputState.toggleKnown(
    v: UnitToolCatalog.Var,
    goal: UnitToolCatalog.Goal
): SpeedInputState {
    return if (pickerKnown.contains(v)) {
        copy(
            pickerKnown = pickerKnown - v,
            pickerValues = pickerValues - v,
            pickerError = null
        )
    } else {
        val group = mutexGroupsForGoal(goal).firstOrNull { it.contains(v) }
        val conflicts = group?.minus(v).orEmpty()
        copy(
            pickerKnown = (pickerKnown - conflicts) + v,
            pickerValues = pickerValues - conflicts,
            pickerError = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDropDown(
    label: String,
    selected: UnitToolCatalog.Goal?,
    onSelect: (UnitToolCatalog.Goal) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val value = selected?.title ?: "Select method…"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            supportingText = selected?.let { { Text(it.subtitle) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SPEED_PRODUCING_GOALS.forEach { g ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(g.title)
                            Text(
                                g.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(g)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StringDropDown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
