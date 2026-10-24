package com.focusverse.ai

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.roundToInt

@Composable
fun ProfileScreen() {
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val usageOk = remember(refresh) { hasUsageAccess(ctx) }
    val overlayOk = remember(refresh) { hasOverlayAccess(ctx) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile & Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Panel {
            Text("⭐ Level ${AppState.level} · ${AppState.xp} XP", fontWeight = FontWeight.Bold)
            Text("🔥 Streak: ${AppState.streak()} day(s)")
            Text("Sessions completed: ${AppState.sessions.count { it.status == "completed" }}")
        }

        Panel {
            Text("Your details", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = AppState.name,
                onValueChange = {
                    AppState.name = it.take(30)
                    AppState.save()
                },
                label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text("Class", modifier = Modifier.padding(top = 8.dp))
            ChipGroup(
                listOf("Class 9", "Class 10", "Class 11 / 1st PUC", "Class 12 / 2nd PUC", "Other"), AppState.cls
            ) {
                AppState.cls = it
                AppState.save()
            }
            Text("Target exam", modifier = Modifier.padding(top = 8.dp))
            ChipGroup(listOf("KCET", "JEE", "NEET", "Board exams", "Custom"), AppState.exam) {
                AppState.exam = it
                AppState.save()
            }
            Text("Daily goal: ${AppState.dailyGoalMin} min", modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = AppState.dailyGoalMin.toFloat(),
                onValueChange = {
                    AppState.dailyGoalMin = ((it / 30f).roundToInt() * 30).coerceIn(30, 360)
                },
                onValueChangeFinished = { AppState.save() },
                valueRange = 30f..360f, steps = 10
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark theme", modifier = Modifier.weight(1f))
                Switch(checked = AppState.dark, onCheckedChange = {
                    AppState.dark = it
                    AppState.save()
                })
            }
        }

        Panel {
            Text("Permissions", fontWeight = FontWeight.Bold)
            Text(if (usageOk) "✅ Usage Access (detect blocked apps)" else "⬜ Usage Access (detect blocked apps)")
            Text(if (overlayOk) "✅ Show over other apps (interruption screen)" else "⬜ Show over other apps (interruption screen)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text("Usage Access") }
                OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                }) { Text("Overlay") }
            }
            OutlinedButton(
                onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    )
                },
                modifier = Modifier.padding(top = 4.dp)
            ) { Text("App settings (notifications, battery)") }
        }

        Panel {
            Text("AI Tutor", fontWeight = FontWeight.Bold)
            Text(if (AppState.apiKey.isBlank()) "Not connected. Add your key on the Tutor tab." else "Connected with your saved key.")
        }

        Panel {
            Text("What works in this version", fontWeight = FontWeight.Bold)
            Text(
                "✅ Home dashboard, focus timer, Focus Shield (whole-app blocking), session history, XP, streaks\n" +
                    "✅ Formula Book (${Content.formulas.size} formulas, search, bookmarks, quick revision)\n" +
                    "✅ Formula Pop and Memory Match games\n" +
                    "✅ AI Tutor with voice (needs your own API key and internet)\n\n" +
                    "⏳ Not built yet: accounts, friends, private chat, photo sharing, push notifications, " +
                    "full KCET syllabus tracker, mock tests, spaced-repetition calendar, Biology content. " +
                    "These need an online backend (Firebase) and are planned as the next phases.",
                fontSize = 13.sp
            )
        }

        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete all my data and reset")
        }
        Text("FocusVerse AI v0.1.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Delete everything?") },
            text = { Text("This erases your progress, sessions, notes and saved key from this phone. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    AppState.resetAll()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}
