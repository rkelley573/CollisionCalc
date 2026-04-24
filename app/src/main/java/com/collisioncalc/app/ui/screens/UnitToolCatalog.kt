package com.collisioncalc.app.ui.screens

import com.collisioncalc.app.data.CalcValue
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Unit Tool Catalog (NON-Momentum)
 *
 * UI rules:
 * - Speed is mph
 * - Velocity is ft/s
 */
object UnitToolCatalog {

    const val G_FTPS2 = 32.2
    private const val MPH_TO_FPS = 5280.0 / 3600.0

    enum class DistanceUnit(val label: String) { FT("ft"), M("m") }
    enum class TimeUnit(val label: String) { SEC("sec"), MIN("min"), HOUR("hr") }
    enum class SignMode(val label: String) { PLUS("(+) use +"), MINUS("(−) use −") }

    enum class Goal(val title: String, val subtitle: String) {

        // Conversions
        SPEED_FROM_VELOCITY("Speed from velocity", "S(mph) = V(ft/s) / 1.466"),
        VELOCITY_FROM_SPEED("Velocity from speed", "V(ft/s) = S(mph) · 1.466"),

        // Speed / skid / curve / vault
        SPEED_FROM_D_MU_E("Minimum speed from distance + μ + efficiency", "S = √(30 · D · μ · e)"),
        SPEED_FROM_D_MU_G_E("Minimum speed with grade adjustment", "S = √(30 · D · (μ ± g) · e)"),
        COMBINED_SPEED("Combined speed", "S = √(S1² + S2² + …)"),
        YAW_SPEED("Yaw / critical curve speed", "S = √(15 · R · μ)"),

        FLIP_VAULT_SPEED("Flip vault speed (45° take-off assumed)", "S = 3.87·d / √(d ± h)"),
        FALL_SPEED("Fall speed (known distance/height/grade)", "S = 2.73·d / √(h ± d·g)"),
        LONG_VAULT_SPEED("Long vault speed (known d, h, θ)", "S = 2.73·d / √(d·cosθ·sinθ ± h·cos²θ)"),

        // Velocity / kinematics
        AVG_VELOCITY_FROM_D_T("Average velocity", "V = d / T"),
        VELOCITY_FROM_A_T("Velocity from rate + time (from/to stop)", "V = a · T"),
        VF_FROM_V0_A_T("Final velocity from V0 + a + time", "VF = V0 ± a·T"),
        VF_FROM_V0_A_D("Final velocity from V0 + a + distance", "VF = √(V0² ± 2·a·d)"),

        // Speed wrappers (derived inputs shortcut)
        FINAL_SPEED_FROM_V0_A_T("Final speed from initial + a + time", "SF = fps→mph(V0 ± a·T)"),
        FINAL_SPEED_FROM_V0_MU_T("Final speed from initial + μ + time", "SF = fps→mph(V0 ± (32.2·μ)·T)"),

        // Factor / rate
        A_FROM_MU("Rate from factor", "a = 32.2 · μ"),
        MU_FROM_A("Factor from rate", "μ = a / 32.2"),
        MU_FROM_S_D("Factor from speed + distance (from/to stop)", "μ = S² / (30·d)"),
        MU_FROM_D_T("Factor from distance + time (from/to stop)", "μ = d / (16.1·T²)"),
        MU_FROM_V_T("Factor from velocity + time (from/to stop)", "μ = V / (32.2·T)"),
        MU_ACCEL_BETWEEN_V_IN_T("μ accelerating between 2 velocities (time)", "μ = (VF − V0) / (32.2·T)"),
        MU_DECEL_BETWEEN_V_IN_T("μ decelerating between 2 velocities (time)", "μ = (V0 − VF) / (32.2·T)"),
        MU_FROM_TWO_V_AND_D("μ between 2 velocities (distance)", "μ = (VF² − V0²) / (64.4·d)"),
        MU_FROM_FORCE_WEIGHT("μ from force & weight (drag sled)", "μ = F / W"),
        MU_FROM_D_T_V0("μ from distance, time, and initial velocity", "μ = (d − V0·T) / (16.1·T²)"),

        // Time
        TIME_FROM_D_V("Time (constant velocity)", "T = d / V"),
        TIME_FROM_V_A("Time from velocity + rate (from/to stop)", "T = V / a"),
        TIME_DECEL_V0_VF_A("Time decelerating between 2 velocities", "T = (V0 − VF) / a"),
        TIME_ACCEL_V0_VF_A("Time accelerating between 2 velocities", "T = (VF − V0) / a"),
        TIME_FROM_D_MU("Time from distance + factor (from/to stop)", "T = 0.25 · √(d / μ)"),

        // Distance
        DIST_FROM_V_T("Distance (constant velocity)", "d = v·t"),
        DIST_FROM_S_MU("Distance from speed + factor (from/to stop)", "d = S² / (30·μ)"),
        DIST_FROM_MU_T("Distance from factor + time (from/to stop)", "d = 16.1·μ·t²"),
        DIST_FROM_A_T("Distance from rate + time (from/to stop)", "d = (a·t²)/2"),
        DIST_FROM_V0_T_A("Distance from V0 + time + rate", "d = V0·t ± (a·t²)/2"),
        PERCEPTION_REACTION_DISTANCE("Perception/Reaction distance", "d(p/r) = V(ft/s) · 1.5"),
        TOTAL_STOPPING_DISTANCE("Total stopping distance", "d(total) = d(p/r) + d(braking)"),

        // Trig / misc
        GRADE_FROM_RISE_RUN("Grade (slope)", "g = rise / run"),
        PYTHAG_C_FROM_A_B("Pythagorean theorem", "C = √(A² + B²)"),
        TAKEOFF_ANGLE_FROM_D_H("Take-off angle from distance & height", "θ = atan(h/d)"),
        UNKNOWN_TAKEOFF_LANDING_LOWER("Unknown take-off (landing lower)", "θ = atan(h/d) / 2"),
        UNKNOWN_TAKEOFF_LANDING_HIGHER("Unknown take-off (landing higher)", "θ = 90 − atan(h/d)/2"),
        RADIUS_FROM_CHORD_MIDORD("Radius from chord & middle-ordinate", "R = C²/(8·m) + m/2"),
        SIN_FROM_OPP_HYP("sinθ from opp/hyp", "sinθ = opp/hyp"),
        COS_FROM_ADJ_HYP("cosθ from adj/hyp", "cosθ = adj/hyp"),
        TAN_FROM_OPP_ADJ("tanθ from opp/adj", "tanθ = opp/adj"),

        // Grade / angle conversions
        GRADE_TO_ANGLE("Grade to angle", "θ = atan(g)°"),
        ANGLE_TO_GRADE("Angle to grade", "g = tan(θ)"),

        // Starting speed / velocity (solving backwards to a stop)
        V0_FROM_A_D_STOP("Starting velocity from rate + distance (to stop)", "V0 = √(2·a·d)"),
        S0_FROM_A_D_STOP("Starting speed from rate + distance (to stop)", "S0 = fps→mph(√(2·a·d))")
    }

    enum class Var(val label: String, val unitHint: String) {

        // Speed / velocity
        SPEED_MPH("Speed", "mph"),
        VELOCITY_FPS("Velocity", "ft/s"),
        V0_FPS("Original velocity (V0)", "ft/s"),
        VF_FPS("Final velocity (VF)", "ft/s"),

        // Shortcuts: initial/final speed as mph
        V0_SPEED_MPH("Original speed (S0)", "mph"),
        VF_SPEED_MPH("Final speed (SF)", "mph"),

        // Distance / time
        DISTANCE("Distance", "ft or m"),
        TIME("Time", "sec/min/hr"),

        // Rates / factors
        MU("Drag factor (μ)", "unitless"),
        A_FTPS2("Acceleration/Decel rate (a)", "ft/s²"),
        EFF("Braking efficiency (e)", "0–1"),
        GRADE("Grade (g)", "decimal (e.g., 0.03)"),

        // Combined speed components
        S1("S1", "mph"),
        S2("S2", "mph"),
        S3("S3", "mph"),
        S4("S4", "mph"),

        // Yaw / radius
        RADIUS("Radius (R)", "ft"),
        CHORD("Chord (C)", "ft"),
        MID_ORD("Middle-ordinate (m)", "ft"),

        // Vault / fall
        HEIGHT("Height (h)", "ft"),
        TAKEOFF_ANGLE_DEG("Take-off angle (θ)", "degrees"),

        // Force/weight
        FORCE("Force (F)", "lb"),
        WEIGHT("Weight (W)", "lb"),

        // Generic trig inputs
        OPP("Opposite", "units"),
        ADJ("Adjacent", "units"),
        HYP("Hypotenuse", "units")
    }

    data class Context(
        val distanceUnit: DistanceUnit,
        val timeUnit: TimeUnit,
        val signMode: SignMode
    )

    data class Computed(
        val title: String,
        val equationText: String,
        val inputs: List<CalcValue>,
        val outputs: List<CalcValue>,
        val steps: List<String>
    )

    data class SignQuestion(
        val label: String,
        val plusLabel: String,
        val minusLabel: String
    )

    data class Formula(
        val id: String,
        val goal: Goal,
        val requires: Set<Var>,
        val equationText: String,
        val signQuestion: SignQuestion? = null,
        val compute: (ctx: Context, values: Map<Var, Double>) -> Computed
    )

    val SQ_ACCEL_DECEL  = SignQuestion("Motion type",     "Accelerating",               "Decelerating")
    val SQ_GRADE_SPEED  = SignQuestion("Grade direction", "Uphill (adds to drag)",       "Downhill (reduces drag)")
    val SQ_VAULT        = SignQuestion("Landing height",  "Landing lower",               "Landing higher")
    val SQ_FALL_GRADE   = SignQuestion("Grade direction", "Downgrade (aids fall)",       "Upgrade (opposes fall)")

    /* ---------------- helpers ---------------- */

    private fun Map<Var, Double>.req(k: Var): Double =
        this[k] ?: throw IllegalArgumentException("Missing input: ${k.label}")

    private fun fmt(x: Double, dp: Int = 4): String =
        "%.${dp}f".format(x).trimEnd('0').trimEnd('.')

    private fun safeSqrt(x: Double): Double {
        require(x >= 0.0) {
            "Math domain error: value under square root is negative. Check sign choice and inputs (e.g., μ ± g must stay positive)."
        }
        return sqrt(x)
    }

    private fun degToRad(deg: Double) = deg * Math.PI / 180.0

    /* ---------------- unit normalization ---------------- */

    fun mphToFps(mph: Double) = mph * MPH_TO_FPS
    fun fpsToMph(fps: Double) = fps / MPH_TO_FPS

    fun distToFt(v: Double, unit: DistanceUnit): Double =
        when (unit) {
            DistanceUnit.FT -> v
            DistanceUnit.M -> v / 0.3048
        }

