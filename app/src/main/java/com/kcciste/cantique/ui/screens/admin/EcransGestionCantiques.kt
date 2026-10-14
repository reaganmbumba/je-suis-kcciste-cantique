package com.kcciste.cantique.ui.screens.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Langue
import com.kcciste.cantique.data.LangueTraduction
import com.kcciste.cantique.data.PisteAudio
import com.kcciste.cantique.data.Recherche
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.data.StatutSuggestion
import com.kcciste.cantique.data.Strophe
import com.kcciste.cantique.data.Traduction
import com.kcciste.cantique.data.TypePiste
import com.kcciste.cantique.enligne.Admin
import com.kcciste.cantique.enligne.AudiosEnLigne
import com.kcciste.cantique.enligne.ContenuEnLigne
import com.kcciste.cantique.enligne.SuggestionsEnLigne
import com.kcciste.cantique.enligne.VersionCantique
import com.kcciste.cantique.ui.components.EtatVide
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.LigneCantique
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.Or
import kotlinx.coroutines.launch
import java.util.UUID

class ActionsCantiqueAdmin(
    val retour: () -> Unit,
    val modifier: (String) -> Unit,
    val audios: (String) -> Unit,
    val traductions: (String) -> Unit,
    val versions: (String) -> Unit,
)

/** Liste des cantiques à gérer, puis fiche d'un cantique avec ses actions. */
@Composable
fun EcranGestionCantiques(ouvrir: (String) -> Unit, retour: () -> Unit) = PageAdmin("Gestion des cantiques", retour) {
    val cantiques by Recueil.flux.collectAsState()
    var filtre by rememberSaveable { mutableStateOf("") }
    var langue by rememberSaveable { mutableStateOf<Langue?>(null) }
    val liste = remember(cantiques, filtre, langue) { Recherche.rechercher(cantiques, filtre, langue) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Champ(filtre, { filtre = it }, "Rechercher (numéro, titre, paroles)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = langue == null, onClick = { langue = null }, label = { Text("Toutes") })
            Langue.entries.forEach { l -> FilterChip(selected = langue == l, onClick = { langue = l }, label = { Text(l.libelle) }) }
        }
    }
    LazyColumn {
        items(liste, key = { it.cantique.id }) { r ->
            val c = r.cantique
            LigneCantique(c.numeroFormate, c.titre, "${c.langue.libelle} · v${c.version} · ${c.audios.size} audio(s) · ${c.traductions.size} trad.", c.audios.isNotEmpty(), false, false) { ouvrir(c.id) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun EcranFicheCantique(id: String, a: ActionsCantiqueAdmin) = PageAdmin("Fiche du cantique", a.retour) {
    val cantiques by Recueil.flux.collectAsState()
    val c = cantiques.firstOrNull { it.id == id }
    if (c == null) { EtatVide("📖", "Cantique introuvable", id); return@PageAdmin }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("${c.langue.libelle} n° ${c.numero}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        Text(c.titre, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text("Identifiant : ${c.id} · version ${c.version}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Catégorie : ${c.categorie.ifBlank { "—" }}", fontSize = 13.sp)
        Text("Crédits : ${c.credits.ifBlank { "non renseignés" }}", fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        GrosBouton("✏️ Modifier le titre, les paroles, les crédits", BleuNuit, couleurTexte = Or) { a.modifier(c.id) }
        Spacer(Modifier.height(8.dp))
        GrosBouton("🎧 Pistes audio (${c.audios.size})", BleuNuit, couleurTexte = Or) { a.audios(c.id) }
        Spacer(Modifier.height(8.dp))
        GrosBouton("🌐 Traductions (${c.traductions.size})", BleuNuit, couleurTexte = Or) { a.traductions(c.id) }
        Spacer(Modifier.height(8.dp))
        GrosBouton("🕘 Historique des versions", BleuNuit, couleurTexte = Or) { a.versions(c.id) }
    }
}

/**
 * Ajout (id = null) ou modification d'un cantique. Publier crée une nouvelle version ;
 * l'ancienne reste dans l'historique et peut être restaurée.
 */
@Composable
fun EcranEditionCantique(id: String?, retour: () -> Unit) = PageAdmin(if (id == null) "Ajouter un cantique" else "Modifier le cantique", retour) {
    val scope = rememberCoroutineScope()
    val origine = remember(id) { id?.let { Recueil.trouver(it) } }
    val suggestion = remember { EtatAdmin.suggestionAAppliquer?.takeIf { it.cantiqueId == id } }
    var titre by rememberSaveable {
        mutableStateOf(suggestion?.takeIf { it.indexStrophe < 0 && it.correction.isNotBlank() }?.correction ?: origine?.titre ?: "")
    }
    var numero by rememberSaveable { mutableStateOf(origine?.numero?.toString() ?: "") }
    var langue by rememberSaveable { mutableStateOf(origine?.langue ?: Langue.LINGALA) }
    var categorie by rememberSaveable { mutableStateOf(origine?.categorie ?: "") }
    var credits by rememberSaveable { mutableStateOf(origine?.credits ?: "") }
    var note by rememberSaveable { mutableStateOf(suggestion?.let { "Correction ${it.reference}" } ?: "") }
    val strophes = remember {
        mutableStateListOf<Strophe>().apply {
            addAll(origine?.strophes ?: listOf(Strophe("", "")))
            // Correction proposée : pré-remplie dans la strophe concernée, à vérifier avant publication.
            suggestion?.let { s ->
                if (s.correction.isNotBlank()) {
                    if (s.indexStrophe in indices) set(s.indexStrophe, get(s.indexStrophe).copy(texte = s.correction))
                }
            }
        }
    }
    var erreur by remember { mutableStateOf<String?>(null) }
    var attente by remember { mutableStateOf(false) }
    var publie by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (suggestion != null) {
            Text(
                "Correction ${suggestion.reference} appliquée dans le formulaire : vérifiez puis publiez.",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)).padding(10.dp),
            )
        }
        if (id == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Langue.entries.forEach { l -> FilterChip(selected = langue == l, onClick = { langue = l }, label = { Text(l.libelle) }) }
            }
            Champ(numero, { v -> numero = v.filter { it.isDigit() }.take(4) }, "Numéro")
        }
        Champ(titre, { titre = it }, "Titre")
        Champ(categorie, { categorie = it }, "Catégorie (facultatif)")
        Champ(credits, { credits = it }, "Crédits et droits (auteur, compositeur, autorisation)", lignes = 2)
        TitreSection("Paroles")
        strophes.forEachIndexed { i, s ->
            Card(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${i + 1}.", fontWeight = FontWeight.Bold)
                        listOf("", "Solo", "Tous").forEach { v ->
                            FilterChip(selected = s.voix == v && !s.estRefrain, onClick = { strophes[i] = s.copy(voix = v, type = if (s.estRefrain) "" else s.type) }, label = { Text(v.ifBlank { "—" }) })
                        }
                        FilterChip(selected = s.estRefrain, onClick = { strophes[i] = s.copy(type = if (s.estRefrain) "" else "refrain") }, label = { Text("Refrain") })
                    }
                    Champ(s.texte, { strophes[i] = s.copy(texte = it) }, "Texte", lignes = 3)
                    Row {
                        if (i > 0) TextButton(onClick = { strophes.add(i - 1, strophes.removeAt(i)) }) { Text("↑") }
                        if (i < strophes.lastIndex) TextButton(onClick = { strophes.add(i + 1, strophes.removeAt(i)) }) { Text("↓") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { strophes.removeAt(i) }, enabled = strophes.size > 1) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        OutlinedButton(onClick = { strophes.add(Strophe("", "")) }, modifier = Modifier.fillMaxWidth()) { Text("➕ Ajouter une strophe") }
        Champ(note, { note = it }, "Note de version (ce qui a changé)")
        MessageErreur(erreur)
        publie?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        if (attente) CircularProgressIndicator(color = Or)
        else GrosBouton("📢 PUBLIER UNE NOUVELLE VERSION", BleuNuit, couleurTexte = Or) {
            val n = numero.toIntOrNull()
            val propres = strophes.filter { it.texte.isNotBlank() }.map { it.copy(texte = it.texte.trim()) }
            erreur = when {
                titre.isBlank() -> "Le titre est obligatoire."
                n == null || n <= 0 -> "Le numéro est obligatoire."
                propres.isEmpty() -> "Ajoutez au moins une strophe."
                id == null && Recueil.trouver(langue, n) != null -> "Le ${langue.libelle} n° $n existe déjà : modifiez-le au lieu d'en créer un nouveau."
                else -> null
            }
            if (erreur != null) return@GrosBouton
            val nouveau = (origine ?: Cantique(n!!, titre, langue, emptyList())).copy(
                titre = titre.trim(), strophes = propres, categorie = categorie.trim(), credits = credits.trim(),
            )
            attente = true
            scope.launch {
                runCatching {
                    val v = ContenuEnLigne.publier(nouveau, Admin.nom(), note.trim())
                    suggestion?.let { SuggestionsEnLigne.changerStatut(it, StatutSuggestion.PUBLIEE, "Publiée en version $v", Admin.nom()) }
                    EtatAdmin.suggestionAAppliquer = null
                    v
                }.onSuccess { publie = "✅ Version $it publiée. Les téléphones la reçoivent à leur prochaine connexion." }
                    .onFailure { erreur = "Publication impossible : ${it.message}" }
                attente = false
            }
        }
    }
}

/** Pistes audio d'un cantique : ajout par fichier (Firebase Storage) ou par URL autorisée, suppression. */
@Composable
fun EcranPistesAudio(id: String, retour: () -> Unit) = PageAdmin("Pistes audio", retour) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cantiques by Recueil.flux.collectAsState()
    val c = cantiques.firstOrNull { it.id == id }
    if (c == null) { EtatVide("📖", "Cantique introuvable", id); return@PageAdmin }
    var type by rememberSaveable { mutableStateOf(TypePiste.CHANTEE) }
    var url by rememberSaveable { mutableStateOf("") }
    var credits by rememberSaveable { mutableStateOf("") }
    var autorise by rememberSaveable { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var attente by remember { mutableStateOf(false) }

    fun publier(nouvelles: List<PisteAudio>, note: String) {
        attente = true
        erreur = null
        scope.launch {
            runCatching { ContenuEnLigne.publier(c.copy(audios = nouvelles), Admin.nom(), note) }
                .onSuccess { url = ""; credits = ""; autorise = false }
                .onFailure { erreur = "Échec : ${it.message}" }
            attente = false
        }
    }

    val choisirFichier = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pisteId = "${c.id}-${type.name.lowercase()}-${UUID.randomUUID().toString().take(6)}"
        attente = true
        erreur = null
        scope.launch {
            runCatching {
                val d = AudiosEnLigne.deposer(context, c.id, pisteId, uri)
                ContenuEnLigne.publier(
                    c.copy(audios = c.audios + PisteAudio(pisteId, type, d.url, d.taille, 0, credits.trim())),
                    Admin.nom(), "Ajout audio : ${type.libelle}",
                )
            }.onSuccess { credits = ""; autorise = false }.onFailure { erreur = "Échec : ${it.message}" }
            attente = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("${c.numero}. ${c.titre}", fontWeight = FontWeight.Black)
        TitreSection("Pistes associées")
        if (c.audios.isEmpty()) Text("Aucune piste.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        c.audios.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.type.libelle, fontWeight = FontWeight.SemiBold)
                    Text(p.url, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (p.credits.isNotBlank()) Text("Crédits : ${p.credits}", fontSize = 12.sp)
                }
                TextButton(onClick = { publier(c.audios - p, "Suppression audio : ${p.type.libelle}") }, enabled = !attente) {
                    Text("Retirer", color = MaterialTheme.colorScheme.error)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        TitreSection("Ajouter une piste")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TypePiste.entries.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.libelle.removePrefix("Version "), fontSize = 12.sp) }) }
        }
        Champ(credits, { credits = it }, "Crédits de l'audio (chorale, interprète)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = autorise, onCheckedChange = { autorise = it })
            Text("  Je confirme avoir l'autorisation de diffuser cet audio.", fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { choisirFichier.launch("audio/*") }, enabled = autorise && !attente, modifier = Modifier.fillMaxWidth()) {
            Text("📁 Importer un fichier audio (50 Mo max)")
        }
        Spacer(Modifier.height(8.dp))
        Champ(url, { url = it.trim() }, "ou URL autorisée (https://…)")
        OutlinedButton(
            onClick = {
                if (!url.startsWith("https://")) { erreur = "L'URL doit commencer par https://"; return@OutlinedButton }
                val pisteId = "${c.id}-${type.name.lowercase()}-${UUID.randomUUID().toString().take(6)}"
                publier(c.audios + PisteAudio(pisteId, type, url, 0, 0, credits.trim()), "Ajout audio par URL : ${type.libelle}")
            },
            enabled = autorise && url.isNotBlank() && !attente,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("🔗 Associer cette URL") }
        MessageErreur(erreur)
        if (attente) CircularProgressIndicator(Modifier.padding(12.dp), color = Or)
    }
}

/** Traductions portugais / anglais : une zone par strophe, statut provisoire ou validé. */
@Composable
fun EcranTraductions(id: String, retour: () -> Unit) = PageAdmin("Traductions", retour) {
    val scope = rememberCoroutineScope()
    val c = remember(id) { Recueil.trouver(id) }
    if (c == null) { EtatVide("📖", "Cantique introuvable", id); return@PageAdmin }
    var langue by rememberSaveable { mutableStateOf(LangueTraduction.PORTUGAIS) }
    val textes = remember(langue) {
        mutableStateListOf<String>().apply {
            val t = c.traduction(langue)
            addAll(c.strophes.indices.map { t?.strophes?.getOrNull(it) ?: "" })
        }
    }
    var validee by remember(langue) { mutableStateOf(c.traduction(langue)?.validee ?: false) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf<String?>(null) }
    var attente by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("${c.numero}. ${c.titre}", fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LangueTraduction.entries.forEach { l -> FilterChip(selected = langue == l, onClick = { langue = l; ok = null }, label = { Text(l.libelle) }) }
        }
        Text(
            "Les paroles originales ne sont jamais modifiées ici. Une traduction non validée est affichée « provisoire ».",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        c.strophes.forEachIndexed { i, s ->
            Spacer(Modifier.height(10.dp))
            Text("Strophe ${i + 1} ${if (s.voix.isNotBlank()) "(${s.voix})" else ""}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(s.texte, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Champ(textes[i], { textes[i] = it }, langue.libelle, lignes = 3)
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = validee, onCheckedChange = { validee = it })
            Text("  Traduction vérifiée et approuvée")
        }
        MessageErreur(erreur)
        ok?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        if (attente) CircularProgressIndicator(color = Or)
        else GrosBouton("📢 PUBLIER LA TRADUCTION", BleuNuit, couleurTexte = Or) {
            val actuel = Recueil.trouver(id) ?: c
            val vide = textes.all { it.isBlank() }
            val autres = actuel.traductions.filter { it.langue != langue }
            val nouvelles = if (vide) autres else autres + Traduction(langue, textes.map { it.trim() }, validee, Admin.nom())
            attente = true
            erreur = null
            scope.launch {
                runCatching {
                    ContenuEnLigne.publier(
                        actuel.copy(traductions = nouvelles), Admin.nom(),
                        if (vide) "Traduction ${langue.libelle} retirée" else "Traduction ${langue.libelle}${if (validee) " validée" else " (provisoire)"}",
                    )
                }.onSuccess { ok = "✅ Publiée (version $it)." }.onFailure { erreur = "Échec : ${it.message}" }
                attente = false
            }
        }
    }
}

/** Historique des versions d'un cantique et restauration. */
@Composable
fun EcranVersions(id: String, retour: () -> Unit) = PageAdmin("Historique des versions", retour) {
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf<List<VersionCantique>?>(null) }
    var ouverte by remember { mutableStateOf<Int?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var recharger by remember { mutableStateOf(0) }
    LaunchedEffect(recharger) {
        runCatching { ContenuEnLigne.versions(id) }.onSuccess { versions = it }.onFailure { erreur = it.message }
    }
    MessageErreur(erreur)
    info?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.tertiary) }
    val l = versions
    when {
        l == null && erreur == null -> CircularProgressIndicator(Modifier.padding(24.dp), color = Or)
        l.isNullOrEmpty() -> EtatVide("🕘", "Aucune version en ligne", "Ce cantique n'a jamais été modifié : seule la version d'origine (1) existe, dans l'application.")
        else -> LazyColumn(Modifier.padding(horizontal = 16.dp)) {
            items(l, key = { it.version }) { v ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    border = BorderStroke(1.dp, if (v == l.first()) Or else MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Version ${v.version}${if (v == l.first()) " (actuelle)" else ""}", fontWeight = FontWeight.Bold)
                        Text("${dateHeure(v.dateMillis)} · ${v.admin}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (v.note.isNotBlank()) Text(v.note, fontSize = 13.sp)
                        Row {
                            TextButton(onClick = { ouverte = if (ouverte == v.version) null else v.version }) { Text(if (ouverte == v.version) "Masquer" else "Voir le texte") }
                            if (v != l.first()) TextButton(onClick = {
                                scope.launch {
                                    runCatching { ContenuEnLigne.restaurer(v, Admin.nom()) }
                                        .onSuccess { info = "✅ Version ${v.version} restaurée comme version $it."; recharger++ }
                                        .onFailure { erreur = "Échec : ${it.message}" }
                                }
                            }) { Text("Restaurer") }
                        }
                        if (ouverte == v.version) {
                            Text(v.cantique.titre, fontWeight = FontWeight.Bold)
                            v.cantique.strophes.forEach { s -> Text((if (s.voix.isNotBlank()) "${s.voix} : " else "") + s.texte, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp)) }
                        }
                    }
                }
            }
        }
    }
}
