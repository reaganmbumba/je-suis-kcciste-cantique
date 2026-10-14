package com.kcciste.cantique.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kcciste.cantique.BuildConfig
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.Enregistrement
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.share.Partage
import com.kcciste.cantique.ui.theme.Rouge
import com.kcciste.cantique.ui.theme.VertWhatsApp
import java.io.File

internal sealed interface EtapeEnvoi {
    data object Confirmation : EtapeEnvoi
    data object WhatsAppOuvert : EtapeEnvoi
    data object WhatsAppAbsent : EtapeEnvoi
    data object MenuOuvert : EtapeEnvoi
    data class Erreur(val message: String) : EtapeEnvoi
}

@Stable
class EnvoiCreateur {
    internal var enregistrement by mutableStateOf<Enregistrement?>(null)
    internal var fichier by mutableStateOf<File?>(null)
    internal var etape by mutableStateOf<EtapeEnvoi?>(null)

    /** Rien ne part sans cette confirmation explicite. */
    fun demander(e: Enregistrement, f: File) {
        enregistrement = e
        fichier = f
        etape = EtapeEnvoi.Confirmation
    }

    internal fun fermer() {
        etape = null
    }
}

@Composable
fun rememberEnvoiCreateur(): EnvoiCreateur = remember { EnvoiCreateur() }

private const val MSG_TROP_GROS =
    "L'enregistrement est trop volumineux pour être partagé. Veuillez réduire sa durée."
private const val MSG_INTROUVABLE = "Le fichier audio est introuvable. Il a peut-être été supprimé."

private fun resultatVersEtape(r: Partage.Resultat, siOuvert: EtapeEnvoi): EtapeEnvoi = when (r) {
    Partage.Resultat.Ouvert -> siOuvert
    Partage.Resultat.WhatsAppAbsent -> EtapeEnvoi.WhatsAppAbsent
    Partage.Resultat.TropVolumineux -> EtapeEnvoi.Erreur(MSG_TROP_GROS)
    Partage.Resultat.FichierIntrouvable -> EtapeEnvoi.Erreur(MSG_INTROUVABLE)
}

/** Les dialogues « Avant l'envoi », erreurs et retour de WhatsApp. */
@Composable
fun DialoguesEnvoi(etat: EnvoiCreateur) {
    val context = LocalContext.current
    val e = etat.enregistrement ?: return
    val f = etat.fichier ?: return
    when (val etape = etat.etape) {
        null -> Unit

        EtapeEnvoi.Confirmation -> AlertDialog(
            onDismissRequest = etat::fermer,
            title = { Text("Envoyer cet enregistrement au créateur ?") },
            text = {
                Column {
                    Text("Vous êtes sur le point de partager cet enregistrement avec ${Createur.NOM} sur WhatsApp.")
                    Spacer(Modifier.height(12.dp))
                    Text("🎵 Cantique : ${e.titre}")
                    Text("🌍 Langue : ${e.langue.libelle}")
                    Text("⏱ Durée : ${Formats.duree(e.dureeMs)}")
                    Text("📁 Fichier : ${e.nomFichier}")
                    Spacer(Modifier.height(8.dp))
                    Text("📱 ${Createur.TELEPHONE_AFFICHE} (WhatsApp uniquement)")
                }
            },
            dismissButton = { TextButton(onClick = etat::fermer) { Text("ANNULER") } },
            confirmButton = {
                TextButton(onClick = {
                    etat.etape = resultatVersEtape(
                        Partage.envoyerAuCreateur(context, e, f),
                        EtapeEnvoi.WhatsAppOuvert,
                    )
                }) { Text("💬 CONTINUER VERS WHATSAPP", color = VertWhatsApp, fontWeight = FontWeight.Bold) }
            },
        )

        // Affiché quand l'utilisateur revient dans l'application : on ne prétend jamais que c'est envoyé.
        EtapeEnvoi.WhatsAppOuvert -> AlertDialog(
            onDismissRequest = etat::fermer,
            title = { Text("WhatsApp a été ouvert") },
            text = {
                Text(
                    "Votre enregistrement et le message ont été préparés dans WhatsApp.\n\n" +
                        "L'envoi n'a lieu que si vous avez appuyé sur le bouton Envoyer dans WhatsApp.\n\n" +
                        "Si la conversation de ${Createur.NOM} ne s'est pas ouverte automatiquement, " +
                        "choisissez le contact ${Createur.TELEPHONE_AFFICHE}.\n\n" +
                        "Le message a aussi été copié : vous pouvez le coller si WhatsApp ne l'a pas repris.",
                )
            },
            confirmButton = { TextButton(onClick = etat::fermer) { Text("OK") } },
        )

        EtapeEnvoi.MenuOuvert -> AlertDialog(
            onDismissRequest = etat::fermer,
            title = { Text("Menu de partage ouvert") },
            text = {
                Text(
                    "L'enregistrement n'est envoyé que si vous terminez l'envoi dans l'application choisie.\n\n" +
                        "Destinataire : ${Createur.NOM}, ${Createur.TELEPHONE_AFFICHE}.\n" +
                        "Le message a été copié dans le presse-papiers.",
                )
            },
            confirmButton = { TextButton(onClick = etat::fermer) { Text("OK") } },
        )

        EtapeEnvoi.WhatsAppAbsent -> AlertDialog(
            onDismissRequest = etat::fermer,
            title = { Text("WhatsApp introuvable") },
            text = {
                Text(
                    "WhatsApp n'est pas installé sur cet appareil.\n\n" +
                        "Vous pouvez partager le fichier autrement : e-mail, autre application compatible, " +
                        "ou vous l'envoyer pour le transmettre ensuite depuis WhatsApp Web " +
                        "au ${Createur.TELEPHONE_AFFICHE}.",
                )
            },
            dismissButton = { TextButton(onClick = etat::fermer) { Text("ANNULER") } },
            confirmButton = {
                TextButton(onClick = {
                    etat.etape = resultatVersEtape(
                        Partage.partagerAutrement(context, e, f, pourCreateur = true),
                        EtapeEnvoi.MenuOuvert,
                    )
                }) { Text("PARTAGER AUTREMENT", fontWeight = FontWeight.Bold) }
            },
        )

        is EtapeEnvoi.Erreur -> AlertDialog(
            onDismissRequest = etat::fermer,
            title = { Text("Envoi impossible", color = Rouge) },
            text = {
                Column {
                    Text(etape.message)
                    if (etape.message == MSG_TROP_GROS) {
                        Text("\nLimite actuelle : ${BuildConfig.MAX_SHARE_SIZE_MB} Mo. Taille du fichier : ${Formats.taille(f.length())}.")
                    }
                }
            },
            confirmButton = { TextButton(onClick = etat::fermer) { Text("OK") } },
        )
    }
}

/** Partage simple (📤 Partager) depuis « Mes enregistrements ». */
fun partagerSimple(context: android.content.Context, e: Enregistrement, f: File): String? =
    when (Partage.partagerAutrement(context, e, f, pourCreateur = false)) {
        Partage.Resultat.Ouvert -> null
        Partage.Resultat.TropVolumineux -> MSG_TROP_GROS
        else -> MSG_INTROUVABLE
    }
