package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.collisioncalc.app.data.CrashLocation
import com.collisioncalc.app.util.formatCrashLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationEditor(
    value: CrashLocation,
    onChange: (CrashLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    // Normalize Unspecified -> default editor type (Non-Intersection is your preference)
    val current: CrashLocation = when (value) {
        is CrashLocation.Unspecified -> CrashLocation.NonIntersection()
        else -> value
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Location", style = MaterialTheme.typography.titleMedium)

        val preview = formatCrashLocation(current)
        if (preview.isNotBlank()) {
            Text(preview, style = MaterialTheme.typography.bodySmall)
        }

        val isIntersection = current is CrashLocation.Intersection

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isIntersection,
                onClick = { if (!isIntersection) onChange(CrashLocation.Intersection()) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Intersection") }

            SegmentedButton(
                selected = !isIntersection,
                onClick = { if (isIntersection) onChange(CrashLocation.NonIntersection()) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Non-Intersection") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (current) {
                    is CrashLocation.Intersection -> IntersectionFields(current, onChange)
                    is CrashLocation.NonIntersection -> NonIntersectionFields(current, onChange)
                    is CrashLocation.Unspecified -> Unit // unreachable due to normalization
                }
            }
        }
    }
}

@Composable
private fun IntersectionFields(
    v: CrashLocation.Intersection,
    onChange: (CrashLocation) -> Unit
) {
    OutlinedTextField(
        value = v.street1,
        onValueChange = { onChange(v.copy(street1 = it)) },
        label = { Text("Street 1") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = v.street2,
        onValueChange = { onChange(v.copy(street2 = it)) },
        label = { Text("Street 2") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = v.city,
        onValueChange = { onChange(v.copy(city = it)) },
        label = { Text("City") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = v.state,
            onValueChange = { onChange(v.copy(state = it.trim().uppercase().take(2))) },
            label = { Text("State (2-letter)") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        OutlinedTextField(
            value = v.zip,
            onValueChange = { onChange(v.copy(zip = normalizeZip(it))) },
            label = { Text("Zip") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = v.speedLimitMph,
        onValueChange = { onChange(v.copy(speedLimitMph = it.filter(Char::isDigit))) },
        label = { Text("Speed Limit") },
        suffix = { Text("mph") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun NonIntersectionFields(
    v: CrashLocation.NonIntersection,
    onChange: (CrashLocation) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = v.blockNumber,
            onValueChange = { onChange(v.copy(blockNumber = it.filter(Char::isDigit))) },
            label = { Text("Block #") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        OutlinedTextField(
            value = v.streetName,
            onValueChange = { onChange(v.copy(streetName = it)) },
            label = { Text("Street name") },
            modifier = Modifier.weight(2f),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = v.city,
        onValueChange = { onChange(v.copy(city = it)) },
        label = { Text("City") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = v.state,
            onValueChange = { onChange(v.copy(state = it.trim().uppercase().take(2))) },
            label = { Text("State (2-letter)") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        OutlinedTextField(
            value = v.zip,
            onValueChange = { onChange(v.copy(zip = normalizeZip(it))) },
            label = { Text("Zip") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = v.speedLimitMph,
        onValueChange = { onChange(v.copy(speedLimitMph = it.filter(Char::isDigit))) },
        label = { Text("Speed Limit") },
        suffix = { Text("mph") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun normalizeZip(s: String): String {
    val digits = s.filter { it.isDigit() }
    return when {
        digits.length <= 5 -> digits
        else -> digits.substring(0, 5) + "-" + digits.substring(5, minOf(9, digits.length))
    }
}