    fun timeToSec(v: Double, unit: TimeUnit): Double =
        when (unit) {
            TimeUnit.SEC -> v
            TimeUnit.MIN -> v * 60.0
            TimeUnit.HOUR -> v * 3600.0
        }

    /* ---------------- derived inputs ---------------- */

    /**
     * Expand entered values with derived values:
     * - speed ↔ velocity
     * - μ ↔ a
     * - V0 from initial speed (S0 mph) or from SPEED_MPH if user used “speed” as initial
     */
    fun resolveDerivedInputs(ctx: Context, entered: Map<Var, Double>): Map<Var, Double> {
        val out = entered.toMutableMap()

        // speed ↔ velocity
        if (!out.containsKey(Var.VELOCITY_FPS) && out.containsKey(Var.SPEED_MPH)) {
            out[Var.VELOCITY_FPS] = mphToFps(out.req(Var.SPEED_MPH))
        }
        if (!out.containsKey(Var.SPEED_MPH) && out.containsKey(Var.VELOCITY_FPS)) {
            out[Var.SPEED_MPH] = fpsToMph(out.req(Var.VELOCITY_FPS))
        }

        // μ ↔ a
        if (!out.containsKey(Var.A_FTPS2) && out.containsKey(Var.MU)) {
            out[Var.A_FTPS2] = G_FTPS2 * out.req(Var.MU)
        }
        if (!out.containsKey(Var.MU) && out.containsKey(Var.A_FTPS2)) {
            out[Var.MU] = out.req(Var.A_FTPS2) / G_FTPS2
        }

        // V0 from initial speed (S0)
        if (!out.containsKey(Var.V0_FPS) && out.containsKey(Var.V0_SPEED_MPH)) {
            out[Var.V0_FPS] = mphToFps(out.req(Var.V0_SPEED_MPH))
        }

        // VF from final speed (SF)
        if (!out.containsKey(Var.VF_FPS) && out.containsKey(Var.VF_SPEED_MPH)) {
            out[Var.VF_FPS] = mphToFps(out.req(Var.VF_SPEED_MPH))
        }

        // allow SPEED_MPH to serve as initial speed if V0 missing (your “make it easy” behavior)
        if (!out.containsKey(Var.V0_FPS) && out.containsKey(Var.SPEED_MPH)) {
            out[Var.V0_FPS] = mphToFps(out.req(Var.SPEED_MPH))
        }

        // grade ↔ angle
        if (!out.containsKey(Var.TAKEOFF_ANGLE_DEG) && out.containsKey(Var.GRADE)) {
            out[Var.TAKEOFF_ANGLE_DEG] = atan(out.req(Var.GRADE)) * 180.0 / Math.PI
        }
        if (!out.containsKey(Var.GRADE) && out.containsKey(Var.TAKEOFF_ANGLE_DEG)) {
            out[Var.GRADE] = tan(degToRad(out.req(Var.TAKEOFF_ANGLE_DEG)))
        }

        return out
    }

    /**
     * Check if a formula can be satisfied by a set of vars after considering derivations.
     */
    fun canSatisfyWithDerivations(formula: Formula, ctx: Context, available: Set<Var>): Boolean {
        val known = available.toMutableSet()

        // speed ↔ velocity
        if (known.contains(Var.SPEED_MPH)) known.add(Var.VELOCITY_FPS)
        if (known.contains(Var.VELOCITY_FPS)) known.add(Var.SPEED_MPH)

        // μ ↔ a
        if (known.contains(Var.MU)) known.add(Var.A_FTPS2)
        if (known.contains(Var.A_FTPS2)) known.add(Var.MU)

        // initial/final speed → V0/VF
        if (known.contains(Var.V0_SPEED_MPH)) known.add(Var.V0_FPS)
        if (known.contains(Var.VF_SPEED_MPH)) known.add(Var.VF_FPS)
        if (known.contains(Var.SPEED_MPH)) known.add(Var.V0_FPS)

        // grade ↔ angle
        if (known.contains(Var.GRADE)) known.add(Var.TAKEOFF_ANGLE_DEG)
        if (known.contains(Var.TAKEOFF_ANGLE_DEG)) known.add(Var.GRADE)

        return known.containsAll(formula.requires)
    }

    /* ---------------- formula list ---------------- */

    fun allFormulas(): List<Formula> {
        val list = mutableListOf<Formula>()

        // conversions
        list += Formula(
            id = "speed_from_velocity",
            goal = Goal.SPEED_FROM_VELOCITY,
            requires = setOf(Var.VELOCITY_FPS),
            equationText = "S = V / 1.466"
        ) { _, v ->
            val V = v.req(Var.VELOCITY_FPS)
            val S = fpsToMph(V)
            Computed(
                title = "Speed: ${fmt(S)} mph",
                equationText = "S = V / 1.466",
                inputs = listOf(CalcValue("V", V, "ft/s")),
                outputs = listOf(CalcValue("S", S, "mph")),
                steps = listOf("S = ${fmt(V)} / 1.466 = ${fmt(S)} mph")
            )
        }

        list += Formula(
            id = "velocity_from_speed",
            goal = Goal.VELOCITY_FROM_SPEED,
            requires = setOf(Var.SPEED_MPH),
            equationText = "V = S · 1.466"
        ) { _, v ->
            val S = v.req(Var.SPEED_MPH)
            val V = mphToFps(S)
            Computed(
                title = "Velocity: ${fmt(V)} ft/s",
                equationText = "V = S · 1.466",
                inputs = listOf(CalcValue("S", S, "mph")),
                outputs = listOf(CalcValue("V", V, "ft/s")),
                steps = listOf("V = ${fmt(S)} · 1.466 = ${fmt(V)} ft/s")
            )
        }

        // skid speed
        list += Formula(
            id = "speed_from_d_mu_e",
            goal = Goal.SPEED_FROM_D_MU_E,
            requires = setOf(Var.DISTANCE, Var.MU, Var.EFF),
            equationText = "S = √(30 · D · μ · e)"
        ) { ctx, v ->
            val D_in = v.req(Var.DISTANCE)
            val mu = v.req(Var.MU)
            val e = v.req(Var.EFF)

            require(mu > 0) { "μ must be > 0." }
            require(e > 0) { "e must be > 0." }

            val Dft = distToFt(D_in, ctx.distanceUnit)
            val inside = 30.0 * Dft * mu * e
            val S = safeSqrt(inside)

            Computed(
                title = "Speed: ${fmt(S)} mph",
                equationText = "S = √(30 · D · μ · e)",
                inputs = listOf(
                    CalcValue("D", D_in, ctx.distanceUnit.label),
                    CalcValue("μ", mu, "drag"),
                    CalcValue("e", e, "eff")
                ),
                outputs = listOf(
                    CalcValue("S", S, "mph"),
                    CalcValue("V", mphToFps(S), "ft/s")
                ),
                steps = listOf(
                    "D(ft) = ${fmt(D_in)} ${ctx.distanceUnit.label} → ${fmt(Dft)} ft",
                    "Formula:    S = √(30 · D · μ · e)",
                    "  D = ${fmt(Dft)} ft        (skid distance)",
                    "  μ = ${fmt(mu, 6)}         (drag factor)",
                    "  e = ${fmt(e, 4)}          (braking efficiency)",
                    "Substitute: S = √(30 · ${fmt(Dft)} · ${fmt(mu, 6)} · ${fmt(e, 4)})",
                    "Compute:    inside = ${fmt(inside)}",
                    "Result:     S = ${fmt(S)} mph"
                )
            )
        }

        list += Formula(
            id = "speed_from_d_mu_g_e",
            goal = Goal.SPEED_FROM_D_MU_G_E,
            requires = setOf(Var.DISTANCE, Var.MU, Var.GRADE, Var.EFF),
            equationText = "S = √(30 · D · (μ ± g) · e)",
            signQuestion = SQ_GRADE_SPEED
        ) { ctx, v ->
            val D_in = v.req(Var.DISTANCE)
            val mu = v.req(Var.MU)
            val g = v.req(Var.GRADE)
            val e = v.req(Var.EFF)

            require(mu > 0) { "μ must be > 0." }
            require(e > 0) { "e must be > 0." }

            val Dft = distToFt(D_in, ctx.distanceUnit)
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val muAdj = mu + sign * g
            require(muAdj > 0) { "μ ± g must be > 0." }

            val inside = 30.0 * Dft * muAdj * e
            val S = safeSqrt(inside)

            Computed(
                title = "Speed: ${fmt(S)} mph",
                equationText = "S = √(30 · D · (μ ± g) · e)",
                inputs = listOf(
                    CalcValue("D", D_in, ctx.distanceUnit.label),
                    CalcValue("μ", mu, "drag"),
                    CalcValue("g", g, "grade"),
                    CalcValue("e", e, "eff")
                ),
                outputs = listOf(
                    CalcValue("S", S, "mph"),
                    CalcValue("V", mphToFps(S), "ft/s")
                ),
                steps = listOf(
                    "D(ft) = ${fmt(D_in)} ${ctx.distanceUnit.label} → ${fmt(Dft)} ft",
                    "Formula:    S = √(30 · D · (μ ± g) · e)",
                    "  D = ${fmt(Dft)} ft        (skid distance)",
                    "  μ = ${fmt(mu, 6)}         (drag factor)",
                    "  g = ${fmt(g, 6)}          (grade, + uphill / − downhill)",
                    "  e = ${fmt(e, 4)}          (braking efficiency)",
                    "Substitute: μ ${if (sign > 0) "+" else "−"} g = ${fmt(muAdj, 6)}",
                    "Substitute: S = √(30 · ${fmt(Dft)} · ${fmt(muAdj, 6)} · ${fmt(e, 4)})",
                    "Compute:    inside = ${fmt(inside)}",
                    "Result:     S = ${fmt(S)} mph"
                )
            )
        }

        // combined speed (2–4)
        fun combined(id: String, vars: List<Var>): Formula {
            return Formula(
                id = id,
                goal = Goal.COMBINED_SPEED,
                requires = vars.toSet(),
                equationText = "S = √(" + vars.joinToString("² + ") { it.label } + "²)"
            ) { _, v ->
                val parts = vars.map { it to v.req(it) }
                val sumSq = parts.sumOf { it.second * it.second }
                val St = sqrt(sumSq)
                val inputs = parts.map { (k, x) -> CalcValue(k.label, x, "mph") }

                Computed(
                    title = "Combined speed: ${fmt(St)} mph",
                    equationText = "S = √(" + vars.joinToString("² + ") { it.label } + "²)",
                    inputs = inputs,
                    outputs = listOf(
                        CalcValue("S(total)", St, "mph"),
                        CalcValue("V(total)", mphToFps(St), "ft/s")
                    ),
                    steps = listOf(
                        "S(total) = √(" + parts.joinToString("² + ") { fmt(it.second) } + "²)",
                        "S(total) = √(${fmt(sumSq)}) = ${fmt(St)} mph"
                    )
                )
            }
        }
        list += combined("combined_2", listOf(Var.S1, Var.S2))
        list += combined("combined_3", listOf(Var.S1, Var.S2, Var.S3))
        list += combined("combined_4", listOf(Var.S1, Var.S2, Var.S3, Var.S4))

        // yaw/curve
        list += Formula(
            id = "yaw_speed",
            goal = Goal.YAW_SPEED,
            requires = setOf(Var.RADIUS, Var.MU),
            equationText = "S = √(15 · R · μ)"
        ) { _, v ->
            val R = v.req(Var.RADIUS)
            val mu = v.req(Var.MU)
            require(R > 0) { "Radius must be > 0." }
            require(mu > 0) { "μ must be > 0." }
            val inside = 15.0 * R * mu
            val S = safeSqrt(inside)
            Computed(
                title = "Speed: ${fmt(S)} mph",
                equationText = "S = √(15 · R · μ)",
                inputs = listOf(CalcValue("R", R, "ft"), CalcValue("μ", mu, "drag")),
                outputs = listOf(CalcValue("S", S, "mph"), CalcValue("V", mphToFps(S), "ft/s")),
                steps = listOf(
                    "Formula:    S = √(15 · R · μ)",
                    "  R = ${fmt(R)} ft           (radius of curve)",
                    "  μ = ${fmt(mu, 6)}          (drag factor)",
                    "Substitute: S = √(15 · ${fmt(R)} · ${fmt(mu, 6)})",
                    "Compute:    inside = ${fmt(inside)}",
                    "Result:     S = ${fmt(S)} mph"
                )
            )
        }

        // flip vault
        list += Formula(
            id = "flip_vault",
            goal = Goal.FLIP_VAULT_SPEED,
            requires = setOf(Var.DISTANCE, Var.HEIGHT),
            equationText = "S = 3.87·d / √(d ± h)",
            signQuestion = SQ_VAULT
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val hIn = v.req(Var.HEIGHT)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val hFt = distToFt(hIn, ctx.distanceUnit)
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val denomInside = dFt + sign * hFt
            val denom = safeSqrt(denomInside)
            val S = (3.87 * dFt) / denom
            Computed(
                title = "Speed: ${fmt(S)} mph",
                equationText = "S = 3.87·d / √(d ± h)",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("h", hIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("S", S, "mph"), CalcValue("V", mphToFps(S), "ft/s")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "h(ft) = ${fmt(hIn)} → ${fmt(hFt)} ft",
                    "Formula:    S = 3.87·d / √(d ± h)  [45° take-off assumed]",
                    "  d = ${fmt(dFt)} ft       (horizontal distance)",
                    "  h = ${fmt(hFt)} ft       (height, + landing lower / − landing higher)",
                    "Substitute: S = 3.87·${fmt(dFt)} / √(${fmt(dFt)} ${if (sign > 0) "+" else "−"} ${fmt(hFt)})",
                    "Compute:    denominator = √(${fmt(denomInside)}) = ${fmt(denom)}",
                    "Result:     S = ${fmt(S)} mph"
                )
            )
        }

        // fall speed
        list += Formula(
            id = "fall_speed",
            goal = Goal.FALL_SPEED,
            requires = setOf(Var.DISTANCE, Var.HEIGHT, Var.GRADE),
            equationText = "S = 2.73·d / √(h ± d·g)",
            signQuestion = SQ_FALL_GRADE
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val hIn = v.req(Var.HEIGHT)
            val g = v.req(Var.GRADE)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val hFt = distToFt(hIn, ctx.distanceUnit)
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val inside = hFt + sign * (dFt * g)
            val denom = safeSqrt(inside)
            val S = (2.73 * dFt) / denom
            Computed(
                title = "Speed: ${fmt(S)} mph",
                equationText = "S = 2.73·d / √(h ± d·g)",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("h", hIn, ctx.distanceUnit.label),
                    CalcValue("g", g, "grade")
                ),
                outputs = listOf(CalcValue("S", S, "mph"), CalcValue("V", mphToFps(S), "ft/s")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "h(ft) = ${fmt(hIn)} → ${fmt(hFt)} ft",
                    "Formula:    S = 2.73·d / √(h ± d·g)",
                    "  d = ${fmt(dFt)} ft       (horizontal distance)",
                    "  h = ${fmt(hFt)} ft       (fall height)",
                    "  g = ${fmt(g, 6)}         (grade, + downgrade / − upgrade)",
                    "Substitute: S = 2.73·${fmt(dFt)} / √(${fmt(hFt)} ${if (sign > 0) "+" else "−"} ${fmt(dFt)}·${fmt(g, 6)})",
                    "Compute:    inside = ${fmt(inside)}",
                    "Result:     S = ${fmt(S)} mph"
                )
            )
        }

