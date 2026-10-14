package com.kcciste.cantique.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.BuildConfig
import com.kcciste.cantique.data.EnregistrementsRepository
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.data.Statut
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.CarteContactCreateur
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.Gris

@Composable
private fun PageInfo(titre: String, retour: () -> Unit, contenu: @Composable () -> Unit) {
    Scaffold(topBar = { Barre(titre, retour) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            contenu()
            Spacer(Modifier.height(24.dp))
            CarteContactCreateur()
        }
    }
}

@Composable
fun EcranAPropos(retour: () -> Unit) = PageInfo("À propos", retour) {
    Text("KCCiste Cantique", fontWeight = FontWeight.Black, fontSize = 26.sp, color = Bleu)
    Text("Cantiques Lingala & Kikongo", color = Gris)
    Text("Version ${BuildConfig.VERSION_NAME}", color = Gris, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
    Text(
        "Application mobile permettant de consulter les recueils de cantiques Lingala et Kikongo, " +
            "d'enregistrer son interprétation et de l'envoyer au créateur.",
        textAlign = TextAlign.Center,
    )
}

@Composable
fun EcranAide(retour: () -> Unit) = PageInfo("Aide", retour) {
    val etapes = listOf(
        "1. Ouvrez un cantique, puis appuyez sur « 🔴 ENREGISTRER CE CANTIQUE ».",
        "2. Autorisez le microphone lorsque l'application le demande.",
        "3. Appuyez sur 🔴 ENREGISTRER et chantez. Vous pouvez faire ⏸ PAUSE puis ▶ REPRENDRE.",
        "4. Appuyez sur ⏹ ARRÊTER. Écoutez, refaites ou sauvegardez.",
        "5. « 💬 ENVOYER À REAGAN MBUMBA » ouvre WhatsApp avec le fichier et le message préparés. " +
            "L'envoi n'est fait que lorsque vous appuyez vous-même sur Envoyer dans WhatsApp.",
        "6. Si WhatsApp n'ouvre pas directement la bonne conversation, choisissez le contact +243 840 238 592.",
        "Durée maximale d'un enregistrement : ${Formats.duree(BuildConfig.MAX_RECORDING_SECONDS * 1000L)}.",
        "Vos enregistrements restent privés sur ce téléphone, dans « 🎙️ Mes enregistrements ».",
    )
    Text("Comment enregistrer et envoyer un cantique", fontWeight = FontWeight.Bold, color = Bleu, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    etapes.forEach { Text(it, Modifier.padding(vertical = 4.dp)) }
}

@Composable
fun EcranProfil(retour: () -> Unit, mesEnregistrements: () -> Unit) = PageInfo("Mon profil", retour) {
    val context = LocalContext.current
    val liste by remember { EnregistrementsRepository.get(context) }.liste.collectAsState()
    Text("👤", fontSize = 48.sp)
    Text("Mes statistiques", fontWeight = FontWeight.Bold, color = Bleu, fontSize = 18.sp)
    Text("${liste.size} enregistrement(s) sauvegardé(s)")
    Text("${liste.count { it.statut == Statut.PARTAGE }} partagé(s)")
    Text("Durée totale : ${Formats.duree(liste.sumOf { it.dureeMs })}")
    Spacer(Modifier.height(12.dp))
    GrosBouton("🎙️ MES ENREGISTREMENTS", Bleu, onClick = mesEnregistrements)
}
