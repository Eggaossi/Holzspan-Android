package de.ossi.holzspan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design "Helles Holz"
val Birke = Color(0xFFF4EBDD)       // Hintergrund
val Ahorn = Color(0xFFFBF6EE)       // Karten
val Maserung = Color(0xFFE2D3BC)    // Linien, Maserung
val Nussbaum = Color(0xFF6B4A2F)    // Hauptfarbe
val Kupfer = Color(0xFFC08A4D)      // Akzent
val Dunkelholz = Color(0xFF3B2A1C)  // Text
val Warnrot = Color(0xFFA8432F)

private val Farben = lightColorScheme(
    primary = Nussbaum,
    onPrimary = Ahorn,
    secondary = Kupfer,
    onSecondary = Dunkelholz,
    background = Birke,
    onBackground = Dunkelholz,
    surface = Ahorn,
    onSurface = Dunkelholz,
    surfaceVariant = Maserung,
    onSurfaceVariant = Nussbaum,
    outline = Maserung,
    secondaryContainer = Color(0xFFEBD6B6),
    onSecondaryContainer = Dunkelholz,
    error = Warnrot,
)

@Composable
fun HolzspanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Farben, content = content)
}
