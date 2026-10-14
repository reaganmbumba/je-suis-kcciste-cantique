package com.kcciste.cantique.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.audio.Enregistreur
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.data.Suggestions
import com.kcciste.cantique.enligne.ServiceFirebase
import com.kcciste.cantique.enligne.SuggestionsEnLigne
import com.kcciste.cantique.share.PartageTexte
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.EtatVide
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.LecteurAudio
import com.kcciste.cantique.ui.components.LogoKCC
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.components.rememberPermissionMicro
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.Or
import com.kcciste.cantique.ui.theme.Rouge
import com.kcciste.cantique.ui.theme.VertWhatsApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Formulaire « Signaler une erreur / Proposer une correction » (texte et/ou message vocal). */
@Composable
fun EcranCorrection(id: String, retour: () -> Unit, envoye: (String) -> Unit) {
    val c = Recueil.trouver(id)
    if (c == null) {
        Scaffold(topBar = { Barre("Proposer une correction", retour) }) { pad -> Column(Modifier.padding(pad)) { EtatVide("📖", "Cantique introuvable", "") } }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // -1 = titre ; 0..n = strophes
    var index by rememberSaveable { mutableStateOf(if (c.strophes.isEmpty()) -1 else 0) }
    val actuel = if (index < 0) c.titre else c.strophes.getOrNull(index)?.texte.orEmpty()
    var correction by rememberSaveable(index) { mutableStateOf(actuel) }
    var explication by rememberSaveable { mutableStateOf("") }
    var nom by rememberSaveable { mutableStateOf("") }
    var choix by remember { mutableStateOf(false) }
    var erreurs by remember { mutableStateOf<List<String>>(emptyList()) }
    var envoi by remember { mutableStateOf(false) }

    // Message vocal
    val dossier = remember { File(context.cacheDir, "suggestion").apply { mkdirs() } }
    var vocal by remember { mutableStateOf<File?>(null) }
    var mime by remember { mutableStateOf<String?>(null) }
    var dureeVocal by remember { mutableLongStateOf(0L) }
    val enregistreur = remember { Enregistreur(context) }
    var enregistrement by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { if (enregistreur.actif) enregistreur.annuler() } }
    val micro = rememberPermissionMicro(onRefus = { erreurs = listOf("Sans le microphone, vous pouvez importer un fichier audio.") })

    fun arreterEnregistrement() {
        dureeVocal = runCatching { enregistreur.arreter() }.getOrDefault(0L)
        enregistrement = false
        mime = "audio/mp4"
    }
    LaunchedEffect(enregistrement) {
        while (enregistrement) {
            delay(250)
            dureeVocal = enregistreur.dureeMs
            if (dureeVocal >= Suggestions.MAX_AUDIO_SECONDES * 1000L) arreterEnregistrement()
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val type = context.contentResolver.getType(uri)
        val taille = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        erreurs = when {
            !Suggestions.typeAudioAccepte(type) -> listOf("Ce type de fichier n'est pas accepté ($type). Formats acceptés : m4a, mp3, ogg, wav, aac, amr.")
            taille > Suggestions.MAX_AUDIO_OCTETS -> listOf("Le fichier fait ${Formats.taille(taille)} : la limite est ${Formats.taille(Suggestions.MAX_AUDIO_OCTETS)}.")
            else -> {
                val f = File(dossier, "import.audio")
                runCatching { context.contentResolver.openInputStream(uri)!!.use { e -> f.outputStream().use { e.copyTo(it) } } }
                    .onSuccess { vocal = f; mime = type; dureeVocal = 0 }
                emptyList()
            }
        }
    }

    Scaffold(topBar = { Barre("Proposer une correction", retour) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("${c.numero}. ${c.titre}", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("${c.langue.libelle} · version ${c.version}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)

            TitreSection("1. Ligne concernée")
            OutlinedButton(onClick = { choix = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (index < 0) "Titre du cantique" else "Strophe ${index + 1} ${c.strophes.getOrNull(index)?.voix?.let { "($it)" } ?: ""}", Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(expanded = choix, onDismissRequest = { choix = false }) {
                DropdownMenuItem(text = { Text("Titre du cantique") }, onClick = { index = -1; choix = false })
                c.strophes.forEachIndexed { i, s ->
                    DropdownMenuItem(
                        text = { Text("Strophe ${i + 1} ${if (s.voix.isNotBlank()) "(${s.voix}) " else ""}: ${s.texte.lineSequence().first().take(40)}…") },
                        onClick = { index = i; choix = false },
                    )
                }
            }

            TitreSection("2. Texte actuellement affiché")
            Text(
                actuel,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(12.dp),
            )

            TitreSection("3. Correction proposée")
            OutlinedTextField(
                value = correction, onValueChange = { correction = it.take(Suggestions.MAX_CORRECTION) },
                modifier = Modifier.fillMaxWidth(), minLines = 4,
                supportingText = { Text("Modifiez le texte ci-dessus. ${correction.length}/${Suggestions.MAX_CORRECTION}") },
            )
            OutlinedTextField(
                value = explication, onValueChange = { explication = it.take(Suggestions.MAX_EXPLICATION) },
                label = { Text("Explication (facultatif)") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            OutlinedTextField(
                value = nom, onValueChange = { nom = it.take(60) },
                label = { Text("Votre nom (facultatif)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            TitreSection("4. Message vocal (facultatif)")
            Text(
                "Chantez ou dites la bonne version. ${Suggestions.MAX_AUDIO_SECONDES / 60} minutes et 10 Mo au plus.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            when {
                enregistrement -> {
                    Text("🔴 Enregistrement… ${Formats.duree(dureeVocal)}", color = Rouge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    GrosBouton("⏹ ARRÊTER", BleuNuit) { arreterEnregistrement() }
                }
                vocal != null -> {
                    val f = vocal!!
                    LecteurAudio(f, dureeVocal)
                    TextButton(onClick = { f.delete(); vocal = null; mime = null }) { Text("🗑 Supprimer le message vocal", color = MaterialTheme.colorScheme.error) }
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        micro.avecMicro {
                            val f = File(dossier, "vocal.m4a")
                            runCatching { enregistreur.demarrer(f) }
                                .onSuccess { vocal = f; enregistrement = true; dureeVocal = 0 }
                                .onFailure { erreurs = listOf("Impossible de démarrer l'enregistrement.") }
                        }
                    }, modifier = Modifier.weight(1f)) { Text("🎙️ Enregistrer") }
                    OutlinedButton(onClick = { importer.launch("audio/*") }, modifier = Modifier.weight(1f)) { Text("📁 Importer") }
                }
            }

            erreurs.forEach { Text("⚠️ $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(20.dp))
            if (envoi) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Or)
                    Text("  Envoi en cours…")
                }
            } else {
                val corrigeTexte = if (correction.trim() == actuel.trim()) "" else correction
                GrosBouton("📤 ENVOYER À L'ADMINISTRATION", BleuNuit, couleurTexte = Or, actif = !enregistrement) {
                    erreurs = Suggestions.valider(corrigeTexte, explication, vocal != null)
                    if (erreurs.isNotEmpty()) return@GrosBouton
                    if (!ServiceFirebase.estConfigure(context)) {
                        erreurs = listOf("L'envoi en ligne n'est pas encore activé dans cette version. Utilisez WhatsApp ci-dessous.")
                        return@GrosBouton
                    }
                    envoi = true
                    scope.launch {
                        runCatching {
                            SuggestionsEnLigne.envoyer(
                                context,
                                SuggestionsEnLigne.Brouillon(c, index, actuel, corrigeTexte, explication, nom, vocal?.let { Uri.fromFile(it) }, mime),
                            )
                        }.onSuccess { ref ->
                            vocal?.delete()
                            envoye(ref)
                        }.onFailure {
                            erreurs = listOf("Envoi impossible. Vérifiez votre connexion Internet puis réessayez. (${it.message})")
                        }
                        envoi = false
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = {
                val msg = "Correction pour le cantique ${c.langue.libelle} n° ${c.numero} « ${c.titre} »\n" +
                    "Texte actuel : $actuel\nCorrection : $correction" + if (explication.isNotBlank()) "\nExplication : $explication" else ""
                PartageTexte.copier(context, msg, "Correction")
                PartageTexte.contacter(context, "Correction de cantique")
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Ou envoyer par WhatsApp (le texte est copié, collez-le)", color = VertWhatsApp, textAlign = TextAlign.Center)
            }
            Text("Aucune correction n'est publiée automatiquement : l'administration vérifie chaque proposition.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EcranConfirmation(reference: String, terminer: () -> Unit) {
    Scaffold(topBar = { Barre("Suggestion envoyée", null) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LogoKCC(96.dp)
            Spacer(Modifier.height(16.dp))
            Text("✅ Merci !", fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text("Votre proposition a bien été reçue par l'administration.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, Or),
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Numéro de référence", fontSize = 13.sp)
                    Text(reference, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Gardez ce numéro : il permet de suivre votre suggestion avec ${Createur.NOM}.",
                textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            GrosBouton("TERMINER", BleuNuit, couleurTexte = Or, onClick = terminer)
        }
    }
}

