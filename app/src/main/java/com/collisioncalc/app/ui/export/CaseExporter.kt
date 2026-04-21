package com.collisioncalc.app.ui.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.CrashLocation
import com.collisioncalc.app.data.InsuranceInfo
import com.collisioncalc.app.data.Occupant
import com.collisioncalc.app.data.PedestrianUnit
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.Vehicle
import com.collisioncalc.app.data.VehicleId
import com.collisioncalc.app.data.VehicleUnit
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
        val agencyName: String,
        val reportTitle: String,
        val serviceNumber: String,
        val reportDateIso: String,
        val preparedBy: String,
        val reviewedBy: String,
        val generatedLine: String,

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
        val generated = "Generated: ${formatEpoch(System.currentTimeMillis())} (Local)"

        return Payload(
            agencyName = meta.agencyName.ifBlank { "Agency" },
            reportTitle = meta.reportTitle.ifBlank { "Collision Reconstruction Worksheet" },
            serviceNumber = caseFile.serviceNumber.ifBlank { "—" },
            reportDateIso = meta.reportDateIso.ifBlank { "—" },
            preparedBy = meta.preparedBy,
            reviewedBy = meta.reviewedBy,
            generatedLine = generated,

            crashLines = buildCrashLines(caseFile),
            unitSections = buildUnitSections(caseFile),
            calcSections = buildCalcSections(caseFile, meta.showFullWork),
            caseNotesLines = buildCaseNotesLines(caseFile),

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
       PDF (Option A: Cover -> Crash Info; NO redundant Case Summary page)
    ---------------------------- */

    private fun writePdf(payload: Payload, out: OutputStream) {
        val doc = PdfDocument()

        val pageWidth = 612
        val pageHeight = 792
        val margin = 40
        val lineGap = 16

        val headerTop = 28
        val footerBottom = 24

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }

        val coverTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val coverSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val sectionBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

        // ---------------------------
        // COVER PAGE (no running header)
        // ---------------------------
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        val centerX = pageWidth / 2f
        var cy = 140f

        fun drawCentered(text: String, p: Paint) {
            val w = p.measureText(text)
            canvas.drawText(text, centerX - w / 2f, cy, p)
            cy += (p.textSize + 12f)
        }

        drawCentered(payload.agencyName, coverSubPaint)
        drawCentered(payload.reportTitle, coverTitlePaint)

        cy += 8f
        drawCentered("Service #: ${payload.serviceNumber}", coverSubPaint)
        drawCentered("Report Date: ${payload.reportDateIso}", bodyPaint)

        if (payload.preparedBy.isNotBlank()) drawCentered("Prepared By: ${payload.preparedBy}", bodyPaint)
        if (payload.reviewedBy.isNotBlank()) drawCentered("Reviewed By: ${payload.reviewedBy}", bodyPaint)
        drawCentered(payload.generatedLine, bodyPaint)

        // Disclaimer (wrapped)
        val disclaimer =
            "This document is a working collision-reconstruction worksheet generated by CollisionCalc. Values are based on entered assumptions and should be independently verified."
        var y = 360f

        fun ensureSpace(linesNeeded: Int) {
            val needed = linesNeeded * lineGap
            if (y + needed > (pageHeight - margin - 120)) {
                // cover should never overflow; shrink by forcing less text
            }
        }

        fun drawWrapped(text: String, paintUse: Paint = bodyPaint, indent: Int = 0) {
            val maxWidth = pageWidth - margin * 2 - indent
            val words = text.split(" ")
            var line = ""
            for (w in words) {
                val candidate = if (line.isEmpty()) w else "$line $w"
                if (paintUse.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    ensureSpace(1)
                    canvas.drawText(line, (margin + indent).toFloat(), y, paintUse)
                    y += lineGap
                    line = w
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(1)
                canvas.drawText(line, (margin + indent).toFloat(), y, paintUse)
                y += lineGap
            }
        }

        drawWrapped(disclaimer, paintUse = bodyPaint)

        // Signature lines
        y = (pageHeight - 220).toFloat()
        fun sigLine(label: String) {
            canvas.drawText(label, margin.toFloat(), y, bodyPaint)
            y += 18f
            canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, dividerPaint)
            y += 24f
        }
        sigLine("Prepared By")
        sigLine("Reviewed By")

        doc.finishPage(page)

        // ---------------------------
        // CONTENT PAGES (with running header/footer)
        // ---------------------------
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = contentTop.toFloat()

        fun drawHeaderFooter() {
            val headerY1 = (margin - 14).coerceAtLeast(12).toFloat()
            val headerY2 = (margin - 2).coerceAtLeast(22).toFloat()

            val left1 = payload.agencyName
            val right1 = "Service #: ${payload.serviceNumber}"
            canvas.drawText(left1, margin.toFloat(), headerY1, smallPaint)
            canvas.drawText(
                right1,
                (pageWidth - margin - smallPaint.measureText(right1)).toFloat(),
                headerY1,
                smallPaint
            )

            canvas.drawText(payload.reportTitle, margin.toFloat(), headerY2, smallPaint)

            canvas.drawLine(
                margin.toFloat(),
                (margin + 2).toFloat(),
                (pageWidth - margin).toFloat(),
                (margin + 2).toFloat(),
                dividerPaint
            )

            val footerY = (pageHeight - margin + 10).toFloat()
            val leftF = "CollisionCalc Export"
            val rightF = "Page $pageNumber"
            canvas.drawText(leftF, margin.toFloat(), footerY, smallPaint)
            canvas.drawText(
                rightF,
                (pageWidth - margin - smallPaint.measureText(rightF)).toFloat(),
                footerY,
                smallPaint
            )

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

        fun ensureContentSpace(linesNeeded: Int) {
            val needed = linesNeeded * lineGap
            if (y + needed > contentBottom) newPage()
        }

        fun drawWrappedContent(text: String, paintUse: Paint = bodyPaint, indent: Int = 0) {
            val maxWidth = pageWidth - margin * 2 - indent
            val words = text.split(" ")
            var line = ""
            for (w in words) {
                val candidate = if (line.isEmpty()) w else "$line $w"
                if (paintUse.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    ensureContentSpace(1)
                    canvas.drawText(line, (margin + indent).toFloat(), y, paintUse)
                    y += lineGap
                    line = w
                }
            }
            if (line.isNotEmpty()) {
                ensureContentSpace(1)
                canvas.drawText(line, (margin + indent).toFloat(), y, paintUse)
                y += lineGap
            }
        }

        fun sectionHeader(text: String) {
            ensureContentSpace(3)
            val bandTop = y - 12
            val bandBottom = y + 10
            canvas.drawRect(
                margin.toFloat(),
                bandTop.toFloat(),
                (pageWidth - margin).toFloat(),
                bandBottom.toFloat(),
                sectionBandPaint
            )
            canvas.drawText(text, margin.toFloat(), y, sectionTitlePaint)
            y += (lineGap + 6)
        }

        // Start first content page with header/footer
        drawHeaderFooter()

        // -------- Option A: Go straight to Crash Info (no Case Summary) --------
        sectionHeader("Crash Info")
        payload.crashLines.drop(1).forEach { drawWrappedContent(it) }

        // Units: each unit on its own page
        if (payload.unitSections.isNotEmpty()) {
            payload.unitSections.forEach { u ->
                newPage()
                sectionHeader("Unit Information")
                drawWrappedContent(u.title, paintUse = sectionTitlePaint)
                u.lines.forEach { drawWrappedContent(it, indent = 16) }
            }
        }

        // Calculations: each calc section on its own page
        if (payload.calcSections.isEmpty()) {
            newPage()
            sectionHeader("Calculations")
            drawWrappedContent("No calculations.")
        } else {
            payload.calcSections.forEach { sec ->
                newPage()
                sectionHeader("Calculations")
                drawWrappedContent(sec.title, paintUse = sectionTitlePaint)

                sec.calcs.forEachIndexed { idx, c ->
                    ensureContentSpace(3)
                    drawWrappedContent("Calculation ${idx + 1}: ${c.title}", paintUse = boldBodyPaint)
                    drawWrappedContent(c.whenLine, indent = 16)

                    c.equation?.let { eq ->
                        drawWrappedContent("Equation:", indent = 16, paintUse = boldBodyPaint)
                        drawWrappedContent(eq, indent = 32)
                    }

                    drawWrappedContent("Outputs:", indent = 16, paintUse = boldBodyPaint)
                    c.outputs.forEach { drawWrappedContent("• $it", indent = 32) }

                    drawWrappedContent("Inputs:", indent = 16, paintUse = boldBodyPaint)
                    c.inputs.forEach { drawWrappedContent("• $it", indent = 32) }

                    if (payload.showFullWork) {
                        drawWrappedContent("Work Shown:", indent = 16, paintUse = boldBodyPaint)
                        c.work.forEach { drawWrappedContent(it, indent = 32) }
                    }

                    c.calcNotes?.let { n ->
                        drawWrappedContent("Calc Notes:", indent = 16, paintUse = boldBodyPaint)
                        drawWrappedContent(n, indent = 32)
                    }

                    y += 10
                }
            }
        }

        // Case Notes: own page
        newPage()
        sectionHeader("Case Notes")
        payload.caseNotesLines.drop(1).forEach { line ->
            if (line.isBlank()) y += 6 else drawWrappedContent(line)
        }

        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    /* ---------------------------
       DOCX (Option A: remove redundant Case Summary section)
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

        fun bulletLine(text: String) {
            para("• $text", bold = false, size = 11)
        }

        // ---------------------------
        // COVER PAGE (+ TOC on cover)
        // ---------------------------
        para(payload.agencyName, bold = true, size = 16, align = ParagraphAlignment.CENTER)
        para(payload.reportTitle, bold = true, size = 22, align = ParagraphAlignment.CENTER)
        spacer(1)
        para("Service #: ${payload.serviceNumber}", bold = true, size = 12, align = ParagraphAlignment.CENTER)
        para("Report Date: ${payload.reportDateIso}", size = 12, align = ParagraphAlignment.CENTER)
        if (payload.preparedBy.isNotBlank()) para("Prepared By: ${payload.preparedBy}", size = 12, align = ParagraphAlignment.CENTER)
        if (payload.reviewedBy.isNotBlank()) para("Reviewed By: ${payload.reviewedBy}", size = 12, align = ParagraphAlignment.CENTER)
        para(payload.generatedLine, size = 11, align = ParagraphAlignment.CENTER)
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

        spacer(2)
        subsectionTitle("Table of Contents")
        bulletLine("Crash Info")
        if (payload.unitSections.isNotEmpty()) bulletLine("Unit Information (one section per unit)")
        bulletLine("Calculations (grouped by unit/section)")
        bulletLine("Case Notes")

        pageBreak()

        // ---------------------------
        // CRASH INFO (Option A: first content section)
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

    private fun formatEpoch(epochMs: Long): String =
        dtf.format(Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDateTime())

    private fun fmt3(x: Double): String {
        if (!x.isFinite()) return "—"
        val s = "%.3f".format(abs(x))
        return s.trimEnd('0').trimEnd('.')
    }
}
