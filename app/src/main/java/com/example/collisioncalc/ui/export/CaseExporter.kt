package com.example.collisioncalc.ui.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.collisioncalc.data.*
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object CaseExporter {

    data class ExportMeta(
        val agencyName: String,
        val reportTitle: String,
        val preparedBy: String,
        val reviewedBy: String,
        val reportDateIso: String, // "YYYY-MM-DD"
        val showFullWork: Boolean
    )

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun exportPdf(context: Context, caseFile: CaseFile, uri: Uri, meta: ExportMeta) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val payload = buildPayload(caseFile, meta)
            writePdf(payload, out)
        } ?: error("Unable to open output stream")
    }

    fun exportDocx(context: Context, caseFile: CaseFile, uri: Uri, meta: ExportMeta) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val payload = buildPayload(caseFile, meta)
            writeDocx(payload, out)
        } ?: error("Unable to open output stream")
    }

    private data class Payload(
        val headerLines: List<String>,
        val crashLines: List<String>,
        val unitSections: List<UnitSection>,
        val calcSections: List<CalcSection>,
        val caseNotesLines: List<String>,
        val showFullWork: Boolean
    )

    private data class UnitSection(
        val title: String,
        val lines: List<String>
    )

    private data class CalcSection(
        val title: String,
        val calcs: List<CalcBlock>
    )

    private data class CalcBlock(
        val title: String,
        val whenLine: String,
        val equation: String?,
        val outputs: List<String>,
        val inputs: List<String>,
        val work: List<String>,
        val calcNotes: String?
    )

    private fun buildPayload(caseFile: CaseFile, meta: ExportMeta): Payload {
        val header = buildList {
            add(meta.agencyName)
            add(meta.reportTitle)
            add("Service #: ${caseFile.serviceNumber}")
            add("Report Date: ${meta.reportDateIso}")
            if (meta.preparedBy.isNotBlank()) add("Prepared By: ${meta.preparedBy}")
            if (meta.reviewedBy.isNotBlank()) add("Reviewed By: ${meta.reviewedBy}")
        }

        val crash = buildCrashLines(caseFile)
        val unitSections = buildUnitSections(caseFile)
        val calcSections = buildCalcSections(caseFile, meta.showFullWork)
        val caseNotesLines = buildCaseNotesLines(caseFile)

        return Payload(
            headerLines = header,
            crashLines = crash,
            unitSections = unitSections,
            calcSections = calcSections,
            caseNotesLines = caseNotesLines,
            showFullWork = meta.showFullWork
        )
    }

    private fun buildCrashLines(caseFile: CaseFile): List<String> {
        val c = caseFile.crashInfo
        val date = c.dateIso.ifBlank { "—" }
        val time = c.time24h.ifBlank { "—" }

        fun locLines(label: String, loc: CrashLocation): List<String> = buildList {
            add(label)
            when (loc) {
                is CrashLocation.Unspecified -> add("  —")
                is CrashLocation.Intersection -> {
                    add("  Street 1: ${loc.street1.ifBlank { "—" }}")
                    add("  Street 2: ${loc.street2.ifBlank { "—" }}")
                    add("  City: ${loc.city.ifBlank { "—" }}")
                    add("  State: ${loc.state.ifBlank { "—" }}")
                    add("  Zip: ${loc.zip.ifBlank { "—" }}")
                    add("  Speed Limit: ${loc.speedLimitMph.ifBlank { "—" }} mph")
                }
                is CrashLocation.NonIntersection -> {
                    add("  Block #: ${loc.blockNumber.ifBlank { "—" }}")
                    add("  Street: ${loc.streetName.ifBlank { "—" }}")
                    add("  City: ${loc.city.ifBlank { "—" }}")
                    add("  State: ${loc.state.ifBlank { "—" }}")
                    add("  Zip: ${loc.zip.ifBlank { "—" }}")
                    add("  Speed Limit: ${loc.speedLimitMph.ifBlank { "—" }} mph")
                }
            }
        }

        return buildList {
            add("Crash Info")
            add("Date: $date")
            add("Time: $time")
            addAll(locLines("Primary Location:", c.location))
            addAll(locLines("Nearest Reference Location:", c.nearestReference))
        }
    }

    private fun buildUnitSections(caseFile: CaseFile): List<UnitSection> {
        if (caseFile.units.isEmpty()) return emptyList()

        fun vehicleOf(vehicleId: VehicleId): Vehicle? =
            caseFile.vehicles.firstOrNull { it.vehicleId == vehicleId }

        fun yn(v: Boolean?): String = when (v) {
            true -> "Yes"
            false -> "No"
            null -> "Unknown"
        }

        fun insuranceLines(ins: InsuranceInfo): List<String> {
            if (ins.company.isBlank() && ins.policyNumber.isBlank() && ins.phone.isBlank()) {
                return listOf("  Insurance: —")
            }
            return buildList {
                add("  Insurance:")
                add("    Company: ${ins.company.ifBlank { "—" }}")
                add("    Policy #: ${ins.policyNumber.ifBlank { "—" }}")
                add("    Phone: ${ins.phone.ifBlank { "—" }}")
            }
        }

        fun occupantLines(occupants: List<Occupant>): List<String> {
            if (occupants.isEmpty()) return listOf("  Occupants: —")

            return buildList {
                add("  Occupants (${occupants.size}):")
                occupants.forEachIndexed { idx, o ->
                    val nameLine = o.name.display().takeIf { it.isNotBlank() } ?: "—"
                    val seat = o.seatingPosition.ifBlank { "—" }
                    val dob = o.dobIso.ifBlank { "—" }
                    val belt = yn(o.seatbeltWorn)
                    add("    (${idx + 1}) $nameLine")
                    add("      Seating: $seat")
                    add("      DOB: $dob")
                    add("      Seatbelt: $belt")
                    if (o.phone.isNotBlank()) add("      Phone: ${o.phone}")

                    // If you filled driver fields, export them (regardless of seating label)
                    val hasId = o.idNumber.isNotBlank() || o.idClass.isNotBlank() || o.idRestrictions.isNotBlank()
                    if (hasId) {
                        add("      ID #: ${o.idNumber.ifBlank { "—" }}")
                        add("      Class: ${o.idClass.ifBlank { "—" }}")
                        if (o.idRestrictions.isNotBlank()) add("      Restrictions: ${o.idRestrictions}")
                    }
                }
            }
        }

        return caseFile.units.map { u ->
            when (u) {
                is VehicleUnit -> {
                    val v = vehicleOf(u.vehicleId)
                    val title = u.label.ifBlank { "Vehicle Unit" }

                    val lines = buildList {
                        add("Kind: VEHICLE")
                        if (v == null) {
                            add("Vehicle: —")
                        } else {
                            add("Vehicle: ${v.label.ifBlank { "Vehicle" }}")

                            val ymm = listOf(v.year, v.make, v.model)
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                            add("  Y/M/M: ${if (ymm.isBlank()) "—" else ymm}")

                            add("  Color: ${v.color.ifBlank { "—" }}")
                            add("  VIN: ${v.vin.ifBlank { "—" }}")
                            add("  Weight: ${v.weightLb?.let { fmt3(it) } ?: "—"} lb")

                            // NEW: Insurance + Occupants
                            addAll(insuranceLines(v.insurance))
                            addAll(occupantLines(v.occupants))

                            if (v.notes.isNotBlank()) add("  Vehicle Notes: ${v.notes}")
                        }
                    }

                    UnitSection(title = title, lines = lines)
                }

                is PedestrianUnit -> {
                    val title = u.label.ifBlank { "Pedestrian" }
                    val lines = buildList {
                        add("Kind: PEDESTRIAN")
                        val nameLine = u.name.display().takeIf { it.isNotBlank() } ?: "—"
                        add("Name: $nameLine")
                        add("DOB: ${u.dobIso.ifBlank { "—" }}")
                        add("Address: ${u.address.ifBlank { "—" }}")
                        add("Phone: ${u.phone.ifBlank { "—" }}")
                    }
                    UnitSection(title = title, lines = lines)
                }
            }
        }
    }

    private fun buildCalcSections(caseFile: CaseFile, showFullWork: Boolean): List<CalcSection> {
        val sections = mutableListOf<CalcSection>()

        caseFile.units.forEach { unit ->
            val unitLabel = unit.label.ifBlank { unit.kind.name }
            val calcsForUnit = caseFile.calculations
                .filter { it.attributedUnitIds.contains(unit.unitId) }
                .sortedBy { it.createdAtEpochMs }

            if (calcsForUnit.isNotEmpty()) {
                sections += CalcSection(
                    title = unitLabel,
                    calcs = calcsForUnit.map { toCalcBlock(it, showFullWork) }
                )
            }
        }

        val unassigned = caseFile.calculations
            .filter { it.attributedUnitIds.isEmpty() && it.attributedVehicleIds.isEmpty() }
            .sortedBy { it.createdAtEpochMs }

        if (unassigned.isNotEmpty()) {
            sections += CalcSection(
                title = "Unassigned",
                calcs = unassigned.map { toCalcBlock(it, showFullWork) }
            )
        }

        return sections
    }

    private fun toCalcBlock(c: SavedCalculation, showFullWork: Boolean): CalcBlock {
        val whenLine = "Created: ${formatEpoch(c.createdAtEpochMs)}"
        val outputs = if (c.outputs.isEmpty()) listOf("—") else c.outputs.map { v ->
            "${v.name}: ${fmt3(v.value)} ${v.unit}".trim()
        }
        val inputs = if (c.inputs.isEmpty()) listOf("—") else c.inputs.map { v ->
            "${v.name}: ${fmt3(v.value)} ${v.unit}".trim()
        }
        val work = if (!showFullWork) emptyList() else (if (c.steps.isEmpty()) listOf("—") else c.steps)

        return CalcBlock(
            title = c.title,
            whenLine = whenLine,
            equation = c.equationText.takeIf { it.isNotBlank() },
            outputs = outputs,
            inputs = inputs,
            work = work,
            calcNotes = c.notes.takeIf { it.isNotBlank() }
        )
    }

    private fun buildCaseNotesLines(caseFile: CaseFile): List<String> {
        if (caseFile.notes.isEmpty()) return listOf("Case Notes", "—")

        val sorted = caseFile.notes.sortedBy { it.createdAtEpochMs }
        return buildList {
            add("Case Notes")
            sorted.forEachIndexed { idx, n ->
                add("Note ${idx + 1} — ${formatEpoch(n.createdAtEpochMs)}")
                add(n.text)
                add("")
            }
        }.dropLastWhile { it.isBlank() }
    }

    /* ---------------------------
       PDF
    ---------------------------- */

    private fun writePdf(payload: Payload, out: OutputStream) {
        val doc = PdfDocument()

        val pageWidth = 612
        val pageHeight = 792
        val margin = 36
        val lineGap = 16

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(linesNeeded: Int) {
            val needed = linesNeeded * lineGap
            if (y + needed > pageHeight - margin) newPage()
        }

        fun drawWrapped(text: String, paintUse: Paint = paint, indent: Int = 0) {
            val maxWidth = pageWidth - margin * 2 - indent
            val words = text.split(" ")
            var line = ""
            for (w in words) {
                val candidate = if (line.isEmpty()) w else "$line $w"
                if (paintUse.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    ensureSpace(1)
                    canvas.drawText(line, (margin + indent).toFloat(), y.toFloat(), paintUse)
                    y += lineGap
                    line = w
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(1)
                canvas.drawText(line, (margin + indent).toFloat(), y.toFloat(), paintUse)
                y += lineGap
            }
        }

        fun sectionHeader(text: String) {
            ensureSpace(2)
            canvas.drawText(text, margin.toFloat(), y.toFloat(), hPaint)
            y += (lineGap + 4)
        }

        ensureSpace(2)
        canvas.drawText(payload.headerLines.firstOrNull() ?: "Report", margin.toFloat(), y.toFloat(), titlePaint)
        y += 24

        payload.headerLines.drop(1).forEach { drawWrapped(it) }
        y += 6

        sectionHeader("Crash Info")
        payload.crashLines.drop(1).forEach { drawWrapped(it) }

        if (payload.unitSections.isNotEmpty()) {
            sectionHeader("Units")
            payload.unitSections.forEach { u ->
                ensureSpace(2)
                drawWrapped(u.title, paintUse = hPaint)
                u.lines.forEach { drawWrapped(it, indent = 16) }
                y += 6
            }
        }

        sectionHeader("Calculations")
        if (payload.calcSections.isEmpty()) {
            drawWrapped("No calculations.")
        } else {
            payload.calcSections.forEach { sec ->
                ensureSpace(2)
                drawWrapped(sec.title, paintUse = hPaint)

                sec.calcs.forEach { c ->
                    ensureSpace(2)
                    drawWrapped("• ${c.title}", paintUse = paint)
                    drawWrapped(c.whenLine, indent = 16)

                    c.equation?.let { eq ->
                        drawWrapped("Equation:", indent = 16)
                        drawWrapped(eq, indent = 32)
                    }

                    drawWrapped("Outputs:", indent = 16)
                    c.outputs.forEach { drawWrapped("• $it", indent = 32) }

                    drawWrapped("Inputs:", indent = 16)
                    c.inputs.forEach { drawWrapped("• $it", indent = 32) }

                    if (payload.showFullWork) {
                        drawWrapped("Work Shown:", indent = 16)
                        c.work.forEach { drawWrapped(it, indent = 32) }
                    }

                    c.calcNotes?.let { n ->
                        drawWrapped("Calc Notes:", indent = 16)
                        drawWrapped(n, indent = 32)
                    }

                    y += 8
                }

                y += 8
            }
        }

        sectionHeader("Case Notes")
        payload.caseNotesLines.drop(1).forEach { line ->
            if (line.isBlank()) y += 6 else drawWrapped(line)
        }

        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    /* ---------------------------
       DOCX
    ---------------------------- */

    private fun writeDocx(payload: Payload, out: OutputStream) {
        val doc = XWPFDocument()

        fun p(text: String = "", bold: Boolean = false, size: Int? = null, align: ParagraphAlignment? = null) {
            val para = doc.createParagraph()
            if (align != null) para.alignment = align
            val run = para.createRun()
            run.isBold = bold
            if (size != null) run.fontSize = size
            run.setText(text)
        }

        fun blockTitle(text: String) = p(text, bold = true, size = 14)
        fun sectionTitle(text: String) = p(text, bold = true, size = 12)
        fun line(text: String) = p(text, bold = false, size = 11)
        fun spacer() = p("")

        blockTitle(payload.headerLines.firstOrNull() ?: "Report")
        payload.headerLines.drop(1).forEach { line(it) }
        spacer()

        sectionTitle("Crash Info")
        payload.crashLines.drop(1).forEach { line(it) }
        spacer()

        if (payload.unitSections.isNotEmpty()) {
            sectionTitle("Units")
            payload.unitSections.forEach { u ->
                line(u.title)
                u.lines.forEach { line("  $it") }
                spacer()
            }
        }

        sectionTitle("Calculations")
        if (payload.calcSections.isEmpty()) {
            line("No calculations.")
        } else {
            payload.calcSections.forEach { sec ->
                line(sec.title)
                spacer()

                sec.calcs.forEach { c ->
                    line("• ${c.title}")
                    line("  ${c.whenLine}")

                    c.equation?.let { eq ->
                        line("  Equation:")
                        line("    $eq")
                    }

                    line("  Outputs:")
                    c.outputs.forEach { line("    • $it") }

                    line("  Inputs:")
                    c.inputs.forEach { line("    • $it") }

                    if (payload.showFullWork) {
                        line("  Work Shown:")
                        c.work.forEach { line("    $it") }
                    }

                    c.calcNotes?.let { n ->
                        line("  Calc Notes:")
                        line("    $n")
                    }

                    spacer()
                }

                spacer()
            }
        }

        sectionTitle("Case Notes")
        payload.caseNotesLines.drop(1).forEach { ln ->
            if (ln.isBlank()) spacer() else line(ln)
        }

        doc.write(out)
        doc.close()
    }

    private fun formatEpoch(epochMs: Long): String =
        dtf.format(Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDateTime())

    private fun fmt3(x: Double): String {
        if (!x.isFinite()) return "—"
        val s = "%.3f".format(abs(x))
        return s.trimEnd('0').trimEnd('.')
    }
}
