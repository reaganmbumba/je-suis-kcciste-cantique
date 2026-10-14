package com.kcciste.cantique.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.audio.Lecteur
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.Gris
import kotlinx.coroutines.delay
import java.io.File

/** ▶ ÉCOUTER : lecteur réel avec barre de progression. */
@Composable
fun LecteurAudio(fichier: File, dureeConnueMs: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lecteur = remember { Lecteur() }
    var enLecture by remember { mutableStateOf(false) }
    var position by remember { mutableFloatStateOf(0f) }

    DisposableEffect(fichier) {
        onDispose { lecteur.arreter() }
    }
    LaunchedEffect(enLecture) {
        while (enLecture) {
            position = lecteur.positionMs.toFloat()
            delay(200)
        }
    }

    val total = (lecteur.dureeMs.takeIf { it > 0 }?.toLong() ?: dureeConnueMs).coerceAtLeast(1)

    Column(modifier.fillMaxWidth()) {
        Slider(
            value = position.coerceIn(0f, total.toFloat()),
            onValueChange = { position = it; lecteur.allerA(it.toInt()) },
            valueRange = 0f..total.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Bleu, activeTrackColor = Bleu),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Formats.duree(position.toLong()), color = Gris, fontSize = 12.sp)
            Row(Modifier.weight(1f), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                TextButton(onClick = {
                    if (enLecture) {
                        lecteur.pause()
                        enLecture = false
                    } else {
                        val ok = lecteur.lire(fichier) {
                            enLecture = false
                            position = 0f
                        }
                        if (ok) {
                            if (position > 0) lecteur.allerA(position.toInt())
                            enLecture = true
                        } else {
                            Toast.makeText(context, "Impossible de lire ce fichier audio.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text(if (enLecture) "⏸ PAUSE" else "▶ ÉCOUTER", fontWeight = FontWeight.Bold, color = Bleu)
                }
            }
            Text(Formats.duree(total), color = Gris, fontSize = 12.sp)
        }
    }
}
