package com.kcciste.cantique.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.data.StatutSuggestion
import com.kcciste.cantique.enligne.Admin
import com.kcciste.cantique.enligne.ContenuEnLigne
import com.kcciste.cantique.enligne.ServiceFirebase
import com.kcciste.cantique.enligne.SuggestionsEnLigne
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.CarteMenu
import com.kcciste.cantique.ui.components.EtatVide
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.LogoKCC
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.Or
import kotlinx.coroutines.launch

class ActionsAdmin(
    val retour: () -> Unit,
    val cantiques: () -> Unit,
    val suggestions: () -> Unit,
    val importer: () -> Unit,
    val journal: () -> Unit,
    val nouveau: () -> Unit,
)

/** Connexion administrateur puis tableau de bord. */
@Composable
fun EcranTableauDeBord(a: ActionsAdmin) {
    val context = LocalContext.current
    val session by Admin.session.collectAsState()
    val configure = remember { ServiceFirebase.estConfigure(context) }
    LaunchedEffect(configure) { if (configure) runCatching { Admin.verifier() } }

    Scaffold(topBar = {
        Barre("Administration", a.retour) {
            if (session != null) TextButton(onClick = { Admin.deconnecter() }) { Text("Déconnexion") }
        }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                !configure -> EtatVide(
                    "🔐", "Administration non activée",
                    "Le projet Firebase n'est pas encore relié à l'application. Suivez GUIDE_FIREBASE.md (fichier google-services.json et comptes administrateurs).",
                )
                session == null -> Connexion()
                else -> TableauDeBord(a)
            }
        }
    }
}

@Composable
private fun Connexion() {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var mdp by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf<String?>(null) }
    var attente by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LogoKCC(96.dp)
        Spacer(Modifier.height(12.dp))
        Text("Connexion administrateur", fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text("Réservé aux administrateurs autorisés.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Adresse e-mail") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mdp, { mdp = it }, label = { Text("Mot de passe") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        MessageErreur(erreur)
        Spacer(Modifier.height(12.dp))
        if (attente) CircularProgressIndicator(color = Or)
        else GrosBouton("SE CONNECTER", BleuNuit, couleurTexte = Or, actif = email.isNotBlank() && mdp.isNotBlank()) {
            attente = true
            erreur = null
            scope.launch {
                Admin.connecter(email, mdp).onFailure { erreur = it.message ?: "Connexion impossible." }
                attente = false
            }
        }
    }
}

@Composable
private fun TableauDeBord(a: ActionsAdmin) {
    val cantiques by Recueil.flux.collectAsState()
    val suggestions by EtatAdmin.suggestions.collectAsState()
    var erreur by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val ecoute = SuggestionsEnLigne.ecouter({ EtatAdmin.suggestions.value = it; erreur = null }, { erreur = "Suggestions illisibles : ${it.message}" })
        onDispose { ecoute.remove() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Connecté : ${Admin.nom()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        MessageErreur(erreur)
        TitreSection("Vue d'ensemble")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CarteMenu("📖", "${cantiques.size}", "cantiques", Modifier.weight(1f)) { a.cantiques() }
            CarteMenu("📨", "${suggestions.count { it.statut == StatutSuggestion.EN_ATTENTE }}", "suggestion(s) en attente", Modifier.weight(1f)) { a.suggestions() }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CarteMenu("🎧", "${cantiques.count { it.audios.isNotEmpty() }}", "cantiques avec audio", Modifier.weight(1f)) { a.cantiques() }
            CarteMenu("🌐", "${cantiques.count { c -> c.traductions.any { !it.validee } }}", "traduction(s) à valider", Modifier.weight(1f)) { a.cantiques() }
        }
        TitreSection("Gestion")
        val menu = listOf(
            Triple("📖", "Gestion des cantiques", "Modifier, audios, traductions, versions") to a.cantiques,
            Triple("➕", "Ajouter un cantique", "Nouveau cantique, version 1") to a.nouveau,
            Triple("📨", "Modération des suggestions", "Comparer, écouter, accepter, publier") to a.suggestions,
            Triple("📥", "Importer en lot", "Fichier JSON ou CSV vérifié") to a.importer,
            Triple("🧾", "Journal des actions", "Qui a fait quoi, et quand") to a.journal,
        )
        menu.forEach { (t, action) ->
            CarteMenu(t.first, t.second, t.third, Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(bottom = 8.dp), onClick = action)
        }
    }
}

@Composable
fun EcranJournal(retour: () -> Unit) = PageAdmin("Journal des actions", retour) {
    var entrees by remember { mutableStateOf<List<ContenuEnLigne.EntreeJournal>?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { ContenuEnLigne.journal() }.onSuccess { entrees = it }.onFailure { erreur = it.message }
    }
    MessageErreur(erreur)
    val l = entrees
    when {
        l == null && erreur == null -> CircularProgressIndicator(Modifier.padding(24.dp), color = Or)
        l.isNullOrEmpty() -> EtatVide("🧾", "Journal vide", "Les publications, imports et décisions apparaîtront ici.")
        else -> LazyColumn {
            items(l) { e ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("${dateHeure(e.dateMillis)} · ${e.admin}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${e.action}${if (e.cantiqueId.isNotBlank()) " · ${e.cantiqueId}" else ""}", fontWeight = FontWeight.SemiBold)
                    if (e.details.isNotBlank()) Text(e.details, fontSize = 13.sp)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
