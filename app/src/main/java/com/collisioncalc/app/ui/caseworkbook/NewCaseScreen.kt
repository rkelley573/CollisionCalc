package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCaseScreen(
    onBack: () -> Unit,
    onCreate: (serviceNumber: String) -> Unit
) {
    var service by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Case") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            OutlinedTextField(
                value = service,
                onValueChange = { service = it; error = null },
                label = { Text("Service #") },
                placeholder = { Text("Example: 25-12345") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                trailingIcon = {
                    if (service.isNotEmpty()) {
                        IconButton(onClick = { service = ""; error = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val s = service.trim()
                    if (s.isEmpty()) { error = "Service # is required."; return@Button }
                    onCreate(s)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Case") }
        }
    }
}
