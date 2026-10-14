package com.kcciste.cantique.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.audio.LecteurCantiques
import com.kcciste.cantique.ui.theme.Or

/** Barre de lecture compacte, visible en bas des écrans tant qu'un audio est chargé. */
@Composable
fun MiniLecteur(ouvrir: () -> Unit) {
    val etat by LecteurCantiques.etat.collectAsState()
    val e = etat.element ?: return
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { if (etat.dureeMs > 0) (etat.positionMs.toFloat() / etat.dureeMs).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = Or,
            )
            Row(
                Modifier.fillMaxWidth().clickable(onClick = ouvrir).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${e.cantique.numero}. ${e.cantique.titre}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        etat.erreur ?: (e.piste.type.libelle + if (etat.horsLigne) " · hors ligne" else ""),
                        fontSize = 12.sp,
                        color = if (etat.erreur != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { LecteurCantiques.lecturePause() }) {
                    Text(if (etat.enLecture) "⏸" else "▶", fontSize = 22.sp)
                }
                IconButton(onClick = { LecteurCantiques.arreter() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer le lecteur")
                }
            }
        }
    }
}
