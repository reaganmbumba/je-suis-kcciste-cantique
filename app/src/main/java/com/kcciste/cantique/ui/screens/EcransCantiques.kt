package com.kcciste.cantique.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Langue
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.Pastille
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.BleuClair
import com.kcciste.cantique.ui.theme.Gris
import com.kcciste.cantique.ui.theme.Or
import com.kcciste.cantique.ui.theme.Rouge

@Composable
fun EcranAccueil(
    ouvrir: (Cantique) -> Unit,
    mesEnregistrements: () -> Unit,
    aPropos: () -> Unit,
    aide: () -> Unit,
    profil: () -> Unit,
    discussion: () -> Unit,
) {
    var langue by rememberSaveable { mutableStateOf(Langue.LINGALA) }
    var recherche by rememberSaveable { mutableStateOf("") }
    val liste = Recueil.cantiques.filter { c ->
        c.langue == langue && (recherche.isBlank() || c.titre.contains(recherche.trim(), ignoreCase = true) ||
            c.numero.toString() == recherche.trim())
    }
    Scaffold(topBar = { Barre("KCCiste Cantique", null) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                Card(
                    Modifier.fillMaxWidth().clickable(onClick = mesEnregistrements),
                    colors = CardDefaults.cardColors(containerColor = Bleu),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🎙️ MES ENREGISTREMENTS", color = Or, fontWeight = FontWeight.Black)
                        Text("Écoutez, partagez ou envoyez vos interprétations au créateur.", color = BleuClair, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    Modifier.fillMaxWidth().clickable(onClick = discussion),
                    colors = CardDefaults.cardColors(containerColor = BleuClair),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("💬 GROUPE DE DISCUSSION", color = Bleu, fontWeight = FontWeight.Black)
                        Text("Échangez des messages avec tous les membres KCCiste.", color = Gris, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = profil, Modifier.weight(1f)) { Text("Profil", fontSize = 12.sp) }
                    OutlinedButton(onClick = aide, Modifier.weight(1f)) { Text("Aide", fontSize = 12.sp) }
                    OutlinedButton(onClick = aPropos, Modifier.weight(1f)) { Text("À propos", fontSize = 12.sp) }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Langue.entries.forEach { l ->
                        FilterChip(selected = langue == l, onClick = { langue = l }, label = { Text("Cantiques ${l.libelle}") })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    placeholder = { Text("Rechercher un cantique (titre ou numéro)…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${liste.size} cantique(s) ${langue.libelle}", color = Gris, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            items(liste, key = { "${it.langue}-${it.numero}" }) { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { ouvrir(c) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.numeroFormate, color = Bleu, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                    Text(c.titre, Modifier.weight(1f))
                    Text("🎙️", fontSize = 16.sp)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EcranCantique(cantique: Cantique, retour: () -> Unit, enregistrer: () -> Unit) {
    Scaffold(topBar = { Barre("${cantique.numeroFormate}. ${cantique.titre}", retour) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(cantique.titre.uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = Bleu)
            Pastille(cantique.langue.libelle, Bleu)
            Spacer(Modifier.height(16.dp))
            cantique.strophes.forEach { s ->
                if (s.voix.isNotEmpty()) Pastille(s.voix, Or, couleurTexte = Bleu)
                Text(s.texte, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Section « MON ENREGISTREMENT »
            Card(colors = CardDefaults.cardColors(containerColor = BleuClair), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎙️ MON ENREGISTREMENT", fontWeight = FontWeight.Black, color = Bleu, fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    GrosBouton("🔴 ENREGISTRER CE CANTIQUE", Rouge, onClick = enregistrer)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "« Chantez ce cantique et envoyez votre enregistrement au créateur. »",
                        color = Gris, textAlign = TextAlign.Center, fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
