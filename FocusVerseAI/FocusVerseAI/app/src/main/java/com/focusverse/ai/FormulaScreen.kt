package com.focusverse.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FormulaScreen() {
    var query by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("All") }
    var savedOnly by remember { mutableStateOf(false) }
    var revision by remember { mutableStateOf(false) }

    val list = Content.formulas.filter { f ->
        (subject == "All" || f.subject == subject) &&
            (!savedOnly || f.name in AppState.bookmarks) &&
            (query.isBlank() ||
                f.name.contains(query, ignoreCase = true) ||
                f.chapter.contains(query, ignoreCase = true) ||
                f.expr.contains(query, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Formula Book", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            placeholder = { Text("Search formula, chapter…") }, singleLine = true
        )
        ChipGroup(listOf("All") + Content.subjects, subject) { subject = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = savedOnly, onCheckedChange = { savedOnly = it })
            Text("  ★ Saved only", modifier = Modifier.weight(1f))
            Switch(checked = revision, onCheckedChange = { revision = it })
            Text("  Quick revision")
        }
        if (revision) {
            Text("Formulas are hidden. Try to recall, then tap a card to reveal.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${list.size} formulas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f).padding(top = 6.dp)) {
            items(list, key = { it.name }) { f ->
                var shown by remember(revision) { mutableStateOf(!revision) }
                Panel(modifier = Modifier.clickable { shown = !shown }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${f.subject} · ${f.chapter}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(f.name, fontWeight = FontWeight.Bold)
                        }
                        val saved = f.name in AppState.bookmarks
                        TextButton(onClick = {
                            AppState.bookmarks = if (saved) AppState.bookmarks - f.name else AppState.bookmarks + f.name
                            AppState.save()
                        }) { Text(if (saved) "★" else "☆", fontSize = 22.sp) }
                    }
                    if (shown) {
                        Text(f.expr, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(f.note, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Tap to reveal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
