package com.example.collisioncalc.ui.export

import com.example.collisioncalc.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class ExportDoc(
    val title: String,
    val sections: List<ExportSection>
)

data class ExportSection(
    val heading: String,
    val blocks: List<ExportBlock>
)

sealed class ExportBlock {
    data class Paragraph(val text: String) : ExportBlock()
    data class BulletList(val items: List<String>) : ExportBlock()
    data class Divider(val text: String = "────────────────────────────────") : ExportBlock()
}

private fun formatLocal(epochMs: Long): String {
    val sdf = SimpleDateFormat("MM-dd-yyyy HH:mm", Locale.US)
    return sdf.format(Date(epochMs))
}

private fun formatNum(v: Double): String {
    if (!v.isFinite()) return "—"
    val s = "%.3f".format(abs(v))
    return s.trimEnd('0').trimEnd('.')
}

private fun crashLocationToLines(label: String, loc: CrashLocation): List<String> = buildList {
    add(label)
    when (loc) {
        is CrashLocation.Unspecified -> add("—")
        is CrashLocation.Intersection -> {
            add("Street 1: ${loc.street1.ifBlank { "—" }}")
            add("Street 2: ${loc.street2.ifBlank { "—" }}")
            add("City: ${loc.city.ifBlank { "—" }}")
            add("State: ${loc.state.ifBlank { "—" }}")
            add("Zip: ${loc.zip.ifBlank { "—" }}")
            add("Speed Limit: ${loc.speedLimitMph.ifBlank { "—" }} mph")
        }
        is CrashLocation.NonIntersection -> {
            add("Block #: ${loc.blockNumber.ifBlank { "—" }}")
            add("Street: ${loc.streetName.ifBlank { "—" }}")
            add("City: ${loc.city.ifBlank { "—" }}")
            add("State: ${loc.state.ifBlank { "—" }}")
            add("Zip: ${loc.zip.ifBlank { "—" }}")
            add("Speed Limit: ${loc.speedLimitMph.ifBlank { "—" }} mph")
        }
    }
}

private fun unitDetailsLines(caseFile: CaseFile, u: UnitEntity): List<String> {
    val base = mutableListOf<String>()
    base += "Unit: ${u.label.ifBlank { u.kind.name }}"
    base += "Kind: ${u.kind.name}"

    when (u) {
        is VehicleUnit -> {
            val v = caseFile.vehicles.firstOrNull { it.vehicleId == u.vehicleId }
            if (v == null) {
                base += "Vehicle: (missing link)"
            } else {
                base += "Vehicle: ${v.label.ifBlank { "Vehicle" }}"
                val ymm = listOf(v.year, v.make, v.model).filter { it.isNotBlank() }.joinToString(" ")
                base += "Y/M/M: ${if (ymm.isBlank()) "—" else ymm}"
                base += "VIN: ${v.vin.ifBlank { "—" }}"
                base += "Weight: ${v.weightLb?.let { formatNum(it) } ?: "—"} lb"
                if (v.notes.isNotBlank()) base += "Vehicle Notes: ${v.notes}"
            }
        }

        is PedestrianUnit -> {
            val nameLine = u.name.display().takeIf { it.isNotBlank() } ?: "—"
            base += "Name: $nameLine"
            base += "DOB: ${u.dobIso.ifBlank { "—" }}"
            base += "Address: ${u.address.ifBlank { "—" }}"
            base += "Phone: ${u.phone.ifBlank { "—" }}"
        }
    }

    return base
}

private fun calcToBlocks(caseFile: CaseFile, c: SavedCalculation): List<ExportBlock> {
    val blocks = mutableListOf<ExportBlock>()

    val attributed = if (c.attributedUnitIds.isNotEmpty()) {
        val labels = caseFile.units
            .filter { c.attributedUnitIds.contains(it.unitId) }
            .map { it.label.ifBlank { it.kind.name } }
        if (labels.isEmpty()) "Unassigned" else labels.joinToString(", ")
    } else "Unassigned"

    blocks += ExportBlock.Paragraph("CALCULATION")
    blocks += ExportBlock.Paragraph(c.title)
    blocks += ExportBlock.BulletList(
        listOf(
            "Type: ${c.type.name}",
            "Created: ${formatLocal(c.createdAtEpochMs)} (Local)",
            "Attributed: $attributed"
        ) + (if (c.equationText.isNotBlank()) listOf("Equation: ${c.equationText}") else emptyList())
    )

    blocks += ExportBlock.Divider()

    blocks += ExportBlock.Paragraph("OUTPUTS")
    blocks += ExportBlock.BulletList(
        if (c.outputs.isEmpty()) listOf("• —")
        else c.outputs.map { "• ${it.name}: ${formatNum(it.value)} ${it.unit}".trim() }
    )

    blocks += ExportBlock.Paragraph("INPUTS")
    blocks += ExportBlock.BulletList(
        if (c.inputs.isEmpty()) listOf("• —")
        else c.inputs.map { "• ${it.name}: ${formatNum(it.value)} ${it.unit}".trim() }
    )

    blocks += ExportBlock.Paragraph("WORK SHOWN")
    blocks += ExportBlock.BulletList(
        if (c.steps.isEmpty()) listOf("• —") else c.steps.map { "• $it" }
    )

    if (c.notes.isNotBlank()) {
        blocks += ExportBlock.Paragraph("NOTES")
        blocks += ExportBlock.BulletList(listOf("• ${c.notes}"))
    }

    return blocks
}

