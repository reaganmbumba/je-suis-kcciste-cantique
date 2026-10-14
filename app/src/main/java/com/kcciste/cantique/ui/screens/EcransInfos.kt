package com.kcciste.cantique.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.BuildConfig
import com.kcciste.cantique.data.Bibliotheque
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.EnregistrementsRepository
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.data.Reglages
import com.kcciste.cantique.data.ReglagesRepository
import com.kcciste.cantique.data.Statut
import com.kcciste.cantique.data.Theme
import com.kcciste.cantique.share.PartageTexte
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.CarteContactCreateur
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.LogoKCC
import com.kcciste.cantique.ui.components.TitreSection
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.Or
import com.kcciste.cantique.ui.theme.VertWhatsApp

@Composable
private fun PageInfo(titre: String, retour: () -> Unit, contact: Boolean = true, contenu: @Composable () -> Unit) {
    Scaffold(topBar = { Barre(titre, retour) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            contenu()
            if (contact) {
                Spacer(Modifier.height(24.dp))
                CarteContactCreateur()
            }
        }
    }
}

@Composable
fun EcranAPropos(retour: () -> Unit) = PageInfo("À propos", retour) {
    LogoKCC(120.dp)
    Spacer(Modifier.height(12.dp))
    Text(Createur.APPLICATION, fontWeight = FontWeight.Black, fontSize = 24.sp, textAlign = TextAlign.Center)
    Text("Cantiques Kikongo & Lingala · traductions portugais et anglais", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    Spacer(Modifier.height(16.dp))
    Text(
        "Recueil numérique de cantiques avec paroles, lecteur audio, téléchargement hors connexion, " +
            "traductions, partage et corrections communautaires. Vous pouvez aussi enregistrer votre voix " +
            "et l'envoyer au concepteur, et échanger dans le groupe KCCiste.",
        textAlign = TextAlign.Center,
    )
    TitreSection("Copyright", Modifier.fillMaxWidth())
    Text(
        "© ${Createur.MARQUE} — ${Createur.COMMUNAUTE}\nConception : ${Createur.NOM}",
        textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Cette signature concerne l'application. Les paroles et les audios restent la propriété de leurs auteurs ; " +
            "les crédits et droits de chaque cantique sont indiqués sur sa page lorsqu'ils sont connus.",
        textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TitreSection("Confidentialité", Modifier.fillMaxWidth())
    Text(
        "Favoris, historique, enregistrements et audios téléchargés restent sur votre téléphone. " +
            "Seuls les messages du groupe et les suggestions de correction que vous envoyez sont transmis, " +
            "après une action de votre part.",
        textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun EcranAide(retour: () -> Unit) = PageInfo("Aide", retour) {
    val etapes = listOf(
        "Rechercher : tapez un numéro, un mot du titre ou des paroles. Les accents ne comptent pas.",
        "Lire : dans un cantique, A− / A+ changent la taille du texte, « Plein écran » affiche les paroles en grand. Balayez pour passer au suivant.",
        "Écouter : « ▶ Écouter » lance l'audio, qui continue écran éteint. « ⬇ » le télécharge pour l'écouter sans Internet.",
        "Traductions : choisissez Portugais ou Anglais. Une traduction « provisoire » n'a pas encore été validée.",
        "Corriger : « Proposer une correction » envoie votre texte ou votre message vocal à l'administration, avec un numéro de référence.",
        "Enregistrer sa voix : « 🔴 ENREGISTRER CE CANTIQUE », puis « 💬 ENVOYER À REAGAN MBUMBA ». L'envoi se fait quand vous appuyez sur Envoyer dans WhatsApp.",
        "Durée maximale d'un enregistrement : ${Formats.duree(BuildConfig.MAX_RECORDING_SECONDS * 1000L)}.",
    )
    Text("Comment utiliser l'application", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    etapes.forEachIndexed { i, t -> Text("${i + 1}. $t", Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
}

@Composable
fun EcranProfil(retour: () -> Unit, mesEnregistrements: () -> Unit) = PageInfo("Mon profil", retour) {
    val context = LocalContext.current
    val liste by remember { EnregistrementsRepository.get(context) }.liste.collectAsState()
    Text("👤", fontSize = 48.sp)
    Text("Mes statistiques", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text("${liste.size} enregistrement(s) sauvegardé(s)")
    Text("${liste.count { it.statut == Statut.PARTAGE }} partagé(s)")
    Text("Durée totale : ${Formats.duree(liste.sumOf { it.dureeMs })}")
    Spacer(Modifier.height(12.dp))
    GrosBouton("🎙️ MES ENREGISTREMENTS", Bleu, onClick = mesEnregistrements)
}

@Composable
fun EcranContact(retour: () -> Unit) = PageInfo("Contact", retour, contact = false) {
    val context = LocalContext.current
    var motif by rememberSaveable { mutableStateOf<String?>(null) }
    LogoKCC(90.dp)
    Spacer(Modifier.height(10.dp))
    Text(Createur.NOM, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text("WhatsApp : ${Createur.TELEPHONE_AFFICHE}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    TitreSection("Motif du contact", Modifier.fillMaxWidth())
    Createur.MOTIFS.forEach { m ->
        Row(
            Modifier.fillMaxWidth().selectable(selected = motif == m, onClick = { motif = m }).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = motif == m, onClick = { motif = m })
            Text(m)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Message envoyé :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
    Text("« ${Createur.messageContact(motif)} »", fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    GrosBouton("💬 OUVRIR WHATSAPP", VertWhatsApp) {
        if (!PartageTexte.contacter(context, motif)) {
            Toast.makeText(context, "Impossible d'ouvrir ${Createur.LIEN_WHATSAPP}", Toast.LENGTH_LONG).show()
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("Vous pourrez relire et modifier le message dans WhatsApp avant de l'envoyer.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
fun EcranParametres(retour: () -> Unit, telechargements: () -> Unit, admin: () -> Unit, aide: () -> Unit, profil: () -> Unit) =
    PageInfo("Paramètres", retour, contact = false) {
        val context = LocalContext.current
        val r by ReglagesRepository.flux.collectAsState()
        Column(Modifier.fillMaxWidth()) {
            TitreSection("Apparence")
            Theme.entries.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().selectable(selected = r.theme == t, onClick = { ReglagesRepository.modifier { it.copy(theme = t) } }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = r.theme == t, onClick = { ReglagesRepository.modifier { it.copy(theme = t) } })
                    Text(t.libelle)
                }
            }
            TitreSection("Lecture des paroles")
            Text("Taille du texte : ${r.tailleTexte.toInt()}")
            Slider(
                value = r.tailleTexte,
                onValueChange = { v -> ReglagesRepository.modifier { it.copy(tailleTexte = v) } },
                valueRange = Reglages.TAILLE_MIN..Reglages.TAILLE_MAX,
                colors = SliderDefaults.colors(thumbColor = Or, activeTrackColor = Or),
            )
            Text("Exemple de paroles", fontSize = r.tailleTexte.sp, lineHeight = (r.tailleTexte * 1.4f).sp)
            Interrupteur("Garder l'écran allumé pendant la lecture", r.ecranAllume) { v -> ReglagesRepository.modifier { it.copy(ecranAllume = v) } }
            Interrupteur("Afficher « Solo » / « Tous »", r.afficherVoix) { v -> ReglagesRepository.modifier { it.copy(afficherVoix = v) } }

            TitreSection("Données")
            Lien("⬇ Gérer les téléchargements", telechargements)
            Lien("🕘 Effacer l'historique") {
                Bibliotheque.get(context).viderHistorique()
                Toast.makeText(context, "Historique effacé", Toast.LENGTH_SHORT).show()
            }
            Lien("👤 Mon profil", profil)
            Lien("❓ Aide", aide)
            TitreSection("Administration")
            Lien("🔐 Espace administrateur", admin)
        }
    }

@Composable
private fun Interrupteur(texte: String, valeur: Boolean, changer: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texte, Modifier.weight(1f))
        Switch(checked = valeur, onCheckedChange = changer)
    }
}

@Composable
private fun Lien(texte: String, onClick: () -> Unit) {
    Text(texte, Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

