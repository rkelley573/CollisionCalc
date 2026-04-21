package com.collisioncalc.app.ui.export

import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.CrashLocation
import com.collisioncalc.app.data.PedestrianUnit
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.UnitEntity
import com.collisioncalc.app.data.VehicleUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class ExportUnitBlock(
    val title: String,
    val kindLabel: String,
    val vehicleInfoLines: List<String>,
    val calculations: List<SavedCalculation>
)

data class ExportPayload(
    val caseHeaderLines: List<String>,
    val unitBlocks: List<ExportUnitBlock>,
    val unassignedCalculations: List<SavedCalculation>,
    val caseNotesLines: List<String>
)

private val CHICAGO: ZoneId = ZoneId.of("America/Chicago")

private val TS_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm").withZone(CHICAGO)

fun epochToLocalLine(epochMs: Long): String =
    TS_FMT.format(Instant.ofEpochMilli(epochMs))

fun buildExportPayload(caseFile: CaseFile): ExportPayload {
    val header = buildList {
        add("Service #: ${caseFile.serviceNumber}")

        val date = caseFile.crashInfo.dateIso.ifBlank { "—" }
        val time = caseFile.crashInfo.time24h.ifBlank { "—" }
        add("Date: $date")
        add("Time: $time")

        addAll(buildLocationLines("Primary Location", caseFile.crashInfo.location))
        addAll(buildLocationLines("Nearest Reference Location", caseFile.crashInfo.nearestReference))
    }

    val unitBlocks = caseFile.units.mapNotNull { u ->
        val label = u.label.ifBlank { u.kind.name }
        val kindLabel = when (u) {
            is VehicleUnit -> "Vehicle"
            is PedestrianUnit -> "Pedestrian"
        }

        val calcs = caseFile.calculations
            .filter { c -> c.attributedUnitIds.contains(u.unitId) }
            .sortedBy { it.createdAtEpochMs }

        if (calcs.isEmpty()) return@mapNotNull null

        ExportUnitBlock(
            title = label,
            kindLabel = kindLabel,
            vehicleInfoLines = buildVehicleInfoLines(caseFile, u),
            calculations = calcs
        )
    }

    val unassigned = caseFile.calculations
        .filter { c -> c.attributedUnitIds.isEmpty() }
        .sortedBy { it.createdAtEpochMs }

    val notesLines = buildList {
        if (caseFile.notes.isEmpty()) {
            add("No notes entered.")
        } else {
            caseFile.notes
                .sortedBy { it.createdAtEpochMs }
                .forEachIndexed { idx, n ->
                    add("Note ${idx + 1} — ${epochToLocalLine(n.createdAtEpochMs)}")
                    add(n.text)
                    add("")
                }
        }
    }

    return ExportPayload(
        caseHeaderLines = header,
        unitBlocks = unitBlocks,
        unassignedCalculations = unassigned,
        caseNotesLines = notesLines
    )
}

private fun buildLocationLines(label: String, loc: CrashLocation): List<String> = buildList {
    add("$label:")
    when (loc) {
        is CrashLocation.Unspecified -> add("  —")
        is CrashLocation.Intersection -> {
            add("  Street 1: ${loc.street1.ifBlank { "—" }}")
            add("  Street 2: ${loc.street2.ifBlank { "—" }}")
            add("  City: ${loc.city.ifBlank { "—" }}")
            add("  State: ${loc.state.ifBlank { "—" }}")
            add("  Zip: ${loc.zip.ifBlank { "—" }}")
            add("  Speed Limit (mph): ${loc.speedLimitMph.ifBlank { "—" }}")
        }
        is CrashLocation.NonIntersection -> {
            add("  Block #: ${loc.blockNumber.ifBlank { "—" }}")
            add("  Street: ${loc.streetName.ifBlank { "—" }}")
            add("  City: ${loc.city.ifBlank { "—" }}")
            add("  State: ${loc.state.ifBlank { "—" }}")
            add("  Zip: ${loc.zip.ifBlank { "—" }}")
            add("  Speed Limit (mph): ${loc.speedLimitMph.ifBlank { "—" }}")
        }
    }
}

private fun buildVehicleInfoLines(caseFile: CaseFile, u: UnitEntity): List<String> {
    if (u !is VehicleUnit) return emptyList()

    val v = caseFile.vehicles.firstOrNull { it.vehicleId == u.vehicleId } ?: return listOf("Linked Vehicle: —")

    return buildList {
        add("Linked Vehicle: ${v.label.ifBlank { "Vehicle" }}")
        val ymm = listOf(v.year, v.make, v.model).filter { it.isNotBlank() }.joinToString(" ")
        add("Year/Make/Model: ${if (ymm.isBlank()) "—" else ymm}")
        add("VIN: ${v.vin.ifBlank { "—" }}")
        add("Weight (lb): ${v.weightLb?.toString() ?: "—"}")
        if (v.notes.isNotBlank()) add("Vehicle Notes: ${v.notes}")
    }
}

fun format3(v: Double): String {
    if (!v.isFinite()) return "—"
    val s = "%.3f".format(abs(v))
    return s.trimEnd('0').trimEnd('.')
}
