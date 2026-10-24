package com.focusverse.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Gold = Color(0xFFD4AF37)
val Green = Color(0xFF4ADE80)

private val DarkScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = Color(0xFF3A3320),
    onPrimaryContainer = Color(0xFFF3E3A6),
    secondary = Green,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3A3320),
    onSecondaryContainer = Color(0xFFF3E3A6),
    background = Color(0xFF0D0D0F),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF17171A),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFFB4B4BC),
    outline = Color(0xFF55555E),
    error = Color(0xFFFF6B6B),
    errorContainer = Color(0xFF4A1F1F),
    onErrorContainer = Color(0xFFFFD9D9)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF8A6D00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E7A8),
    onPrimaryContainer = Color(0xFF2A2000),
    secondary = Color(0xFF1B8A4B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E7A8),
    onSecondaryContainer = Color(0xFF2A2000),
    background = Color(0xFFF7F5EF),
    onBackground = Color(0xFF1B1B1D),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1D),
    surfaceVariant = Color(0xFFECE9DF),
    onSurfaceVariant = Color(0xFF55555E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun FocusTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                label = { Text(o) }
            )
        }
    }
}

fun fmtClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%02d:%02d", m, sec)
}
