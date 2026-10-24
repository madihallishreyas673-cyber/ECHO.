package com.focusverse.ai

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GamesScreen() {
    var game by remember { mutableIntStateOf(0) } // 0 menu, 1 pop, 2 match
    var subject by remember { mutableStateOf("All") }
    var level by remember { mutableStateOf("Easy") }
    var runId by remember { mutableIntStateOf(0) }

    when (game) {
        1 -> Column(Modifier.fillMaxSize().padding(12.dp)) {
            OutlinedButton(onClick = { game = 0 }) { Text("← Back to games") }
            FormulaPop(subject, level, key = runId) { game = 0 }
        }
        2 -> Column(Modifier.fillMaxSize().padding(12.dp)) {
            OutlinedButton(onClick = { game = 0 }) { Text("← Back to games") }
            MemoryMatch(subject)
        }
        else -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Focus Games", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Quick brain-training rounds. Earn XP for correct answers. No ads, no gambling, no random rewards.")
            Panel {
                Text("Subject", fontWeight = FontWeight.Bold)
                ChipGroup(listOf("All") + Content.subjects, subject) { subject = it }
                Spacer(Modifier.height(8.dp))
                Text("Difficulty (Formula Pop)", fontWeight = FontWeight.Bold)
                ChipGroup(listOf("Easy", "Medium", "Hard"), level) { level = it }
            }
            Panel {
                Text("🫧 Formula Pop", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("A question appears. Tap the bubble with the right formula before you lose interest. 10 questions per round.")
                Text("Personal best: ${AppState.bestPop}/10", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    runId++
                    game = 1
                }) { Text("Play") }
            }
            Panel {
                Text("🃏 Memory Match", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Match each formula name with its formula. Fewer moves is better.")
                Text(
                    if (AppState.bestMatch > 0) "Personal best: ${AppState.bestMatch} moves" else "No best score yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { game = 2 }) { Text("Play") }
            }
            Text(
                "Coming later: Rapid Recall, Focus Quest, Friend Challenge.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------------------------------------------------------------ Formula Pop

private data class Bubble(val id: Int, val text: String, val correct: Boolean, val x: Float, val y: Float, val speed: Float)
private data class PopFx(val id: Int, val x: Dp, val y: Dp)

@Composable
private fun FormulaPop(subject: String, level: String, key: Int, onExit: () -> Unit) {
    val pool = remember(subject) { Content.formulas.filter { subject == "All" || it.subject == subject } }
    val total = 10
    val questions = remember(key, subject) { pool.shuffled().take(total) }
    val count = when (level) {
        "Easy" -> 3
        "Medium" -> 4
        else -> 5
    }
    val baseSpeed = when (level) {
        "Easy" -> 0.07f
        "Medium" -> 0.11f
        else -> 0.17f
    }

    var qIndex by remember(key) { mutableIntStateOf(0) }
    var score by remember(key) { mutableIntStateOf(0) }
    var finished by remember(key) { mutableStateOf(false) }
    var feedback by remember(key) { mutableStateOf("") }
    var nextId by remember(key) { mutableIntStateOf(1) }
    val bubbles = remember(key) { mutableStateListOf<Bubble>() }
    val pops = remember(key) { mutableStateListOf<PopFx>() }

    fun spawn(q: Fm) {
        bubbles.clear()
        val wrong = pool.filter { it.expr != q.expr }.shuffled().take(count - 1)
        val opts = (wrong + q).shuffled()
        opts.forEachIndexed { i, f ->
            val x = if (opts.size > 1) i.toFloat() / (opts.size - 1) else 0.5f
            bubbles.add(
                Bubble(
                    id = nextId++, text = f.expr, correct = f === q,
                    x = x, y = 1.0f + i * 0.28f,
                    speed = baseSpeed * (0.8f + Random.nextFloat() * 0.5f)
                )
            )
        }
    }

    fun next() {
        if (qIndex + 1 >= questions.size) {
            finished = true
            bubbles.clear()
            if (score > AppState.bestPop) AppState.bestPop = score
            AppState.save()
        } else {
            qIndex++
            spawn(questions[qIndex])
        }
    }

    LaunchedEffect(key) {
        if (questions.isNotEmpty()) spawn(questions[0])
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                if (last != 0L && !finished) {
                    val dt = ((t - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    for (i in bubbles.indices) {
                        val b = bubbles[i]
                        var ny = b.y - b.speed * dt
                        if (ny < -0.3f) ny = 1.1f
                        bubbles[i] = b.copy(y = ny)
                    }
                }
                last = t
            }
        }
    }

    if (questions.isEmpty()) {
        Text("No questions available.")
        return
    }

    if (finished) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Round complete! 🎉", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Score: $score / ${questions.size}", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            Text("+${score * 2} XP earned", color = MaterialTheme.colorScheme.secondary)
            Text("Personal best: ${AppState.bestPop}/10")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onExit) { Text("Back to games") }
        }
        return
    }

    val q = questions[qIndex]
    Column(modifier = Modifier.fillMaxSize()) {
        Panel(modifier = Modifier.padding(top = 8.dp)) {
            Text("Question ${qIndex + 1}/${questions.size} · Score $score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Which is the formula for:", fontSize = 13.sp)
            Text(q.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        if (feedback.isNotBlank()) {
            Text(feedback, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val size = 104.dp
            val w = maxWidth
            val h = maxHeight
            for (b in bubbles) {
                val bx = (w - size) * b.x
                val by = (h - size) * b.y
                Box(
                    modifier = Modifier
                        .offset(x = bx, y = by)
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            if (b.correct) {
                                score++
                                AppState.addXp(2)
                                feedback = "✅ ${q.name}: ${q.expr}"
                                pops.add(PopFx(b.id, bx + size / 2, by + size / 2))
                            } else {
                                feedback = "❌ Not quite. ${b.text} is a different formula. ${q.name}: ${q.expr}. ${q.note}"
                            }
                            next()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        b.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 4,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp)
                    )
                }
            }
            for (p in pops) {
                PopBurst(p.x, p.y) { pops.remove(p) }
            }
        }
    }
}

@Composable
private fun PopBurst(x: Dp, y: Dp, onDone: () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        a.animateTo(1f, tween(450))
        onDone()
    }
    val gold = MaterialTheme.colorScheme.primary
    val ring = (40f + 110f * a.value).dp
    Box(
        modifier = Modifier
            .offset(x = x - ring / 2, y = y - ring / 2)
            .size(ring)
            .border(3.dp, gold.copy(alpha = 1f - a.value), CircleShape)
    )
    for (k in 0 until 8) {
        val ang = k * Math.PI / 4
        val dx = (cos(ang) * 70 * a.value).toFloat().dp
        val dy = (sin(ang) * 70 * a.value).toFloat().dp
        Box(
            modifier = Modifier
                .offset(x = x + dx - 4.dp, y = y + dy - 4.dp)
                .size(8.dp)
                .background(gold.copy(alpha = 1f - a.value), CircleShape)
        )
    }
}

// ------------------------------------------------------------------ Memory Match

private data class MemCard(val id: Int, val pair: Int, val text: String)

@Composable
private fun MemoryMatch(subject: String) {
    var seed by remember { mutableIntStateOf(0) }
    val cards = remember(seed, subject) {
        Content.formulas
            .filter { subject == "All" || it.subject == subject }
            .shuffled().take(6)
            .flatMapIndexed { i, f -> listOf(MemCard(i * 2, i, f.name), MemCard(i * 2 + 1, i, f.expr)) }
            .shuffled()
    }
    val up = remember(seed, subject) { mutableStateListOf<Int>() }
    val matched = remember(seed, subject) { mutableStateListOf<Int>() }
    var moves by remember(seed, subject) { mutableIntStateOf(0) }
    var busy by remember(seed, subject) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val won = matched.size == cards.size / 2 && cards.isNotEmpty()

    fun tap(c: MemCard) {
        if (busy || c.id in up || c.pair in matched) return
        up.add(c.id)
        if (up.size == 2) {
            moves++
            val a = cards.first { it.id == up[0] }
            val b = cards.first { it.id == up[1] }
            if (a.pair == b.pair) {
                matched.add(a.pair)
                up.clear()
                if (matched.size == cards.size / 2) {
                    AppState.addXp(10)
                    if (AppState.bestMatch == 0 || moves < AppState.bestMatch) {
                        AppState.bestMatch = moves
                        AppState.save()
                    }
                }
            } else {
                busy = true
                scope.launch {
                    delay(900)
                    up.clear()
                    busy = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Memory Match · Moves: $moves", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        if (won) {
            Panel {
                Text("🎉 Cleared in $moves moves! +10 XP", fontWeight = FontWeight.Bold)
                Text("Best: ${AppState.bestMatch} moves")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { seed++ }) { Text("Play again") }
            }
            Spacer(Modifier.height(8.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(cards, key = { it.id }) { c ->
                val show = c.id in up || c.pair in matched
                val isDone = c.pair in matched
                Box(
                    modifier = Modifier
                        .height(92.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                            else if (show) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { tap(c) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (show) c.text else "?",
                        fontSize = if (show) 12.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(6.dp),
                        maxLines = 4
                    )
                }
            }
        }
    }
}
