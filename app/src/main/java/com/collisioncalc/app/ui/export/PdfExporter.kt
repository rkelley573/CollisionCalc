package com.collisioncalc.app.ui.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.collisioncalc.app.data.CaseFile
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

object PdfExporter {

    /**
     * Creates: <serviceNumber>.pdf (plain)
     * Stored in: context.getExternalFilesDir(DIRECTORY_DOCUMENTS)/exports/
     */
    fun exportCasePdf(context: Context, caseFile: CaseFile): File {
        val payload = buildExportPayload(caseFile)

        val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
        outDir.mkdirs()

        val safeName = sanitizeFileName(caseFile.serviceNumber.ifBlank { "CASE" })
        val outFile = File(outDir, "$safeName.pdf")

        val doc = PdfDocument()

        val pageWidth = 612   // 8.5" @ 72dpi
        val pageHeight = 792  // 11"  @ 72dpi
        val margin = 48f

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
        }
        val paintBold = Paint(paint).apply { isFakeBoldText = true }
        val paintTitle = Paint(paint).apply { textSize = 16f; isFakeBoldText = true }
        val paintHead = Paint(paint).apply { textSize = 13f; isFakeBoldText = true }

        val lineHeight = ceil(paint.fontSpacing.toDouble()).toFloat()
        val maxTextWidth = pageWidth - margin * 2

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

        fun ensureSpace(lines: Int) {
            val needed = lines * lineHeight
            if (y + needed > pageHeight - margin) newPage()
        }

        fun drawLine(text: String, p: Paint = paint) {
            val wrapped = wrapText(text, p, maxTextWidth)
            ensureSpace(wrapped.size)
            for (w in wrapped) {
                canvas.drawText(w, margin, y, p)
                y += lineHeight
            }
        }

        fun spacer(lines: Int = 1) {
            ensureSpace(lines)
            y += lineHeight * lines
        }

        fun hr() {
            spacer(1)
            ensureSpace(1)
            canvas.drawText("────────────────────────", margin, y, paint)
            y += lineHeight
            spacer(1)
        }

        // ---- Content ----
        drawLine("CASE INFORMATION", paintTitle)
        spacer(1)
        payload.caseHeaderLines.forEach { drawLine(it) }

        hr()

        payload.unitBlocks.forEach { ub ->
            drawLine("UNIT: ${ub.title}", paintHead)
            drawLine("Type: ${ub.kindLabel}", paint)
            ub.vehicleInfoLines.forEach { drawLine(it) }
            spacer(1)

            ub.calculations.forEachIndexed { idx, c ->
                drawLine("CALCULATION ${idx + 1}", paintHead)
                drawLine("${c.title} — ${epochToLocalLine(c.createdAtEpochMs)}")
                if (c.equationText.isNotBlank()) drawLine("Equation: ${c.equationText}")
                spacer(1)

                if (c.outputs.isNotEmpty()) {
                    drawLine("Result", paintBold)
                    c.outputs.forEach { v -> drawLine("• ${v.name}: ${format3(v.value)} ${v.unit}".trim()) }
                    spacer(1)
                }

                if (c.inputs.isNotEmpty()) {
                    drawLine("Inputs", paintBold)
                    c.inputs.forEach { v -> drawLine("• ${v.name}: ${format3(v.value)} ${v.unit}".trim()) }
                    spacer(1)
                }

                drawLine("Work Shown", paintBold)
                if (c.steps.isEmpty()) {
                    drawLine("—")
                } else {
                    c.steps.forEach { drawLine(it) }
                }

                if (c.notes.isNotBlank()) {
                    spacer(1)
                    drawLine("Calc Notes", paintBold)
                    drawLine(c.notes)
                }

                spacer(1)
            }

            hr()
        }

        if (payload.unassignedCalculations.isNotEmpty()) {
            drawLine("UNASSIGNED CALCULATIONS", paintHead)
            spacer(1)

            payload.unassignedCalculations.forEachIndexed { idx, c ->
                drawLine("CALCULATION ${idx + 1}", paintHead)
                drawLine("${c.title} — ${epochToLocalLine(c.createdAtEpochMs)}")
                if (c.equationText.isNotBlank()) drawLine("Equation: ${c.equationText}")
                spacer(1)

                if (c.outputs.isNotEmpty()) {
                    drawLine("Result", paintBold)
                    c.outputs.forEach { v -> drawLine("• ${v.name}: ${format3(v.value)} ${v.unit}".trim()) }
                    spacer(1)
                }

                if (c.inputs.isNotEmpty()) {
                    drawLine("Inputs", paintBold)
                    c.inputs.forEach { v -> drawLine("• ${v.name}: ${format3(v.value)} ${v.unit}".trim()) }
                    spacer(1)
                }

                drawLine("Work Shown", paintBold)
                if (c.steps.isEmpty()) drawLine("—") else c.steps.forEach { drawLine(it) }

                if (c.notes.isNotBlank()) {
                    spacer(1)
                    drawLine("Calc Notes", paintBold)
                    drawLine(c.notes)
                }

                spacer(1)
            }

            hr()
        }

        drawLine("CASE NOTES", paintHead)
        spacer(1)
        payload.caseNotesLines.forEach { drawLine(it) }

        doc.finishPage(page)

        FileOutputStream(outFile).use { fos -> doc.writeTo(fos) }
        doc.close()

        return outFile
    }

    private fun sanitizeFileName(input: String): String {
        return input
            .trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(60)
            .ifBlank { "CASE" }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""

        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                // If one word is too long, hard-break it
                if (paint.measureText(w) > maxWidth) {
                    lines += hardBreakWord(w, paint, maxWidth)
                    current = ""
                } else {
                    current = w
                }
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    private fun hardBreakWord(word: String, paint: Paint, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = start + 1
            while (end <= word.length && paint.measureText(word.substring(start, end)) <= maxWidth) {
                end++
            }
            val sliceEnd = (end - 1).coerceAtLeast(start + 1)
            out += word.substring(start, sliceEnd)
            start = sliceEnd
        }
        return out
    }
}
