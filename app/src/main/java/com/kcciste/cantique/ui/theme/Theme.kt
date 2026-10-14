package com.kcciste.cantique.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bleu = Color(0xFF0B2A84)
val BleuClair = Color(0xFFE8EEFC)
val Or = Color(0xFFF5B800)
val Rouge = Color(0xFFD32F2F)
val RougeClair = Color(0xFFFDECEC)
val VertWhatsApp = Color(0xFF1DA851)
val Gris = Color(0xFF5F6472)

@Composable
fun KCCisteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Bleu,
            onPrimary = Color.White,
            secondary = Or,
            onSecondary = Color(0xFF1B1B1B),
            error = Rouge,
            surface = Color.White,
            background = Color.White,
        ),
        content = content,
    )
}
