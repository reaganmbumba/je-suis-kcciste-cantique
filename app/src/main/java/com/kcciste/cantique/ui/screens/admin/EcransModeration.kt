package com.kcciste.cantique.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.audio.ElementLecture
import com.kcciste.cantique.audio.LecteurCantiques
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Comparaison
import com.kcciste.cantique.data.ImportCantiques
import com.kcciste.cantique.data.Langue
import com.kcciste.cantique.data.PisteAudio
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.data.StatutSuggestion
import com.kcciste.cantique.data.Suggestion
import com.kcciste.cantique.data.TypePiste
import com.kcciste.cantique.enligne.Admin
import com.kcciste.cantique.enligne.ContenuEnLigne
import com.kcciste.cantique.enligne.SuggestionsEnLigne
import com.kcciste.cantique.ui.components.EtatVide
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.Pastille
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.Or
import kotlinx.coroutines.launch

private fun couleurStatut(s: StatutSuggestion) = when (s) {
    StatutSuggestion.EN_ATTENTE -> Color(0xFFB26A00)
    StatutSuggestion.EN_EXAMEN -> Color(0xFF1565C0)
    StatutSuggestion.ACCEPTEE -> Color(0xFF2E7D32)
    StatutSuggestion.REFUSEE -> Color(0xFF8E1B1B)
    StatutSuggestion.PUBLIEE -> BleuNuit
}

