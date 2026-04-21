package com.collisioncalc.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.collisioncalc.app.util.isValidUiDate
import com.collisioncalc.app.util.isoToUiDate
import com.collisioncalc.app.util.uiToIsoDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    label: String,
    isoValue: String,
    onIsoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var uiText by remember(isoValue) { mutableStateOf(isoToUiDate(isoValue)) }
    val isError = uiText.isNotBlank() && !isValidUiDate(uiText)

    OutlinedTextField(
        value = uiText,
        onValueChange = { new ->
            uiText = new
            val iso = uiToIsoDate(new)
            if (iso.isNotBlank() || new.isBlank()) onIsoChange(iso)
        },
        label = { Text(label) },
        supportingText = { Text("MM-DD-YYYY") },
        isError = isError,
        singleLine = true,
        modifier = modifier
    )
}