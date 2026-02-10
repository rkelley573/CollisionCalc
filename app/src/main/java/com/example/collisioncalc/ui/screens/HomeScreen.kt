package com.example.collisioncalc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenUnitConverter: () -> Unit,
    onOpenTireCompare: () -> Unit,
    onOpenCaseWorkbook: () -> Unit,
    onOpenMomentumQuick: () -> Unit,
    onOpenQuickToolsCalcs: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("CollisionCalc") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("Quick Tools", style = MaterialTheme.typography.titleMedium)

            HomeCard(
                title = "Unit Tool",
                subtitle = "Find unknowns • formulas • save results",
                onClick = onOpenUnitConverter
            )

            HomeCard(
                title = "Tire Size Compare",
                subtitle = "Stock vs actual • speed correction",
                onClick = onOpenTireCompare
            )

            HomeCard(
                title = "Momentum Wizard",
                subtitle = "360° method • no case needed",
                onClick = onOpenMomentumQuick
            )

            HomeCard(
                title = "Quick Tools History",
                subtitle = "View saved quick calculations",
                onClick = onOpenQuickToolsCalcs
            )

            Spacer(Modifier.height(8.dp))

            Text("Case Workbook", style = MaterialTheme.typography.titleMedium)

            HomeCard(
                title = "Open Case Workbook",
                subtitle = "Service # • vehicles • notes • saved calcs • export",
                onClick = onOpenCaseWorkbook
            )
        }
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
