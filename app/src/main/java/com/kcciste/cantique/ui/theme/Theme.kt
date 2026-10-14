package com.kcciste.cantique.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kcciste.cantique.data.Theme

// Identité KCCISTE CANTIQUE : fond blanc, accents dorés, texte bleu nuit.
val Or = Color(0xFFC9A227)
val OrFonce = Color(0xFF9A7B12)
val OrClair = Color(0xFFF7F1E1)
val BleuNuit = Color(0xFF14213D)
val Bleu = BleuNuit
val BleuClair = OrClair
val Rouge = Color(0xFFC62828)
val RougeClair = Color(0xFFFDECEC)
val VertWhatsApp = Color(0xFF1DA851)
val Gris = Color(0xFF5F6472)

private val Clair = lightColorScheme(
    primary = BleuNuit,
    onPrimary = Color.White,
    secondary = Or,
    onSecondary = BleuNuit,
    tertiary = OrFonce,
    primaryContainer = OrClair,
    onPrimaryContainer = BleuNuit,
    secondaryContainer = OrClair,
    onSecondaryContainer = BleuNuit,
    background = Color.White,
    onBackground = BleuNuit,
    surface = Color.White,
    onSurface = BleuNuit,
    surfaceVariant = Color(0xFFF6F5F2),
    onSurfaceVariant = Gris,
    outline = Color(0xFFE2DCCB),
    outlineVariant = Color(0xFFEDE8DA),
    error = Rouge,
)

private val Sombre = darkColorScheme(
    primary = Color(0xFFE6C766),
    onPrimary = Color(0xFF1A1407),
    secondary = Color(0xFFE0B84A),
    onSecondary = Color(0xFF1A1407),
    tertiary = Color(0xFFE0B84A),
    primaryContainer = Color(0xFF2A2412),
    onPrimaryContainer = Color(0xFFF3E6BD),
    secondaryContainer = Color(0xFF2A2412),
    onSecondaryContainer = Color(0xFFF3E6BD),
    background = Color(0xFF0E1320),
    onBackground = Color(0xFFE9E6DE),
    surface = Color(0xFF0E1320),
    onSurface = Color(0xFFE9E6DE),
    surfaceVariant = Color(0xFF1A2133),
    onSurfaceVariant = Color(0xFFB5B9C4),
    outline = Color(0xFF3A3F4F),
    outlineVariant = Color(0xFF2A3040),
    error = Color(0xFFEF9A9A),
)

@Composable
fun KCCisteTheme(theme: Theme = Theme.SYSTEME, content: @Composable () -> Unit) {
    val sombre = when (theme) {
        Theme.SYSTEME -> isSystemInDarkTheme()
        Theme.CLAIR -> false
        Theme.SOMBRE -> true
    }
    MaterialTheme(colorScheme = if (sombre) Sombre else Clair, content = content)
}
