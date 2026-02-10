package com.example.collisioncalc.ui.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.collisioncalc.data.*
import org.apache.poi.xwpf.usermodel.BreakType
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
        val margin = 40
        val lineGap = 16

        // Court-friendly: consistent header/footer on every content page.
        val headerTop = 28
        val footerBottom = 24

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val coverTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val coverSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val sectionBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // Light gray band
            color = 0xFFEFEFEF.toInt()
            style = Paint.Style.FILL
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB0B0B0.toInt()
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val contentTop = margin + headerTop
        val contentBottom = pageHeight - margin - footerBottom

        var pageNumber = 1

        // "Cover" is page 1 but does not show the running header.
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin.toFloat()

        val agency = payload.headerLines.getOrNull(0) ?: "Agency"
        val reportTitle = payload.headerLines.getOrNull(1) ?: "Collision Reconstruction Worksheet"
        val serviceLine = payload.headerLines.firstOrNull { it.startsWith("Service #") } ?: ""
        val reportDateLine = payload.headerLines.firstOrNull { it.startsWith("Report Date") } ?: ""
        val preparedByLine = payload.headerLines.firstOrNull { it.startsWith("Prepared By") } ?: ""
        val reviewedByLine = payload.headerLines.firstOrNull { it.startsWith("Reviewed By") } ?: ""
        val generatedLine = "Generated: ${formatEpoch(System.currentTimeMillis())} (Local)"

        fun drawHeaderFooter() {
            // Header line
            val headerY = (margin - 14).coerceAtLeast(12).toFloat()
            canvas.drawText(agency, margin.toFloat(), headerY, smallPaint)
            canvas.drawText(serviceLine, (pageWidth - margin - smallPaint.measureText(serviceLine)).toFloat(), headerY, smallPaint)

            val header2Y = (margin - 2).coerceAtLeast(22).toFloat()
            canvas.drawText(reportTitle, margin.toFloat(), header2Y, smallPaint)

            // Divider under header
            canvas.drawLine(
                margin.toFloat(),
                (margin + 2).toFloat(),
                (pageWidth - margin).toFloat(),
                (margin + 2).toFloat(),
                dividerPaint
            )

            // Footer
            val footerY = (pageHeight - margin + 10).toFloat()
            val left = "CollisionCalc Export"
            val right = "Page $pageNumber"
            canvas.drawText(left, margin.toFloat(), footerY, smallPaint)
            canvas.drawText(right, (pageWidth - margin - smallPaint.measureText(right)).toFloat(), footerY, smallPaint)

            // Divider over footer
            canvas.drawLine(
                margin.toFloat(),
                (pageHeight - margin - footerBottom).toFloat(),
                (pageWidth - margin).toFloat(),
                (pageHeight - margin - footerBottom).toFloat(),
                dividerPaint
            )
        }

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = contentTop.toFloat()
            drawHeaderFooter()
        }

        fun ensureSpace(linesNeeded: Int) {
            val needed = linesNeeded * lineGap
            if (y + needed > contentBottom) newPage()
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
            ensureSpace(3)
            // Shaded band
            val bandTop = y - 12
            val bandBottom = y + 10
            canvas.drawRect(
                margin.toFloat(),
                bandTop.toFloat(),
                (pageWidth - margin).toFloat(),
                bandBottom.toFloat(),
                sectionBandPaint
            )
            canvas.drawText(text, margin.toFloat(), y.toFloat(), hPaint)
            y += (lineGap + 6)
        }

        // ---------------------------
        // COVER PAGE (court-friendly)
        // ---------------------------
        // Centered title block
        val centerX = pageWidth / 2f
        var cy = 140f
        fun drawCentered(text: String, p: Paint) {
            val w = p.measureText(text)
            canvas.drawText(text, centerX - w / 2f, cy, p)
            cy += (p.textSize + 12f)
        }

        drawCentered(agency, coverSubPaint)
        drawCentered(reportTitle, coverTitlePaint)

        cy += 8f
        if (serviceLine.isNotBlank()) drawCentered(serviceLine, coverSubPaint)
        if (reportDateLine.isNotBlank()) drawCentered(reportDateLine, paint)
        if (preparedByLine.isNotBlank()) drawCentered(preparedByLine, paint)
        if (reviewedByLine.isNotBlank()) drawCentered(reviewedByLine, paint)
        drawCentered(generatedLine, paint)

        cy += 30f
        val disclaimer = "This document is a working collision-reconstruction worksheet generated by CollisionCalc. Values are based on entered assumptions and should be independently verified."
        // Use drawWrapped for disclaimer using a temporary y
        y = (cy).coerceAtLeast(340f)
        val oldY = y
        // Simple wrap using existing helper
        drawWrapped(disclaimer, paintUse = paint)
        y = oldY

        // Signature block
        y = (pageHeight - 220).toFloat()
        fun sigLine(label: String) {
            canvas.drawText(label, margin.toFloat(), y, paint)
            y += 18f
            canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, dividerPaint)
            y += 24f
        }
        sigLine("Prepared By")
        sigLine("Reviewed By")

        doc.finishPage(page)

        // ---------------------------
        // CONTENT PAGES
        // ---------------------------
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = contentTop.toFloat()
        drawHeaderFooter()

        // Case Summary (header lines after agency/title)
        sectionHeader("Case Summary")
        payload.headerLines.drop(2).forEach { drawWrapped(it) }
        drawWrapped(generatedLine)

        // Crash Info starts a new page (court-friendly separation)
        newPage()
        sectionHeader("Crash Info")
        payload.crashLines.drop(1).forEach { drawWrapped(it) }

        // Units: each unit on its own page for readability
        if (payload.unitSections.isNotEmpty()) {
            payload.unitSections.forEach { u ->
                newPage()
                sectionHeader("Unit Information")
                drawWrapped(u.title, paintUse = hPaint)
                u.lines.forEach { drawWrapped(it, indent = 16) }
            }
        }

        // Calculations: each calc section on its own page; each calculation starts with a block header
        if (payload.calcSections.isEmpty()) {
            newPage()
            sectionHeader("Calculations")
            drawWrapped("No calculations.")
        } else {
            payload.calcSections.forEach { sec ->
                newPage()
                sectionHeader("Calculations")
                drawWrapped(sec.title, paintUse = hPaint)

                sec.calcs.forEachIndexed { idx, c ->
                    ensureSpace(3)
                    drawWrapped("Calculation ${idx + 1}: ${c.title}", paintUse = paintBold())
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

                    y += 10
                }
            }
        }

        // Case Notes on its own page
        newPage()
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

        fun para(
            text: String = "",
            bold: Boolean = false,
            size: Int? = null,
            align: ParagraphAlignment? = null
        ) {
            val p = doc.createParagraph()
            if (align != null) p.alignment = align
            val r = p.createRun()
            r.isBold = bold
            if (size != null) r.fontSize = size
            if (text.isNotEmpty()) r.setText(text)
        }

        fun spacer(lines: Int = 1) {
            repeat(lines) { para("") }
        }

        fun pageBreak() {
            val p = doc.createParagraph()
            val r = p.createRun()
            r.addBreak(BreakType.PAGE)
        }

        fun sectionTitle(text: String) {
            val p = doc.createParagraph().apply { alignment = ParagraphAlignment.LEFT }
            val r = p.createRun()
            r.isBold = true
            r.fontSize = 14
            r.setText(text)
        }

        fun subsectionTitle(text: String) {
            val p = doc.createParagraph().apply { alignment = ParagraphAlignment.LEFT }
            val r = p.createRun()
            r.isBold = true
            r.fontSize = 12
            r.setText(text)
        }

        fun keyValueTable(lines: List<String>) {
            // Expect "Key: Value" format.
            val table = doc.createTable(lines.size.coerceAtLeast(1), 2)
            lines.forEachIndexed { i, ln ->
                val parts = ln.split(":", limit = 2)
                val k = parts.getOrNull(0)?.trim().orEmpty()
                val v = parts.getOrNull(1)?.trim().orEmpty()
                table.getRow(i).getCell(0).text = k
                table.getRow(i).getCell(1).text = v
            }
        }

        fun bulletLine(text: String) {
            para("• $text", bold = false, size = 11)
        }

        val agency = payload.headerLines.getOrNull(0) ?: "Agency"
        val reportTitle = payload.headerLines.getOrNull(1) ?: "Collision Reconstruction Worksheet"
        val metaLines = payload.headerLines.drop(2)
        val generatedLine = "Generated: ${formatEpoch(System.currentTimeMillis())} (Local)"

        // ---------------------------
        // COVER PAGE
        // ---------------------------
        para(agency, bold = true, size = 16, align = ParagraphAlignment.CENTER)
        para(reportTitle, bold = true, size = 22, align = ParagraphAlignment.CENTER)
        spacer(1)
        metaLines.forEach { para(it, size = 12, align = ParagraphAlignment.CENTER) }
        para(generatedLine, size = 11, align = ParagraphAlignment.CENTER)
        spacer(2)
        para(
            "This document is a working collision-reconstruction worksheet generated by CollisionCalc. Values are based on entered assumptions and should be independently verified.",
            size = 11,
            align = ParagraphAlignment.CENTER
        )
        spacer(3)
        para("Prepared By: ________________________________   Date: ____________", size = 11, align = ParagraphAlignment.CENTER)
        spacer(1)
        para("Reviewed By: ________________________________   Date: ____________", size = 11, align = ParagraphAlignment.CENTER)

        // Manual TOC (court-friendly structure)
        spacer(2)
        subsectionTitle("Table of Contents")
        bulletLine("Case Summary")
        bulletLine("Crash Info")
        if (payload.unitSections.isNotEmpty()) bulletLine("Unit Information (one section per unit)")
        bulletLine("Calculations (grouped by unit/section)")
        bulletLine("Case Notes")

        pageBreak()

        // ---------------------------
        // CASE SUMMARY
        // ---------------------------
        sectionTitle("Case Summary")
        keyValueTable(metaLines + listOf(generatedLine))
        spacer(1)

        pageBreak()

        // ---------------------------
        // CRASH INFO
        // ---------------------------
        sectionTitle("Crash Info")
        payload.crashLines.drop(1).forEach { bulletLine(it) }

        // ---------------------------
        // UNITS
        // ---------------------------
        if (payload.unitSections.isNotEmpty()) {
            payload.unitSections.forEach { u ->
                pageBreak()
                sectionTitle("Unit Information")
                subsectionTitle(u.title)
                u.lines.forEach { bulletLine(it) }
            }
        }

        // ---------------------------
        // CALCULATIONS
        // ---------------------------
        pageBreak()
        sectionTitle("Calculations")

        if (payload.calcSections.isEmpty()) {
            para("No calculations.", size = 11)
        } else {
            payload.calcSections.forEach { sec ->
                pageBreak()
                subsectionTitle(sec.title)

                sec.calcs.forEachIndexed { idx, c ->
                    spacer(1)
                    para("Calculation ${idx + 1}: ${c.title}", bold = true, size = 12)
                    para(c.whenLine, size = 11)

                    c.equation?.let {
                        para("Equation:", bold = true, size = 11)
                        para(it, size = 11)
                    }

                    para("Outputs:", bold = true, size = 11)
                    c.outputs.forEach { bulletLine(it) }

                    para("Inputs:", bold = true, size = 11)
                    c.inputs.forEach { bulletLine(it) }

                    if (payload.showFullWork) {
                        para("Work Shown:", bold = true, size = 11)
                        if (c.work.isEmpty()) bulletLine("—") else c.work.forEach { bulletLine(it) }
                    }

                    c.calcNotes?.let {
                        para("Calc Notes:", bold = true, size = 11)
                        para(it, size = 11)
                    }
                }
            }
        }

        // ---------------------------
        // CASE NOTES
        // ---------------------------
        pageBreak()
        sectionTitle("Case Notes")
        payload.caseNotesLines.drop(1).forEach { ln ->
            if (ln.isBlank()) spacer(1) else para(ln, size = 11)
        }

        doc.write(out)
        doc.close()
    }

    private fun paintBold(): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    private fun formatEpoch(epochMs: Long): String =
        dtf.format(Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDateTime())

    private fun fmt3(x: Double): String {
        if (!x.isFinite()) return "—"
        val s = "%.3f".format(abs(x))
        return s.trimEnd('0').trimEnd('.')
    }
}
