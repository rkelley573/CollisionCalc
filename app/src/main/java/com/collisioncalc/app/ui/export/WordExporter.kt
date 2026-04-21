package com.collisioncalc.app.ui.export

import android.content.Context
import android.os.Environment
import com.collisioncalc.app.data.CaseFile
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream

object WordExporter {

    /**
     * Creates: <serviceNumber>.docx (plain)
     * Stored in: context.getExternalFilesDir(DIRECTORY_DOCUMENTS)/exports/
     */
    fun exportCaseDocx(context: Context, caseFile: CaseFile): File {
        val payload = buildExportPayload(caseFile)

        val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
        outDir.mkdirs()

        val safeName = sanitizeFileName(caseFile.serviceNumber.ifBlank { "CASE" })
        val outFile = File(outDir, "$safeName.docx")

        val doc = XWPFDocument()

        // ---- Helpers ----
        fun addTitle(text: String) {
            val p = doc.createParagraph().apply {
                alignment = ParagraphAlignment.LEFT
            }
            val r = p.createRun().apply {
                isBold = true
                fontSize = 16
                setText(text)
            }
            doc.createParagraph() // spacer
        }

        fun addHeading(text: String) {
            val p = doc.createParagraph()
            val r = p.createRun().apply {
                isBold = true
                fontSize = 13
                setText(text)
            }
        }

        fun addLine(text: String) {
            val p = doc.createParagraph()
            p.createRun().apply {
                fontSize = 11
                setText(text)
            }
        }

        fun addBullet(text: String) {
            // Simple "• " bullet (Word-friendly, no numbering complexity)
            addLine("• $text")
        }

        fun addSpacer() {
            doc.createParagraph()
        }

        // ---- Document ----
        addTitle("CASE INFORMATION")
        payload.caseHeaderLines.forEach { addLine(it) }

        addSpacer()
        addLine("────────────────────────")
        addSpacer()

        // Units blocks
        payload.unitBlocks.forEach { ub ->
            addHeading("UNIT: ${ub.title}")
            addLine("Type: ${ub.kindLabel}")
            ub.vehicleInfoLines.forEach { addLine(it) }

            addSpacer()

            ub.calculations.forEachIndexed { idx, c ->
                addHeading("CALCULATION ${idx + 1}")
                addLine("${c.title} — ${epochToLocalLine(c.createdAtEpochMs)}")

                if (c.equationText.isNotBlank()) addLine("Equation: ${c.equationText}")

                addSpacer()

                if (c.outputs.isNotEmpty()) {
                    addHeading("Result")
                    c.outputs.forEach { v ->
                        addBullet("${v.name}: ${format3(v.value)} ${v.unit}".trim())
                    }
                    addSpacer()
                }

                if (c.inputs.isNotEmpty()) {
                    addHeading("Inputs")
                    c.inputs.forEach { v ->
                        addBullet("${v.name}: ${format3(v.value)} ${v.unit}".trim())
                    }
                    addSpacer()
                }

                addHeading("Work Shown")
                if (c.steps.isEmpty()) {
                    addLine("—")
                } else {
                    c.steps.forEach { line -> addLine(line) }
                }

                if (c.notes.isNotBlank()) {
                    addSpacer()
                    addHeading("Calc Notes")
                    addLine(c.notes)
                }

                addSpacer()
            }

            addLine("────────────────────────")
            addSpacer()
        }

        // Unassigned
        if (payload.unassignedCalculations.isNotEmpty()) {
            addHeading("UNASSIGNED CALCULATIONS")
            addSpacer()

            payload.unassignedCalculations.forEachIndexed { idx, c ->
                addHeading("CALCULATION ${idx + 1}")
                addLine("${c.title} — ${epochToLocalLine(c.createdAtEpochMs)}")
                if (c.equationText.isNotBlank()) addLine("Equation: ${c.equationText}")

                addSpacer()

                if (c.outputs.isNotEmpty()) {
                    addHeading("Result")
                    c.outputs.forEach { v ->
                        addBullet("${v.name}: ${format3(v.value)} ${v.unit}".trim())
                    }
                    addSpacer()
                }

                if (c.inputs.isNotEmpty()) {
                    addHeading("Inputs")
                    c.inputs.forEach { v ->
                        addBullet("${v.name}: ${format3(v.value)} ${v.unit}".trim())
                    }
                    addSpacer()
                }

                addHeading("Work Shown")
                if (c.steps.isEmpty()) addLine("—") else c.steps.forEach { addLine(it) }

                if (c.notes.isNotBlank()) {
                    addSpacer()
                    addHeading("Calc Notes")
                    addLine(c.notes)
                }

                addSpacer()
            }

            addLine("────────────────────────")
            addSpacer()
        }

        // Case notes
        addHeading("CASE NOTES")
        addSpacer()
        payload.caseNotesLines.forEach { addLine(it) }

        FileOutputStream(outFile).use { fos -> doc.write(fos) }
        doc.close()

        return outFile
    }

    private fun sanitizeFileName(input: String): String {
        // Keep it super safe for Android file systems
        return input
            .trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(60)
            .ifBlank { "CASE" }
    }
}