@Composable
fun EcranModeration(ouvrir: (String) -> Unit, retour: () -> Unit) = PageAdmin("Modération des suggestions", retour) {
    val liste by EtatAdmin.suggestions.collectAsState()
    var erreur by remember { mutableStateOf<String?>(null) }
    var filtre by rememberSaveable { mutableStateOf<StatutSuggestion?>(StatutSuggestion.EN_ATTENTE) }
    DisposableEffect(Unit) {
        val ecoute = SuggestionsEnLigne.ecouter({ EtatAdmin.suggestions.value = it; erreur = null }, { erreur = it.message })
        onDispose { ecoute.remove() }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = filtre == null, onClick = { filtre = null }, label = { Text("Toutes (${liste.size})") })
        StatutSuggestion.entries.forEach { s ->
            FilterChip(selected = filtre == s, onClick = { filtre = s }, label = { Text("${s.libelle} (${liste.count { it.statut == s }})") })
        }
    }
    MessageErreur(erreur)
    val visibles = liste.filter { filtre == null || it.statut == filtre }
    if (visibles.isEmpty()) EtatVide("📨", "Aucune suggestion", "Rien à traiter dans cette catégorie.")
    else LazyColumn {
        items(visibles, key = { it.id }) { s ->
            Column(Modifier.fillMaxWidth().clickable { ouvrir(s.id) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row {
                    Text(s.reference, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Pastille(s.statut.libelle, couleurStatut(s.statut))
                }
                Text(s.titreCantique, fontSize = 14.sp)
                Text(
                    "${dateHeure(s.dateMillis)}${if (s.nom.isNotBlank()) " · ${s.nom}" else ""}${if (s.cheminAudio.isNotBlank()) " · 🎙️ vocal" else ""}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Détail : comparaison ancien / proposé, écoute du message vocal, décision. */
@Composable
fun EcranSuggestion(id: String, appliquer: (cantiqueId: String) -> Unit, retour: () -> Unit) = PageAdmin("Suggestion", retour) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liste by EtatAdmin.suggestions.collectAsState()
    val s = liste.firstOrNull { it.id == id }
    if (s == null) { EtatVide("📨", "Suggestion introuvable", id); return@PageAdmin }
    val c = Recueil.trouver(s.cantiqueId)
    var note by rememberSaveable { mutableStateOf(s.noteAdmin) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var attente by remember { mutableStateOf(false) }

    fun decider(statut: StatutSuggestion) {
        attente = true
        scope.launch {
            runCatching { SuggestionsEnLigne.changerStatut(s, statut, note.trim(), Admin.nom()) }.onFailure { erreur = "Échec : ${it.message}" }
            attente = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row {
            Text(s.reference, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Pastille(s.statut.libelle, couleurStatut(s.statut))
        }
        Text("${s.titreCantique} · ${if (s.indexStrophe < 0) "titre" else "strophe ${s.indexStrophe + 1}"} · version ${s.versionCantique}", fontSize = 13.sp)
        Text("${dateHeure(s.dateMillis)}${if (s.nom.isNotBlank()) " · par ${s.nom}" else ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (c != null && c.version != s.versionCantique) {
            Text("⚠️ Le cantique est passé en version ${c.version} depuis cette suggestion.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        TitreSection("Comparaison")
        if (s.correction.isBlank()) Text("Pas de texte proposé (voir le message vocal).", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Comparaison.lignes(s.texteActuel, s.correction).forEach { l ->
            val (fond, prefixe) = when (l.etat) {
                Comparaison.Etat.IDENTIQUE -> Color.Transparent to "  "
                Comparaison.Etat.SUPPRIMEE -> Color(0x33E53935) to "− "
                Comparaison.Etat.AJOUTEE -> Color(0x3343A047) to "+ "
            }
            Text(
                prefixe + l.texte,
                modifier = Modifier.fillMaxWidth().background(fond, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                textDecoration = if (l.etat == Comparaison.Etat.SUPPRIMEE) TextDecoration.LineThrough else null,
            )
        }
        if (s.explication.isNotBlank()) {
            TitreSection("Explication")
            Text(s.explication)
        }
        if (s.cheminAudio.isNotBlank()) {
            TitreSection("Message vocal")
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { SuggestionsEnLigne.urlAudio(s.cheminAudio) }.onSuccess { uri ->
                        val support = c ?: Cantique(0, s.titreCantique, Langue.LINGALA, emptyList(), id = s.cantiqueId)
                        LecteurCantiques.jouer(context, listOf(ElementLecture(support.copy(titre = "Suggestion ${s.reference}"), PisteAudio(s.id, TypePiste.REPETITION, uri.toString()))), 0)
                    }.onFailure { erreur = "Écoute impossible : ${it.message}" }
                }
            }) { Text("▶ Écouter le message vocal") }
            Text("La lecture se contrôle dans la barre du lecteur.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        TitreSection("Décision")
        Champ(note, { note = it }, "Note interne (facultatif)", lignes = 2)
        MessageErreur(erreur)
        if (attente) CircularProgressIndicator(color = Or)
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { decider(StatutSuggestion.EN_EXAMEN) }, Modifier.weight(1f)) { Text("En examen") }
                OutlinedButton(onClick = { decider(StatutSuggestion.REFUSEE) }, Modifier.weight(1f)) { Text("Refuser", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { decider(StatutSuggestion.ACCEPTEE) }, Modifier.fillMaxWidth()) { Text("Accepter (sans publier)") }
            Spacer(Modifier.height(8.dp))
            GrosBouton("✏️ APPLIQUER ET PUBLIER…", BleuNuit, couleurTexte = Or, actif = c != null) {
                EtatAdmin.suggestionAAppliquer = s
                appliquer(s.cantiqueId)
            }
            Text(
                "Ouvre l'éditeur avec la correction déjà placée. Rien n'est publié tant que vous n'appuyez pas sur « Publier ».",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Import en lot (JSON ou CSV) : vérification complète avant tout envoi, jamais de remplacement. */
@Composable
fun EcranImport(retour: () -> Unit) = PageAdmin("Importer des cantiques", retour) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rapport by remember { mutableStateOf<ImportCantiques.Rapport?>(null) }
    var langue by rememberSaveable { mutableStateOf<Langue?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var resultat by remember { mutableStateOf<String?>(null) }
    var attente by remember { mutableStateOf(false) }
    val choisir = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        resultat = null
        erreur = null
        runCatching { context.contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() } }
            .onSuccess { texte -> rapport = ImportCantiques.analyser(texte, Recueil.cantiques, langue) }
            .onFailure { erreur = "Fichier illisible : ${it.message}" }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Formats acceptés : JSON (même format que les recueils de l'application) ou CSV avec les colonnes " +
                "numero, titre, langue, paroles (et facultatif : categorie, credits, id). Voir GUIDE_IMPORTATION.md. " +
                "Pour un document Word, convertissez-le d'abord avec outils/build_json.py puis vérifiez le résultat.",
            fontSize = 13.sp,
        )
        Text("Langue si le fichier ne la précise pas :", Modifier.padding(top = 10.dp), fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = langue == null, onClick = { langue = null }, label = { Text("Aucune") })
            Langue.entries.forEach { l -> FilterChip(selected = langue == l, onClick = { langue = l }, label = { Text(l.libelle) }) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { choisir.launch("*/*") }, modifier = Modifier.fillMaxWidth()) { Text("📁 Choisir un fichier JSON ou CSV") }
        MessageErreur(erreur)
        rapport?.let { r ->
            TitreSection("Vérification")
            Text("✅ ${r.nouveaux.size} nouveau(x) cantique(s) prêt(s) à importer", fontWeight = FontWeight.Bold)
            r.nouveaux.take(30).forEach { Text("• ${it.langue.libelle} n° ${it.numero} — ${it.titre}", fontSize = 13.sp) }
            if (r.nouveaux.size > 30) Text("… et ${r.nouveaux.size - 30} autre(s)", fontSize = 13.sp)
            if (r.ignores.isNotEmpty()) {
                Text("⏭ ${r.ignores.size} déjà présent(s), ignoré(s) (jamais remplacés)", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                r.ignores.take(20).forEach { Text("• $it", fontSize = 13.sp) }
            }
            if (r.erreurs.isNotEmpty()) {
                Text("⚠️ ${r.erreurs.size} erreur(s) à corriger dans le fichier", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                r.erreurs.take(20).forEach { Text("• $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(12.dp))
            if (attente) CircularProgressIndicator(color = Or)
            else GrosBouton("📥 IMPORTER ${r.nouveaux.size} CANTIQUE(S)", BleuNuit, couleurTexte = Or, actif = r.nouveaux.isNotEmpty()) {
                attente = true
                scope.launch {
                    runCatching { ContenuEnLigne.importer(r.nouveaux, Admin.nom()) }
                        .onSuccess { resultat = "✅ $it cantique(s) importé(s)."; rapport = null }
                        .onFailure { erreur = "Import impossible : ${it.message}" }
                    attente = false
                }
            }
        }
        resultat?.let { Text(it, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) }
    }
}

