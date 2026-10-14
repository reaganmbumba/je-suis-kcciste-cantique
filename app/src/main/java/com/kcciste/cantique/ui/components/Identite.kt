package com.kcciste.cantique.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.ui.theme.Or

/** Logo KCC : lettres majuscules dorées, cercle doré complet, fond blanc pur (même en mode sombre). */
@Composable
fun LogoKCC(taille: Dp, modifier: Modifier = Modifier) {
    val tailleTexte = with(LocalDensity.current) { (taille * 0.27f).toSp() }
    val espacement = with(LocalDensity.current) { (taille * 0.02f).toSp() }
    Box(
        modifier
            .size(taille)
            .clip(CircleShape)
            .background(Color.White)
            .border(taille * 0.045f, Or, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "KCC",
            color = Or,
            fontSize = tailleTexte,
            letterSpacing = espacement,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            maxLines = 1,
        )
    }
}

/** Carte de menu de l'accueil : icône (emoji), titre, sous-titre. */
@Composable
fun CarteMenu(icone: String, titre: String, sousTitre: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(icone, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(titre, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sousTitre, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Écran vide propre (recueil non importé, aucun favori…). */
@Composable
fun EtatVide(icone: String, titre: String, texte: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icone, fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(titre, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(texte, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Titre de section avec un petit trait doré. */
@Composable
fun TitreSection(texte: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).background(Or, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(texte, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Ligne d'un cantique dans une liste : numéro doré, titre, indicateurs audio / téléchargé / favori. */
@Composable
fun LigneCantique(
    numero: String,
    titre: String,
    sousTitre: String?,
    audio: Boolean,
    telecharge: Boolean,
    favori: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(42.dp).border(1.5.dp, Or, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(numero, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Column(Modifier.weight(1f)) {
            Text(titre, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!sousTitre.isNullOrBlank()) {
                Text(sousTitre, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (favori) Text("★", color = Or, fontSize = 16.sp)
        if (telecharge) Text("⬇", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
        else if (audio) Text("🎧", fontSize = 14.sp)
    }
}
