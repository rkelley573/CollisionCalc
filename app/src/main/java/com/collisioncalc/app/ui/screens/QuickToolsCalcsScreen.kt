package com.collisioncalc.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CalcId
import com.collisioncalc.app.data.CaseFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickToolsCalcsScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,
    onOpenCalc: (CalcId) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Tools — Calculations") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        if (caseFile.calculations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("No saved calculations yet.")
                Text("Run a Quick Tool and hit Save.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = caseFile.calculations.sortedByDescending { it.createdAtEpochMs },
                    key = { it.calcId }
                )
                { c ->
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        onClick = { onOpenCalc(c.calcId) }
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(c.title)
                            Text(
                                text = c.outputs.take(2).joinToString(" • ") { o ->
                                    "${o.name} = ${"%.3f".format(o.value)} ${o.unit}".trim()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
