package com.focusverse.ai

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(this)
        handleIntent(intent)
        setContent { FocusVerseApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val pkg = i?.getStringExtra("interrupt")
        if (pkg != null) {
            AppState.interruptPkg = pkg
            i?.removeExtra("interrupt")
        }
    }
}

@Composable
fun FocusVerseApp() {
    FocusTheme(AppState.dark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!AppState.onboarded) Onboarding() else MainScaffold()
        }
    }
}

@Composable
fun MainScaffold() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            AppState.checkExpiry()
            delay(1000)
        }
    }

    val tabs = listOf(
        "Home" to "🏠", "Focus" to "🎯", "Formulas" to "📐",
        "Games" to "🎮", "Tutor" to "🤖", "Me" to "👤"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(t.second) },
                        label = { Text(t.first, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen { tab = it }
                1 -> FocusScreen()
                2 -> FormulaScreen()
                3 -> GamesScreen()
                4 -> TutorScreen()
                else -> ProfileScreen()
            }
        }
    }

    // Focus Shield interruption screen
    val blockedPkg = AppState.interruptPkg
    if (blockedPkg != null && AppState.focusEnd > 0L) {
        val label = remember(blockedPkg) {
            try {
                val pm = ctx.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(blockedPkg, PackageManager.GET_META_DATA)).toString()
            } catch (e: Exception) {
                blockedPkg
            }
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Stay focused 🎯") },
            text = {
                Text(
                    "$label is blocked during your focus session." +
                        (if (AppState.focusGoal.isNotBlank()) "\n\nYour goal: ${AppState.focusGoal}" else "") +
                        "\n\nYou can do this. Head back to your studies."
                )
            },
            confirmButton = {
                Button(onClick = {
                    AppState.interruptPkg = null
                    tab = 1
                }) { Text("Back to studying") }
            },
            dismissButton = {
                TextButton(onClick = {
                    AppState.abandonFocus()
                    AppState.interruptPkg = null
                }) { Text("End session") }
            }
        )
    }
}

@Composable
fun Onboarding() {
    var name by remember { mutableStateOf("") }
    var cls by remember { mutableStateOf("Class 12 / 2nd PUC") }
    var exam by remember { mutableStateOf("KCET") }
    var goal by remember { mutableFloatStateOf(120f) }
    val goalMin = ((goal / 30f).roundToInt() * 30).coerceIn(30, 360)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("FocusVerse AI", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Gold)
        Text("Make learning rewarding. Make distraction less convenient. Make progress visible.")
        Spacer(Modifier.height(20.dp))
        Panel {
            Text("Your name", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(30) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Shreyas") }
            )
            Spacer(Modifier.height(12.dp))
            Text("Class", fontWeight = FontWeight.Bold)
            ChipGroup(
                listOf("Class 9", "Class 10", "Class 11 / 1st PUC", "Class 12 / 2nd PUC", "Other"), cls
            ) { cls = it }
            Spacer(Modifier.height(12.dp))
            Text("Target exam", fontWeight = FontWeight.Bold)
            ChipGroup(listOf("KCET", "JEE", "NEET", "Board exams", "Custom"), exam) { exam = it }
            Spacer(Modifier.height(12.dp))
            Text("Daily study goal: $goalMin min", fontWeight = FontWeight.Bold)
            Slider(value = goal, onValueChange = { goal = it }, valueRange = 30f..360f, steps = 10)
        }
        Spacer(Modifier.height(16.dp))
        Panel {
            Text("Privacy", fontWeight = FontWeight.Bold)
            Text(
                "Everything stays on this phone. There are no accounts or servers in this version. " +
                    "Focus Shield only checks WHICH app is open during a session you start, and only if you grant " +
                    "Usage Access. It never reads messages, passwords or screen content."
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                AppState.name = name.trim().ifEmpty { "Student" }
                AppState.cls = cls
                AppState.exam = exam
                AppState.dailyGoalMin = goalMin
                AppState.onboarded = true
                AppState.save()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Get started") }
        OutlinedButton(
            onClick = {
                AppState.name = "Guest"
                AppState.onboarded = true
                AppState.save()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Explore in demo mode") }
        Spacer(Modifier.height(24.dp))
    }
}
