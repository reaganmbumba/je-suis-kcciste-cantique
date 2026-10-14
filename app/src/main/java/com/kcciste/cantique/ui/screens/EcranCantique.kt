package com.kcciste.cantique.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kcciste.cantique.audio.ElementLecture
import com.kcciste.cantique.audio.LecteurCantiques
import com.kcciste.cantique.audio.Telechargements
import com.kcciste.cantique.data.Bibliotheque
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.data.LangueTraduction
import com.kcciste.cantique.data.PisteAudio
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.data.ReglagesRepository
import com.kcciste.cantique.data.TexteCantique
import com.kcciste.cantique.data.Traduction
import com.kcciste.cantique.share.PartageTexte
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.MiniLecteur
import com.kcciste.cantique.ui.components.Pastille
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.Or
import com.kcciste.cantique.ui.theme.Rouge
import com.kcciste.cantique.ui.theme.VertWhatsApp

class ActionsCantique(
    val retour: () -> Unit,
    val corriger: (Cantique) -> Unit,
    val enregistrer: (Cantique) -> Unit,
    val lecteur: () -> Unit,
)

/** Écran de lecture : un cantique par page, balayer à gauche / à droite pour le suivant / précédent. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EcranCantique(id: String, a: ActionsCantique) {
    val context = LocalContext.current
    val tous by Recueil.flux.collectAsState()
    val depart = tous.firstOrNull { it.id == id }
    if (depart == null) {
        Scaffold(topBar = { Barre("Cantique", a.retour) }) { pad ->
            Box(Modifier.padding(pad)) { com.kcciste.cantique.ui.components.EtatVide("📖", "Cantique introuvable", "Il a peut-être été retiré du recueil.") }
        }
        return
    }
    val liste = remember(tous, depart.langue) { tous.filter { it.langue == depart.langue } }
    val pager = rememberPagerState(initialPage = liste.indexOfFirst { it.id == id }.coerceAtLeast(0)) { liste.size }
    val courant = liste.getOrElse(pager.currentPage) { depart }
    val biblio = remember { Bibliotheque.get(context) }
    val favoris by biblio.favoris.collectAsState()
    val reglages by ReglagesRepository.flux.collectAsState()
    var traduction by rememberSaveable { mutableStateOf<String?>(null) }
    var pleinEcran by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var partage by remember { mutableStateOf(false) }

    LaunchedEffect(courant.id) { biblio.consulte(courant.id) }

    // Écran maintenu allumé pendant la lecture (répétitions, cultes) si activé dans les paramètres.
    val view = LocalView.current
    DisposableEffect(reglages.ecranAllume) {
        view.keepScreenOn = reglages.ecranAllume
        onDispose { view.keepScreenOn = false }
    }
    // Plein écran : on masque les barres système.
    DisposableEffect(pleinEcran) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        if (pleinEcran) {
            ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            ctrl?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    BackHandler(enabled = pleinEcran) { pleinEcran = false }

    val langueTrad = LangueTraduction.depuisCode(traduction)
    val texteAPartager = { TexteCantique.pourPartage(courant, langueTrad?.let { courant.traduction(it) }) }

    Scaffold(
        topBar = {
            if (!pleinEcran) Barre("${courant.numero}. ${courant.titre}", a.retour) {
                IconButton(onClick = { biblio.basculerFavori(courant.id) }) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = if (courant.id in favoris) "Retirer des favoris" else "Ajouter aux favoris",
                        tint = if (courant.id in favoris) Or else MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(onClick = { partage = true }) { Icon(Icons.Filled.Share, contentDescription = "Partager") }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Plus") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("📋 Copier les paroles") }, onClick = { menu = false; PartageTexte.copier(context, texteAPartager()) })
                        DropdownMenuItem(text = { Text("✏️ Proposer une correction") }, onClick = { menu = false; a.corriger(courant) })
                        DropdownMenuItem(text = { Text("⛶ Plein écran") }, onClick = { menu = false; pleinEcran = true })
                        DropdownMenuItem(text = { Text("🎙️ Enregistrer ma voix") }, onClick = { menu = false; a.enregistrer(courant) })
                    }
                }
            }
        },
        bottomBar = { if (!pleinEcran) MiniLecteur(a.lecteur) },
    ) { pad ->
        HorizontalPager(pager, Modifier.padding(pad).fillMaxSize(), key = { liste[it].id }) { page ->
            val c = liste[page]
            if (pleinEcran) PleinEcran(c, langueTrad, reglages.tailleTexte + 4, reglages.afficherVoix) { pleinEcran = false }
            else PageCantique(c, langueTrad, reglages.tailleTexte, reglages.afficherVoix, a, choisirTraduction = { traduction = it?.code }, pleinEcran = { pleinEcran = true })
        }
    }

    if (partage) {
        AlertDialog(
            onDismissRequest = { partage = false },
            title = { Text("Partager ce cantique") },
            text = {
                Column {
                    TextButton(onClick = { partage = false; PartageTexte.whatsapp(context, texteAPartager()) }) { Text("💬  WhatsApp", color = VertWhatsApp) }
                    TextButton(onClick = { partage = false; PartageTexte.facebook(context, texteAPartager()) }) { Text("📘  Facebook") }
                    TextButton(onClick = { partage = false; PartageTexte.menuAndroid(context, texteAPartager()) }) { Text("📤  Autres applications") }
                    TextButton(onClick = { partage = false; PartageTexte.copier(context, texteAPartager()) }) { Text("📋  Copier les paroles") }
                }
            },
            confirmButton = { TextButton(onClick = { partage = false }) { Text("Fermer") } },
        )
    }
}

@Composable
private fun PageCantique(
    c: Cantique,
    langueTrad: LangueTraduction?,
    taille: Float,
    afficherVoix: Boolean,
    a: ActionsCantique,
    choisirTraduction: (LangueTraduction?) -> Unit,
    pleinEcran: () -> Unit,
) {
    val trad = langueTrad?.let { c.traduction(it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("Cantique n° ${c.numero}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 13.sp)
        Text(c.titre, fontWeight = FontWeight.Black, fontSize = (taille + 4).sp, lineHeight = (taille + 10).sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pastille(c.langue.libelle, BleuNuit)
            if (c.categorie.isNotBlank()) Pastille(c.categorie, Or, couleurTexte = BleuNuit)
            if (c.audios.isNotEmpty()) Pastille("🎧 Audio", Or, couleurTexte = BleuNuit)
        }

        // Taille du texte, plein écran
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { ReglagesRepository.modifier { it.copy(tailleTexte = it.tailleTexte - 2) } }) { Text("A−") }
            OutlinedButton(onClick = { ReglagesRepository.modifier { it.copy(tailleTexte = it.tailleTexte + 2) } }) { Text("A+", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = pleinEcran) { Text("⛶ Plein écran") }
        }

        // Traductions
        Row(
            Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = langueTrad == null, onClick = { choisirTraduction(null) }, label = { Text(c.langue.libelle) })
            LangueTraduction.entries.forEach { l ->
                FilterChip(selected = langueTrad == l, onClick = { choisirTraduction(l) }, label = { Text("🌐 ${l.libelle}") })
            }
        }
        if (langueTrad != null) {
            Spacer(Modifier.height(8.dp))
            when {
                trad == null -> Info("Pas encore de traduction en ${langueTrad.libelle.lowercase()} pour ce cantique. Les paroles originales sont affichées.")
                !trad.validee -> Info("Traduction provisoire, pas encore validée par l'administration.")
            }
        }

        Spacer(Modifier.height(14.dp))
        if (c.strophes.isEmpty()) Info("Les paroles de ce cantique n'ont pas encore été saisies.")
        c.strophes.forEachIndexed { i, s ->
            val texte = trad?.strophes?.getOrNull(i)?.takeIf { it.isNotBlank() } ?: s.texte
            BlocStrophe(s.voix, s.estRefrain, texte, taille, afficherVoix)
        }

        // Audio
        TitreSection("Écouter")
        if (c.audios.isEmpty()) {
            Text("Aucun audio n'est encore associé à ce cantique.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            c.audios.forEach { p -> LignePiste(c, p, a.lecteur) }
        }

        // Crédits et version
        TitreSection("Crédits et droits")
        Text(c.credits.ifBlank { "Auteur et droits d'utilisation non renseignés." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Version du texte : ${c.version}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { a.corriger(c) }, modifier = Modifier.fillMaxWidth()) { Text("✏️ Signaler une erreur / Proposer une correction") }

        // Fonction existante : enregistrer sa voix et l'envoyer au créateur.
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎙️ MON ENREGISTREMENT", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                GrosBouton("🔴 ENREGISTRER CE CANTIQUE", Rouge) { a.enregistrer(c) }
                Spacer(Modifier.height(8.dp))
                Text(
                    "« Chantez ce cantique et envoyez votre enregistrement au créateur. »",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("← Balayez pour le cantique précédent ou suivant →", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Info(texte: String) {
    Text(
        texte,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)).padding(10.dp),
    )
}

/** Une strophe : étiquette Solo / Tous, refrain en italique avec un trait doré. */
@Composable
private fun BlocStrophe(voix: String, refrain: Boolean, texte: String, taille: Float, afficherVoix: Boolean, couleur: Color = Color.Unspecified) {
    Row(Modifier.padding(bottom = 18.dp).height(IntrinsicSize.Min)) {
        if (refrain) Box(Modifier.width(3.dp).fillMaxHeight().background(Or))
        Column(Modifier.padding(start = if (refrain) 12.dp else 0.dp)) {
            val etiquette = if (refrain) "Refrain" else voix
            if (afficherVoix && etiquette.isNotBlank()) {
                Text(etiquette.uppercase(), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            }
            Text(
                texte,
                fontSize = taille.sp,
                lineHeight = (taille * 1.45f).sp,
                fontStyle = if (refrain) FontStyle.Italic else FontStyle.Normal,
                color = couleur,
            )
        }
    }
}

@Composable
private fun LignePiste(c: Cantique, p: PisteAudio, ouvrirLecteur: () -> Unit) {
    val context = LocalContext.current
    val tel = remember { Telechargements.get(context) }
    val enCours by tel.enCours.collectAsState()
    val liste by tel.liste.collectAsState()
    val telecharge = liste.any { it.pisteId == p.id }
    val prog = enCours[p.id]
    Card(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.type.libelle, fontWeight = FontWeight.SemiBold)
                    val infos = listOfNotNull(
                        p.dureeMs.takeIf { it > 0 }?.let { Formats.duree(it) },
                        p.tailleOctets.takeIf { it > 0 }?.let { Formats.taille(it) },
                        if (telecharge) "téléchargé" else null,
                    )
                    if (infos.isNotEmpty()) Text(infos.joinToString(" · "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    LecteurCantiques.jouer(context, listeDeLecture(c, p), indexDe(c, p))
                    ouvrirLecteur()
                }) { Text("▶ Écouter", fontWeight = FontWeight.Bold) }
                when {
                    telecharge -> TextButton(onClick = { tel.supprimer(p.id) }) { Text("🗑", fontSize = 16.sp) }
                    prog != null && prog.erreur == null -> TextButton(onClick = { tel.annuler(p) }) { Text("Annuler") }
                    else -> TextButton(onClick = { tel.oublierErreur(p.id); tel.telecharger(c, p) }) { Text("⬇") }
                }
            }
            if (prog != null) {
                if (prog.erreur != null) {
                    Text(prog.erreur, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                } else {
                    LinearProgressIndicator(progress = { prog.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = Or)
                    Text(
                        if (prog.total > 0) "${Formats.taille(prog.recu)} / ${Formats.taille(prog.total)}" else Formats.taille(prog.recu),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Liste de lecture : les cantiques du même recueil qui ont un audio,
 * avec de préférence le même type de piste (chantée, instrumentale, répétition).
 */
fun listeDeLecture(depart: Cantique, piste: PisteAudio): List<ElementLecture> =
    Recueil.cantiques.filter { it.langue == depart.langue && it.audios.isNotEmpty() }.map { c ->
        if (c.id == depart.id) ElementLecture(c, piste)
        else ElementLecture(c, c.audios.firstOrNull { it.type == piste.type } ?: c.audios.first())
    }

fun indexDe(depart: Cantique, piste: PisteAudio): Int =
    listeDeLecture(depart, piste).indexOfFirst { it.cantique.id == depart.id }.coerceAtLeast(0)

/** Plein écran : paroles seules, grandes, pour les répétitions et les cultes. Toucher le titre pour sortir. */
@Composable
private fun PleinEcran(c: Cantique, langueTrad: LangueTraduction?, taille: Float, afficherVoix: Boolean, sortir: () -> Unit) {
    val trad: Traduction? = langueTrad?.let { c.traduction(it) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth().clickable(onClick = sortir), verticalAlignment = Alignment.CenterVertically) {
            Text("${c.numero}. ${c.titre}", fontWeight = FontWeight.Black, fontSize = (taille + 2).sp, modifier = Modifier.weight(1f))
            Text("✕", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Or)
        c.strophes.forEachIndexed { i, s ->
            BlocStrophe(s.voix, s.estRefrain, trad?.strophes?.getOrNull(i)?.takeIf { it.isNotBlank() } ?: s.texte, taille, afficherVoix)
        }
    }
}
