package com.focusverse.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(goto: (Int) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val doy = cal.get(Calendar.DAY_OF_YEAR)
    val greet = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val clock = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(now))
    val dateStr = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(now))

    val todayMin = AppState.minutesOnDay(0)
    val goalMin = AppState.dailyGoalMin.coerceAtLeast(1)
    val frac = (todayMin.toFloat() / goalMin).coerceIn(0f, 1f)
    val streak = AppState.streak()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text("$greet, ${AppState.name.ifEmpty { "Student" }}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(clock, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(dateStr, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(Content.quotes[doy % Content.quotes.size], color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Daily goal
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { frac },
                        modifier = Modifier.size(104.dp),
                        strokeWidth = 10.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text("${(frac * 100).toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Today's goal", fontWeight = FontWeight.Bold)
                    Text("$todayMin / $goalMin min")
                    Text("🔥 Streak: $streak day${if (streak == 1) "" else "s"}")
                    Text("⭐ Level ${AppState.level} · ${AppState.xp} XP")
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (AppState.xp % 100) / 100f },
                        modifier = Modifier.width(160.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        Button(onClick = { goto(1) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(if (AppState.focusEnd > 0L) "Resume focus session" else "Start Focus", fontSize = 17.sp)
        }

        // Today's plan
        val subjects = if (AppState.exam == "NEET") listOf("Physics", "Chemistry") else Content.subjects
        val subject = subjects[doy % subjects.size]
        val chapters = Content.formulas.filter { it.subject == subject }.map { it.chapter }.distinct()
        val chapter = chapters[(doy / subjects.size) % chapters.size]
        Panel {
            Text("Today's recommended plan", fontWeight = FontWeight.Bold)
            Text("30 minutes of $chapter ($subject), 15 practice questions, and a 10-minute revision of yesterday's mistakes.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { goto(2) }) { Text("Formula Book") }
                OutlinedButton(onClick = { goto(4) }) { Text("Ask AI Tutor") }
            }
        }

        // Daily challenge
        val done = AppState.completedToday(25)
        Panel {
            Text("Daily challenge", fontWeight = FontWeight.Bold)
            Text(if (done) "✅ Done: you finished a 25+ minute focus session today." else "⬜ Finish one focus session of 25 minutes or more.")
            Spacer(Modifier.height(4.dp))
            Text("Bonus: play Formula Pop for +2 XP per correct answer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Weekly chart
        Panel {
            Text("Last 7 days (minutes studied)", fontWeight = FontWeight.Bold)
            val mins = (6 downTo 0).map { AppState.minutesOnDay(it) }
            val maxM = (mins.maxOrNull() ?: 0).coerceAtLeast(1)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                mins.forEachIndexed { idx, m ->
                    val ago = 6 - idx
                    val label = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(AppState.dayStartAgo(ago)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$m", fontSize = 10.sp)
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height((4f + 60f * m / maxM).dp)
                                .background(
                                    if (ago == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    RoundedCornerShape(6.dp)
                                )
                        )
                        Text(label, fontSize = 10.sp)
                    }
                }
            }
        }

        // Formula of the day
        val f = Content.formulas[doy % Content.formulas.size]
        Panel {
            Text("Formula of the day · ${f.subject}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(f.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(f.expr, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Start)
            Text(f.note, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Quick notes
        Panel {
            Text("Quick notes", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = AppState.notes,
                onValueChange = {
                    AppState.notes = it
                    AppState.save()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Jot down doubts, mistakes, reminders…") },
                minLines = 3
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
