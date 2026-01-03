package com.vidora.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryColor = Color(0xFF00FF9D)
private val BackgroundColor = Color(0xFF0F0F0F)
private val SurfaceColor = Color(0xFF1A1A1A)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = Color(0xFF00CC7D),
    background = BackgroundColor,
    surface = SurfaceColor,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun VidoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
