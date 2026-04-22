package com.collisioncalc.app.ui.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.collisioncalc.app.data.CalcType
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.CrashLocation
import com.collisioncalc.app.data.PedestrianUnit
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.VehicleUnit
import org.apache.poi.xwpf.usermodel.BreakType
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder
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
        val reportDateIso: String,
        val showFullWork: Boolean
    )

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun exportPdf(context: Context, caseFile: CaseFile, uri: Uri, meta: ExportMeta) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            writePdf(buildPayload(caseFile, meta), out)
        } ?: error("Unable to open output stream")
    }

    fun exportDocx(context: Context, caseFile: CaseFile, uri: Uri, meta: ExportMeta) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            writeDocx(buildPayload(caseFile, meta), out)
        } ?: error("Unable to open output stream")
    }

    // ---- Data structures ----

    private data class Payload(
        val agencyName: String,
        val reportTitle: String,
        val serviceNumber: String,
        val reportDateIso: String,
        val preparedBy: String,
        val reviewedBy: String,
        val generatedLine: String,
        val crashSection: CrashSection,
        val unitSections: List<UnitSection>,
        val calcSections: List<CalcSection>,
        val caseNotesLines: List<String>,
        val showFullWork: Boolean
    )

    private data class CrashSection(
        val date: String,
        val time: String,
        val primaryLocation: LocationBlock,
        val nearestReference: LocationBlock
    )

    private sealed class LocationBlock {
        data object Unspecified : LocationBlock()
        data class Intersection(
            val street1: String, val street2: String,
            val city: String, val state: String,
            val zip: String, val speedLimit: String
        ) : LocationBlock()
        data class NonIntersection(
            val blockNumber: String, val streetName: String,
            val city: String, val state: String,
            val zip: String, val speedLimit: String
        ) : LocationBlock()
    }

    private data class UnitSection(val title: String, val fields: List<Pair<String, String>>, val subsections: List<UnitSubsection>)
    private data class UnitSubsection(val title: String, val fields: List<Pair<String, String>>)
    private data class CalcSection(val title: String, val calcs: List<CalcBlock>)
    private data class CalcBlock(
        val title: String,
        val calcType: CalcType?,
        val whenLine: String,
        val formulaExplanation: List<String>,
        val outputs: List<String>,
        val inputs: List<String>,
        val work: List<String>,
        val calcNotes: String?
    )

    // ---- Payload builder ----

    private fun buildPayload(caseFile: CaseFile, meta: ExportMeta): Payload = Payload(
        agencyName = meta.agencyName.ifBlank { "Agency" },
        reportTitle = meta.reportTitle.ifBlank { "Collision Reconstruction Worksheet" },
        serviceNumber = caseFile.serviceNumber.ifBlank { "—" },
        reportDateIso = meta.reportDateIso.ifBlank { "—" },
        preparedBy = meta.preparedBy,
        reviewedBy = meta.reviewedBy,
        generatedLine = "Generated: ${formatEpoch(System.currentTimeMillis())} (Local)",
        crashSection = buildCrashSection(caseFile),
        unitSections = buildUnitSections(caseFile),
        calcSections = buildCalcSections(caseFile, meta.showFullWork),
        caseNotesLines = buildCaseNotesLines(caseFile),
        showFullWork = meta.showFullWork
    )

    private fun buildCrashSection(caseFile: CaseFile): CrashSection {
        fun toBlock(loc: CrashLocation): LocationBlock = when (loc) {
            is CrashLocation.Unspecified -> LocationBlock.Unspecified
            is CrashLocation.Intersection -> LocationBlock.Intersection(
                street1 = loc.street1.ifBlank { "—" }, street2 = loc.street2.ifBlank { "—" },
                city = loc.city.ifBlank { "—" }, state = loc.state.ifBlank { "—" },
                zip = loc.zip.ifBlank { "—" }, speedLimit = loc.speedLimitMph.ifBlank { "—" }
            )
            is CrashLocation.NonIntersection -> LocationBlock.NonIntersection(
                blockNumber = loc.blockNumber.ifBlank { "—" }, streetName = loc.streetName.ifBlank { "—" },
                city = loc.city.ifBlank { "—" }, state = loc.state.ifBlank { "—" },
                zip = loc.zip.ifBlank { "—" }, speedLimit = loc.speedLimitMph.ifBlank { "—" }
            )
        }
        return CrashSection(
            date = caseFile.crashInfo.dateIso.ifBlank { "—" },
            time = caseFile.crashInfo.time24h.ifBlank { "—" },
            primaryLocation = toBlock(caseFile.crashInfo.location),
            nearestReference = toBlock(caseFile.crashInfo.nearestReference)
        )
    }

    private fun buildUnitSections(caseFile: CaseFile): List<UnitSection> {
        fun yn(v: Boolean?) = when (v) { true -> "Yes"; false -> "No"; null -> "Unknown" }

        return caseFile.units.map { u ->
            when (u) {
                is VehicleUnit -> {
                    val v = caseFile.vehicles.firstOrNull { it.vehicleId == u.vehicleId }
                    val mainFields = buildList<Pair<String, String>> {
                        add("Kind" to "Vehicle")
                        if (v != null) {
                            val ymm = listOf(v.year, v.make, v.model).map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
                            add("Vehicle" to v.label.ifBlank { "Vehicle" })
                            add("Year / Make / Model" to ymm.ifBlank { "—" })
                            add("Color" to v.color.ifBlank { "—" })
                            add("VIN" to v.vin.ifBlank { "—" })
                            add("Weight" to "${v.weightLb?.let { fmt3(it) } ?: "—"} lb")
                            add("Insurance — Company" to v.insurance.company.ifBlank { "—" })
                            add("Insurance — Policy #" to v.insurance.policyNumber.ifBlank { "—" })
                            add("Insurance — Phone" to v.insurance.phone.ifBlank { "—" })
                            add("Vehicle Notes" to v.notes.ifBlank { "—" })
                        }
                    }
                    val occupantSubs = v?.occupants?.mapIndexed { idx, o ->
                        UnitSubsection(
                            title = "Occupant ${idx + 1} — ${o.name.display().takeIf { it.isNotBlank() } ?: "—"}",
                            fields = buildList {
                                add("Seating Position" to o.seatingPosition.ifBlank { "—" })
                                add("Date of Birth" to formatDob(o.dobIso))
                                add("Seatbelt Worn" to yn(o.seatbeltWorn))
                                add("Phone" to o.phone.ifBlank { "—" })
                                add("ID Number" to o.idNumber.ifBlank { "—" })
                                add("ID Class" to o.idClass.ifBlank { "—" })
                                add("ID State" to "—")
                                add("ID Restrictions" to o.idRestrictions.ifBlank { "—" })
                            }
                        )
                    } ?: emptyList()
                    UnitSection(title = u.label.ifBlank { "Vehicle Unit" }, fields = mainFields, subsections = occupantSubs)
                }
                is PedestrianUnit -> {
                    val fields = listOf(
                        "Kind" to "Pedestrian",
                        "Name" to (u.name.display().takeIf { it.isNotBlank() } ?: "—"),
                        "Date of Birth" to formatDob(u.dobIso),
                        "Address" to u.address.ifBlank { "—" },
                        "Phone" to u.phone.ifBlank { "—" }
                    )
                    UnitSection(title = u.label.ifBlank { "Pedestrian" }, fields = fields, subsections = emptyList())
                }
            }
        }
    }

    private fun buildCalcSections(caseFile: CaseFile, showFullWork: Boolean): List<CalcSection> {
        val sections = mutableListOf<CalcSection>()
        caseFile.units.forEach { unit ->
            val calcs = caseFile.calculations.filter { it.attributedUnitIds.contains(unit.unitId) }.sortedBy { it.createdAtEpochMs }
            if (calcs.isNotEmpty()) sections += CalcSection(unit.label.ifBlank { unit.kind.name }, calcs.map { toCalcBlock(it, showFullWork) })
        }
        val unassigned = caseFile.calculations.filter { it.attributedUnitIds.isEmpty() && it.attributedVehicleIds.isEmpty() }.sortedBy { it.createdAtEpochMs }
        if (unassigned.isNotEmpty()) sections += CalcSection("Unassigned", unassigned.map { toCalcBlock(it, showFullWork) })
        return sections
    }

    private fun toCalcBlock(c: SavedCalculation, showFullWork: Boolean) = CalcBlock(
        title = c.title,
        calcType = c.type,
        whenLine = "Created: ${formatEpoch(c.createdAtEpochMs)}",
        formulaExplanation = formulaExplanation(c.type),
        outputs = c.outputs.map { "${it.name}: ${fmt3(it.value)} ${it.unit}".trim() }.ifEmpty { listOf("—") },
        inputs = c.inputs.map { "${it.name}: ${fmt3(it.value)} ${it.unit}".trim() }.ifEmpty { listOf("—") },
        work = if (!showFullWork) emptyList() else c.steps.ifEmpty { listOf("—") },
        calcNotes = c.notes.takeIf { it.isNotBlank() }
    )

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

    // ---- Formula explanations ----

    private fun formulaExplanation(type: CalcType): List<String> = when (type) {
        CalcType.COMBINED_SPEED -> listOf(
            "Combined Speed Formula:  S = √(S1² + S2² + S3² + ...)",
            "Used to calculate total pre-impact speed from multiple post-impact",
            "speed components (skid, yaw, vault, etc.). Each component is squared,",
            "summed, then square-rooted."
        )
        CalcType.SPEED_TIME_DISTANCE,
        CalcType.UNIT_CONVERTER,
        CalcType.UNIT_TOOL -> emptyList()
        CalcType.MOMENTUM_REAR_END_SOLVE_S1 -> listOf(
            "Collinear Momentum — Rear End (solving S1):",
            "  W1·S1 + W2·S2 = W1·S1' + W2·S2'",
            "  S1 = S1' + (W2·S2')/W1 − (W2·S2)/W1",
            "W = weight (lb), S = pre-impact speed, S' = post-impact speed (mph).",
            "Conservation of momentum: total momentum before = total momentum after."
        )
        CalcType.MOMENTUM_REAR_END_SOLVE_S2 -> listOf(
            "Collinear Momentum — Rear End (solving S2):",
            "  W1·S1 + W2·S2 = W1·S1' + W2·S2'",
            "  S2 = (W1·S1' + W2·S2' − W1·S1) / W2"
        )
        CalcType.MOMENTUM_HEAD_ON_SOLVE_S1 -> listOf(
            "Collinear Momentum — Head On (solving S1):",
            "  S1 = S1' + (W2·S2')/W1 + (W2·S2)/W1",
            "For head-on collisions S2 momentum is added (opposing directions)."
        )
        CalcType.MOMENTUM_HEAD_ON_SOLVE_S2 -> listOf(
            "Collinear Momentum — Head On (solving S2):",
            "  S2 = (W1·S1' + W2·S2' + W1·S1) / W2"
        )
        CalcType.MOMENTUM_GENERAL_SOLVE_S1,
        CalcType.MOMENTUM_GENERAL_SOLVE_S2 -> listOf(
            "Collinear Momentum — General:",
            "  W1·S1 + W2·S2 = W1·S1' + W2·S2'",
            "Solved algebraically for the unknown pre-impact speed."
        )
        CalcType.MOMENTUM_360 -> listOf(
            "360° Method of Computing Momentum:",
            "  X (cosine) equation — solves S1:",
            "    S1 = S1'·cosθ1' + (W2·S2'·cosθ2')/W1 − (W2·S2·cosθ2)/W1",
            "  Y (sine) equation — solves S2:",
            "    S2 = (W1·S1'·sinθ1' + W2·S2'·sinθ2') / (W2·sinθ2)",
            "  Angle convention: 0°=right, 90°=down(−Y), 180°=left, 270°=up.",
            "  ΔV = √(S² + S'² − 2·S·S'·cos θ_included)",
            "    θ_included = min angular difference between pre and post headings.",
            "  PDOF = arcsin(S'·sin θ_included / ΔV)",
            "    Direction from which crash force was applied to each unit."
        )
        CalcType.TIRE_SPEED_CORRECTION -> listOf(
            "Tire Size Speed Correction:",
            "  Corrected Speed = Indicated Speed × (Actual Radius / Stock Radius)",
            "Used when non-stock tires affect speedometer calibration."
        )
    }

    // ---- DOCX writer ----

    private fun writeDocx(payload: Payload, out: OutputStream) {
        val doc = XWPFDocument()

        fun para(text: String = "", bold: Boolean = false, size: Int? = null,
                 align: ParagraphAlignment? = null, italic: Boolean = false) {
            val p = doc.createParagraph()
            if (align != null) p.alignment = align
            val r = p.createRun()
            r.isBold = bold; r.isItalic = italic
            if (size != null) r.fontSize = size
            if (text.isNotEmpty()) r.setText(text)
        }

        fun spacer(lines: Int = 1) = repeat(lines) { para("") }

        fun pageBreak() {
            val p = doc.createParagraph()
            p.createRun().addBreak(BreakType.PAGE)
        }

        fun sectionTitle(text: String) {
            val p = doc.createParagraph()
            val r = p.createRun()
            r.isBold = true; r.fontSize = 15
            r.setUnderline(UnderlinePatterns.SINGLE)
            r.setText(text)
        }

        fun subsectionTitle(text: String) {
            val p = doc.createParagraph()
            val r = p.createRun()
            r.isBold = true; r.fontSize = 12
            r.setText(text)
        }

        fun fieldRow(label: String, value: String) {
            val p = doc.createParagraph()
            val rLabel = p.createRun(); rLabel.isBold = true; rLabel.fontSize = 11
            rLabel.setText("$label: ")
            val rValue = p.createRun(); rValue.fontSize = 11
            rValue.setText(value)
        }

        fun twoColFields(left: Pair<String, String>, right: Pair<String, String>) {
            val table = doc.createTable(1, 2)
            table.setWidth("100%")
            fun fill(cell: XWPFTableCell, label: String, value: String) {
                cell.removeBorders()
                val p = cell.paragraphs[0]
                val rL = p.createRun(); rL.isBold = true; rL.fontSize = 11; rL.setText("$label: ")
                val rV = p.createRun(); rV.fontSize = 11; rV.setText(value)
            }
            fill(table.getRow(0).getCell(0), left.first, left.second)
            fill(table.getRow(0).getCell(1), right.first, right.second)
        }

        fun locationBlock(loc: LocationBlock) {
            when (loc) {
                is LocationBlock.Unspecified -> fieldRow("Location", "—")
                is LocationBlock.Intersection -> {
                    twoColFields("Street 1" to loc.street1, "Street 2" to loc.street2)
                    twoColFields("City" to loc.city, "State" to loc.state)
                    twoColFields("Zip" to loc.zip, "Speed Limit" to "${loc.speedLimit} mph")
                }
                is LocationBlock.NonIntersection -> {
                    twoColFields("Block #" to loc.blockNumber, "Street" to loc.streetName)
                    twoColFields("City" to loc.city, "State" to loc.state)
                    twoColFields("Zip" to loc.zip, "Speed Limit" to "${loc.speedLimit} mph")
                }
            }
        }

        fun bulletLine(text: String) = para("• $text", size = 11)

        // ---- COVER PAGE ----
        spacer(3)
        para(payload.agencyName, bold = true, size = 18, align = ParagraphAlignment.CENTER)
        spacer(1)
        para(payload.reportTitle, bold = true, size = 24, align = ParagraphAlignment.CENTER)
        spacer(2)
        para("Service #: ${payload.serviceNumber}", bold = true, size = 14, align = ParagraphAlignment.CENTER)
        para("Report Date: ${payload.reportDateIso}", size = 12, align = ParagraphAlignment.CENTER)
        if (payload.preparedBy.isNotBlank()) para("Prepared By: ${payload.preparedBy}", size = 12, align = ParagraphAlignment.CENTER)
        if (payload.reviewedBy.isNotBlank()) para("Reviewed By: ${payload.reviewedBy}", size = 12, align = ParagraphAlignment.CENTER)
        para(payload.generatedLine, size = 10, align = ParagraphAlignment.CENTER)
        spacer(3)
        para(
            "This document is a working collision-reconstruction worksheet generated by CollisionCalc. " +
                    "Values are based on entered assumptions and should be independently verified.",
            size = 11, align = ParagraphAlignment.CENTER, italic = true
        )
        spacer(4)
        para("Prepared By: ________________________________   Date: ____________", size = 11, align = ParagraphAlignment.CENTER)
        spacer(1)
        para("Reviewed By: ________________________________   Date: ____________", size = 11, align = ParagraphAlignment.CENTER)

        // ---- TABLE OF CONTENTS PAGE ----
        pageBreak()
        sectionTitle("Table of Contents")
        spacer(1)
        bulletLine("Crash Information")
        if (payload.unitSections.isNotEmpty()) {
            bulletLine("Unit Information")
            payload.unitSections.forEach { bulletLine("    ${it.title}") }
        }
        if (payload.calcSections.isNotEmpty()) {
            bulletLine("Calculations")
            payload.calcSections.forEach { sec ->
                bulletLine("    ${sec.title} (${sec.calcs.size} calculation${if (sec.calcs.size != 1) "s" else ""})")
            }
        }
        bulletLine("Case Notes")

        // ---- CRASH INFO ----
        pageBreak()
        sectionTitle("Crash Information")
        spacer(1)
        subsectionTitle("Date & Time")
        twoColFields("Date" to payload.crashSection.date, "Time" to payload.crashSection.time)
        spacer(1)
        subsectionTitle("Primary Location")
        locationBlock(payload.crashSection.primaryLocation)
        spacer(1)
        subsectionTitle("Nearest Reference Location")
        locationBlock(payload.crashSection.nearestReference)

        // ---- UNITS ----
        payload.unitSections.forEach { u ->
            pageBreak()
            sectionTitle("Unit Information — ${u.title}")
            spacer(1)
            val fieldPairs = u.fields.chunked(2)
            fieldPairs.forEach { chunk ->
                if (chunk.size == 2) twoColFields(chunk[0], chunk[1])
                else fieldRow(chunk[0].first, chunk[0].second)
            }
            u.subsections.forEach { sub ->
                spacer(1)
                subsectionTitle(sub.title)
                val subPairs = sub.fields.chunked(2)
                subPairs.forEach { chunk ->
                    if (chunk.size == 2) twoColFields(chunk[0], chunk[1])
                    else fieldRow(chunk[0].first, chunk[0].second)
                }
            }
        }

        // ---- CALCULATIONS ----
        if (payload.calcSections.isEmpty()) {
            pageBreak()
            sectionTitle("Calculations")
            spacer(1)
            para("No calculations recorded.", size = 11, italic = true)
        } else {
            payload.calcSections.forEach { sec ->
                pageBreak()
                sectionTitle("Calculations — ${sec.title}")
                sec.calcs.forEachIndexed { idx, c ->
                    spacer(1)
                    subsectionTitle("${idx + 1}. ${c.title}")
                    para(c.whenLine, size = 10, italic = true)
                    spacer(1)
                    para("Results:", bold = true, size = 11)
                    c.outputs.forEach { bulletLine(it) }
                    spacer(1)
                    para("Inputs Used:", bold = true, size = 11)
                    c.inputs.forEach { bulletLine(it) }
                    spacer(1)
                    if (c.formulaExplanation.isNotEmpty()) {
                        para("Formula:", bold = true, size = 11)
                        c.formulaExplanation.forEach { para(it, size = 11, italic = true) }
                        spacer(1)
                    }
                    if (payload.showFullWork && c.work.isNotEmpty()) {
                        para("Work Shown:", bold = true, size = 11)
                        c.work.forEach { para(it, size = 10) }
                        spacer(1)
                    }
                    c.calcNotes?.let {
                        para("Notes:", bold = true, size = 11)
                        para(it, size = 11)
                        spacer(1)
                    }
                }
            }
        }

        // ---- CASE NOTES ----
        pageBreak()
        sectionTitle("Case Notes")
        spacer(1)
        if (payload.caseNotesLines.drop(1).all { it.isBlank() }) {
            para("No case notes recorded.", size = 11, italic = true)
        } else {
            payload.caseNotesLines.drop(1).forEach { ln ->
                if (ln.isBlank()) spacer(1) else para(ln, size = 11)
            }
        }

        doc.write(out)
        doc.close()
    }

    // ---- Cell border removal helper ----

    private fun XWPFTableCell.removeBorders() {
        val tcPr = ctTc.tcPr ?: ctTc.addNewTcPr()
        val borders = tcPr.tcBorders ?: tcPr.addNewTcBorders()
        listOf(
            borders.addNewTop(), borders.addNewBottom(),
            borders.addNewLeft(), borders.addNewRight(),
            borders.addNewInsideH(), borders.addNewInsideV()
        ).forEach { it.`val` = STBorder.NONE }
    }

    // ---- PDF writer ----

    private fun writePdf(payload: Payload, out: OutputStream) {
        val doc = PdfDocument()
        val pageWidth = 612; val pageHeight = 792
        val margin = 40; val lineGap = 16
        val headerTop = 28; val footerBottom = 24

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
        val italicPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val coverTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val coverSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEFEFEF.toInt(); style = Paint.Style.FILL
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB0B0B0.toInt(); strokeWidth = 1f; style = Paint.Style.STROKE
        }

        val contentTop = margin + headerTop
        val contentBottom = pageHeight - margin - footerBottom
        var pageNumber = 1

        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        val centerX = pageWidth / 2f
        var cy = 140f

        fun drawCentered(text: String, p: Paint) {
            canvas.drawText(text, centerX - p.measureText(text) / 2f, cy, p)
            cy += p.textSize + 12f
        }

        drawCentered(payload.agencyName, coverSubPaint)
        drawCentered(payload.reportTitle, coverTitlePaint)
        cy += 8f
        drawCentered("Service #: ${payload.serviceNumber}", coverSubPaint)
        drawCentered("Report Date: ${payload.reportDateIso}", bodyPaint)
        if (payload.preparedBy.isNotBlank()) drawCentered("Prepared By: ${payload.preparedBy}", bodyPaint)
        if (payload.reviewedBy.isNotBlank()) drawCentered("Reviewed By: ${payload.reviewedBy}", bodyPaint)
        drawCentered(payload.generatedLine, bodyPaint)

        var y = 360f

        fun drawWrapped(text: String, paintUse: Paint = bodyPaint, indent: Int = 0) {
            val maxW = pageWidth - margin * 2 - indent
            var line = ""
            for (w in text.split(" ")) {
                val c = if (line.isEmpty()) w else "$line $w"
                if (paintUse.measureText(c) <= maxW) line = c
                else { canvas.drawText(line, (margin + indent).toFloat(), y, paintUse); y += lineGap; line = w }
            }
            if (line.isNotEmpty()) { canvas.drawText(line, (margin + indent).toFloat(), y, paintUse); y += lineGap }
        }

        drawWrapped("This document is a working collision-reconstruction worksheet generated by CollisionCalc. Values are based on entered assumptions and should be independently verified.")
        y = (pageHeight - 220).toFloat()
        fun sigLine(label: String) {
            canvas.drawText(label, margin.toFloat(), y, bodyPaint); y += 18f
            canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, dividerPaint); y += 24f
        }
        sigLine("Prepared By"); sigLine("Reviewed By")
        doc.finishPage(page)

        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = contentTop.toFloat()

        fun drawHeaderFooter() {
            val hY1 = (margin - 14).coerceAtLeast(12).toFloat()
            val hY2 = (margin - 2).coerceAtLeast(22).toFloat()
            canvas.drawText(payload.agencyName, margin.toFloat(), hY1, smallPaint)
            val r1 = "Service #: ${payload.serviceNumber}"
            canvas.drawText(r1, (pageWidth - margin - smallPaint.measureText(r1)).toFloat(), hY1, smallPaint)
            canvas.drawText(payload.reportTitle, margin.toFloat(), hY2, smallPaint)
            canvas.drawLine(margin.toFloat(), (margin + 2).toFloat(), (pageWidth - margin).toFloat(), (margin + 2).toFloat(), dividerPaint)
            val fY = (pageHeight - margin + 10).toFloat()
            canvas.drawText("CollisionCalc Export", margin.toFloat(), fY, smallPaint)
            val rF = "Page $pageNumber"
            canvas.drawText(rF, (pageWidth - margin - smallPaint.measureText(rF)).toFloat(), fY, smallPaint)
            canvas.drawLine(margin.toFloat(), (pageHeight - margin - footerBottom).toFloat(), (pageWidth - margin).toFloat(), (pageHeight - margin - footerBottom).toFloat(), dividerPaint)
        }

        fun newPage() {
            doc.finishPage(page); pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas; y = contentTop.toFloat()
            drawHeaderFooter()
        }

        fun ensureSpace(n: Int) { if (y + n * lineGap > contentBottom) newPage() }

        fun drawWrappedContent(text: String, paintUse: Paint = bodyPaint, indent: Int = 0) {
            val maxW = pageWidth - margin * 2 - indent
            var line = ""
            for (w in text.split(" ")) {
                val c = if (line.isEmpty()) w else "$line $w"
                if (paintUse.measureText(c) <= maxW) line = c
                else { ensureSpace(1); canvas.drawText(line, (margin + indent).toFloat(), y, paintUse); y += lineGap; line = w }
            }
            if (line.isNotEmpty()) { ensureSpace(1); canvas.drawText(line, (margin + indent).toFloat(), y, paintUse); y += lineGap }
        }

        fun sectionHeader(text: String) {
            ensureSpace(3)
            canvas.drawRect(margin.toFloat(), (y - 12).toFloat(), (pageWidth - margin).toFloat(), (y + 10).toFloat(), sectionBandPaint)
            canvas.drawText(text, margin.toFloat(), y, sectionTitlePaint)
            y += lineGap + 6
        }

        fun twoColRow(label1: String, val1: String, label2: String, val2: String) {
            ensureSpace(1)
            val colW = (pageWidth - margin * 2) / 2f
            val l1 = "$label1: "; canvas.drawText(l1, margin.toFloat(), y, boldBodyPaint)
            canvas.drawText(val1, margin + boldBodyPaint.measureText(l1), y, bodyPaint)
            val l2 = "$label2: "; canvas.drawText(l2, margin + colW, y, boldBodyPaint)
            canvas.drawText(val2, margin + colW + boldBodyPaint.measureText(l2), y, bodyPaint)
            y += lineGap
        }

        fun pdfLocation(loc: LocationBlock) {
            when (loc) {
                is LocationBlock.Unspecified -> drawWrappedContent("—", indent = 16)
                is LocationBlock.Intersection -> {
                    twoColRow("Street 1", loc.street1, "Street 2", loc.street2)
                    twoColRow("City", loc.city, "State", loc.state)
                    twoColRow("Zip", loc.zip, "Speed Limit", "${loc.speedLimit} mph")
                }
                is LocationBlock.NonIntersection -> {
                    twoColRow("Block #", loc.blockNumber, "Street", loc.streetName)
                    twoColRow("City", loc.city, "State", loc.state)
                    twoColRow("Zip", loc.zip, "Speed Limit", "${loc.speedLimit} mph")
                }
            }
        }

        // TOC
        drawHeaderFooter()
        sectionHeader("Table of Contents")
        drawWrappedContent("• Crash Information")
        if (payload.unitSections.isNotEmpty()) {
            drawWrappedContent("• Unit Information")
            payload.unitSections.forEach { drawWrappedContent("    ${it.title}", indent = 16) }
        }
        if (payload.calcSections.isNotEmpty()) {
            drawWrappedContent("• Calculations")
            payload.calcSections.forEach { sec ->
                drawWrappedContent("    ${sec.title} (${sec.calcs.size} calculation${if (sec.calcs.size != 1) "s" else ""})", indent = 16)
            }
        }
        drawWrappedContent("• Case Notes")

        // Crash Info
        newPage()
        sectionHeader("Crash Information")
        y += 4
        twoColRow("Date", payload.crashSection.date, "Time", payload.crashSection.time)
        y += 8
        drawWrappedContent("Primary Location", boldBodyPaint)
        pdfLocation(payload.crashSection.primaryLocation)
        y += 8
        drawWrappedContent("Nearest Reference Location", boldBodyPaint)
        pdfLocation(payload.crashSection.nearestReference)

        // Units
        payload.unitSections.forEach { u ->
            newPage()
            sectionHeader("Unit Information — ${u.title}")
            u.fields.forEach { f -> drawWrappedContent("${f.first}: ${f.second}") }
            u.subsections.forEach { sub ->
                y += 8
                drawWrappedContent(sub.title, boldBodyPaint)
                sub.fields.forEach { f -> drawWrappedContent("${f.first}: ${f.second}", indent = 16) }
            }
        }

        // Calculations
        if (payload.calcSections.isEmpty()) {
            newPage(); sectionHeader("Calculations")
            drawWrappedContent("No calculations recorded.", italicPaint)
        } else {
            payload.calcSections.forEach { sec ->
                newPage()
                sectionHeader("Calculations — ${sec.title}")
                sec.calcs.forEachIndexed { idx, c ->
                    ensureSpace(4)
                    drawWrappedContent("${idx + 1}. ${c.title}", boldBodyPaint)
                    drawWrappedContent(c.whenLine, italicPaint, indent = 16)
                    y += 4
                    drawWrappedContent("Results:", boldBodyPaint, indent = 16)
                    c.outputs.forEach { drawWrappedContent("• $it", indent = 32) }
                    drawWrappedContent("Inputs Used:", boldBodyPaint, indent = 16)
                    c.inputs.forEach { drawWrappedContent("• $it", indent = 32) }
                    if (c.formulaExplanation.isNotEmpty()) {
                        drawWrappedContent("Formula:", boldBodyPaint, indent = 16)
                        c.formulaExplanation.forEach { drawWrappedContent(it, italicPaint, indent = 32) }
                    }
                    if (payload.showFullWork && c.work.isNotEmpty()) {
                        drawWrappedContent("Work Shown:", boldBodyPaint, indent = 16)
                        c.work.forEach { drawWrappedContent(it, indent = 32) }
                    }
                    c.calcNotes?.let {
                        drawWrappedContent("Notes:", boldBodyPaint, indent = 16)
                        drawWrappedContent(it, indent = 32)
                    }
                    y += 8
                }
            }
        }

        // Case Notes
        newPage(); sectionHeader("Case Notes")
        if (payload.caseNotesLines.drop(1).all { it.isBlank() }) {
            drawWrappedContent("No case notes recorded.", italicPaint)
        } else {
            payload.caseNotesLines.drop(1).forEach { ln ->
                if (ln.isBlank()) y += 6 else drawWrappedContent(ln)
            }
        }

        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    // ---- Helpers ----

    private fun formatDob(iso: String): String {
        if (iso.isBlank()) return "—"
        return runCatching {
            val parts = iso.split("-")
            if (parts.size == 3) "${parts[1]}/${parts[2]}/${parts[0]}" else iso
        }.getOrElse { iso }
    }

    private fun formatEpoch(epochMs: Long): String =
        dtf.format(Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDateTime())

    private fun fmt3(x: Double): String {
        if (!x.isFinite()) return "—"
        return "%.3f".format(abs(x)).trimEnd('0').trimEnd('.')
    }
}