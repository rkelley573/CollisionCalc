package com.collisioncalc.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcType
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.ui.components.AttributionPicker
import com.collisioncalc.app.ui.components.AttributionSelection
import kotlin.collections.forEach
import kotlin.math.abs

private enum class Category(val title: String) {
    SPEED("Speed (mph)"),
    VELOCITY("Velocity (ft/s)"),
    TIME("Time"),
    DISTANCE("Distance"),
    FACTOR("Drag factor (μ)"),
    RATE("Accel / Decel rate (a)"),
    GRADE_ANGLE_RADIUS("Grade / Angle / Radius"),
    TRIG_MISC("Trig / Misc")
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun UnitConverterScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    // Cache formulas once.
    val allFormulas = remember { UnitToolCatalog.allFormulas() }
    val goalsWithFormulas = remember(allFormulas) { allFormulas.map { it.goal }.toSet() }

    fun goalsForCategory(cat: Category): List<UnitToolCatalog.Goal> {
        val raw = when (cat) {
            Category.SPEED -> listOf(
                UnitToolCatalog.Goal.SPEED_FROM_VELOCITY,
                UnitToolCatalog.Goal.SPEED_FROM_D_MU_E,
                UnitToolCatalog.Goal.SPEED_FROM_D_MU_G_E,
                UnitToolCatalog.Goal.COMBINED_SPEED,
                UnitToolCatalog.Goal.YAW_SPEED,
                UnitToolCatalog.Goal.FLIP_VAULT_SPEED,
                UnitToolCatalog.Goal.FALL_SPEED,
                UnitToolCatalog.Goal.LONG_VAULT_SPEED,
                UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_A_T,
                UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_MU_T
            )

            Category.VELOCITY -> listOf(
                UnitToolCatalog.Goal.VELOCITY_FROM_SPEED,
                UnitToolCatalog.Goal.AVG_VELOCITY_FROM_D_T,
                UnitToolCatalog.Goal.VELOCITY_FROM_A_T,
                UnitToolCatalog.Goal.VF_FROM_V0_A_T,
                UnitToolCatalog.Goal.VF_FROM_V0_A_D
            )

            Category.TIME -> listOf(
                UnitToolCatalog.Goal.TIME_FROM_D_V,
                UnitToolCatalog.Goal.TIME_FROM_V_A,
                UnitToolCatalog.Goal.TIME_DECEL_V0_VF_A,
                UnitToolCatalog.Goal.TIME_ACCEL_V0_VF_A,
                UnitToolCatalog.Goal.TIME_FROM_D_MU
            )

            Category.DISTANCE -> listOf(
                UnitToolCatalog.Goal.DIST_FROM_V_T,
                UnitToolCatalog.Goal.DIST_FROM_S_MU,
                UnitToolCatalog.Goal.DIST_FROM_MU_T,
                UnitToolCatalog.Goal.DIST_FROM_A_T,
                UnitToolCatalog.Goal.DIST_FROM_V0_T_A,
                UnitToolCatalog.Goal.PERCEPTION_REACTION_DISTANCE,
                UnitToolCatalog.Goal.TOTAL_STOPPING_DISTANCE
            )

            Category.FACTOR -> listOf(
                UnitToolCatalog.Goal.MU_FROM_A,
                UnitToolCatalog.Goal.MU_FROM_S_D,
                UnitToolCatalog.Goal.MU_FROM_D_T,
                UnitToolCatalog.Goal.MU_FROM_V_T,
                UnitToolCatalog.Goal.MU_ACCEL_BETWEEN_V_IN_T,
                UnitToolCatalog.Goal.MU_DECEL_BETWEEN_V_IN_T,
                UnitToolCatalog.Goal.MU_FROM_TWO_V_AND_D,
                UnitToolCatalog.Goal.MU_FROM_FORCE_WEIGHT,
                UnitToolCatalog.Goal.MU_FROM_D_T_V0
            )

            Category.RATE -> listOf(UnitToolCatalog.Goal.A_FROM_MU)

            Category.GRADE_ANGLE_RADIUS -> listOf(
                UnitToolCatalog.Goal.GRADE_FROM_RISE_RUN,
                UnitToolCatalog.Goal.TAKEOFF_ANGLE_FROM_D_H,
                UnitToolCatalog.Goal.UNKNOWN_TAKEOFF_LANDING_LOWER,
                UnitToolCatalog.Goal.UNKNOWN_TAKEOFF_LANDING_HIGHER,
                UnitToolCatalog.Goal.RADIUS_FROM_CHORD_MIDORD
            )

            Category.TRIG_MISC -> listOf(
                UnitToolCatalog.Goal.PYTHAG_C_FROM_A_B,
                UnitToolCatalog.Goal.SIN_FROM_OPP_HYP,
                UnitToolCatalog.Goal.COS_FROM_ADJ_HYP,
                UnitToolCatalog.Goal.TAN_FROM_OPP_ADJ
            )
        }

        return raw.filter { goalsWithFormulas.contains(it) }
    }

