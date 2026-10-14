package com.kcciste.cantique.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.R
import com.kcciste.cantique.data.Bibliotheque
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.Langue
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.ui.components.CarteMenu
import com.kcciste.cantique.ui.components.LigneCantique
import com.kcciste.cantique.ui.components.LogoKCC
import com.kcciste.cantique.ui.components.MiniLecteur
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.Or

/** Destinations ouvertes depuis l'accueil. */
class ActionsAccueil(
    val ouvrir: (Cantique) -> Unit,
    val bibliotheque: (Langue?) -> Unit,
    val recherche: () -> Unit,
    val favoris: () -> Unit,
    val historique: () -> Unit,
    val telechargements: () -> Unit,
    val mesEnregistrements: () -> Unit,
    val discussion: () -> Unit,
    val contact: () -> Unit,
    val parametres: () -> Unit,
    val aPropos: () -> Unit,
    val aide: () -> Unit,
    val lecteur: () -> Unit,
)

@Composable
fun EcranAccueil(a: ActionsAccueil) {
    val context = LocalContext.current
    val cantiques by Recueil.flux.collectAsState()
    val biblio = remember { Bibliotheque.get(context) }
    val historique by biblio.historique.collectAsState()
    val favoris by biblio.favoris.collectAsState()
    var allerA by remember { mutableStateOf(false) }

    Scaffold(bottomBar = { MiniLecteur(a.lecteur) }) { pad ->
        Column(Modifier.padding(bottom = pad.calculateBottomPadding()).fillMaxSize().verticalScroll(rememberScrollState())) {
            Banniere(a)

            Column(Modifier.padding(horizontal = 16.dp)) {
                TitreSection("Recueils")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Langue.entries.forEach { l ->
                        CarteRecueil(l, cantiques.count { it.langue == l }, Modifier.weight(1f)) { a.bibliotheque(l) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Card(
                    Modifier.fillMaxWidth().clickable { allerA = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔢", fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Aller au cantique n°…", fontWeight = FontWeight.Bold)
                            Text("Tapez un numéro pour l'ouvrir directement", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                TitreSection("Mon espace")
                val menu = listOf(
                    Triple("★", "Favoris", "${favoris.size} cantique(s)") to a.favoris,
                    Triple("🕘", "Historique", "Derniers consultés") to a.historique,
                    Triple("⬇", "Téléchargements", "Audios hors ligne") to a.telechargements,
                    Triple("🎙️", "Mes enregistrements", "Ma voix, envoi au créateur") to a.mesEnregistrements,
                    Triple("💬", "Groupe KCCiste", "Discussion de groupe") to a.discussion,
                    Triple("📱", "Contact", "WhatsApp du concepteur") to a.contact,
                    Triple("⚙️", "Paramètres", "Thème, taille du texte") to a.parametres,
                    Triple("ℹ️", "À propos", "Copyright et crédits") to a.aPropos,
                )
                menu.chunked(2).forEach { ligne ->
                    Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ligne.forEach { (t, action) ->
                            CarteMenu(t.first, t.second, t.third, Modifier.weight(1f).heightIn(min = 104.dp), onClick = action)
                        }
                    }
                }

                val recents = historique.mapNotNull { id -> cantiques.firstOrNull { it.id == id } }.take(5)
                if (recents.isNotEmpty()) TitreSection("Récemment consultés")
            }
            historique.mapNotNull { id -> cantiques.firstOrNull { it.id == id } }.take(5).forEach { c ->
                LigneCantique(c.numeroFormate, c.titre, c.langue.libelle, c.audios.isNotEmpty(), false, c.id in favoris) { a.ouvrir(c) }
            }
            Text(
                "© ${Createur.MARQUE} — ${Createur.COMMUNAUTE}\nConception : ${Createur.NOM}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }
    }

    if (allerA) {
        DialogueAllerA(cantiques, fermer = { allerA = false }) { allerA = false; a.ouvrir(it) }
    }
}

@Composable
private fun Banniere(a: ActionsAccueil) {
    Box(Modifier.fillMaxWidth().height(330.dp)) {
        Image(
            painterResource(R.drawable.banniere_accueil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        // Dégradé bleu nuit pour que le texte reste lisible sur la photo.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to BleuNuit.copy(alpha = 0.55f),
                    0.45f to Color.Transparent,
                    1f to BleuNuit.copy(alpha = 0.92f),
                ),
            ),
        )
        Row(Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LogoKCC(44.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Je suis KCCiste", color = Or, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(Createur.MARQUE, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
            }
            IconButton(onClick = a.parametres) { Icon(Icons.Filled.Settings, contentDescription = "Paramètres", tint = Color.White) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("Cantiques Lingala & Kikongo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Paroles, audio et traductions", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .clickable(onClick = a.recherche)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = BleuNuit)
                Spacer(Modifier.width(10.dp))
                Text("Numéro, titre ou paroles…", color = Color(0xFF6B7080))
            }
        }
    }
}

@Composable
private fun CarteRecueil(langue: Langue, nombre: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BleuNuit),
        border = BorderStroke(1.dp, Or),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("📖", fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(langue.libelle, color = Or, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("$nombre cantiques", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}

@Composable
fun DialogueAllerA(cantiques: List<Cantique>, fermer: () -> Unit, ouvrir: (Cantique) -> Unit) {
    var numero by rememberSaveable { mutableStateOf("") }
    var langue by rememberSaveable { mutableStateOf(Langue.LINGALA) }
    val trouve = numero.toIntOrNull()?.let { n -> cantiques.firstOrNull { it.langue == langue && it.numero == n } }
    AlertDialog(
        onDismissRequest = fermer,
        title = { Text("Aller au cantique n°") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Langue.entries.forEach { l ->
                        androidx.compose.material3.FilterChip(selected = langue == l, onClick = { langue = l }, label = { Text(l.libelle) })
                    }
                }
                OutlinedTextField(
                    value = numero,
                    onValueChange = { v -> numero = v.filter { it.isDigit() }.take(4) },
                    label = { Text("Numéro") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { trouve?.let(ouvrir) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        numero.isBlank() -> " "
                        trouve != null -> trouve.titre
                        else -> "Aucun cantique ${langue.libelle} n° $numero"
                    },
                    color = if (trouve == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                )
            }
        },
        confirmButton = { TextButton(onClick = { trouve?.let(ouvrir) }, enabled = trouve != null) { Text("Ouvrir") } },
        dismissButton = { TextButton(onClick = fermer) { Text("Annuler") } },
    )
}
