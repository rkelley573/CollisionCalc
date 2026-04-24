package com.collisioncalc.app.ui.screens

import com.collisioncalc.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcType
import com.collisioncalc.app.data.CalcValue
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.ui.components.AttributionPicker
import com.collisioncalc.app.ui.components.AttributionSelection
import com.collisioncalc.app.ui.components.SpeedInputState
import com.collisioncalc.app.ui.components.SpeedInputWithFormulaPicker
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentumWizardScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,
    onSaveCalculation: (SavedCalculation) -> Unit
) {
    val v1 = caseFile.vehicles.getOrNull(0)
    val v2 = caseFile.vehicles.getOrNull(1)

    var attribution by remember(caseFile.caseId) { mutableStateOf(AttributionSelection()) }

    var w1Text by remember { mutableStateOf(v1?.weightLb?.toString() ?: "") }
    var w2Text by remember { mutableStateOf(v2?.weightLb?.toString() ?: "") }

    var s1PreText by remember { mutableStateOf("") }
    var s2PreText by remember { mutableStateOf("") }

    var s1PostState by remember { mutableStateOf(SpeedInputState()) }
    var s2PostState by remember { mutableStateOf(SpeedInputState()) }

    var t1PreText by remember { mutableStateOf("0") }
    var t2PreText by remember { mutableStateOf("90") }
    var t1PostText by remember { mutableStateOf("0") }
    var t2PostText by remember { mutableStateOf("90") }

    fun d(text: String): Double? = text.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    val w1 = d(w1Text)
    val w2 = d(w2Text)

    val s1Pre = d(s1PreText)
    val s2Pre = d(s2PreText)
    val s1Post = s1PostState.effectiveValue
    val s2Post = s2PostState.effectiveValue

    val t1Pre = d(t1PreText)
    val t2Pre = d(t2PreText)
    val t1Post = d(t1PostText)
    val t2Post = d(t2PostText)

    val w1Ok = (w1 != null && w1 > 0)
    val w2Ok = (w2 != null && w2 > 0)
    val s1PostOk = (s1Post != null && s1Post >= 0)
    val s2PostOk = (s2Post != null && s2Post >= 0)
    val s1PreOk = (s1Pre == null || s1Pre >= 0)
    val s2PreOk = (s2Pre == null || s2Pre >= 0)
    val anglesOk = (t1Pre != null && t2Pre != null && t1Post != null && t2Post != null)
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

    val postCrashDerivationSteps: List<String> = buildList {
        val b1 = s1PostState.derivationBlock("S1′ (post-crash speed)")
        val b2 = s2PostState.derivationBlock("S2′ (post-crash speed)")
        if (b1.isNotEmpty() || b2.isNotEmpty()) {
            add("=== Post-crash speed derivations ===")
            if (b1.isNotEmpty()) {
                addAll(b1)
                add("")
            }
            if (b2.isNotEmpty()) {
                addAll(b2)
                add("")
            }
        }
    }

    val steps = buildList {
        addAll(postCrashDerivationSteps)
        val base = outcome?.steps ?: buildList {
            add("360° Momentum Method")
            add("Fill weights, post speeds, and all angles.")
            add("Leave S1 and/or S2 blank to solve for unknowns.")
            if (!canSolve) add("Missing/invalid: ${missing.joinToString(", ")}")
        }
        addAll(base)
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
                        label = "W1 (Vehicle 1)", value = w1Text, onValueChange = { w1Text = it },
                        unit = "lb", isError = w1Text.isNotBlank() && !w1Ok,
                        supportingText = if (w1Text.isNotBlank() && !w1Ok) "Enter a positive number." else null
                    )
                    NumericField(
                        label = "W2 (Vehicle 2)", value = w2Text, onValueChange = { w2Text = it },
                        unit = "lb", isError = w2Text.isNotBlank() && !w2Ok,
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
                        label = "S1 (pre) — blank = unknown", value = s1PreText,
                        onValueChange = { s1PreText = it }, unit = "mph",
                        isError = s1PreText.isNotBlank() && !s1PreOk,
                        supportingText = if (s1PreText.isNotBlank() && !s1PreOk) "Enter 0 or greater." else null
                    )
                    NumericField(
                        label = "S2 (pre) — blank = unknown", value = s2PreText,
                        onValueChange = { s2PreText = it }, unit = "mph",
                        isError = s2PreText.isNotBlank() && !s2PreOk,
                        supportingText = if (s2PreText.isNotBlank() && !s2PreOk) "Enter 0 or greater." else null
                    )
                    Text(
                        "Post-crash speeds are the speeds of each vehicle after the collision (speed loss). " +
                                "Enter them directly if known, or calculate from skid, yaw, vault, etc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SpeedInputWithFormulaPicker(
                        label = "S1′ (post) — required",
                        state = s1PostState,
                        onStateChange = { s1PostState = it },
                        isError = s1PostState.mode == SpeedInputState.Mode.Manual &&
                                s1PostState.manualText.isNotBlank() && !s1PostOk,
                        supportingText = if (s1PostState.mode == SpeedInputState.Mode.Manual &&
                            s1PostState.manualText.isNotBlank() && !s1PostOk
                        ) "Enter 0 or greater." else null
                    )
                    SpeedInputWithFormulaPicker(
                        label = "S2′ (post) — required",
                        state = s2PostState,
                        onStateChange = { s2PostState = it },
                        isError = s2PostState.mode == SpeedInputState.Mode.Manual &&
                                s2PostState.manualText.isNotBlank() && !s2PostOk,
                        supportingText = if (s2PostState.mode == SpeedInputState.Mode.Manual &&
                            s2PostState.manualText.isNotBlank() && !s2PostOk
                        ) "Enter 0 or greater." else null
                    )
                }
            }

            // Angles
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Angles (degrees)", style = MaterialTheme.typography.titleSmall)
                    Text("0° = right (+X), 90° = down (-Y), 180° = left (-X), 270° = up (+Y).", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumericField(label = "θ1 (pre)", value = t1PreText, onValueChange = { t1PreText = it }, unit = "°", modifier = Modifier.weight(1f))
                        NumericField(label = "θ2 (pre)", value = t2PreText, onValueChange = { t2PreText = it }, unit = "°", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumericField(label = "θ1′ (post)", value = t1PostText, onValueChange = { t1PostText = it }, unit = "°", modifier = Modifier.weight(1f))
                        NumericField(label = "θ2′ (post)", value = t2PostText, onValueChange = { t2PostText = it }, unit = "°", modifier = Modifier.weight(1f))
                    }
                    if (!anglesOk) {
                        Text("All angles must be numeric.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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

                    // Delta-V and PDOF
                    if (outcome != null) {
                        outcome.dv1?.let { Text("ΔV1 = ${"%.2f".format(it)} mph", style = MaterialTheme.typography.bodyMedium) }
                        outcome.dv2?.let { Text("ΔV2 = ${"%.2f".format(it)} mph", style = MaterialTheme.typography.bodyMedium) }
                        outcome.pdof1?.let { Text("PDOF1 = ${"%.2f".format(it)}°", style = MaterialTheme.typography.bodyMedium) }
                        outcome.pdof2?.let { Text("PDOF2 = ${"%.2f".format(it)}°", style = MaterialTheme.typography.bodyMedium) }
                    }

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
                        outcome?.dv1?.let { add(CalcValue("ΔV1", it, "mph")) }
                        outcome?.dv2?.let { add(CalcValue("ΔV2", it, "mph")) }
                        outcome?.pdof1?.let { add(CalcValue("PDOF1", it, "deg")) }
                        outcome?.pdof2?.let { add(CalcValue("PDOF2", it, "deg")) }
                    }

                    val calc = SavedCalculation(
                        type = CalcType.MOMENTUM_360,
                        title = "Momentum (360° Method)",
                        inputs = inputs,
                        outputs = outputs,
                        equationText = "360° Method — S1 from cosine, S2 from sine, with ΔV and PDOF",
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
                        "• 90° is DOWN (-Y), 180° LEFT, 270° UP\n" +
                        "• Angles increase CLOCKWISE\n" +
                        "• Enter angles 0–360 exactly like the diagram",
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
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = { Text(unit) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() }
        ),
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
    val steps: List<String>,
    val dv1: Double? = null,
    val dv2: Double? = null,
    val pdof1: Double? = null,
    val pdof2: Double? = null
)

private data class DvPdof(
    val dv1: Double?,
    val dv2: Double?,
    val pdof1: Double?,
    val pdof2: Double?
)

private fun Double.fmt2(): String = "%.2f".format(this)
private fun Double.fmt4(): String = "%.4f".format(this)
private fun Double.abs2(): String = "%.2f".format(Math.abs(this))

private fun degToRad(deg: Double) = deg * Math.PI / 180.0
private fun cosD(deg: Double) = Math.cos(degToRad(deg))

// Clockwise convention for momentum equations
private fun sinD(deg: Double) = -Math.sin(degToRad(deg))

// Standard CCW sin for ΔV/PDOF (relative angles)
private fun sinStd(deg: Double) = Math.sin(degToRad(deg))
private fun cosStd(deg: Double) = Math.cos(degToRad(deg))

private fun calcDvPdof(
    s1: Double, s2: Double,
    s1Post: Double, s2Post: Double,
    t1Pre: Double, t2Pre: Double,
    t1Post: Double, t2Post: Double,
    steps: MutableList<String>
): DvPdof {
    // Included angle = minimum angular difference between pre and post headings.
    // This automatically handles absolute redirection (obtuse angle cases) without
    // special casing — the geometry falls out correctly from the 360° headings.
    fun includedAngle(tPre: Double, tPost: Double): Double {
        val diff = Math.abs(tPre - tPost) % 360.0
        return if (diff > 180.0) 360.0 - diff else diff
    }

    val a1 = includedAngle(t1Pre, t1Post)
    val a2 = includedAngle(t2Pre, t2Post)

    val dv1Sq = s1 * s1 + s1Post * s1Post - 2 * s1 * s1Post * cosStd(a1)
    val dv1 = if (dv1Sq >= 0) Math.sqrt(dv1Sq) else null

    val dv2Sq = s2 * s2 + s2Post * s2Post - 2 * s2 * s2Post * cosStd(a2)
    val dv2 = if (dv2Sq >= 0) Math.sqrt(dv2Sq) else null

    val pdof1 = if (dv1 != null && dv1 > 1e-9) {
        val ratio = (s1Post * sinStd(a1)) / dv1
        if (ratio in -1.0..1.0) Math.toDegrees(Math.asin(ratio)) else null
    } else null

    val pdof2 = if (dv2 != null && dv2 > 1e-9) {
        val ratio = (s2Post * sinStd(a2)) / dv2
        if (ratio in -1.0..1.0) Math.toDegrees(Math.asin(ratio)) else null
    } else null

    steps += ""
    steps += "--- ΔV and PDOF ---"
    steps += "(Included angle = min angular difference between pre and post headings)"
    steps += ""
    steps += "ΔV1 = √(S1² + S1'² − 2·S1·S1'·cos θ_included1)"
    steps += "  θ_included1 = min(|${t1Pre.fmt2()}° − ${t1Post.fmt2()}°|, 360° − |${t1Pre.fmt2()}° − ${t1Post.fmt2()}°|) = ${a1.fmt2()}°"
    steps += "  = √(${s1.fmt2()}² + ${s1Post.fmt2()}² − 2·${s1.fmt2()}·${s1Post.fmt2()}·cos(${a1.fmt2()}°))"
    steps += "  = √(${(s1*s1).fmt4()} + ${(s1Post*s1Post).fmt4()} − ${(2*s1*s1Post*cosStd(a1)).fmt4()})"
    steps += "  = √${dv1Sq.fmt4()}"
    if (dv1 != null) steps += "  = ${dv1.fmt2()} mph" else steps += "  = undefined"

    steps += ""
    steps += "ΔV2 = √(S2² + S2'² − 2·S2·S2'·cos θ_included2)"
    steps += "  θ_included2 = min(|${t2Pre.fmt2()}° − ${t2Post.fmt2()}°|, 360° − |${t2Pre.fmt2()}° − ${t2Post.fmt2()}°|) = ${a2.fmt2()}°"
    steps += "  = √(${s2.fmt2()}² + ${s2Post.fmt2()}² − 2·${s2.fmt2()}·${s2Post.fmt2()}·cos(${a2.fmt2()}°))"
    steps += "  = √(${(s2*s2).fmt4()} + ${(s2Post*s2Post).fmt4()} − ${(2*s2*s2Post*cosStd(a2)).fmt4()})"
    steps += "  = √${dv2Sq.fmt4()}"
    if (dv2 != null) steps += "  = ${dv2.fmt2()} mph" else steps += "  = undefined"

    steps += ""
    steps += "PDOF1 = arcsin(S1'·sin θ_included1 / ΔV1)"
    if (dv1 != null && dv1 > 1e-9) {
        steps += "      = arcsin(${s1Post.fmt2()}·sin(${a1.fmt2()}°) / ${dv1.fmt2()})"
        steps += "      = arcsin(${(s1Post * sinStd(a1)).fmt4()} / ${dv1.fmt2()})"
        if (pdof1 != null) steps += "      = ${"%.2f".format(pdof1)}°" else steps += "      = undefined"
    } else steps += "      = undefined (ΔV1 ≈ 0)"

    steps += ""
    steps += "PDOF2 = arcsin(S2'·sin θ_included2 / ΔV2)"
    if (dv2 != null && dv2 > 1e-9) {
        steps += "      = arcsin(${s2Post.fmt2()}·sin(${a2.fmt2()}°) / ${dv2.fmt2()})"
        steps += "      = arcsin(${(s2Post * sinStd(a2)).fmt4()} / ${dv2.fmt2()})"
        if (pdof2 != null) steps += "      = ${"%.2f".format(pdof2)}°" else steps += "      = undefined"
    } else steps += "      = undefined (ΔV2 ≈ 0)"

    return DvPdof(dv1, dv2, pdof1, pdof2)
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

    val steps = mutableListOf<String>()
    steps += "360° Method of Computing Momentum"
    steps += "Convention: 0°=right(+X), 90°=down(-Y), 180°=left(-X), 270°=up(+Y)"
    steps += ""
    steps += "Inputs:"
    steps += "  W1=${w1.fmt2()} lb,  W2=${w2.fmt2()} lb"
    steps += "  S1'=${s1Post.fmt2()} mph,  S2'=${s2Post.fmt2()} mph"
    steps += "  S1=${s1Pre?.fmt2() ?: "unknown"},  S2=${s2Pre?.fmt2() ?: "unknown"}"
    steps += "  θ1=${t1Pre.fmt2()}°,  θ2=${t2Pre.fmt2()}°"
    steps += "  θ1'=${t1Post.fmt2()}°,  θ2'=${t2Post.fmt2()}°"
    steps += ""

    val cosT1Post = cosD(t1Post)
    val cosT2Post = cosD(t2Post)
    val sinT1Post = sinD(t1Post)
    val sinT2Post = sinD(t2Post)
    val cosT1Pre  = cosD(t1Pre)
    val cosT2Pre  = cosD(t2Pre)
    val sinT1Pre  = sinD(t1Pre)
    val sinT2Pre  = sinD(t2Pre)

    fun solveS2FromSine(knownS1: Double): Pair<Double?, String> {
        val den = w2 * sinT2Pre
        return if (Math.abs(den) < 1e-6) {
            Pair(null, "  Sine equation denominator ≈ 0 (collinear crash) — skipping Y equation.")
        } else {
            val num = (w1 * s1Post * sinT1Post) + (w2 * s2Post * sinT2Post) - (w1 * knownS1 * sinT1Pre)
            val s2 = num / den
            val detail = buildString {
                appendLine("  Formula: S2 = (W1·S1'·sinθ1' + W2·S2'·sinθ2' - W1·S1·sinθ1) / (W2·sinθ2)")
                appendLine("  Substitute:")
                appendLine("    Numerator = (${w1.fmt2()}·${s1Post.fmt2()}·sin(${t1Post.fmt2()}°))")
                appendLine("              + (${w2.fmt2()}·${s2Post.fmt2()}·sin(${t2Post.fmt2()}°))")
                appendLine("              - (${w1.fmt2()}·${knownS1.fmt2()}·sin(${t1Pre.fmt2()}°))")
                appendLine("            = (${(w1 * s1Post * sinT1Post).fmt4()})")
                appendLine("            + (${(w2 * s2Post * sinT2Post).fmt4()})")
                appendLine("            - (${(w1 * knownS1 * sinT1Pre).fmt4()})")
                appendLine("            = ${num.fmt4()}")
                appendLine("    Denominator = ${w2.fmt2()}·sin(${t2Pre.fmt2()}°) = ${den.fmt4()}")
                append("  S2 = ${num.fmt4()} / ${den.fmt4()} = ${s2.fmt2()} mph")
            }
            Pair(s2, detail)
        }
    }

    fun solveS1FromCosine(knownS2: Double): Pair<Double, String> {
        val term1 = s1Post * cosT1Post
        val term2 = (w2 * s2Post * cosT2Post) / w1
        val term3 = (w2 * knownS2 * cosT2Pre) / w1
        val s1 = term1 + term2 - term3
        val detail = buildString {
            appendLine("  Formula: S1 = S1'·cosθ1' + (W2·S2'·cosθ2')/W1 - (W2·S2·cosθ2)/W1")
            appendLine("  Substitute:")
            appendLine("    Term 1 = ${s1Post.fmt2()}·cos(${t1Post.fmt2()}°) = ${term1.fmt4()}")
            appendLine("    Term 2 = (${w2.fmt2()}·${s2Post.fmt2()}·cos(${t2Post.fmt2()}°)) / ${w1.fmt2()} = ${term2.fmt4()}")
            appendLine("    Term 3 = (${w2.fmt2()}·${knownS2.fmt2()}·cos(${t2Pre.fmt2()}°)) / ${w1.fmt2()} = ${term3.fmt4()}")
            appendLine("  S1 = ${term1.fmt4()} + ${term2.fmt4()} - (${term3.fmt4()})")
            append("  S1 = ${s1.fmt2()} mph")
        }
        return Pair(s1, detail)
    }

    // CASE 1: Both unknown
    if (s1Pre == null && s2Pre == null) {
        steps += "STEP 1 — Solve S2 using Y (sine) equation:"
        steps += "  S2 = (W1·S1'·sinθ1' + W2·S2'·sinθ2') / (W2·sinθ2)"

        val den = w2 * sinT2Pre
        if (Math.abs(den) < 1e-6) {
            steps += "  Sine denominator ≈ 0 — cannot solve both unknowns with collinear angles."
            steps += "  Assign one speed and re-run."
            return SolveOutcome(null, null, steps)
        }

        val num = (w1 * s1Post * sinT1Post) + (w2 * s2Post * sinT2Post)
        val s2Solved = num / den

        steps += "  Substitute:"
        steps += "    Numerator = (${w1.fmt2()}·${s1Post.fmt2()}·sin(${t1Post.fmt2()}°))"
        steps += "              + (${w2.fmt2()}·${s2Post.fmt2()}·sin(${t2Post.fmt2()}°))"
        steps += "              = ${(w1 * s1Post * sinT1Post).fmt4()} + ${(w2 * s2Post * sinT2Post).fmt4()}"
        steps += "              = ${num.fmt4()}"
        steps += "    Denominator = ${w2.fmt2()}·sin(${t2Pre.fmt2()}°) = ${den.fmt4()}"
        steps += "  S2 = ${num.fmt4()} / ${den.fmt4()} = ${s2Solved.fmt2()} mph"
        steps += ""
        steps += "STEP 2 — Solve S1 using X (cosine) equation with S2=${s2Solved.fmt2()} mph:"
        val (s1Solved, s1Detail) = solveS1FromCosine(s2Solved)
        s1Detail.lines().forEach { steps += it }
        steps += ""
        steps += "RESULT: S1 = ${Math.abs(s1Solved).fmt2()} mph,  S2 = ${Math.abs(s2Solved).fmt2()} mph"

        val dp = calcDvPdof(Math.abs(s1Solved), Math.abs(s2Solved), s1Post, s2Post, t1Pre, t2Pre, t1Post, t2Post, steps)
        return SolveOutcome(s1Solved, s2Solved, steps, dp.dv1, dp.dv2, dp.pdof1, dp.pdof2)
    }

    // CASE 2: S2 known, solve S1
    if (s1Pre == null && s2Pre != null) {
        steps += "S2 is known = ${s2Pre.fmt2()} mph"
        steps += ""
        steps += "STEP 1 — Solve S1 using X (cosine) equation:"
        val (s1Solved, s1Detail) = solveS1FromCosine(s2Pre)
        s1Detail.lines().forEach { steps += it }
        steps += ""
        steps += "STEP 2 — Verify using Y (sine) equation:"
        val (s2Check, s2Detail) = solveS2FromSine(s1Solved)
        steps += s2Detail
        if (s2Check != null) {
            steps += "  Check: S2 should be ${s2Pre.fmt2()} mph, sine gives ${s2Check.fmt2()} mph"
            val diff = Math.abs(s2Check - s2Pre)
            steps += if (diff < 0.5) "  ✓ Sine equation confirms result." else "  ⚠ Difference of ${diff.fmt2()} mph — check angles."
        }
        steps += ""
        steps += "RESULT: S1 = ${Math.abs(s1Solved).fmt2()} mph"

        val dp = calcDvPdof(Math.abs(s1Solved), s2Pre, s1Post, s2Post, t1Pre, t2Pre, t1Post, t2Post, steps)
        return SolveOutcome(s1Solved, s2Pre, steps, dp.dv1, dp.dv2, dp.pdof1, dp.pdof2)
    }

    // CASE 3: S1 known, solve S2
    if (s1Pre != null && s2Pre == null) {
        steps += "S1 is known = ${s1Pre.fmt2()} mph"
        steps += ""
        steps += "STEP 1 — Solve S2 using Y (sine) equation:"
        val (s2Solved, s2Detail) = solveS2FromSine(s1Pre)
        if (s2Solved == null) {
            steps += s2Detail
            steps += "  Cannot solve S2 with these angles — try assigning S2 instead."
            return SolveOutcome(s1Pre, null, steps)
        }
        s2Detail.lines().forEach { steps += it }
        steps += ""
        steps += "STEP 2 — Verify using X (cosine) equation:"
        val (s1Check, s1Detail) = solveS1FromCosine(s2Solved)
        s1Detail.lines().forEach { steps += it }
        steps += "  Check: S1 should be ${s1Pre.fmt2()} mph, cosine gives ${s1Check.fmt2()} mph"
        val diff = Math.abs(s1Check - s1Pre)
        steps += if (diff < 0.5) "  ✓ Cosine equation confirms result." else "  ⚠ Difference of ${diff.fmt2()} mph — check angles."
        steps += ""
        steps += "RESULT: S2 = ${Math.abs(s2Solved).fmt2()} mph"

        val dp = calcDvPdof(s1Pre, Math.abs(s2Solved), s1Post, s2Post, t1Pre, t2Pre, t1Post, t2Post, steps)
        return SolveOutcome(s1Pre, s2Solved, steps, dp.dv1, dp.dv2, dp.pdof1, dp.pdof2)
    }

    // Both known
    steps += "Both S1 and S2 are provided — nothing to solve."
    return SolveOutcome(s1Pre, s2Pre, steps)
}