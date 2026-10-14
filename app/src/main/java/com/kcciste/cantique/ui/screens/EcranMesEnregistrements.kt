package com.kcciste.cantique.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.data.Enregistrement
import com.kcciste.cantique.data.EnregistrementsRepository
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.data.Statut
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.DialoguesEnvoi
import com.kcciste.cantique.ui.components.LecteurAudio
import com.kcciste.cantique.ui.components.partagerSimple
import com.kcciste.cantique.ui.components.rememberEnvoiCreateur
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.Gris
import com.kcciste.cantique.ui.theme.Rouge
import com.kcciste.cantique.ui.theme.VertWhatsApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EcranMesEnregistrements(retour: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { EnregistrementsRepository.get(context) }
    val liste by repo.liste.collectAsState()
    val envoi = rememberEnvoiCreateur()
    var enEcoute by remember { mutableStateOf<String?>(null) }
    var aSupprimer by remember { mutableStateOf<Enregistrement?>(null) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    Scaffold(topBar = { Barre("🎙️ Mes enregistrements", retour) }) { pad ->
        if (liste.isEmpty()) {
            Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aucun enregistrement pour le moment.", fontWeight = FontWeight.Bold)
                Text(
                    "Ouvrez un cantique puis appuyez sur « 🔴 ENREGISTRER CE CANTIQUE ». Vos enregistrements restent privés sur ce téléphone.",
                    color = Gris, textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(liste, key = { it.nomFichier }) { e ->
                    val fichier = repo.fichier(e)
                    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF6F8FE))) {
                        Column(Modifier.padding(14.dp)) {
                            Text("🎵 ${e.titre}", fontWeight = FontWeight.Bold, color = Bleu)
                            Text(e.langue.libelle, color = Gris, fontSize = 13.sp)
                            Text(
                                "${dateFmt.format(Date(e.dateMillis))} · ${Formats.duree(e.dureeMs)} · ${Formats.taille(e.tailleOctets)}",
                                fontSize = 13.sp,
                            )
                            Text(
                                if (e.statut == Statut.PARTAGE) "📤 Partagé" else "🟢 Sauvegardé",
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            )
                            if (enEcoute == e.nomFichier) LecteurAudio(fichier, e.dureeMs)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { enEcoute = if (enEcoute == e.nomFichier) null else e.nomFichier }) {
                                    Text(if (enEcoute == e.nomFichier) "✕ Fermer" else "▶ Écouter", fontSize = 12.sp)
                                }
                                TextButton(onClick = {
                                    partagerSimple(context, e, fichier)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                }) { Text("📤 Partager", fontSize = 12.sp) }
                                TextButton(onClick = { envoi.demander(e, fichier) }) {
                                    Text("💬 Créateur", fontSize = 12.sp, color = VertWhatsApp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { aSupprimer = e }) { Text("🗑", fontSize = 14.sp, color = Rouge) }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Les enregistrements restent privés sur cet appareil. Rien n'est envoyé sans votre confirmation.", color = Gris, fontSize = 12.sp)
                }
            }
        }
    }

    DialoguesEnvoi(envoi)

    aSupprimer?.let { e ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Supprimer l'enregistrement ?") },
            text = { Text("« ${e.titre} » sera définitivement effacé de cet appareil.") },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("ANNULER") } },
            confirmButton = {
                TextButton(onClick = {
                    if (enEcoute == e.nomFichier) enEcoute = null
                    repo.supprimer(e.nomFichier)
                    aSupprimer = null
                }) { Text("SUPPRIMER", color = Rouge) }
            },
        )
    }
}
