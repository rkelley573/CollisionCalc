package com.collisioncalc.app.ui.caseworkbook

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.ui.export.CaseExporter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTabContent(
    caseFile: CaseFile,
    onBeforeExport: () -> Unit
) {
    val context = LocalContext.current

    var agency by rememberSaveable { mutableStateOf("Grand Prairie Police Department") }
    var preparedBy by rememberSaveable { mutableStateOf("") }
    var reviewedBy by rememberSaveable { mutableStateOf("") }
    var reportDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) } // YYYY-MM-DD
    var showFullWork by rememberSaveable { mutableStateOf(true) }
    var status by remember { mutableStateOf<String?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val meta = CaseExporter.ExportMeta(
            agencyName = agency,
            reportTitle = "CollisionCalc Case Report",
            preparedBy = preparedBy,
            reviewedBy = reviewedBy,
            reportDateIso = reportDateIso,
            showFullWork = showFullWork
        )

        runCatching {
            onBeforeExport()
            CaseExporter.exportPdf(context, caseFile, uri, meta)
        }.onSuccess {
            status = "PDF exported."
        }.onFailure { e ->
            status = "PDF export failed: ${e.message ?: "Unknown error"}"
        }
    }

    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val meta = CaseExporter.ExportMeta(
            agencyName = agency,
            reportTitle = "CollisionCalc Case Report",
            preparedBy = preparedBy,
            reviewedBy = reviewedBy,
            reportDateIso = reportDateIso,
            showFullWork = showFullWork
        )

        runCatching {
            onBeforeExport()
            CaseExporter.exportDocx(context, caseFile, uri, meta)
        }.onSuccess {
            status = "Word (.docx) exported."
        }.onFailure { e ->
            status = "Word export failed: ${e.message ?: "Unknown error"}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Export", style = MaterialTheme.typography.titleLarge)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = agency,
                    onValueChange = { agency = it },
                    label = { Text("Agency") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = preparedBy,
                    onValueChange = { preparedBy = it },
                    label = { Text("Prepared by") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reviewedBy,
                    onValueChange = { reviewedBy = it },
                    label = { Text("Reviewed by") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reportDateIso,
                    onValueChange = { reportDateIso = it },
                    label = { Text("Report date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Include work shown", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showFullWork, onCheckedChange = { showFullWork = it })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { docxLauncher.launch("CollisionCalc_${caseFile.serviceNumber}.docx") },
                modifier = Modifier.weight(1f)
            ) { Text("Export Word") }

            Button(
                onClick = { pdfLauncher.launch("CollisionCalc_${caseFile.serviceNumber}.pdf") },
                modifier = Modifier.weight(1f)
            ) { Text("Export PDF") }
        }

        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
