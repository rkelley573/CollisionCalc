package com.example.collisioncalc.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.collisioncalc.util.isValidTime24h

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputField(
    label: String,
    value24h: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isError = value24h.isNotBlank() && !isValidTime24h(value24h)

    OutlinedTextField(
        value = value24h,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text("24-hour HH:MM") },
        isError = isError,
        singleLine = true,
        modifier = modifier
    )
}