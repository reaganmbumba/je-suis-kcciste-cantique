package com.kcciste.cantique.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.audio.ElementLecture
import com.kcciste.cantique.audio.LecteurCantiques
import com.kcciste.cantique.audio.Telechargements
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.EtatVide
import com.kcciste.cantique.ui.components.LogoKCC
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.Or

@Composable
fun EcranLecteurAudio(retour: () -> Unit, ouvrirCantique: (String) -> Unit) {
    val etat by LecteurCantiques.etat.collectAsState()
    val e: ElementLecture? = etat.element
    var glisse by remember { mutableStateOf<Float?>(null) }
    Scaffold(topBar = { Barre("Lecteur audio", retour) }) { pad ->
        if (e == null) {
            Box(Modifier.padding(pad)) { EtatVide("🎧", "Aucun audio en cours", "Ouvrez un cantique qui a un audio et touchez « ▶ Écouter ».") }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoKCC(140.dp)
            Spacer(Modifier.height(20.dp))
            Text("Cantique n° ${e.cantique.numero} · ${e.cantique.langue.libelle}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            Text(e.cantique.titre, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center)
            Text(
                e.piste.type.libelle + if (etat.horsLigne) " · hors ligne" else " · en ligne",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (etat.nbElements > 1) Text("Piste ${etat.index + 1} sur ${etat.nbElements}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))
            val total = etat.dureeMs.coerceAtLeast(1).toFloat()
            Slider(
                value = (glisse ?: etat.positionMs.toFloat()).coerceIn(0f, total),
                onValueChange = { glisse = it },
                onValueChangeFinished = { glisse?.let { LecteurCantiques.allerA(it.toLong()) }; glisse = null },
                valueRange = 0f..total,
                colors = SliderDefaults.colors(thumbColor = Or, activeTrackColor = Or),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(Formats.duree((glisse ?: etat.positionMs.toFloat()).toLong()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(if (etat.dureeMs > 0) Formats.duree(etat.dureeMs) else "--:--", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (etat.chargement) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp), color = Or)
            etat.erreur?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { LecteurCantiques.precedent() }) { Text("⏮", fontSize = 26.sp) }
                IconButton(onClick = { LecteurCantiques.deplacer(-10_000) }) { Text("−10", fontWeight = FontWeight.Bold) }
                FilledIconButton(
                    onClick = { LecteurCantiques.effacerErreur(); LecteurCantiques.lecturePause() },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = BleuNuit, contentColor = Or),
                ) { Text(if (etat.enLecture) "⏸" else "▶", fontSize = 30.sp) }
                IconButton(onClick = { LecteurCantiques.deplacer(10_000) }) { Text("+10", fontWeight = FontWeight.Bold) }
                IconButton(onClick = { LecteurCantiques.suivant() }) { Text("⏭", fontSize = 26.sp) }
            }

            TitreSection("Réglages de lecture", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f, 1f, 1.25f).forEach { v ->
                    FilterChip(selected = etat.vitesse == v, onClick = { LecteurCantiques.vitesse(v) }, label = { Text("${v}x".replace('.', ',')) })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🔁 Répéter ce cantique", Modifier.weight(1f))
                Switch(checked = etat.repeter, onCheckedChange = { LecteurCantiques.repeter(it) })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🔈", fontSize = 18.sp)
                Slider(
                    value = etat.volume,
                    onValueChange = { LecteurCantiques.volume(it) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = Or, activeTrackColor = Or),
                )
                Text("🔊", fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { ouvrirCantique(e.cantique.id) }) { Text("📖 Voir les paroles") }
        }
    }
}

@Composable
fun EcranTelechargements(retour: () -> Unit, ouvrirCantique: (String) -> Unit) {
    val context = LocalContext.current
    val tel = remember { Telechargements.get(context) }
    val liste by tel.liste.collectAsState()
    val enCours by tel.enCours.collectAsState()
    val cantiques by Recueil.flux.collectAsState()
    var toutSupprimer by remember { mutableStateOf(false) }
    val total = liste.sumOf { it.fichier.length() }

    Scaffold(topBar = {
        Barre("Téléchargements", retour) {
            if (liste.isNotEmpty()) TextButton(onClick = { toutSupprimer = true }) { Text("Tout supprimer") }
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text("${liste.size} audio(s) hors ligne · ${Formats.taille(total)}", fontWeight = FontWeight.Bold)
                    Text("Espace libre sur le téléphone : ${Formats.taille(tel.espaceLibre())}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Les audios téléchargés s'écoutent sans Internet. Ils restent dans l'application jusqu'à ce que vous les supprimiez.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                HorizontalDivider()
            }
            val actifs = enCours.entries.toList()
            if (actifs.isNotEmpty()) {
                item { TitreSection("En cours", Modifier.padding(horizontal = 16.dp)) }
                items(actifs, key = { "p-" + it.key }) { (pisteId, p) ->
                    val c = cantiques.firstOrNull { cc -> cc.audios.any { it.id == pisteId } }
                    val piste = c?.audios?.firstOrNull { it.id == pisteId }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(c?.let { "${it.numero}. ${it.titre}" } ?: pisteId, fontWeight = FontWeight.SemiBold)
                        if (p.erreur != null) {
                            Text(p.erreur, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            Row {
                                if (c != null && piste != null) TextButton(onClick = { tel.oublierErreur(pisteId); tel.telecharger(c, piste) }) { Text("Réessayer") }
                                TextButton(onClick = { tel.oublierErreur(pisteId) }) { Text("Ignorer") }
                            }
                        } else {
                            LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Or)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (p.total > 0) "${Formats.taille(p.recu)} / ${Formats.taille(p.total)}" else Formats.taille(p.recu),
                                    fontSize = 12.sp, modifier = Modifier.weight(1f),
                                )
                                if (piste != null) TextButton(onClick = { tel.annuler(piste) }) { Text("Annuler") }
                            }
                        }
                    }
                }
            }
            if (liste.isEmpty() && actifs.isEmpty()) {
                item { EtatVide("⬇", "Aucun audio téléchargé", "Dans un cantique, touchez ⬇ à côté d'une piste pour l'écouter hors connexion.", Modifier.height(320.dp)) }
            }
            if (liste.isNotEmpty()) item { TitreSection("Disponibles hors ligne", Modifier.padding(horizontal = 16.dp)) }
            items(liste, key = { it.pisteId }) { a ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.titre, fontWeight = FontWeight.SemiBold)
                        Text("${a.type} · ${Formats.taille(a.fichier.length())}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { ouvrirCantique(a.cantiqueId) }) { Text("Ouvrir") }
                    TextButton(onClick = { tel.supprimer(a.pisteId) }) { Text("🗑") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    if (toutSupprimer) {
        AlertDialog(
            onDismissRequest = { toutSupprimer = false },
            title = { Text("Supprimer tous les audios téléchargés ?") },
            text = { Text("${Formats.taille(total)} seront libérés. Vous pourrez les télécharger à nouveau.") },
            confirmButton = { TextButton(onClick = { tel.supprimerTout(); toutSupprimer = false }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { toutSupprimer = false }) { Text("Annuler") } },
        )
    }
}