    fun needsSign(goal: UnitToolCatalog.Goal): Boolean = goal in setOf(
        UnitToolCatalog.Goal.SPEED_FROM_D_MU_G_E,
        UnitToolCatalog.Goal.FLIP_VAULT_SPEED,
        UnitToolCatalog.Goal.FALL_SPEED,
        UnitToolCatalog.Goal.LONG_VAULT_SPEED,
        UnitToolCatalog.Goal.VF_FROM_V0_A_T,
        UnitToolCatalog.Goal.VF_FROM_V0_A_D,
        UnitToolCatalog.Goal.DIST_FROM_V0_T_A,
        UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_A_T,
        UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_MU_T
    )

    // ---------------- attribution ----------------
    var attribution by remember(caseFile.caseId) { mutableStateOf(AttributionSelection()) }

    // ---------------- state ----------------
    var category by remember { mutableStateOf(Category.VELOCITY) }
    val categoryGoals = remember(category, goalsWithFormulas) { goalsForCategory(category) }

    var goal by remember(categoryGoals) {
        mutableStateOf(categoryGoals.firstOrNull() ?: UnitToolCatalog.Goal.VELOCITY_FROM_SPEED)
    }

    var known by remember { mutableStateOf(setOf<UnitToolCatalog.Var>()) }

    var distUnit by remember { mutableStateOf(UnitToolCatalog.DistanceUnit.FT) }
    var timeUnit by remember { mutableStateOf(UnitToolCatalog.TimeUnit.SEC) }
    var signMode by remember { mutableStateOf(UnitToolCatalog.SignMode.MINUS) }

    val textByVar = remember { mutableStateMapOf<UnitToolCatalog.Var, String>() }

    var error by remember { mutableStateOf<String?>(null) }
    var computed by remember { mutableStateOf<UnitToolCatalog.Computed?>(null) }
    var infoGoal by remember { mutableStateOf<UnitToolCatalog.Goal?>(null) }

    fun clearResult() {
        error = null
        computed = null
    }

    fun parseOrNull(varKey: UnitToolCatalog.Var): Double? {
        val raw = textByVar[varKey]?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return raw.toDoubleOrNull()
    }

    val pickedFormula = remember(goal, known, allFormulas) {
        UnitToolCatalog.pickFormula(goal, known, allFormulas)
    }

    val computeEnabled = remember(goal, pickedFormula, known, textByVar.toMap(), distUnit, timeUnit, signMode) {
        val f = pickedFormula ?: return@remember false
        val ctx = UnitToolCatalog.Context(distUnit, timeUnit, signMode)

        val enteredKeys = textByVar.keys.filter { parseOrNull(it) != null }.toSet()
        if (!UnitToolCatalog.canSatisfyWithDerivations(f, ctx, enteredKeys + known)) return@remember false

        val enteredValues = mutableMapOf<UnitToolCatalog.Var, Double>()
        textByVar.keys.forEach { v -> parseOrNull(v)?.let { enteredValues[v] = it } }
        val resolved = UnitToolCatalog.resolveDerivedInputs(ctx, enteredValues)

        f.requires.all { resolved.containsKey(it) }
    }

