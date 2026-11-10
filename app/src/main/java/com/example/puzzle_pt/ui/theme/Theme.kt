package com.example.puzzle_pt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB9C8FF),
    onPrimary = Color(0xFF20315F),
    primaryContainer = Color(0xFF344A80),
    onPrimaryContainer = Color(0xFFDEE5FF),
    secondaryContainer = Color(0xFF3D465B),
    onSecondaryContainer = Color(0xFFE1E5F2),
    tertiaryContainer = Color(0xFF604558),
    onTertiaryContainer = Color(0xFFFFD9E7),
    background = Color(0xFF11141B),
    surface = Color(0xFF191D25),
    surfaceVariant = Color(0xFF2A303D),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF415FAD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF20396F),
    secondaryContainer = Color(0xFFE8ECF5),
    onSecondaryContainer = Color(0xFF34405A),
    tertiaryContainer = Color(0xFFFFE1EC),
    onTertiaryContainer = Color(0xFF683349),
    background = Color(0xFFF8F9FE),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F2F8),
)

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
