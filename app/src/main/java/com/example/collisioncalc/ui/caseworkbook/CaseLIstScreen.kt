package com.example.collisioncalc.ui.caseworkbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.CaseFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    cases: List<CaseFile>,
    onBack: () -> Unit,
    onNewCase: () -> Unit,
    onOpenCase: (caseId: String) -> Unit
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (cases.isEmpty()) {
                Text("No cases yet.")
                Button(onClick = onNewCase) { Text("Create a Case") }
            } else {
                cases.forEach { c ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCase(c.caseId) }
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(c.serviceNumber, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Vehicles: ${c.vehicles.size} • Calculations: ${c.calculations.size} • Notes: ${c.notes.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
