package dev.devfest.agent.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta neutra "fintech" (marca ficticia: FinAgent)
private val Navy = Color(0xFF0B3D91)
private val Teal = Color(0xFF00897B)
private val Amber = Color(0xFFF59E0B)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Amber,
    secondaryContainer = Color(0xFFFFE8C2),
    tertiary = Teal,
    tertiaryContainer = Color(0xFFB2F1E8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002D6E),
    primaryContainer = Color(0xFF1A4BA0),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFFFC56B),
    secondaryContainer = Color(0xFF5C3F00),
    tertiary = Color(0xFF80D5CA),
    tertiaryContainer = Color(0xFF00504A),
)

@Composable
fun FinAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
