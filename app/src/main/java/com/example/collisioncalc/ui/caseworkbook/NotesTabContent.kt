package com.example.collisioncalc.ui.caseworkbook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.CaseFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed


/* ---------------------------
   Hoisted helper (perf)
---------------------------- */

private val notesTimeDf: SimpleDateFormat by lazy {
    SimpleDateFormat("M/d/yy h:mm a", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
}

private fun formatLocalNoteTime(epochMs: Long): String =
    notesTimeDf.format(Date(epochMs))

/* ---------------------------
   NOTES TAB (Lazy)
---------------------------- */

@Composable
fun NotesTabContent(
    caseFile: CaseFile,
    onAddNote: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Notes are append-only and exported with this case.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        item {
            Button(
                onClick = { onAddNote(text); text = "" },
                enabled = text.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add Note") }
        }

        item { HorizontalDivider() }

        if (caseFile.notes.isEmpty()) {
            item { Text("No notes entered.") }
        } else {
            itemsIndexed(
                items = caseFile.notes,
                key = { _, n -> n.createdAtEpochMs }
            ) { idx, n ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Note ${idx + 1} — ${formatLocalNoteTime(n.createdAtEpochMs)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(n.text)
                    }
                }
            }
        }
    }
}
