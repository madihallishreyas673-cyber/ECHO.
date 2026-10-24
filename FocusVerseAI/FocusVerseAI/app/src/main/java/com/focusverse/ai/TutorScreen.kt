package com.focusverse.ai

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Talks to the Anthropic Messages API directly using the key YOU paste into the app.
 * Fine for personal use. For a public release the key must live on your own server instead.
 */
object Claude {
    const val MODEL = "claude-sonnet-5-5"

    suspend fun ask(key: String, system: String, msgs: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            val conn = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 20000
                conn.readTimeout = 90000
                conn.doOutput = true
                conn.setRequestProperty("x-api-key", key)
                conn.setRequestProperty("anthropic-version", "2023-06-01")
                conn.setRequestProperty("content-type", "application/json")
                val arr = JSONArray()
                msgs.forEach { arr.put(JSONObject().put("role", it.first).put("content", it.second)) }
                val body = JSONObject()
                    .put("model", MODEL)
                    .put("max_tokens", 1500)
                    .put("system", system)
                    .put("messages", arr)
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val txt = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) {
                    val hint = when (code) {
                        401 -> " (check that your API key is correct)"
                        429 -> " (rate limit or no credit left)"
                        else -> ""
                    }
                    throw RuntimeException("AI error $code$hint: ${txt.take(160)}")
                }
                val content = JSONObject(txt).getJSONArray("content")
                val sb = StringBuilder()
                for (i in 0 until content.length()) {
                    val b = content.getJSONObject(i)
                    if (b.optString("type") == "text") sb.append(b.optString("text"))
                }
                sb.toString().trim().ifEmpty { "(The AI returned an empty answer. Please try again.)" }
            } finally {
                conn.disconnect()
            }
        }
}

@Composable
fun TutorScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf<Pair<String, String>>() }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var keyField by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("English") }
    var rate by remember { mutableFloatStateOf(1.0f) }
    val listState = rememberLazyListState()

    val tts = remember {
        val holder = arrayOfNulls<TextToSpeech>(1)
        val engine = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) holder[0]?.setLanguage(Locale("en", "IN"))
        }
        holder[0] = engine
        engine
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val localeFor = mapOf(
        "English" to Locale("en", "IN"),
        "Hindi" to Locale("hi", "IN"),
        "Kannada" to Locale("kn", "IN")
    )
    val tagFor = mapOf("English" to "en-IN", "Hindi" to "hi-IN", "Kannada" to "kn-IN")

    val stt = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val list = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (r.resultCode == Activity.RESULT_OK && !list.isNullOrEmpty()) input = list[0]
    }

    LaunchedEffect(msgs.size, loading) {
        val n = msgs.size + (if (loading) 1 else 0)
        if (n > 0) listState.animateScrollToItem(n - 1)
    }

    val system = "You are FocusVerse Tutor, a friendly, patient study assistant for a student in India: " +
        "${AppState.cls}, preparing for ${AppState.exam}. Explain simply with relatable examples. " +
        "Solve numerical problems step by step and state units. Create MCQs or practice questions when asked, " +
        "and explain wrong answers kindly. If you are not sure about something, say so honestly. " +
        "Never claim to know actual future exam questions or guarantee ranks. " +
        "Reply in $lang. Use plain text only: no markdown symbols like ** or #. Write math in plain readable text."

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || loading) return
        msgs.add("user" to text)
        input = ""
        loading = true
        val history = msgs.filter { it.first == "user" || it.first == "assistant" }
            .takeLast(20).dropWhile { it.first != "user" }.toList()
        scope.launch {
            try {
                val reply = Claude.ask(AppState.apiKey, system, history)
                msgs.add("assistant" to reply)
            } catch (e: Exception) {
                msgs.add("error" to (e.message ?: "Network error. Check your internet connection."))
            }
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI Tutor", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            TextButton(onClick = { tts.stop() }) { Text("🔇 Stop") }
            TextButton(onClick = { msgs.clear() }) { Text("New chat") }
        }

        if (AppState.apiKey.isBlank()) {
            Panel {
                Text("Connect an AI key to start", fontWeight = FontWeight.Bold)
                Text(
                    "This tutor uses Claude. Paste your own API key from console.anthropic.com. " +
                        "It is saved only on this phone (not encrypted), so use this for personal use only. " +
                        "Without a key, the tutor stays off. Nothing is faked.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = keyField, onValueChange = { keyField = it.trim() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true, label = { Text("API key") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    enabled = keyField.length > 20,
                    onClick = {
                        AppState.apiKey = keyField
                        AppState.save()
                        keyField = ""
                    }
                ) { Text("Save key") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Language:", fontSize = 12.sp)
                ChipGroup(listOf("English", "Hindi", "Kannada"), lang) {
                    lang = it
                    tts.setLanguage(localeFor[it] ?: Locale("en", "IN"))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speech speed", fontSize = 12.sp)
                Slider(
                    value = rate, onValueChange = { rate = it; tts.setSpeechRate(it) },
                    valueRange = 0.5f..1.5f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            }
            if (msgs.isEmpty()) {
                Text(
                    "Try: \"Explain Newton's second law with an example\" or \"Give me 5 MCQs on Chemical Kinetics\". " +
                        "Tap 🎤 to speak. Hindi and Kannada voices depend on what is installed on your phone.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(msgs.size) { i ->
                val m = msgs[i]
                ChatBubble(m.first, m.second) {
                    tts.setLanguage(localeFor[lang] ?: Locale("en", "IN"))
                    tts.setSpeechRate(rate)
                    tts.speak(m.second, TextToSpeech.QUEUE_FLUSH, null, "msg$i")
                }
            }
            if (loading) {
                items(1) { Text("Thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        if (AppState.apiKey.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    try {
                        stt.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, tagFor[lang] ?: "en-IN")
                        )
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Voice input is not available on this phone.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("🎤", fontSize = 24.sp) }
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Ask a question…") },
                    maxLines = 4
                )
                TextButton(enabled = !loading && input.isNotBlank(), onClick = { send() }) { Text("Send") }
            }
            TextButton(onClick = { AppState.apiKey = ""; AppState.save() }) {
                Text("Remove saved key", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ChatBubble(role: String, text: String, onSpeak: () -> Unit) {
    val mine = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = when (role) {
                "user" -> MaterialTheme.colorScheme.primaryContainer
                "error" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text)
                if (role == "assistant") {
                    TextButton(onClick = onSpeak) { Text("🔊 Listen") }
                }
            }
        }
    }
}
