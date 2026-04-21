package com.collisioncalc.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext


private fun appVersionName(context: Context): String {
    return try {
        val pkg = context.packageName
        val info = context.packageManager.getPackageInfo(pkg, 0)
        info.versionName ?: "?"
    } catch (e: PackageManager.NameNotFoundException) {
        "?"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenUnitConverter: () -> Unit,
    onOpenMomentumQuick: () -> Unit,
    onOpenTireCompare: () -> Unit,
    onOpenCaseWorkbook: () -> Unit,
    onOpenQuickToolsCalcs: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("CollisionCalc") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Start here", style = MaterialTheme.typography.titleLarge)

            SectionCard(
                title = "Case Workbook",
                subtitle = "Create and manage case files with autosave and export."
            ) {
                Button(onClick = onOpenCaseWorkbook, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Case Workbook")
                }
            }

            SectionCard(
                title = "Quick Tools",
                subtitle = "Standalone calculators (in-memory) for fast field estimates."
            ) {
                ToolRow("Unit Tools", "Conversions + kinematics tools", onOpenUnitConverter)
                ToolRow("Momentum Wizard", "2D momentum / post-impact speed", onOpenMomentumQuick)
                ToolRow("Tire Size Compare", "Compare tire sizes and error", onOpenTireCompare)

                HorizontalDivider(Modifier.padding(vertical = 6.dp))

                OutlinedButton(onClick = onOpenQuickToolsCalcs, modifier = Modifier.fillMaxWidth()) {
                    Text("View Quick Tool Calculations")
                }
            }

            Spacer(Modifier.height(10.dp))

            val context = LocalContext.current
            val version = appVersionName(context)

            Text(
                text = "v$version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun ToolRow(title: String, subtitle: String, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onOpen) { Text("Open") }
        }
    }
}