        // long vault
        list += Formula(
            id = "long_vault",
            goal = Goal.LONG_VAULT_SPEED,
            requires = setOf(Var.DISTANCE, Var.HEIGHT, Var.TAKEOFF_ANGLE_DEG),
            equationText = "S = 2.73·d / √(d·cosθ·sinθ ± h·cos²θ)",
            signQuestion = SQ_VAULT
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val hIn = v.req(Var.HEIGHT)
            val thetaDeg = v.req(Var.TAKEOFF_ANGLE_DEG)

            val dFt = distToFt(dIn, ctx.distanceUnit)
            val hFt = distToFt(hIn, ctx.distanceUnit)
            val theta = degToRad(thetaDeg)
            val cosT = cos(theta)
            val sinT = sin(theta)
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0

            val inside = (dFt * cosT * sinT) + sign * (hFt * cosT * cosT)
            val denom = safeSqrt(inside)
            val S = (2.73 * dFt) / denom

            Computed(
                title = "Speed: ${fmt(S)} mph",
                equationText = "S = 2.73·d / √(d·cosθ·sinθ ± h·cos²θ)",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("h", hIn, ctx.distanceUnit.label),
                    CalcValue("θ", thetaDeg, "deg")
                ),
                outputs = listOf(CalcValue("S", S, "mph"), CalcValue("V", mphToFps(S), "ft/s")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "h(ft) = ${fmt(hIn)} → ${fmt(hFt)} ft",
                    "Formula:    S = 2.73·d / √(d·cosθ·sinθ ± h·cos²θ)",
                    "  d = ${fmt(dFt)} ft       (horizontal distance)",
                    "  h = ${fmt(hFt)} ft       (height, + landing lower / − landing higher)",
                    "  θ = ${fmt(thetaDeg)}°    (take-off angle)",
                    "  cosθ = ${fmt(cosT, 6)},  sinθ = ${fmt(sinT, 6)}",
                    "Substitute: S = 2.73·${fmt(dFt)} / √(${fmt(dFt)}·${fmt(cosT, 6)}·${fmt(sinT, 6)} ${if (sign > 0) "+" else "−"} ${fmt(hFt)}·${fmt(cosT, 6)}²)",
                    "Compute:    inside = ${fmt(inside)}",
                    "Result:     S = ${fmt(S)} mph"
                )
            )
        }

        // avg velocity
        list += Formula(
            id = "avg_velocity",
            goal = Goal.AVG_VELOCITY_FROM_D_T,
            requires = setOf(Var.DISTANCE, Var.TIME),
            equationText = "V = d / T"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val tIn = v.req(Var.TIME)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val V = dFt / tSec
            Computed(
                title = "Velocity: ${fmt(V)} ft/s",
                equationText = "V = d / T",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("T", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("V", V, "ft/s"), CalcValue("S", fpsToMph(V), "mph")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "V = ${fmt(dFt)} / ${fmt(tSec)} = ${fmt(V)} ft/s",
                    "S = ${fmt(fpsToMph(V))} mph"
                )
            )
        }

        // V = a·T
        list += Formula(
            id = "velocity_from_a_t",
            goal = Goal.VELOCITY_FROM_A_T,
            requires = setOf(Var.A_FTPS2, Var.TIME),
            equationText = "V = a · T"
        ) { ctx, v ->
            val a = v.req(Var.A_FTPS2)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val V = a * tSec
            Computed(
                title = "Velocity: ${fmt(V)} ft/s",
                equationText = "V = a · T",
                inputs = listOf(CalcValue("a", a, "ft/s²"), CalcValue("T", tIn, ctx.timeUnit.label)),
                outputs = listOf(CalcValue("V", V, "ft/s"), CalcValue("S", fpsToMph(V), "mph")),
                steps = listOf(
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "V = ${fmt(a)} · ${fmt(tSec)} = ${fmt(V)} ft/s",
                    "S = ${fmt(fpsToMph(V))} mph"
                )
            )
        }

        // VF = V0 ± a·T
        list += Formula(
            id = "vf_from_v0_a_t",
            goal = Goal.VF_FROM_V0_A_T,
            requires = setOf(Var.V0_FPS, Var.A_FTPS2, Var.TIME),
            equationText = "VF = V0 ± a·T",
            signQuestion = SQ_ACCEL_DECEL
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val a = v.req(Var.A_FTPS2)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val vf = v0 + sign * (a * tSec)
            Computed(
                title = "VF: ${fmt(vf)} ft/s",
                equationText = "VF = V0 ± a·T",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("a", a, "ft/s²"),
                    CalcValue("T", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("VF", vf, "ft/s"), CalcValue("SF", fpsToMph(vf), "mph")),
                steps = listOf(
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    VF = V0 ± a·T",
                    "  V0 = ${fmt(v0)} ft/s     (initial velocity)",
                    "  a  = ${fmt(a)} ft/s²     (acceleration/decel rate)",
                    "  T  = ${fmt(tSec)} sec    (elapsed time)",
                    "Substitute: VF = ${fmt(v0)} ${if (sign > 0) "+" else "−"} ${fmt(a)}·${fmt(tSec)}",
                    "Result:     VF = ${fmt(vf)} ft/s  →  SF = ${fmt(fpsToMph(vf))} mph"
                )
            )
        }

        // VF = √(V0² ± 2·a·d)
        list += Formula(
            id = "vf_from_v0_a_d",
            goal = Goal.VF_FROM_V0_A_D,
            requires = setOf(Var.V0_FPS, Var.A_FTPS2, Var.DISTANCE),
            equationText = "VF = √(V0² ± 2·a·d)",
            signQuestion = SQ_ACCEL_DECEL
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val a = v.req(Var.A_FTPS2)
            val dIn = v.req(Var.DISTANCE)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val inside = (v0 * v0) + sign * (2.0 * a * dFt)
            val vf = safeSqrt(inside)
            Computed(
                title = "VF: ${fmt(vf)} ft/s",
                equationText = "VF = √(V0² ± 2·a·d)",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("a", a, "ft/s²"),
                    CalcValue("d", dIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("VF", vf, "ft/s"), CalcValue("SF", fpsToMph(vf), "mph")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "Formula:    VF = √(V0² ± 2·a·d)",
                    "  V0 = ${fmt(v0)} ft/s     (initial velocity)",
                    "  a  = ${fmt(a)} ft/s²     (rate)",
                    "  d  = ${fmt(dFt)} ft      (distance)",
                    "Substitute: VF = √(${fmt(v0)}² ${if (sign > 0) "+" else "−"} 2·${fmt(a)}·${fmt(dFt)})",
                    "Compute:    inside = ${fmt(inside)}",
                    "Result:     VF = ${fmt(vf)} ft/s  →  SF = ${fmt(fpsToMph(vf))} mph"
                )
            )
        }

        // speed wrappers
        list += Formula(
            id = "final_speed_from_v0_a_t",
            goal = Goal.FINAL_SPEED_FROM_V0_A_T,
            requires = setOf(Var.V0_FPS, Var.A_FTPS2, Var.TIME),
            equationText = "SF = fps→mph(V0 ± a·T)",
            signQuestion = SQ_ACCEL_DECEL
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val a = v.req(Var.A_FTPS2)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val vf = v0 + sign * (a * tSec)
            val sf = fpsToMph(vf)
            Computed(
                title = "Final speed: ${fmt(sf)} mph",
                equationText = "SF = fps→mph(V0 ± a·T)",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("a", a, "ft/s²"),
                    CalcValue("T", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("SF", sf, "mph"), CalcValue("VF", vf, "ft/s")),
                steps = listOf(
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    SF = fps→mph(V0 ± a·T)",
                    "  V0 = ${fmt(v0)} ft/s     (initial velocity)",
                    "  a  = ${fmt(a)} ft/s²     (rate)",
                    "  T  = ${fmt(tSec)} sec    (elapsed time)",
                    "Substitute: VF = ${fmt(v0)} ${if (sign > 0) "+" else "−"} ${fmt(a)}·${fmt(tSec)}",
                    "Compute:    VF = ${fmt(vf)} ft/s",
                    "Result:     SF = VF / 1.466 = ${fmt(sf)} mph"
                )
            )
        }

        list += Formula(
            id = "final_speed_from_v0_mu_t",
            goal = Goal.FINAL_SPEED_FROM_V0_MU_T,
            requires = setOf(Var.V0_FPS, Var.A_FTPS2, Var.TIME), // a is derived from μ if user provides μ
            equationText = "SF = fps→mph(V0 ± (32.2·μ)·T)",
            signQuestion = SQ_ACCEL_DECEL
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val a = v.req(Var.A_FTPS2)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val vf = v0 + sign * (a * tSec)
            val sf = fpsToMph(vf)
            Computed(
                title = "Final speed: ${fmt(sf)} mph",
                equationText = "SF = fps→mph(V0 ± (32.2·μ)·T)",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("a", a, "ft/s²"),
                    CalcValue("T", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("SF", sf, "mph"), CalcValue("VF", vf, "ft/s")),
                steps = listOf(
                    "a = 32.2·μ (auto-derived if you entered μ)",
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "VF = ${fmt(v0)} ${if (sign > 0) "+" else "−"} ${fmt(a)}·${fmt(tSec)} = ${fmt(vf)} ft/s",
                    "SF = ${fmt(sf)} mph"
                )
            )
        }

        // a from μ
        list += Formula(
            id = "a_from_mu",
            goal = Goal.A_FROM_MU,
            requires = setOf(Var.MU),
            equationText = "a = 32.2·μ"
        ) { _, v ->
            val mu = v.req(Var.MU)
            val a = G_FTPS2 * mu
            Computed(
                title = "a: ${fmt(a)} ft/s²",
                equationText = "a = 32.2·μ",
                inputs = listOf(CalcValue("μ", mu, "drag")),
                outputs = listOf(CalcValue("a", a, "ft/s²")),
                steps = listOf("a = 32.2 · ${fmt(mu, 6)} = ${fmt(a)} ft/s²")
            )
        }

        // μ from a
        list += Formula(
            id = "mu_from_a",
            goal = Goal.MU_FROM_A,
            requires = setOf(Var.A_FTPS2),
            equationText = "μ = a / 32.2"
        ) { _, v ->
            val a = v.req(Var.A_FTPS2)
            val mu = a / G_FTPS2
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = a / 32.2",
                inputs = listOf(CalcValue("a", a, "ft/s²")),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf("μ = ${fmt(a)} / 32.2 = ${fmt(mu, 6)}")
            )
        }

        // μ from S and d
        list += Formula(
            id = "mu_from_s_d",
            goal = Goal.MU_FROM_S_D,
            requires = setOf(Var.SPEED_MPH, Var.DISTANCE),
            equationText = "μ = S² / (30·d)"
        ) { ctx, v ->
            val S = v.req(Var.SPEED_MPH)
            val dIn = v.req(Var.DISTANCE)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            require(dFt > 0) { "Distance must be > 0." }
            val mu = (S * S) / (30.0 * dFt)
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = S² / (30·d)",
                inputs = listOf(
                    CalcValue("S", S, "mph"),
                    CalcValue("d", dIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "Formula:    μ = S² / (30·d)",
                    "  S = ${fmt(S)} mph         (speed)",
                    "  d = ${fmt(dFt)} ft        (skid distance)",
                    "Substitute: μ = ${fmt(S)}² / (30·${fmt(dFt)})",
                    "Result:     μ = ${fmt(mu, 6)}"
                )
            )
        }

        // μ from d and T
        list += Formula(
            id = "mu_from_d_t",
            goal = Goal.MU_FROM_D_T,
            requires = setOf(Var.DISTANCE, Var.TIME),
            equationText = "μ = d / (16.1·T²)"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val tIn = v.req(Var.TIME)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(dFt > 0) { "Distance must be > 0." }
            require(tSec > 0) { "Time must be > 0." }
            val mu = dFt / (16.1 * tSec * tSec)
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = d / (16.1·T²)",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("T", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    μ = d / (16.1·T²)",
                    "  d = ${fmt(dFt)} ft        (distance)",
                    "  T = ${fmt(tSec)} sec      (time)",
                    "Substitute: μ = ${fmt(dFt)} / (16.1·${fmt(tSec)}²)",
                    "Result:     μ = ${fmt(mu, 6)}"
                )
            )
        }

        // μ from V and T
        list += Formula(
            id = "mu_from_v_t",
            goal = Goal.MU_FROM_V_T,
            requires = setOf(Var.VELOCITY_FPS, Var.TIME),
            equationText = "μ = V / (32.2·T)"
        ) { ctx, v ->
            val V = v.req(Var.VELOCITY_FPS)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val mu = V / (G_FTPS2 * tSec)
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = V / (32.2·T)",
                inputs = listOf(CalcValue("V", V, "ft/s"), CalcValue("T", tIn, ctx.timeUnit.label)),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf(
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    μ = V / (32.2·T)",
                    "  V = ${fmt(V)} ft/s        (velocity from/to stop)",
                    "  T = ${fmt(tSec)} sec      (time to stop)",
                    "Substitute: μ = ${fmt(V)} / (32.2·${fmt(tSec)})",
                    "Result:     μ = ${fmt(mu, 6)}"
                )
            )
        }

        // μ accelerating between V0 and VF in time
        list += Formula(
            id = "mu_accel_between_v",
            goal = Goal.MU_ACCEL_BETWEEN_V_IN_T,
            requires = setOf(Var.V0_FPS, Var.VF_FPS, Var.TIME),
            equationText = "μ = (VF − V0) / (32.2·T)"
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val vf = v.req(Var.VF_FPS)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val mu = (vf - v0) / (G_FTPS2 * tSec)
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = (VF − V0) / (32.2·T)",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("VF", vf, "ft/s"),
                    CalcValue("T", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf(
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    μ = (VF − V0) / (32.2·T)",
                    "  V0 = ${fmt(v0)} ft/s      (initial velocity)",
                    "  VF = ${fmt(vf)} ft/s      (final velocity)",
                    "  T  = ${fmt(tSec)} sec     (elapsed time)",
                    "Substitute: μ = (${fmt(vf)} − ${fmt(v0)}) / (32.2·${fmt(tSec)})",
                    "Result:     μ = ${fmt(mu, 6)}"
                )
            )
        }

        // μ decelerating between V0 and VF in time
        list += Formula(
            id = "mu_decel_between_v",
            goal = Goal.MU_DECEL_BETWEEN_V_IN_T,
            requires = setOf(Var.V0_FPS, Var.VF_FPS, Var.TIME),
            equationText = "μ = (V0 − VF) / (32.2·T)"
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val vf = v.req(Var.VF_FPS)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val mu = (v0 - vf) / (G_FTPS2 * tSec)
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = (V0 − VF) / (32.2·T)",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("VF", vf, "ft/s"),
                    CalcValue("T", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf(
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    μ = (V0 − VF) / (32.2·T)",
                    "  V0 = ${fmt(v0)} ft/s      (initial velocity)",
                    "  VF = ${fmt(vf)} ft/s      (final velocity)",
                    "  T  = ${fmt(tSec)} sec     (elapsed time)",
                    "Substitute: μ = (${fmt(v0)} − ${fmt(vf)}) / (32.2·${fmt(tSec)})",
                    "Result:     μ = ${fmt(mu, 6)}"
                )
            )
        }

        // μ between 2 velocities (distance)
        list += Formula(
            id = "mu_from_two_v_and_d",
            goal = Goal.MU_FROM_TWO_V_AND_D,
            requires = setOf(Var.V0_FPS, Var.VF_FPS, Var.DISTANCE),
            equationText = "μ = (VF² − V0²) / (64.4·d)"
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val vf = v.req(Var.VF_FPS)
            val dIn = v.req(Var.DISTANCE)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            require(dFt > 0) { "Distance must be > 0." }
            val mu = ((vf * vf) - (v0 * v0)) / (64.4 * dFt)
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = (VF² − V0²) / (64.4·d)",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("VF", vf, "ft/s"),
                    CalcValue("d", dIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "Formula:    μ = (VF² − V0²) / (64.4·d)",
                    "  V0 = ${fmt(v0)} ft/s      (initial velocity)",
                    "  VF = ${fmt(vf)} ft/s      (final velocity)",
                    "  d  = ${fmt(dFt)} ft       (distance)",
                    "Substitute: μ = (${fmt(vf)}² − ${fmt(v0)}²) / (64.4·${fmt(dFt)})",
                    "Result:     μ = ${fmt(mu, 6)}"
                )
            )
        }

        // μ from force/weight (drag sled)
        list += Formula(
            id = "mu_from_force_weight",
            goal = Goal.MU_FROM_FORCE_WEIGHT,
            requires = setOf(Var.FORCE, Var.WEIGHT),
            equationText = "μ = F / W"
        ) { _, v ->
            val F = v.req(Var.FORCE)
            val W = v.req(Var.WEIGHT)
            require(W != 0.0) { "Weight must not be 0." }
            val mu = F / W
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = F / W",
                inputs = listOf(CalcValue("F", F, "lb"), CalcValue("W", W, "lb")),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf("μ = ${fmt(F)} / ${fmt(W)} = ${fmt(mu, 6)}")
            )
        }

        // μ from distance, time, and initial velocity
        list += Formula(
            id = "mu_from_d_t_v0",
            goal = Goal.MU_FROM_D_T_V0,
            requires = setOf(Var.DISTANCE, Var.TIME, Var.V0_FPS),
            equationText = "μ = (d − V0·T) / (16.1·T²)"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val tIn = v.req(Var.TIME)
            val v0 = v.req(Var.V0_FPS)

            val dFt = distToFt(dIn, ctx.distanceUnit)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }

            val mu = (dFt - (v0 * tSec)) / (16.1 * tSec * tSec)
            Computed(
                title = "μ: ${fmt(mu, 6)}",
                equationText = "μ = (d − V0·T) / (16.1·T²)",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("T", tIn, ctx.timeUnit.label),
                    CalcValue("V0", v0, "ft/s")
                ),
                outputs = listOf(CalcValue("μ", mu, "drag")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    μ = (d − V0·T) / (16.1·T²)",
                    "  d  = ${fmt(dFt)} ft       (distance)",
                    "  V0 = ${fmt(v0)} ft/s      (initial velocity)",
                    "  T  = ${fmt(tSec)} sec     (elapsed time)",
                    "Substitute: μ = (${fmt(dFt)} − ${fmt(v0)}·${fmt(tSec)}) / (16.1·${fmt(tSec)}²)",
                    "Result:     μ = ${fmt(mu, 6)}"
                )
            )
        }

        // time formulas
        list += Formula(
            id = "time_from_d_v",
            goal = Goal.TIME_FROM_D_V,
            requires = setOf(Var.DISTANCE, Var.VELOCITY_FPS),
            equationText = "T = d / V"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val V = v.req(Var.VELOCITY_FPS)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            require(V != 0.0) { "Velocity must not be 0." }
            val tSec = dFt / V
            val tOut = when (ctx.timeUnit) {
                TimeUnit.SEC -> tSec
                TimeUnit.MIN -> tSec / 60.0
                TimeUnit.HOUR -> tSec / 3600.0
            }
            Computed(
                title = "Time: ${fmt(tOut)} ${ctx.timeUnit.label}",
                equationText = "T = d / V",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("V", V, "ft/s")
                ),
                outputs = listOf(CalcValue("T", tOut, ctx.timeUnit.label)),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "T(sec) = ${fmt(dFt)} / ${fmt(V)} = ${fmt(tSec)} sec",
                    "T(${ctx.timeUnit.label}) = ${fmt(tOut)} ${ctx.timeUnit.label}"
                )
            )
        }

        list += Formula(
            id = "time_from_v_a",
            goal = Goal.TIME_FROM_V_A,
            requires = setOf(Var.VELOCITY_FPS, Var.A_FTPS2),
            equationText = "T = V / a"
        ) { ctx, v ->
            val V = v.req(Var.VELOCITY_FPS)
            val a = v.req(Var.A_FTPS2)
            require(a != 0.0) { "Rate must not be 0." }
            val tSec = V / a
            val tOut = when (ctx.timeUnit) {
                TimeUnit.SEC -> tSec
                TimeUnit.MIN -> tSec / 60.0
                TimeUnit.HOUR -> tSec / 3600.0
            }
            Computed(
                title = "Time: ${fmt(tOut)} ${ctx.timeUnit.label}",
                equationText = "T = V / a",
                inputs = listOf(CalcValue("V", V, "ft/s"), CalcValue("a", a, "ft/s²")),
                outputs = listOf(CalcValue("T", tOut, ctx.timeUnit.label)),
                steps = listOf(
                    "T(sec) = ${fmt(V)} / ${fmt(a)} = ${fmt(tSec)} sec",
                    "T(${ctx.timeUnit.label}) = ${fmt(tOut)} ${ctx.timeUnit.label}"
                )
            )
        }

        list += Formula(
            id = "time_decel_v0_vf_a",
            goal = Goal.TIME_DECEL_V0_VF_A,
            requires = setOf(Var.V0_FPS, Var.VF_FPS, Var.A_FTPS2),
            equationText = "T = (V0 − VF) / a"
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val vf = v.req(Var.VF_FPS)
            val a = v.req(Var.A_FTPS2)
            require(a != 0.0) { "Rate must not be 0." }
            val tSec = (v0 - vf) / a
            val tOut = when (ctx.timeUnit) {
                TimeUnit.SEC -> tSec
                TimeUnit.MIN -> tSec / 60.0
                TimeUnit.HOUR -> tSec / 3600.0
            }
            Computed(
                title = "Time: ${fmt(tOut)} ${ctx.timeUnit.label}",
                equationText = "T = (V0 − VF) / a",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("S0", fpsToMph(v0), "mph"),
                    CalcValue("VF", vf, "ft/s"),
                    CalcValue("SF", fpsToMph(vf), "mph"),
                    CalcValue("a", a, "ft/s²")
                ),
                outputs = listOf(CalcValue("T", tOut, ctx.timeUnit.label)),
                steps = listOf(
                    "Formula:    T = (V0 − VF) / a",
                    "  V0 = ${fmt(v0)} ft/s  (${fmt(fpsToMph(v0))} mph)",
                    "  VF = ${fmt(vf)} ft/s  (${fmt(fpsToMph(vf))} mph)",
                    "  a  = ${fmt(a)} ft/s²",
                    "Substitute: T = (${fmt(v0)} − ${fmt(vf)}) / ${fmt(a)}",
                    "Compute:    T(sec) = ${fmt(tSec)} sec",
                    "Result:     T = ${fmt(tOut)} ${ctx.timeUnit.label}"
                )
            )
        }

        list += Formula(
            id = "time_accel_v0_vf_a",
            goal = Goal.TIME_ACCEL_V0_VF_A,
            requires = setOf(Var.V0_FPS, Var.VF_FPS, Var.A_FTPS2),
            equationText = "T = (VF − V0) / a"
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val vf = v.req(Var.VF_FPS)
            val a = v.req(Var.A_FTPS2)
            require(a != 0.0) { "Rate must not be 0." }
            val tSec = (vf - v0) / a
            val tOut = when (ctx.timeUnit) {
                TimeUnit.SEC -> tSec
                TimeUnit.MIN -> tSec / 60.0
                TimeUnit.HOUR -> tSec / 3600.0
            }
            Computed(
                title = "Time: ${fmt(tOut)} ${ctx.timeUnit.label}",
                equationText = "T = (VF − V0) / a",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("S0", fpsToMph(v0), "mph"),
                    CalcValue("VF", vf, "ft/s"),
                    CalcValue("SF", fpsToMph(vf), "mph"),
                    CalcValue("a", a, "ft/s²")
                ),
                outputs = listOf(CalcValue("T", tOut, ctx.timeUnit.label)),
                steps = listOf(
                    "Formula:    T = (VF − V0) / a",
                    "  V0 = ${fmt(v0)} ft/s  (${fmt(fpsToMph(v0))} mph)",
                    "  VF = ${fmt(vf)} ft/s  (${fmt(fpsToMph(vf))} mph)",
                    "  a  = ${fmt(a)} ft/s²",
                    "Substitute: T = (${fmt(vf)} − ${fmt(v0)}) / ${fmt(a)}",
                    "Compute:    T(sec) = ${fmt(tSec)} sec",
                    "Result:     T = ${fmt(tOut)} ${ctx.timeUnit.label}"
                )
            )
        }

        list += Formula(
            id = "time_from_d_mu",
            goal = Goal.TIME_FROM_D_MU,
            requires = setOf(Var.DISTANCE, Var.MU),
            equationText = "T = 0.25 · √(d / μ)"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val mu = v.req(Var.MU)
            require(mu > 0) { "μ must be > 0." }
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val tSec = 0.25 * safeSqrt(dFt / mu)
            val tOut = when (ctx.timeUnit) {
                TimeUnit.SEC -> tSec
                TimeUnit.MIN -> tSec / 60.0
                TimeUnit.HOUR -> tSec / 3600.0
            }
            Computed(
                title = "Time: ${fmt(tOut)} ${ctx.timeUnit.label}",
                equationText = "T = 0.25 · √(d / μ)",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("μ", mu, "drag")
                ),
                outputs = listOf(CalcValue("T", tOut, ctx.timeUnit.label)),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "Formula:    T = 0.25 · √(d / μ)",
                    "  d = ${fmt(dFt)} ft        (distance, from/to stop)",
                    "  μ = ${fmt(mu, 6)}         (drag factor)",
                    "Substitute: T = 0.25 · √(${fmt(dFt)} / ${fmt(mu, 6)})",
                    "Compute:    T(sec) = ${fmt(tSec)} sec",
                    "Result:     T = ${fmt(tOut)} ${ctx.timeUnit.label}"
                )
            )
        }

        // distance formulas
        list += Formula(
            id = "dist_from_v_t",
            goal = Goal.DIST_FROM_V_T,
            requires = setOf(Var.VELOCITY_FPS, Var.TIME),
            equationText = "d = V·T"
        ) { ctx, v ->
            val V = v.req(Var.VELOCITY_FPS)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            val dFt = V * tSec
            val dOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> dFt
                DistanceUnit.M -> dFt * 0.3048
            }
            Computed(
                title = "Distance: ${fmt(dOut)} ${ctx.distanceUnit.label}",
                equationText = "d = V·T",
                inputs = listOf(CalcValue("V", V, "ft/s"), CalcValue("T", tIn, ctx.timeUnit.label)),
                outputs = listOf(CalcValue("d", dOut, ctx.distanceUnit.label)),
                steps = listOf(
                    "T(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "d(ft) = ${fmt(V)} · ${fmt(tSec)} = ${fmt(dFt)} ft",
                    "d(${ctx.distanceUnit.label}) = ${fmt(dOut)} ${ctx.distanceUnit.label}"
                )
            )
        }

        list += Formula(
            id = "dist_from_s_mu",
            goal = Goal.DIST_FROM_S_MU,
            requires = setOf(Var.SPEED_MPH, Var.MU),
            equationText = "d = S² / (30·μ)"
        ) { ctx, v ->
            val S = v.req(Var.SPEED_MPH)
            val mu = v.req(Var.MU)
            require(mu > 0) { "μ must be > 0." }
            val dFt = (S * S) / (30.0 * mu)
            val dOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> dFt
                DistanceUnit.M -> dFt * 0.3048
            }
            Computed(
                title = "Distance: ${fmt(dOut)} ${ctx.distanceUnit.label}",
                equationText = "d = S² / (30·μ)",
                inputs = listOf(CalcValue("S", S, "mph"), CalcValue("μ", mu, "drag")),
                outputs = listOf(CalcValue("d", dOut, ctx.distanceUnit.label)),
                steps = listOf(
                    "d(ft) = ${fmt(S)}² / (30·${fmt(mu, 6)}) = ${fmt(dFt)} ft",
                    "d(${ctx.distanceUnit.label}) = ${fmt(dOut)} ${ctx.distanceUnit.label}"
                )
            )
        }

        list += Formula(
            id = "dist_from_mu_t",
            goal = Goal.DIST_FROM_MU_T,
            requires = setOf(Var.MU, Var.TIME),
            equationText = "d = 16.1·μ·t²"
        ) { ctx, v ->
            val mu = v.req(Var.MU)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(mu > 0) { "μ must be > 0." }
            val dFt = 16.1 * mu * tSec * tSec
            val dOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> dFt
                DistanceUnit.M -> dFt * 0.3048
            }
            Computed(
                title = "Distance: ${fmt(dOut)} ${ctx.distanceUnit.label}",
                equationText = "d = 16.1·μ·t²",
                inputs = listOf(CalcValue("μ", mu, "drag"), CalcValue("t", tIn, ctx.timeUnit.label)),
                outputs = listOf(CalcValue("d", dOut, ctx.distanceUnit.label)),
                steps = listOf(
                    "t(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "d(ft) = 16.1·${fmt(mu, 6)}·${fmt(tSec)}² = ${fmt(dFt)} ft",
                    "d(${ctx.distanceUnit.label}) = ${fmt(dOut)} ${ctx.distanceUnit.label}"
                )
            )
        }

        list += Formula(
            id = "dist_from_a_t",
            goal = Goal.DIST_FROM_A_T,
            requires = setOf(Var.A_FTPS2, Var.TIME),
            equationText = "d = (a·t²)/2"
        ) { ctx, v ->
            val a = v.req(Var.A_FTPS2)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            val dFt = (a * tSec * tSec) / 2.0
            val dOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> dFt
                DistanceUnit.M -> dFt * 0.3048
            }
            Computed(
                title = "Distance: ${fmt(dOut)} ${ctx.distanceUnit.label}",
                equationText = "d = (a·t²)/2",
                inputs = listOf(CalcValue("a", a, "ft/s²"), CalcValue("t", tIn, ctx.timeUnit.label)),
                outputs = listOf(CalcValue("d", dOut, ctx.distanceUnit.label)),
                steps = listOf(
                    "t(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "d(ft) = (${fmt(a)}·${fmt(tSec)}²)/2 = ${fmt(dFt)} ft",
                    "d(${ctx.distanceUnit.label}) = ${fmt(dOut)} ${ctx.distanceUnit.label}"
                )
            )
        }

        list += Formula(
            id = "dist_from_v0_t_a",
            goal = Goal.DIST_FROM_V0_T_A,
            requires = setOf(Var.V0_FPS, Var.A_FTPS2, Var.TIME),
            equationText = "d = V0·t ± (a·t²)/2",
            signQuestion = SQ_ACCEL_DECEL
        ) { ctx, v ->
            val v0 = v.req(Var.V0_FPS)
            val a = v.req(Var.A_FTPS2)
            val tIn = v.req(Var.TIME)
            val tSec = timeToSec(tIn, ctx.timeUnit)
            require(tSec > 0) { "Time must be > 0." }
            val sign = if (ctx.signMode == SignMode.PLUS) +1.0 else -1.0
            val dFt = (v0 * tSec) + sign * ((a * tSec * tSec) / 2.0)
            val dOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> dFt
                DistanceUnit.M -> dFt * 0.3048
            }
            Computed(
                title = "Distance: ${fmt(dOut)} ${ctx.distanceUnit.label}",
                equationText = "d = V0·t ± (a·t²)/2",
                inputs = listOf(
                    CalcValue("V0", v0, "ft/s"),
                    CalcValue("a", a, "ft/s²"),
                    CalcValue("t", tIn, ctx.timeUnit.label)
                ),
                outputs = listOf(CalcValue("d", dOut, ctx.distanceUnit.label)),
                steps = listOf(
                    "t(sec) = ${fmt(tIn)} → ${fmt(tSec)} sec",
                    "Formula:    d = V0·t ± (a·t²)/2",
                    "  V0 = ${fmt(v0)} ft/s      (initial velocity)",
                    "  a  = ${fmt(a)} ft/s²      (rate)",
                    "  t  = ${fmt(tSec)} sec     (elapsed time)",
                    "Substitute: d = ${fmt(v0)}·${fmt(tSec)} ${if (sign > 0) "+" else "−"} (${fmt(a)}·${fmt(tSec)}²)/2",
                    "Compute:    d = ${fmt(dFt)} ft",
                    "Result:     d = ${fmt(dOut)} ${ctx.distanceUnit.label}"
                )
            )
        }

        // perception reaction distance: V(ft/s) * 1.5
        list += Formula(
            id = "perception_reaction_distance",
            goal = Goal.PERCEPTION_REACTION_DISTANCE,
            requires = setOf(Var.SPEED_MPH),
            equationText = "d(p/r) = V(ft/s) · 1.5"
        ) { ctx, v ->
            val S = v.req(Var.SPEED_MPH)
            val V = mphToFps(S)
            val dFt = V * 1.5
            val dOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> dFt
                DistanceUnit.M -> dFt * 0.3048
            }
            Computed(
                title = "P/R distance: ${fmt(dOut)} ${ctx.distanceUnit.label}",
                equationText = "d(p/r) = V(ft/s) · 1.5",
                inputs = listOf(CalcValue("S", S, "mph")),
                outputs = listOf(CalcValue("d(p/r)", dOut, ctx.distanceUnit.label)),
                steps = listOf(
                    "Formula:    d(p/r) = V(ft/s) · 1.5  [assumes 1.5 sec reaction time]",
                    "  S = ${fmt(S)} mph          (approach speed)",
                    "  V = S·1.466 = ${fmt(V)} ft/s",
                    "Substitute: d = ${fmt(V)} · 1.5",
                    "Compute:    d = ${fmt(dFt)} ft",
                    "Result:     d(p/r) = ${fmt(dOut)} ${ctx.distanceUnit.label}"
                )
            )
        }

        // total stopping distance: pr + braking (S^2/(30μ))
        list += Formula(
            id = "total_stopping_distance",
            goal = Goal.TOTAL_STOPPING_DISTANCE,
            requires = setOf(Var.SPEED_MPH, Var.MU),
            equationText = "d(total) = (V·1.5) + (S²/(30·μ))"
        ) { ctx, v ->
            val S = v.req(Var.SPEED_MPH)
            val mu = v.req(Var.MU)
            require(mu > 0) { "μ must be > 0." }

            val V = mphToFps(S)
            val prFt = V * 1.5
            val brakeFt = (S * S) / (30.0 * mu)
            val totalFt = prFt + brakeFt

            val prOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> prFt
                DistanceUnit.M -> prFt * 0.3048
            }
            val brakeOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> brakeFt
                DistanceUnit.M -> brakeFt * 0.3048
            }
            val totalOut = when (ctx.distanceUnit) {
                DistanceUnit.FT -> totalFt
                DistanceUnit.M -> totalFt * 0.3048
            }

            Computed(
                title = "Total stopping: ${fmt(totalOut)} ${ctx.distanceUnit.label}",
                equationText = "d(total) = d(p/r) + d(braking)",
                inputs = listOf(CalcValue("S", S, "mph"), CalcValue("μ", mu, "drag")),
                outputs = listOf(
                    CalcValue("d(p/r)", prOut, ctx.distanceUnit.label),
                    CalcValue("d(braking)", brakeOut, ctx.distanceUnit.label),
                    CalcValue("d(total)", totalOut, ctx.distanceUnit.label)
                ),
                steps = listOf(
                    "Formula:    d(total) = d(p/r) + d(braking)",
                    "  S = ${fmt(S)} mph,  μ = ${fmt(mu, 6)}",
                    "  V = S·1.466 = ${fmt(V)} ft/s",
                    "d(p/r):     V · 1.5 = ${fmt(V)}·1.5 = ${fmt(prFt)} ft",
                    "d(braking): S²/(30·μ) = ${fmt(S)}²/(30·${fmt(mu, 6)}) = ${fmt(brakeFt)} ft",
                    "Compute:    d(total) = ${fmt(prFt)} + ${fmt(brakeFt)} = ${fmt(totalFt)} ft",
                    "Result:     d(total) = ${fmt(totalOut)} ${ctx.distanceUnit.label}"
                )
            )
        }

        // grade
        list += Formula(
            id = "grade_from_rise_run",
            goal = Goal.GRADE_FROM_RISE_RUN,
            requires = setOf(Var.OPP, Var.ADJ),
            equationText = "g = rise / run"
        ) { _, v ->
            val rise = v.req(Var.OPP)
            val run = v.req(Var.ADJ)
            require(run != 0.0) { "Run must not be 0." }
            val g = rise / run
            Computed(
                title = "Grade: ${fmt(g, 6)}",
                equationText = "g = rise / run",
                inputs = listOf(CalcValue("rise", rise, ""), CalcValue("run", run, "")),
                outputs = listOf(CalcValue("g", g, "grade")),
                steps = listOf("g = ${fmt(rise)} / ${fmt(run)} = ${fmt(g, 6)}")
            )
        }

        // pythag
        list += Formula(
            id = "pythag_c",
            goal = Goal.PYTHAG_C_FROM_A_B,
            requires = setOf(Var.OPP, Var.ADJ),
            equationText = "C = √(A² + B²)"
        ) { _, v ->
            val A = v.req(Var.OPP)
            val B = v.req(Var.ADJ)
            val C = sqrt(A * A + B * B)
            Computed(
                title = "C: ${fmt(C)}",
                equationText = "C = √(A² + B²)",
                inputs = listOf(CalcValue("A", A, ""), CalcValue("B", B, "")),
                outputs = listOf(CalcValue("C", C, "")),
                steps = listOf("C = √(${fmt(A)}² + ${fmt(B)}²) = ${fmt(C)}")
            )
        }

        // takeoff angle
        list += Formula(
            id = "takeoff_angle_from_d_h",
            goal = Goal.TAKEOFF_ANGLE_FROM_D_H,
            requires = setOf(Var.DISTANCE, Var.HEIGHT),
            equationText = "θ = atan(h/d)"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val hIn = v.req(Var.HEIGHT)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val hFt = distToFt(hIn, ctx.distanceUnit)
            require(dFt != 0.0) { "Distance must not be 0." }
            val theta = atan(hFt / dFt) * 180.0 / Math.PI
            Computed(
                title = "θ: ${fmt(theta)}°",
                equationText = "θ = atan(h/d)",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("h", hIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("θ", theta, "deg")),
                steps = listOf(
                    "d(ft) = ${fmt(dFt)},  h(ft) = ${fmt(hFt)}",
                    "Formula:    θ = atan(h/d)",
                    "  h = ${fmt(hFt)} ft        (height difference)",
                    "  d = ${fmt(dFt)} ft        (horizontal distance)",
                    "Substitute: θ = atan(${fmt(hFt)} / ${fmt(dFt)})",
                    "Result:     θ = ${fmt(theta)}°"
                )
            )
        }

        list += Formula(
            id = "unknown_takeoff_lower",
            goal = Goal.UNKNOWN_TAKEOFF_LANDING_LOWER,
            requires = setOf(Var.DISTANCE, Var.HEIGHT),
            equationText = "θ = atan(h/d) / 2"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val hIn = v.req(Var.HEIGHT)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val hFt = distToFt(hIn, ctx.distanceUnit)
            require(dFt != 0.0) { "Distance must not be 0." }
            val base = atan(hFt / dFt) * 180.0 / Math.PI
            val theta = base / 2.0
            Computed(
                title = "θ: ${fmt(theta)}°",
                equationText = "θ = atan(h/d) / 2",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("h", hIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("θ", theta, "deg")),
                steps = listOf(
                    "d(ft) = ${fmt(dFt)},  h(ft) = ${fmt(hFt)}",
                    "Formula:    θ = atan(h/d) / 2  [landing lower, unknown take-off]",
                    "  h = ${fmt(hFt)} ft        (height difference)",
                    "  d = ${fmt(dFt)} ft        (horizontal distance)",
                    "Compute:    base = atan(${fmt(hFt)} / ${fmt(dFt)}) = ${fmt(base)}°",
                    "Result:     θ = base/2 = ${fmt(theta)}°"
                )
            )
        }

        list += Formula(
            id = "unknown_takeoff_higher",
            goal = Goal.UNKNOWN_TAKEOFF_LANDING_HIGHER,
            requires = setOf(Var.DISTANCE, Var.HEIGHT),
            equationText = "θ = 90 − atan(h/d)/2"
        ) { ctx, v ->
            val dIn = v.req(Var.DISTANCE)
            val hIn = v.req(Var.HEIGHT)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            val hFt = distToFt(hIn, ctx.distanceUnit)
            require(dFt != 0.0) { "Distance must not be 0." }
            val base = atan(hFt / dFt) * 180.0 / Math.PI
            val theta = 90.0 - base / 2.0
            Computed(
                title = "θ: ${fmt(theta)}°",
                equationText = "θ = 90 − atan(h/d)/2",
                inputs = listOf(
                    CalcValue("d", dIn, ctx.distanceUnit.label),
                    CalcValue("h", hIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("θ", theta, "deg")),
                steps = listOf(
                    "d(ft) = ${fmt(dFt)},  h(ft) = ${fmt(hFt)}",
                    "Formula:    θ = 90 − atan(h/d)/2  [landing higher, unknown take-off]",
                    "  h = ${fmt(hFt)} ft        (height difference)",
                    "  d = ${fmt(dFt)} ft        (horizontal distance)",
                    "Compute:    base = atan(${fmt(hFt)} / ${fmt(dFt)}) = ${fmt(base)}°",
                    "Result:     θ = 90 − base/2 = ${fmt(theta)}°"
                )
            )
        }

        // radius from chord & middle-ordinate: R = C²/(8m) + m/2
        list += Formula(
            id = "radius_from_chord_midord",
            goal = Goal.RADIUS_FROM_CHORD_MIDORD,
            requires = setOf(Var.CHORD, Var.MID_ORD),
            equationText = "R = C²/(8·m) + m/2"
        ) { _, v ->
            val C = v.req(Var.CHORD)
            val m = v.req(Var.MID_ORD)
            require(m != 0.0) { "Middle-ordinate must not be 0." }
            val R = (C * C) / (8.0 * m) + (m / 2.0)
            Computed(
                title = "R: ${fmt(R)} ft",
                equationText = "R = C²/(8·m) + m/2",
                inputs = listOf(CalcValue("C", C, "ft"), CalcValue("m", m, "ft")),
                outputs = listOf(CalcValue("R", R, "ft")),
                steps = listOf(
                    "Formula:    R = C²/(8·m) + m/2",
                    "  C = ${fmt(C)} ft          (chord length)",
                    "  m = ${fmt(m)} ft          (middle-ordinate)",
                    "Substitute: R = ${fmt(C)}²/(8·${fmt(m)}) + ${fmt(m)}/2",
                    "Result:     R = ${fmt(R)} ft"
                )
            )
        }

        // trig ratios
        list += Formula(
            id = "sin_from_opp_hyp",
            goal = Goal.SIN_FROM_OPP_HYP,
            requires = setOf(Var.OPP, Var.HYP),
            equationText = "sinθ = opp/hyp"
        ) { _, v ->
            val opp = v.req(Var.OPP)
            val hyp = v.req(Var.HYP)
            require(hyp != 0.0) { "Hypotenuse must not be 0." }
            val s = opp / hyp
            Computed(
                title = "sinθ: ${fmt(s, 6)}",
                equationText = "sinθ = opp/hyp",
                inputs = listOf(CalcValue("opp", opp, ""), CalcValue("hyp", hyp, "")),
                outputs = listOf(CalcValue("sinθ", s, "")),
                steps = listOf("sinθ = ${fmt(opp)} / ${fmt(hyp)} = ${fmt(s, 6)}")
            )
        }

        list += Formula(
            id = "cos_from_adj_hyp",
            goal = Goal.COS_FROM_ADJ_HYP,
            requires = setOf(Var.ADJ, Var.HYP),
            equationText = "cosθ = adj/hyp"
        ) { _, v ->
            val adj = v.req(Var.ADJ)
            val hyp = v.req(Var.HYP)
            require(hyp != 0.0) { "Hypotenuse must not be 0." }
            val c = adj / hyp
            Computed(
                title = "cosθ: ${fmt(c, 6)}",
                equationText = "cosθ = adj/hyp",
                inputs = listOf(CalcValue("adj", adj, ""), CalcValue("hyp", hyp, "")),
                outputs = listOf(CalcValue("cosθ", c, "")),
                steps = listOf("cosθ = ${fmt(adj)} / ${fmt(hyp)} = ${fmt(c, 6)}")
            )
        }

        list += Formula(
            id = "tan_from_opp_adj",
            goal = Goal.TAN_FROM_OPP_ADJ,
            requires = setOf(Var.OPP, Var.ADJ),
            equationText = "tanθ = opp/adj"
        ) { _, v ->
            val opp = v.req(Var.OPP)
            val adj = v.req(Var.ADJ)
            require(adj != 0.0) { "Adjacent must not be 0." }
            val t = opp / adj
            Computed(
                title = "tanθ: ${fmt(t, 6)}",
                equationText = "tanθ = opp/adj",
                inputs = listOf(CalcValue("opp", opp, ""), CalcValue("adj", adj, "")),
                outputs = listOf(CalcValue("tanθ", t, "")),
                steps = listOf("tanθ = ${fmt(opp)} / ${fmt(adj)} = ${fmt(t, 6)}")
            )
        }

        // grade ↔ angle
        list += Formula(
            id = "grade_to_angle",
            goal = Goal.GRADE_TO_ANGLE,
            requires = setOf(Var.GRADE),
            equationText = "θ = atan(g)°"
        ) { _, v ->
            val g = v.req(Var.GRADE)
            val theta = atan(g) * 180.0 / Math.PI
            Computed(
                title = "θ: ${fmt(theta)}°",
                equationText = "θ = atan(g)°",
                inputs = listOf(CalcValue("g", g, "grade")),
                outputs = listOf(CalcValue("θ", theta, "deg")),
                steps = listOf(
                    "Formula:    θ = atan(g) × (180/π)",
                    "  g = ${fmt(g, 6)}          (grade as decimal)",
                    "Substitute: θ = atan(${fmt(g, 6)}) × (180/π)",
                    "Result:     θ = ${fmt(theta)}°"
                )
            )
        }

        list += Formula(
            id = "angle_to_grade",
            goal = Goal.ANGLE_TO_GRADE,
            requires = setOf(Var.TAKEOFF_ANGLE_DEG),
            equationText = "g = tan(θ)"
        ) { _, v ->
            val theta = v.req(Var.TAKEOFF_ANGLE_DEG)
            require(kotlin.math.abs(theta) < 89.9) { "Angle must be less than 89.9° (tangent diverges near vertical)." }
            val g = tan(degToRad(theta))
            Computed(
                title = "Grade: ${fmt(g, 6)}",
                equationText = "g = tan(θ)",
                inputs = listOf(CalcValue("θ", theta, "deg")),
                outputs = listOf(CalcValue("g", g, "grade")),
                steps = listOf(
                    "Formula:    g = tan(θ)",
                    "  θ = ${fmt(theta)}°         (angle in degrees)",
                    "Substitute: g = tan(${fmt(theta)}°)",
                    "Result:     g = ${fmt(g, 6)}"
                )
            )
        }

        // starting speed / velocity to a stop
        list += Formula(
            id = "v0_from_a_d_stop",
            goal = Goal.V0_FROM_A_D_STOP,
            requires = setOf(Var.A_FTPS2, Var.DISTANCE),
            equationText = "V0 = √(2·a·d)"
        ) { ctx, v ->
            val a = v.req(Var.A_FTPS2)
            val dIn = v.req(Var.DISTANCE)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            require(a > 0) { "Rate must be > 0." }
            require(dFt > 0) { "Distance must be > 0." }
            val inside = 2.0 * a * dFt
            val v0 = safeSqrt(inside)
            Computed(
                title = "V0: ${fmt(v0)} ft/s",
                equationText = "V0 = √(2·a·d)",
                inputs = listOf(
                    CalcValue("a", a, "ft/s²"),
                    CalcValue("d", dIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("V0", v0, "ft/s"), CalcValue("S0", fpsToMph(v0), "mph")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "Formula:    V0 = √(2·a·d)  [VF = 0, decelerating to stop]",
                    "  a = ${fmt(a)} ft/s²       (decel rate)",
                    "  d = ${fmt(dFt)} ft        (stopping distance)",
                    "Substitute: V0 = √(2·${fmt(a)}·${fmt(dFt)})",
                    "Compute:    inside = ${fmt(inside)}",
                    "Result:     V0 = ${fmt(v0)} ft/s  →  S0 = ${fmt(fpsToMph(v0))} mph"
                )
            )
        }

        list += Formula(
            id = "s0_from_a_d_stop",
            goal = Goal.S0_FROM_A_D_STOP,
            requires = setOf(Var.A_FTPS2, Var.DISTANCE),
            equationText = "S0 = fps→mph(√(2·a·d))"
        ) { ctx, v ->
            val a = v.req(Var.A_FTPS2)
            val dIn = v.req(Var.DISTANCE)
            val dFt = distToFt(dIn, ctx.distanceUnit)
            require(a > 0) { "Rate must be > 0." }
            require(dFt > 0) { "Distance must be > 0." }
            val inside = 2.0 * a * dFt
            val v0 = safeSqrt(inside)
            val s0 = fpsToMph(v0)
            Computed(
                title = "S0: ${fmt(s0)} mph",
                equationText = "S0 = fps→mph(√(2·a·d))",
                inputs = listOf(
                    CalcValue("a", a, "ft/s²"),
                    CalcValue("d", dIn, ctx.distanceUnit.label)
                ),
                outputs = listOf(CalcValue("S0", s0, "mph"), CalcValue("V0", v0, "ft/s")),
                steps = listOf(
                    "d(ft) = ${fmt(dIn)} → ${fmt(dFt)} ft",
                    "Formula:    S0 = fps→mph(√(2·a·d))  [VF = 0, decelerating to stop]",
                    "  a = ${fmt(a)} ft/s²       (decel rate)",
                    "  d = ${fmt(dFt)} ft        (stopping distance)",
                    "Substitute: V0 = √(2·${fmt(a)}·${fmt(dFt)})",
                    "Compute:    V0 = ${fmt(v0)} ft/s",
                    "Result:     S0 = V0 / 1.466 = ${fmt(s0)} mph"
                )
            )
        }

        return list
    }

    /**
     * Variables shown in "What do you know?" for each goal.
     * (Includes derived-friendly options.)
     */
    fun varsForGoal(goal: Goal): List<Var> = when (goal) {
        Goal.SPEED_FROM_VELOCITY -> listOf(Var.VELOCITY_FPS)
        Goal.VELOCITY_FROM_SPEED -> listOf(Var.SPEED_MPH)

        Goal.SPEED_FROM_D_MU_E -> listOf(Var.DISTANCE, Var.MU, Var.EFF)
        Goal.SPEED_FROM_D_MU_G_E -> listOf(Var.DISTANCE, Var.MU, Var.GRADE, Var.EFF)

        Goal.COMBINED_SPEED -> listOf(Var.S1, Var.S2, Var.S3, Var.S4)
        Goal.YAW_SPEED -> listOf(Var.RADIUS, Var.MU)

        Goal.FLIP_VAULT_SPEED -> listOf(Var.DISTANCE, Var.HEIGHT)
        Goal.FALL_SPEED -> listOf(Var.DISTANCE, Var.HEIGHT, Var.GRADE, Var.TAKEOFF_ANGLE_DEG)
        Goal.LONG_VAULT_SPEED -> listOf(Var.DISTANCE, Var.HEIGHT, Var.TAKEOFF_ANGLE_DEG, Var.GRADE)

        Goal.AVG_VELOCITY_FROM_D_T -> listOf(Var.DISTANCE, Var.TIME)

        // allow μ instead of a (derive)
        Goal.VELOCITY_FROM_A_T -> listOf(Var.A_FTPS2, Var.MU, Var.TIME)

        // allow initial speed mph instead of V0 ft/s
        Goal.VF_FROM_V0_A_T -> listOf(Var.V0_FPS, Var.V0_SPEED_MPH, Var.SPEED_MPH, Var.A_FTPS2, Var.MU, Var.TIME)
        Goal.VF_FROM_V0_A_D -> listOf(Var.V0_FPS, Var.V0_SPEED_MPH, Var.SPEED_MPH, Var.A_FTPS2, Var.MU, Var.DISTANCE)

        Goal.FINAL_SPEED_FROM_V0_A_T -> listOf(Var.V0_SPEED_MPH, Var.SPEED_MPH, Var.V0_FPS, Var.A_FTPS2, Var.MU, Var.TIME)
        Goal.FINAL_SPEED_FROM_V0_MU_T -> listOf(Var.V0_SPEED_MPH, Var.SPEED_MPH, Var.V0_FPS, Var.MU, Var.A_FTPS2, Var.TIME)

        Goal.A_FROM_MU -> listOf(Var.MU)
        Goal.MU_FROM_A -> listOf(Var.A_FTPS2)

        Goal.MU_FROM_S_D -> listOf(Var.SPEED_MPH, Var.DISTANCE)
        Goal.MU_FROM_D_T -> listOf(Var.DISTANCE, Var.TIME)
        Goal.MU_FROM_V_T -> listOf(Var.VELOCITY_FPS, Var.SPEED_MPH, Var.TIME)
        Goal.MU_ACCEL_BETWEEN_V_IN_T -> listOf(Var.V0_FPS, Var.VF_FPS, Var.TIME)
        Goal.MU_DECEL_BETWEEN_V_IN_T -> listOf(Var.V0_FPS, Var.VF_FPS, Var.TIME)
        Goal.MU_FROM_TWO_V_AND_D -> listOf(Var.V0_FPS, Var.VF_FPS, Var.DISTANCE)
        Goal.MU_FROM_FORCE_WEIGHT -> listOf(Var.FORCE, Var.WEIGHT)
        Goal.MU_FROM_D_T_V0 -> listOf(Var.DISTANCE, Var.TIME, Var.V0_FPS, Var.V0_SPEED_MPH, Var.SPEED_MPH)

        Goal.TIME_FROM_D_V -> listOf(Var.DISTANCE, Var.VELOCITY_FPS, Var.SPEED_MPH)
        Goal.TIME_FROM_V_A -> listOf(Var.VELOCITY_FPS, Var.SPEED_MPH, Var.A_FTPS2, Var.MU)
        Goal.TIME_DECEL_V0_VF_A -> listOf(Var.V0_FPS, Var.V0_SPEED_MPH, Var.VF_FPS, Var.VF_SPEED_MPH, Var.A_FTPS2, Var.MU)
        Goal.TIME_ACCEL_V0_VF_A -> listOf(Var.V0_FPS, Var.V0_SPEED_MPH, Var.VF_FPS, Var.VF_SPEED_MPH, Var.A_FTPS2, Var.MU)
        Goal.TIME_FROM_D_MU -> listOf(Var.DISTANCE, Var.MU, Var.A_FTPS2)

        Goal.DIST_FROM_V_T -> listOf(Var.VELOCITY_FPS, Var.SPEED_MPH, Var.TIME)
        Goal.DIST_FROM_S_MU -> listOf(Var.SPEED_MPH, Var.MU)
        Goal.DIST_FROM_MU_T -> listOf(Var.MU, Var.TIME)
        Goal.DIST_FROM_A_T -> listOf(Var.A_FTPS2, Var.MU, Var.TIME)
        Goal.DIST_FROM_V0_T_A -> listOf(Var.V0_FPS, Var.V0_SPEED_MPH, Var.SPEED_MPH, Var.A_FTPS2, Var.MU, Var.TIME)

        Goal.PERCEPTION_REACTION_DISTANCE -> listOf(Var.SPEED_MPH)
        Goal.TOTAL_STOPPING_DISTANCE -> listOf(Var.SPEED_MPH, Var.MU)

        Goal.GRADE_FROM_RISE_RUN -> listOf(Var.OPP, Var.ADJ)
        Goal.PYTHAG_C_FROM_A_B -> listOf(Var.OPP, Var.ADJ)
        Goal.TAKEOFF_ANGLE_FROM_D_H -> listOf(Var.DISTANCE, Var.HEIGHT)
        Goal.UNKNOWN_TAKEOFF_LANDING_LOWER -> listOf(Var.DISTANCE, Var.HEIGHT)
        Goal.UNKNOWN_TAKEOFF_LANDING_HIGHER -> listOf(Var.DISTANCE, Var.HEIGHT)
        Goal.RADIUS_FROM_CHORD_MIDORD -> listOf(Var.CHORD, Var.MID_ORD)

        Goal.SIN_FROM_OPP_HYP -> listOf(Var.OPP, Var.HYP)
        Goal.COS_FROM_ADJ_HYP -> listOf(Var.ADJ, Var.HYP)
        Goal.TAN_FROM_OPP_ADJ -> listOf(Var.OPP, Var.ADJ)

        Goal.GRADE_TO_ANGLE -> listOf(Var.GRADE)
        Goal.ANGLE_TO_GRADE -> listOf(Var.TAKEOFF_ANGLE_DEG)

        Goal.V0_FROM_A_D_STOP -> listOf(Var.A_FTPS2, Var.MU, Var.DISTANCE)
        Goal.S0_FROM_A_D_STOP -> listOf(Var.A_FTPS2, Var.MU, Var.DISTANCE)
    }

    /**
     * Pick formula for a goal based on selected vars (allow derivations).
     *
     * IMPORTANT: caller should pass the *cached* formula list so we never rebuild/lose list.
     */
    fun pickFormula(goal: Goal, known: Set<Var>, cachedFormulas: List<Formula>): Formula? {
        val candidates = cachedFormulas.filter { it.goal == goal }
        val dummyCtx = Context(DistanceUnit.FT, TimeUnit.SEC, SignMode.MINUS)

        return candidates
            .filter { canSatisfyWithDerivations(it, dummyCtx, known) }
            // prefer the most-specific formula (more required vars) if multiple match
            .sortedWith(compareByDescending<Formula> { it.requires.size }.thenBy { it.id })
            .firstOrNull()
    }

    /**
     * If a goal has exactly one formula and varsForGoal == that formula's requires,
     * there's no real choice to make — return the auto-selected var set.
     * Returns null if the user needs to pick "what do you know".
     */
    fun autoSelectVarsForGoal(goal: Goal, cachedFormulas: List<Formula>): Set<Var>? {
        val single = cachedFormulas.filter { it.goal == goal }.singleOrNull() ?: return null
        val options = varsForGoal(goal).toSet()
        return if (options == single.requires) single.requires else null
    }

    /**
     * Goals whose primary output is a speed in mph — used by CombinedSpeedScreen
     * to populate the per-segment formula picker.
     */
    fun speedGoals(): List<Goal> = listOf(
        Goal.SPEED_FROM_D_MU_E,
        Goal.SPEED_FROM_D_MU_G_E,
        Goal.YAW_SPEED,
        Goal.FLIP_VAULT_SPEED,
        Goal.FALL_SPEED,
        Goal.LONG_VAULT_SPEED,
        Goal.FINAL_SPEED_FROM_V0_A_T,
        Goal.FINAL_SPEED_FROM_V0_MU_T,
        Goal.S0_FROM_A_D_STOP
    )
}