    fun computeNow() {
        error = null
        computed = null
        try {
            val formula = pickedFormula ?: throw IllegalArgumentException(
                "Select the required values to unlock a valid method."
            )
            val ctx = UnitToolCatalog.Context(distUnit, timeUnit, signMode)

            val entered = mutableMapOf<UnitToolCatalog.Var, Double>()
            textByVar.keys.forEach { v -> parseOrNull(v)?.let { entered[v] = it } }

            val resolved = UnitToolCatalog.resolveDerivedInputs(ctx, entered)
            computed = formula.compute(ctx, resolved)
        } catch (e: IllegalArgumentException) {
            error = e.message ?: "Invalid input."
        } catch (_: Exception) {
            error = "Could not compute. Check inputs."
        }
    }

    LaunchedEffect(category) {
        val list = goalsForCategory(category)
        if (list.isNotEmpty() && !list.contains(goal)) {
            goal = list.first()
            known = emptySet()
            textByVar.clear()
            clearResult()
        }
    }

    LaunchedEffect(goal) {
        clearResult()
        val allowed = UnitToolCatalog.varsForGoal(goal).toSet()

        known = known.intersect(allowed)
        textByVar.keys.toList().forEach { v ->
            if (!allowed.contains(v)) textByVar.remove(v)
        }

        val groups = mutexGroupsForGoal(goal)
        groups.forEach { group ->
            val chosen = known.intersect(group)
            if (chosen.size > 1) {
                val keep = chosen.sortedBy { it.label }.first()
                val remove = chosen - keep
                remove.forEach { textByVar.remove(it) }
                known = (known - remove)
            }
        }
    }