fun buildExportDoc(caseFile: CaseFile): ExportDoc {
    val sections = mutableListOf<ExportSection>()

    // ----- CASE SUMMARY -----
    val headerLines = mutableListOf<String>()
    headerLines += "Service #: ${caseFile.serviceNumber}"
    headerLines += "Case Created: ${formatLocal(caseFile.createdAtEpochMs)} (Local)"
    headerLines += "Crash Date: ${caseFile.crashInfo.dateIso.ifBlank { "—" }}"
    headerLines += "Crash Time: ${caseFile.crashInfo.time24h.ifBlank { "—" }}"
    headerLines.addAll(crashLocationToLines("Primary Location", caseFile.crashInfo.location))
    headerLines.addAll(crashLocationToLines("Nearest Reference Location", caseFile.crashInfo.nearestReference))

    sections.add(
        ExportSection(
            heading = "CASE SUMMARY",
            blocks = listOf(
                ExportBlock.Paragraph("COLLISIONCALC EXPORT"),
                ExportBlock.BulletList(headerLines),
                ExportBlock.Divider()
            )
        )
    )

    // ----- UNITS -----
    if (caseFile.units.isEmpty()) {
        sections.add(
            ExportSection(
                heading = "UNITS",
                blocks = listOf(
                    ExportBlock.Paragraph("No units in this case."),
                    ExportBlock.Divider()
                )
            )
        )
    } else {
        val unitBlocks = mutableListOf<ExportBlock>()
        caseFile.units.forEach { u ->
            unitBlocks.add(ExportBlock.BulletList(unitDetailsLines(caseFile, u).map { "• $it" }))
            unitBlocks.add(ExportBlock.Divider())
        }
        sections.add(ExportSection(heading = "UNITS", blocks = unitBlocks))
    }

    // ----- CALCULATIONS -----
    val calcSections = mutableListOf<ExportSection>()

    // per-unit sections
    caseFile.units.forEach { u ->
        val list = caseFile.calculations
            .filter { it.attributedUnitIds.contains(u.unitId) }
            .sortedBy { it.createdAtEpochMs }

        if (list.isNotEmpty()) {
            val blocks = mutableListOf<ExportBlock>()
            blocks.add(ExportBlock.Paragraph("Unit: ${u.label.ifBlank { u.kind.name }}"))
            blocks.add(ExportBlock.Divider())
            list.forEachIndexed { idx, c ->
                blocks.add(ExportBlock.Paragraph("(${idx + 1})"))
                blocks.addAll(calcToBlocks(caseFile, c))
                blocks.add(ExportBlock.Divider())
            }

            calcSections.add(
                ExportSection(
                    heading = "CALCULATIONS — ${u.label.ifBlank { u.kind.name }}",
                    blocks = blocks
                )
            )
        }
    }

    // unassigned section
    val unassigned = caseFile.calculations
        .filter { it.attributedUnitIds.isEmpty() && it.attributedVehicleIds.isEmpty() }
        .sortedBy { it.createdAtEpochMs }

    if (unassigned.isNotEmpty()) {
        val blocks = mutableListOf<ExportBlock>()
        unassigned.forEachIndexed { idx, c ->
            blocks.add(ExportBlock.Paragraph("(${idx + 1})"))
            blocks.addAll(calcToBlocks(caseFile, c))
            blocks.add(ExportBlock.Divider())
        }

        calcSections.add(
            ExportSection(
                heading = "CALCULATIONS — UNASSIGNED",
                blocks = blocks
            )
        )
    }

    if (calcSections.isEmpty()) {
        sections.add(
            ExportSection(
                heading = "CALCULATIONS",
                blocks = listOf(ExportBlock.Paragraph("No calculations in this case."))
            )
        )
    } else {
        sections.addAll(calcSections)
    }

    // ----- CASE NOTES -----
    val noteBlocks = mutableListOf<ExportBlock>()
    if (caseFile.notes.isEmpty()) {
        noteBlocks.add(ExportBlock.Paragraph("No case notes."))
    } else {
        val items = caseFile.notes
            .sortedBy { it.createdAtEpochMs }
            .mapIndexed { idx, n ->
                "• Note ${idx + 1} — ${formatLocal(n.createdAtEpochMs)} (Local)\n  ${n.text}"
            }
        noteBlocks.add(ExportBlock.BulletList(items))
    }
    sections.add(ExportSection("CASE NOTES", noteBlocks))

    return ExportDoc(
        title = "CollisionCalc Export — ${caseFile.serviceNumber}",
        sections = sections
    )
}
