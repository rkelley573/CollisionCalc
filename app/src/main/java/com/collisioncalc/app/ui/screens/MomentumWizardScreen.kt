package com.collisioncalc.app.ui.screens

import com.collisioncalc.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcType
import com.collisioncalc.app.data.CalcValue
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.ui.components.AttributionPicker
import com.collisioncalc.app.ui.components.AttributionSelection
import com.collisioncalc.app.data.*
import kotlin.math.*
import com.collisioncalc.app.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentumWizardScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    val v1 = caseFile.vehicles.getOrNull(0)
    val v2 = caseFile.vehicles.getOrNull(1)

    // Attribution for this calc
    var attribution by remember(caseFile.caseId) { mutableStateOf(AttributionSelection()) }

    // Defaults from vehicle weights if present
    var w1Text by remember { mutableStateOf(v1?.weightLb?.toString() ?: "") }
    var w2Text by remember { mutableStateOf(v2?.weightLb?.toString() ?: "") }

    // Pre speeds (leave blank = unknown)
    var s1PreText by remember { mutableStateOf("") }
    var s2PreText by remember { mutableStateOf("") }

    // Post speeds (required)
    var s1PostText by remember { mutableStateOf("") } // S1′
    var s2PostText by remember { mutableStateOf("") } // S2′

    // Angles (degrees, 0° right, CCW positive)
    var t1PreText by remember { mutableStateOf("0") }
    var t2PreText by remember { mutableStateOf("90") }
    var t1PostText by remember { mutableStateOf("0") }
    var t2PostText by remember { mutableStateOf("90") }

    fun d(text: String): Double? = text.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    val w1 = d(w1Text)
    val w2 = d(w2Text)

    val s1Pre = d(s1PreText)   // nullable => unknown
    val s2Pre = d(s2PreText)   // nullable => unknown
    val s1Post = d(s1PostText)
    val s2Post = d(s2PostText)

    val t1Pre = d(t1PreText)
    val t2Pre = d(t2PreText)
    val t1Post = d(t1PostText)
    val t2Post = d(t2PostText)

    // Validation
    val w1Ok = (w1 != null && w1 > 0)
    val w2Ok = (w2 != null && w2 > 0)

    val s1PostOk = (s1Post != null && s1Post >= 0)
    val s2PostOk = (s2Post != null && s2Post >= 0)

    val s1PreOk = (s1Pre == null || s1Pre >= 0)
    val s2PreOk = (s2Pre == null || s2Pre >= 0)

    val anglesOk = (t1Pre != null && t2Pre != null && t1Post != null && t2Post != null)

    // Must have at least one unknown to "solve"
    val unknownCount = listOf(s1Pre, s2Pre).count { it == null }
    val hasUnknown = unknownCount >= 1

    val canSolve =
        w1Ok && w2Ok &&
                s1PostOk && s2PostOk &&
                s1PreOk && s2PreOk &&
                anglesOk &&
                hasUnknown

    val outcome: SolveOutcome? =
        if (!canSolve) null
        else solveMomentum360Outcome(
            w1 = w1!!,
            w2 = w2!!,
            s1Pre = s1Pre,
            s2Pre = s2Pre,
            s1Post = s1Post!!,
            s2Post = s2Post!!,
            t1Pre = t1Pre!!,
            t2Pre = t2Pre!!,
            t1Post = t1Post!!,
            t2Post = t2Post!!
        )

    val solvedS1 = outcome?.solvedS1?.takeIf { it.isFinite() }
    val solvedS2 = outcome?.solvedS2?.takeIf { it.isFinite() }

    val missing = buildList {
        if (!w1Ok) add("W1")
        if (!w2Ok) add("W2")
        if (!s1PostOk) add("S1′")
        if (!s2PostOk) add("S2′")
        if (!anglesOk) add("Angles")
        if (!hasUnknown) add("At least one unknown (leave S1 and/or S2 blank)")
        if (!s1PreOk) add("S1 (pre) must be ≥ 0")
        if (!s2PreOk) add("S2 (pre) must be ≥ 0")
    }

    val steps = outcome?.steps ?: buildList {
        add("360° Momentum Method")
        add("Fill weights, post speeds, and all angles.")
        add("Leave S1 and/or S2 blank to solve for unknowns.")
        if (!canSolve) add("Missing/invalid: ${missing.joinToString(", ")}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Momentum (360° Method)") },
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

            AttributionPicker(
                caseFile = caseFile,
                selection = attribution,
                onSelectionChange = { attribution = it },
                title = "Attribution"
            )
            Text(
                "If this calc belongs to specific Units/Vehicles, select them before saving.",
                style = MaterialTheme.typography.bodySmall
            )

            MomentumAngleReferenceCard()

            // Weights
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Weights", style = MaterialTheme.typography.titleSmall)

                    NumericField(
                        label = "W1 (Vehicle 1)",
                        value = w1Text,
                        onValueChange = { w1Text = it },
                        unit = "lb",
                        isError = w1Text.isNotBlank() && !w1Ok,
                        supportingText = if (w1Text.isNotBlank() && !w1Ok) "Enter a positive number." else null
                    )
                    NumericField(
                        label = "W2 (Vehicle 2)",
                        value = w2Text,
                        onValueChange = { w2Text = it },
                        unit = "lb",
                        isError = w2Text.isNotBlank() && !w2Ok,
                        supportingText = if (w2Text.isNotBlank() && !w2Ok) "Enter a positive number." else null
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { w1Text = v1?.weightLb?.toString().orEmpty() },
                            enabled = v1?.weightLb != null
                        ) { Text("Use V1") }

                        OutlinedButton(
                            onClick = { w2Text = v2?.weightLb?.toString().orEmpty() },
                            enabled = v2?.weightLb != null
                        ) { Text("Use V2") }
                    }
                }
            }

            // Speeds
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Speeds (mph)", style = MaterialTheme.typography.titleSmall)
                    Text("Leave S1 and/or S2 blank if unknown.", style = MaterialTheme.typography.bodySmall)

                    NumericField(
                        label = "S1 (pre) — blank = unknown",
                        value = s1PreText,
                        onValueChange = { s1PreText = it },
                        unit = "mph",
                        isError = s1PreText.isNotBlank() && !s1PreOk,
                        supportingText = if (s1PreText.isNotBlank() && !s1PreOk) "Enter 0 or greater." else null
                    )

                    NumericField(
                        label = "S2 (pre) — blank = unknown",
                        value = s2PreText,
                        onValueChange = { s2PreText = it },
                        unit = "mph",
                        isError = s2PreText.isNotBlank() && !s2PreOk,
                        supportingText = if (s2PreText.isNotBlank() && !s2PreOk) "Enter 0 or greater." else null
                    )

                    NumericField(
                        label = "S1′ (post) — required",
                        value = s1PostText,
                        onValueChange = { s1PostText = it },
                        unit = "mph",
                        isError = s1PostText.isNotBlank() && !s1PostOk,
                        supportingText = if (s1PostText.isNotBlank() && !s1PostOk) "Enter 0 or greater." else null
                    )

                    NumericField(
                        label = "S2′ (post) — required",
                        value = s2PostText,
                        onValueChange = { s2PostText = it },
                        unit = "mph",
                        isError = s2PostText.isNotBlank() && !s2PostOk,
                        supportingText = if (s2PostText.isNotBlank() && !s2PostOk) "Enter 0 or greater." else null
                    )
                }
            }

            // Angles
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Angles (degrees)", style = MaterialTheme.typography.titleSmall)
                    Text("0° = right (+X), 90° = up (+Y), increasing CCW.", style = MaterialTheme.typography.bodySmall)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumericField(
                            label = "θ1 (pre)",
                            value = t1PreText,
                            onValueChange = { t1PreText = it },
                            unit = "°",
                            modifier = Modifier.weight(1f)
                        )
                        NumericField(
                            label = "θ2 (pre)",
                            value = t2PreText,
                            onValueChange = { t2PreText = it },
                            unit = "°",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumericField(
                            label = "θ1′ (post)",
                            value = t1PostText,
                            onValueChange = { t1PostText = it },
                            unit = "°",
                            modifier = Modifier.weight(1f)
                        )
                        NumericField(
                            label = "θ2′ (post)",
                            value = t2PostText,
                            onValueChange = { t2PostText = it },
                            unit = "°",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!anglesOk) {
                        Text(
                            "All angles must be numeric.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Result + Work
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Result", style = MaterialTheme.typography.titleSmall)

                    val line1 = when {
                        solvedS1 != null && solvedS2 != null -> "Solved: S1=${solvedS1.abs2()} mph, S2=${solvedS2.abs2()} mph"
                        solvedS1 != null -> "Solved: S1=${solvedS1.abs2()} mph"
                        solvedS2 != null -> "Solved: S2=${solvedS2.abs2()} mph"
                        else -> "—"
                    }

                    Text(line1, style = MaterialTheme.typography.titleMedium)

                    if (!canSolve && missing.isNotEmpty()) {
                        Text(
                            "To solve, enter: ${missing.joinToString(", ")}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

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

            // Save
            Button(
                onClick = {
                    val outS1 = solvedS1
                    val outS2 = solvedS2
                    if (outS1 == null && outS2 == null) return@Button

                    val inputs = buildList {
                        add(CalcValue("W1", w1 ?: return@Button, "lb"))
                        add(CalcValue("W2", w2 ?: return@Button, "lb"))

                        s1Pre?.let { add(CalcValue("S1 (pre)", it, "mph")) }
                        s2Pre?.let { add(CalcValue("S2 (pre)", it, "mph")) }

                        add(CalcValue("S1′ (post)", s1Post ?: return@Button, "mph"))
                        add(CalcValue("S2′ (post)", s2Post ?: return@Button, "mph"))

                        add(CalcValue("θ1 (pre)", t1Pre ?: return@Button, "deg"))
                        add(CalcValue("θ2 (pre)", t2Pre ?: return@Button, "deg"))
                        add(CalcValue("θ1′ (post)", t1Post ?: return@Button, "deg"))
                        add(CalcValue("θ2′ (post)", t2Post ?: return@Button, "deg"))
                    }

                    val outputs = buildList {
                        outS1?.let { add(CalcValue("S1", abs(it), "mph")) }
                        outS2?.let { add(CalcValue("S2", abs(it), "mph")) }
                    }

                    val calc = SavedCalculation(
                        type = CalcType.MOMENTUM_360,
                        title = "Momentum (360° Method)",
                        inputs = inputs,
                        outputs = outputs,
                        equationText = "Vector momentum in X/Y with 360° headings",
                        steps = steps,
                        attributedUnitIds = attribution.unitIds,
                        attributedVehicleIds = attribution.vehicleIds
                    )

                    onSaveCalculation(calc)
                    onBack()
                },
                enabled = (outcome != null && (solvedS1 != null || solvedS2 != null)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}

/* ---------------------------
   Angle reference UI
---------------------------- */

@Composable
private fun MomentumAngleReferenceCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Angle Reference", style = MaterialTheme.typography.titleSmall)

            Image(
                painter = painterResource(id = R.drawable.momentum_360_reference),
                contentDescription = "360 method angle reference",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                contentScale = ContentScale.Fit
            )

            Text("How to measure angles:", style = MaterialTheme.typography.bodySmall)
            Text(
                "• 0° points RIGHT (+X)\n" +
                        "• Angles increase COUNTER-CLOCKWISE\n" +
                        "• 90° is UP (+Y), 180° LEFT, 270° DOWN\n" +
                        "• Enter angles 0–360 exactly like the diagram\n\n" +
                        "If you measured clockwise: θCCW = (360 − θCW) mod 360",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/* ---------------------------
   UI helper
---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumericField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = { Text(unit) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } }
    )
}

/* ---------------------------
   Structures + math
---------------------------- */

private data class SolveOutcome(
    val solvedS1: Double?,
    val solvedS2: Double?,
    val steps: List<String>
)

private fun Double.clean6(): String = "%.6f".format(this).trimEnd('0').trimEnd('.')
private fun Double.abs2(): String = "%.2f".format(abs(this))

private fun degToRad(deg: Double) = deg * Math.PI / 180.0
private fun cosD(deg: Double) = cos(degToRad(deg))
private fun sinD(deg: Double) = sin(degToRad(deg))

private data class Solve2(val s1: Double, val s2: Double)

private fun solve2x2(
    a11: Double, a12: Double,
    a21: Double, a22: Double,
    b1: Double, b2: Double
): Solve2? {
    val det = a11 * a22 - a12 * a21
    if (abs(det) < 1e-9) return null
    val s1 = (b1 * a22 - a12 * b2) / det
    val s2 = (a11 * b2 - b1 * a21) / det
    return Solve2(s1, s2)
}

private fun solveMomentum360Outcome(
    w1: Double,
    w2: Double,
    s1Pre: Double?,
    s2Pre: Double?,
    s1Post: Double,
    s2Post: Double,
    t1Pre: Double,
    t2Pre: Double,
    t1Post: Double,
    t2Post: Double
): SolveOutcome {

    val bX = (w1 * s1Post * cosD(t1Post)) + (w2 * s2Post * cosD(t2Post))
    val bY = (w1 * s1Post * sinD(t1Post)) + (w2 * s2Post * sinD(t2Post))

    val a11 = w1 * cosD(t1Pre)
    val a21 = w1 * sinD(t1Pre)

    val a12 = w2 * cosD(t2Pre)
    val a22 = w2 * sinD(t2Pre)

    val s1Unknown = (s1Pre == null)
    val s2Unknown = (s2Pre == null)

    val steps = mutableListOf<String>()
    steps += "360° Momentum Method"
    steps += "X: W1·S1·cosθ1 + W2·S2·cosθ2 = W1·S1′·cosθ1′ + W2·S2′·cosθ2′"
    steps += "Y: W1·S1·sinθ1 + W2·S2·sinθ2 = W1·S1′·sinθ1′ + W2·S2′·sinθ2′"
    steps += ""
    steps += "Inputs:"
    steps += "W1=${w1.clean6()}, W2=${w2.clean6()} (lb)"
    steps += "S1=${s1Pre?.clean6() ?: "?"}, S2=${s2Pre?.clean6() ?: "?"}, S1′=${s1Post.clean6()}, S2′=${s2Post.clean6()} (mph)"
    steps += "θ1=${t1Pre.clean6()}, θ2=${t2Pre.clean6()}, θ1′=${t1Post.clean6()}, θ2′=${t2Post.clean6()} (deg)"
    steps += ""
    steps += "Compute RHS (post momentum):"
    steps += "bX = ${bX.clean6()}"
    steps += "bY = ${bY.clean6()}"
    steps += ""
    steps += "Coefficient matrix:"
    steps += "a11=W1·cosθ1=${a11.clean6()}   a12=W2·cosθ2=${a12.clean6()}"
    steps += "a21=W1·sinθ1=${a21.clean6()}   a22=W2·sinθ2=${a22.clean6()}"
    steps += ""

    if (s1Unknown && s2Unknown) {
        val sol = solve2x2(a11, a12, a21, a22, bX, bY)
        if (sol == null) {
            steps += "System is singular/unstable (det≈0). Check angles."
            return SolveOutcome(null, null, steps)
        }
        steps += "Solve 2×2 for S1 and S2:"
        val det = (a11 * a22 - a12 * a21)
        steps += "det = a11·a22 − a12·a21 = ${det.clean6()}"
        steps += "S1 = ${sol.s1.clean6()} mph"
        steps += "S2 = ${sol.s2.clean6()} mph"
        return SolveOutcome(sol.s1, sol.s2, steps)
    }

    fun chooseAxis(denX: Double, denY: Double): Boolean = abs(denX) >= abs(denY)

    if (s1Unknown && !s2Unknown) {
        val knownS2 = s2Pre!!

        val numX = bX - (w2 * knownS2 * cosD(t2Pre))
        val numY = bY - (w2 * knownS2 * sinD(t2Pre))

        val useX = chooseAxis(a11, a21)
        val denom = if (useX) a11 else a21
        val num = if (useX) numX else numY

        steps += "Solve S1 with S2 known:"
        steps += "numX = bX − W2·S2·cosθ2 = ${numX.clean6()}"
        steps += "numY = bY − W2·S2·sinθ2 = ${numY.clean6()}"
        steps += "Using ${if (useX) "X(cos)" else "Y(sin)"} axis"
        if (abs(denom) < 1e-9) {
            steps += "Denominator too small; check angles."
            return SolveOutcome(null, knownS2, steps)
        }
        val s1 = num / denom
        steps += "S1 = ${num.clean6()} / ${denom.clean6()} = ${s1.clean6()} mph"
        return SolveOutcome(s1, knownS2, steps)
    }

    if (!s1Unknown && s2Unknown) {
        val knownS1 = s1Pre!!

        val numX = bX - (w1 * knownS1 * cosD(t1Pre))
        val numY = bY - (w1 * knownS1 * sinD(t1Pre))

        val useX = chooseAxis(a12, a22)
        val denom = if (useX) a12 else a22
        val num = if (useX) numX else numY

        steps += "Solve S2 with S1 known:"
        steps += "numX = bX − W1·S1·cosθ1 = ${numX.clean6()}"
        steps += "numY = bY − W1·S1·sinθ1 = ${numY.clean6()}"
        steps += "Using ${if (useX) "X(cos)" else "Y(sin)"} axis"
        if (abs(denom) < 1e-9) {
            steps += "Denominator too small; check angles."
            return SolveOutcome(knownS1, null, steps)
        }
        val s2 = num / denom
        steps += "S2 = ${num.clean6()} / ${denom.clean6()} = ${s2.clean6()} mph"
        return SolveOutcome(knownS1, s2, steps)
    }

    steps += "Nothing to solve (both S1 and S2 provided)."
    return SolveOutcome(s1Pre, s2Pre, steps)
}
