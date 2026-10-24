package com.focusverse.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen() {
    if (AppState.focusEnd > 0L) ActiveSession() else SetupSession()
}

@Composable
private fun ActiveSession() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmEnd by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val total = (AppState.focusDur * 60_000L).coerceAtLeast(1L)
    val remaining = (AppState.focusEnd - now).coerceAtLeast(0L)
    val progress = (1f - remaining.toFloat() / total).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Focus session in progress", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(260.dp),
                strokeWidth = 14.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(fmtClock(remaining), fontSize = 52.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        if (AppState.focusGoal.isNotBlank()) {
            Panel {
                Text("Your goal", fontWeight = FontWeight.Bold)
                Text(AppState.focusGoal)
            }
            Spacer(Modifier.height(12.dp))
        }
        Panel {
            Text("Focus Shield", fontWeight = FontWeight.Bold)
            if (AppState.focusEnforced) {
                Text("🛡️ Active: ${AppState.blocked.size} app(s) blocked. If you open one, FocusVerse will bring you back.")
            } else if (AppState.blocked.isNotEmpty()) {
                Text("⚠️ Not enforced: Usage Access was not granted when this session started.")
            } else {
                Text("No apps selected for this session. Timer only.")
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = { confirmEnd = true }) { Text("End session early") }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End this session?") },
            text = { Text("You won't earn XP for an unfinished session, but the minutes you focused still count toward today's goal.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    AppState.abandonFocus()
                }) { Text("End session") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Keep going") } }
        )
    }
}

@Composable
private fun SetupSession() {
    val ctx = LocalContext.current
    val labels = listOf("25 min", "30 min", "45 min", "1 hour", "2 hours", "3 hours", "Custom")
    val values = listOf(25, 30, 45, 60, 120, 180, -1)
    var sel by remember { mutableIntStateOf(0) }
    var custom by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val usageOk = remember(refresh) { hasUsageAccess(ctx) }
    val overlayOk = remember(refresh) { hasOverlayAccess(ctx) }
    val apps = remember { launchableApps(ctx) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val effective = if (values[sel] == -1) (custom.toIntOrNull() ?: 0).coerceIn(0, 720) else values[sel]

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Focus Shield", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        if (AppState.lastResult.isNotBlank()) {
            Panel {
                Text(AppState.lastResult)
                TextButton(onClick = {
                    AppState.lastResult = ""
                    AppState.save()
                }) { Text("Dismiss") }
            }
        }

        Panel {
            Text("Session length", fontWeight = FontWeight.Bold)
            ChipGroup(labels, labels[sel]) { sel = labels.indexOf(it) }
            if (values[sel] == -1) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Minutes (1–720)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Goal for this session", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Finish Laws of Motion problems") },
                singleLine = true
            )
        }

        Panel {
            Text("Apps to block during the session", fontWeight = FontWeight.Bold)
            Text(
                if (AppState.blocked.isEmpty()) "None selected. Everything else on your phone (study apps, music, calls) stays available."
                else "${AppState.blocked.size} app(s) selected. Everything else stays available.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showPicker = true }) { Text("Choose apps") }
                OutlinedButton(onClick = {
                    val installed = apps.map { it.first }.toSet()
                    AppState.blocked = AppState.blocked + Content.distractingApps.filter { it in installed }
                    AppState.save()
                }) { Text("Add popular distractions") }
            }
        }

        Panel {
            Text("Permissions", fontWeight = FontWeight.Bold)
            Text(
                "Blocking needs two permissions that only you can switch on. FocusVerse only checks which app is open " +
                    "during a session. It never reads messages, passwords or screen content. You can turn them off anytime.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (usageOk) "✅ Usage Access" else "⬜ Usage Access", modifier = Modifier.weight(1f))
                if (!usageOk) OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) { Text("Grant") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (overlayOk) "✅ Show over other apps" else "⬜ Show over other apps", modifier = Modifier.weight(1f))
                if (!overlayOk) OutlinedButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                    )
                }) { Text("Grant") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Honest limits: Android does not let apps block just Reels or Shorts inside Instagram or YouTube. " +
                    "Focus Shield blocks the whole app for the session. Some phone brands (Xiaomi, Oppo, Vivo, Realme…) " +
                    "also kill background apps; if blocking stops, allow FocusVerse to run in the background / autostart in phone settings.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            enabled = effective > 0,
            onClick = {
                val enforced = AppState.blocked.isNotEmpty() && usageOk
                AppState.startFocus(effective, goal.trim(), enforced)
                try {
                    ContextCompat.startForegroundService(ctx, Intent(ctx, FocusService::class.java))
                } catch (e: Exception) {
                    // Timer still works from the app screen.
                }
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text(if (effective > 0) "Start $effective-minute session" else "Choose a length", fontSize = 17.sp) }

        // History
        val recent = AppState.sessions.takeLast(10).reversed()
        Panel {
            Text("Recent sessions", fontWeight = FontWeight.Bold)
            if (recent.isEmpty()) Text("No sessions yet. Your first one is waiting.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
            recent.forEach { s ->
                val icon = when (s.status) {
                    "completed" -> "✅"
                    "abandoned" -> "⏹️"
                    else -> "❔"
                }
                Text(
                    "$icon ${fmt.format(Date(s.start))} · ${s.actualMin}/${s.durMin} min · ${s.status}" +
                        (if (s.goal.isNotBlank()) " · ${s.goal}" else ""),
                    fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Start
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Apps to block") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(apps, key = { it.first }) { item ->
                        val pkg = item.first
                        val toggle = {
                            AppState.blocked = if (pkg in AppState.blocked) AppState.blocked - pkg else AppState.blocked + pkg
                            AppState.save()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { toggle() }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = pkg in AppState.blocked, onCheckedChange = { toggle() })
                            Text(item.second)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Done") } }
        )
    }
}
