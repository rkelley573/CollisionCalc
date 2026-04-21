package com.collisioncalc.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CollisionInfo
import com.collisioncalc.app.util.formatCrashLocation
import com.collisioncalc.app.util.isoToUiDate

@Composable
fun CrashSummaryCard(
    crashInfo: CollisionInfo,
    modifier: Modifier = Modifier
) {
    val dateUi = isoToUiDate(crashInfo.dateIso).ifBlank { "—" }
    val timeUi = crashInfo.time24h.ifBlank { "—" }
    val locUi = formatCrashLocation(crashInfo.location).ifBlank { "—" }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Crash Info", style = MaterialTheme.typography.titleSmall)
            Text("Date: $dateUi   Time: $timeUi", style = MaterialTheme.typography.bodySmall)
            Text("Location: $locUi", style = MaterialTheme.typography.bodySmall)
        }
    }
}