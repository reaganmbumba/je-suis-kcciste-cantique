package com.kcciste.cantique.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.audio.Telechargements
import com.kcciste.cantique.data.Bibliotheque
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Langue
import com.kcciste.cantique.data.Recherche
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.EtatVide
import com.kcciste.cantique.ui.components.LigneCantique
import com.kcciste.cantique.ui.components.MiniLecteur
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.Or

@Composable
private fun ChampRecherche(valeur: String, changer: (String) -> Unit, modifier: Modifier = Modifier, indice: String = "Numéro, titre ou paroles…") {
    OutlinedTextField(
        value = valeur,
        onValueChange = changer,
        placeholder = { Text(indice) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (valeur.isNotEmpty()) IconButton(onClick = { changer("") }) { Icon(Icons.Filled.Clear, contentDescription = "Effacer") }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Liste de cantiques commune : affiche numéro, titre, extrait, audio, téléchargé, favori. */
@Composable
private fun ListeCantiques(
    resultats: List<Recherche.Resultat>,
    ouvrir: (Cantique) -> Unit,
    avecLangue: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val favoris by Bibliotheque.get(context).favoris.collectAsState()
    val telecharges by Telechargements.get(context).liste.collectAsState()
    val idsTelecharges = telecharges.map { it.cantiqueId }.toSet()
    LazyColumn(modifier.fillMaxSize()) {
        items(resultats, key = { it.cantique.id }) { r ->
            val c = r.cantique
            val sous = listOfNotNull(
                if (avecLangue) c.langue.libelle else null,
                c.categorie.takeIf { it.isNotBlank() },
                r.extrait?.let { "« $it »" },
            ).joinToString(" · ")
            LigneCantique(c.numeroFormate, c.titre, sous, c.audios.isNotEmpty(), c.id in idsTelecharges, c.id in favoris) { ouvrir(c) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun EcranBibliotheque(langueInitiale: Langue?, ouvrir: (Cantique) -> Unit, retour: () -> Unit, lecteur: () -> Unit) {
    val cantiques by Recueil.flux.collectAsState()
    var langue by rememberSaveable { mutableStateOf(langueInitiale ?: Langue.LINGALA) }
    var categorie by rememberSaveable { mutableStateOf<String?>(null) }
    var filtre by rememberSaveable { mutableStateOf("") }
    val categories = remember(cantiques, langue) { Recueil.categories(langue) }
    val resultats = remember(cantiques, langue, categorie, filtre) {
        Recherche.rechercher(cantiques.filter { categorie == null || it.categorie == categorie }, filtre, langue)
    }

    Scaffold(topBar = { Barre("Bibliothèque", retour) }, bottomBar = { MiniLecteur(lecteur) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(
                selectedTabIndex = langue.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[langue.ordinal]), color = Or) },
            ) {
                Langue.entries.forEach { l ->
                    Tab(selected = langue == l, onClick = { langue = l; categorie = null }, text = {
                        Text("${l.libelle} (${cantiques.count { it.langue == l }})", fontWeight = FontWeight.SemiBold)
                    })
                }
            }
            ChampRecherche(filtre, { filtre = it }, Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            if (categories.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = categorie == null, onClick = { categorie = null }, label = { Text("Toutes") })
                    categories.forEach { cat -> FilterChip(selected = categorie == cat, onClick = { categorie = cat }, label = { Text(cat) }) }
                }
            }
            when {
                cantiques.none { it.langue == langue } -> EtatVide(
                    "📖", "Recueil ${langue.libelle} pas encore importé",
                    "Les cantiques apparaîtront ici dès que l'administrateur les aura importés.",
                )
                resultats.isEmpty() -> EtatVide("🔍", "Aucun résultat", "Essayez un autre numéro, un mot du titre ou des paroles.")
                else -> ListeCantiques(resultats, ouvrir, avecLangue = false)
            }
        }
    }
}

@Composable
fun EcranRecherche(ouvrir: (Cantique) -> Unit, retour: () -> Unit) {
    val cantiques by Recueil.flux.collectAsState()
    var requete by rememberSaveable { mutableStateOf("") }
    var langue by rememberSaveable { mutableStateOf<Langue?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val resultats = remember(cantiques, requete, langue) {
        if (requete.isBlank()) emptyList() else Recherche.rechercher(cantiques, requete, langue)
    }
    Scaffold(topBar = { Barre("Recherche", retour) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ChampRecherche(requete, { requete = it }, Modifier.padding(horizontal = 16.dp, vertical = 10.dp).focusRequester(focus))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = langue == null, onClick = { langue = null }, label = { Text("Toutes") })
                Langue.entries.forEach { l -> FilterChip(selected = langue == l, onClick = { langue = l }, label = { Text(l.libelle) }) }
            }
            when {
                requete.isBlank() -> EtatVide("🔍", "Rechercher un cantique", "Par numéro (ex. 12), par titre ou par des mots des paroles. Les accents ne comptent pas.")
                resultats.isEmpty() -> EtatVide("🔍", "Aucun résultat", "Aucun cantique ne contient « $requete ».")
                else -> Column {
                    Text("${resultats.size} résultat(s)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    ListeCantiques(resultats, ouvrir, avecLangue = true)
                }
            }
        }
    }
}

@Composable
fun EcranFavoris(ouvrir: (Cantique) -> Unit, retour: () -> Unit) {
    val context = LocalContext.current
    val cantiques by Recueil.flux.collectAsState()
    val favoris by Bibliotheque.get(context).favoris.collectAsState()
    val liste = favoris.mapNotNull { id -> cantiques.firstOrNull { it.id == id } }
    Scaffold(topBar = { Barre("★ Favoris", retour) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (liste.isEmpty()) EtatVide("★", "Aucun favori", "Ouvrez un cantique et touchez l'étoile pour le retrouver ici.")
            else ListeCantiques(liste.map { Recherche.Resultat(it, Recherche.Trouve.TITRE, null) }, ouvrir, avecLangue = true)
        }
    }
}

@Composable
fun EcranHistorique(ouvrir: (Cantique) -> Unit, retour: () -> Unit) {
    val context = LocalContext.current
    val biblio = remember { Bibliotheque.get(context) }
    val cantiques by Recueil.flux.collectAsState()
    val historique by biblio.historique.collectAsState()
    val audios by biblio.audiosEcoutes.collectAsState()
    val consultes = historique.mapNotNull { id -> cantiques.firstOrNull { it.id == id } }
    val ecoutes = audios.mapNotNull { cle ->
        val (cid, pid) = cle.split('|').let { it[0] to it.getOrElse(1) { "" } }
        val c = cantiques.firstOrNull { it.id == cid } ?: return@mapNotNull null
        c to (c.audios.firstOrNull { it.id == pid }?.type?.libelle ?: "Audio")
    }
    Scaffold(topBar = {
        Barre("Historique", retour) {
            if (consultes.isNotEmpty() || ecoutes.isNotEmpty()) TextButton(onClick = { biblio.viderHistorique() }) { Text("Effacer") }
        }
    }) { pad ->
        if (consultes.isEmpty() && ecoutes.isEmpty()) {
            Box(Modifier.padding(pad)) { EtatVide("🕘", "Historique vide", "Les cantiques consultés et les audios écoutés apparaîtront ici.") }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize()) {
                if (ecoutes.isNotEmpty()) {
                    item { TitreSection("Derniers audios écoutés", Modifier.padding(horizontal = 16.dp)) }
                    items(ecoutes) { (c, type) ->
                        LigneCantique(c.numeroFormate, c.titre, "${c.langue.libelle} · $type", true, false, false) { ouvrir(c) }
                    }
                }
                if (consultes.isNotEmpty()) {
                    item { TitreSection("Derniers cantiques consultés", Modifier.padding(horizontal = 16.dp)) }
                    items(consultes, key = { "c-" + it.id }) { c ->
                        LigneCantique(c.numeroFormate, c.titre, c.langue.libelle, c.audios.isNotEmpty(), false, false) { ouvrir(c) }
                    }
                }
            }
        }
    }
}
