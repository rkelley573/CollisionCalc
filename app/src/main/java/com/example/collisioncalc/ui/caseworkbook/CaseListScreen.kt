package com.example.collisioncalc.ui.caseworkbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.CaseSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dtf: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    cases: List<CaseSummary>,
    onBack: () -> Unit,
    onNewCase: () -> Unit,
    onOpenCase: (caseId: String) -> Unit,
    onDeleteCase: (caseId: String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<CaseSummary?>(null) }

    val filtered = remember(cases, query) {
        val q = query.trim().lowercase(Locale.US)
        if (q.isBlank()) cases
        else cases.filter { c ->
            c.serviceNumber.lowercase(Locale.US).contains(q) ||
                    c.caseId.lowercase(Locale.US).contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cases") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
                actions = { TextButton(onClick = onNewCase) { Text("+ New") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search (service #)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) { Text("✕") }
                    }
                }
            )

            if (filtered.isEmpty()) {
                Text("No matches.")
            } else {
                filtered.forEach { c ->
                    val lastEdited = dtf.format(Instant.ofEpochMilli(c.lastActivityEpochMs))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCase(c.caseId) }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.serviceNumber, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Last edited: $lastEdited",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(onClick = { pendingDelete = c }) {
                                    Text("Delete")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CountChip("Vehicles", c.vehiclesCount)
                                CountChip("Units", c.unitsCount)
                                CountChip("Calcs", c.calculationsCount)
                                CountChip("Notes", c.notesCount)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete case?") },
            text = { Text("This will permanently delete case “${c.serviceNumber}” and all related data.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCase(c.caseId)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CountChip(label: String, value: Int) {
    AssistChip(
        onClick = { /* no-op */ },
        label = { Text("$label: $value") }
    )
}