    infoGoal?.let { g ->
        AlertDialog(
            onDismissRequest = { infoGoal = null },
            title = { Text(g.title) },
            text = { Text(g.subtitle) },
            confirmButton = { TextButton(onClick = { infoGoal = null }) { Text("OK") } }
        )
    }

    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unit Tool") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ✅ Real attribution UI (Units + Vehicles, multi-select)
            AttributionPicker(
                caseFile = caseFile,
                selection = attribution,
                onSelectionChange = { attribution = it }
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1) What are you trying to find?", style = MaterialTheme.typography.titleSmall)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Category.entries.forEach { cat ->
                            FilterChip(
                                selected = cat == category,
                                onClick = { category = cat; clearResult() },
                                label = { Text(cat.title) }
                            )
                        }
                    }

                    Text("Speed = mph • Velocity = ft/s", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pick the method", style = MaterialTheme.typography.titleSmall)

                    val methods = categoryGoals
                    if (methods.isEmpty()) {
                        Text("No formulas are available for this category yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        methods.forEach { g ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(selected = g == goal, onClick = { goal = g; clearResult() })
                                Text(g.title, modifier = Modifier.weight(1f))
                                IconButton(onClick = { infoGoal = g }) {
                                    Icon(Icons.Filled.Info, contentDescription = "Info")
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        DropDownField(
                            label = "Distance unit",
                            value = distUnit.label,
                            options = UnitToolCatalog.DistanceUnit.entries.map { it.label },
                            onSelect = { sel ->
                                distUnit = UnitToolCatalog.DistanceUnit.entries.first { it.label == sel }
                                clearResult()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DropDownField(
                            label = "Time unit",
                            value = timeUnit.label,
                            options = UnitToolCatalog.TimeUnit.entries.map { it.label },
                            onSelect = { sel ->
                                timeUnit = UnitToolCatalog.TimeUnit.entries.first { it.label == sel }
                                clearResult()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (needsSign(goal)) {
                        DropDownField(
                            label = "± choice",
                            value = signMode.label,
                            options = UnitToolCatalog.SignMode.entries.map { it.label },
                            onSelect = { sel ->
                                signMode = UnitToolCatalog.SignMode.entries.first { it.label == sel }
                                clearResult()
                            }
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2) What do you know?", style = MaterialTheme.typography.titleSmall)
                    Text("Select what you have. The tool will auto-derive what it can.", style = MaterialTheme.typography.bodySmall)

                    val options = UnitToolCatalog.varsForGoal(goal)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { v ->
                            FilterChip(
                                selected = known.contains(v),
                                onClick = {
                                    known = known.mutexApplyToggle(v, goal, textByVar)
                                    clearResult()
                                },
                                label = { Text(v.label) }
                            )
                        }
                    }

                    if (known.isNotEmpty() && pickedFormula == null) {
                        Text(
                            "Those selections don’t form a valid method (even with derivations).",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3) Enter values", style = MaterialTheme.typography.titleSmall)

                    val varsToShow = known.toList().sortedBy { it.label }
                    if (varsToShow.isEmpty()) {
                        Text("Select what you know above.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        varsToShow.forEach { v ->
                            NumericInput(
                                label = v.label,
                                value = textByVar[v].orEmpty(),
                                onValueChange = { textByVar[v] = it; error = null; computed = null },
                                trailing = v.unitHint
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { computeNow() },
                enabled = computeEnabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Compute") }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            computed?.let { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Result", style = MaterialTheme.typography.titleSmall)

                        c.outputs.forEach { out ->
                            Text(
                                "${out.name}: ${format3(out.value)} ${out.unit}".trim(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        var showWork by remember { mutableStateOf(false) }
                        TextButton(onClick = { showWork = !showWork }) {
                            Text(if (showWork) "Hide Work" else "Show Work")
                        }
                        if (showWork) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                c.steps.forEach { line ->
                                    Text(line, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val calc = SavedCalculation(
                            type = CalcType.UNIT_TOOL,
                            title = c.title,
                            inputs = c.inputs,
                            outputs = c.outputs,
                            equationText = c.equationText,
                            steps = c.steps,
                            attributedVehicleIds = attribution.vehicleIds,
                            attributedUnitIds = attribution.unitIds
                        )
                        onSaveCalculation(calc)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save") }
            }

            OutlinedButton(
                onClick = {
                    known = emptySet()
                    textByVar.clear()
                    clearResult()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------------- mutex helpers ---------------- */

private fun mutexGroupsForGoal(goal: UnitToolCatalog.Goal): List<Set<UnitToolCatalog.Var>> {
    val needsInitialSourceMutex = goal in setOf(
        UnitToolCatalog.Goal.VF_FROM_V0_A_T,
        UnitToolCatalog.Goal.VF_FROM_V0_A_D,
        UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_A_T,
        UnitToolCatalog.Goal.FINAL_SPEED_FROM_V0_MU_T,
        UnitToolCatalog.Goal.DIST_FROM_V0_T_A,
        UnitToolCatalog.Goal.MU_FROM_D_T_V0
    )

    return buildList {
        add(setOf(UnitToolCatalog.Var.SPEED_MPH, UnitToolCatalog.Var.VELOCITY_FPS))
        add(setOf(UnitToolCatalog.Var.MU, UnitToolCatalog.Var.A_FTPS2))

        if (needsInitialSourceMutex) {
            add(setOf(UnitToolCatalog.Var.V0_FPS, UnitToolCatalog.Var.V0_SPEED_MPH, UnitToolCatalog.Var.SPEED_MPH))
        }
    }
}

private fun Set<UnitToolCatalog.Var>.mutexApplyToggle(
    toggled: UnitToolCatalog.Var,
    goal: UnitToolCatalog.Goal,
    textByVar: MutableMap<UnitToolCatalog.Var, String>
): Set<UnitToolCatalog.Var> {
    if (contains(toggled)) {
        textByVar.remove(toggled)
        return this - toggled
    }

    val group = mutexGroupsForGoal(goal).firstOrNull { it.contains(toggled) }
    val conflicts = group?.minus(toggled).orEmpty()

    conflicts.forEach { textByVar.remove(it) }
    return (this - conflicts) + toggled
}

/* ---------------- UI helpers ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropDownField(
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

@Composable
private fun NumericInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    trailing: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = { Text(trailing) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun format3(x: Double): String {
    if (!x.isFinite()) return "—"
    val s = "%.3f".format(abs(x))
    return s.trimEnd('0').trimEnd('.')
}
