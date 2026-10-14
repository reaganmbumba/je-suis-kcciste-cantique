package com.kcciste.cantique.share

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.kcciste.cantique.BuildConfig
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.Enregistrement
import com.kcciste.cantique.data.EnregistrementsRepository
import com.kcciste.cantique.data.Formats
import java.io.File

/**
 * Tout ce qui fait sortir un enregistrement de l'appareil passe par ici,
 * et uniquement après une action explicite de l'utilisateur.
 *
 * Rappel important : Android ne renvoie AUCUNE confirmation d'envoi depuis WhatsApp.
 * L'application ne peut savoir que « le partage a été ouvert », jamais « le message est parti ».
 */
object Partage {

    private val PAQUETS_WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")
    private const val MIME_AUDIO = "audio/mp4"

    sealed interface Resultat {
        /** WhatsApp (ou l'app choisie) s'est ouvert avec le fichier. Rien n'est encore envoyé. */
        data object Ouvert : Resultat
        data object WhatsAppAbsent : Resultat
        data object TropVolumineux : Resultat
        data object FichierIntrouvable : Resultat
    }

    fun paquetWhatsApp(context: Context): String? = PAQUETS_WHATSAPP.firstOrNull { pkg ->
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") context.packageManager.getPackageInfo(pkg, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun tropVolumineux(fichier: File): Boolean =
        fichier.length() > BuildConfig.MAX_SHARE_SIZE_MB.toLong() * 1024 * 1024

    fun message(e: Enregistrement): String =
        Formats.messageWhatsApp(e.titre, e.langue.libelle, Formats.duree(e.dureeMs))

    private fun uri(context: Context, fichier: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)

    private fun intentEnvoi(context: Context, fichier: File, message: String): Intent {
        val u = uri(context, fichier)
        return Intent(Intent.ACTION_SEND).apply {
            type = MIME_AUDIO
            putExtra(Intent.EXTRA_STREAM, u)
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "Enregistrement KCCiste Cantique")
            clipData = ClipData.newUri(context.contentResolver, fichier.name, u)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Ouvre WhatsApp avec le fichier audio et le message prêts.
     * L'extra « jid » demande à WhatsApp d'ouvrir directement la conversation du créateur ;
     * si la version installée l'ignore, WhatsApp affiche sa liste de conversations et
     * l'utilisateur choisit lui-même +243 840 238 592.
     * Le message est aussi copié dans le presse-papiers, car WhatsApp n'affiche pas
     * toujours la légende jointe à un fichier audio.
     */
    fun envoyerAuCreateur(context: Context, e: Enregistrement, fichier: File): Resultat {
        if (!fichier.exists()) return Resultat.FichierIntrouvable
        if (tropVolumineux(fichier)) return Resultat.TropVolumineux
        val pkg = paquetWhatsApp(context) ?: return Resultat.WhatsAppAbsent
        val msg = message(e)
        copierMessage(context, msg)
        val intent = intentEnvoi(context, fichier, msg).apply {
            setPackage(pkg)
            putExtra("jid", Createur.JID_WHATSAPP)
        }
        return try {
            context.startActivity(intent)
            EnregistrementsRepository.get(context).marquerPartage(e.nomFichier)
            Resultat.Ouvert
        } catch (_: ActivityNotFoundException) {
            Resultat.WhatsAppAbsent
        }
    }

    /**
     * Menu de partage Android (WhatsApp, WhatsApp Web via navigateur, e-mail, Bluetooth…).
     * Le statut « Partagé » n'est posé que si l'utilisateur choisit réellement une application
     * (voir [ChoixPartageReceiver]).
     */
    fun partagerAutrement(context: Context, e: Enregistrement, fichier: File, pourCreateur: Boolean): Resultat {
        if (!fichier.exists()) return Resultat.FichierIntrouvable
        if (tropVolumineux(fichier)) return Resultat.TropVolumineux
        val msg = message(e)
        if (pourCreateur) copierMessage(context, msg)
        val retour = Intent(context, ChoixPartageReceiver::class.java)
            .putExtra(ChoixPartageReceiver.EXTRA_NOM, e.nomFichier)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val pi = PendingIntent.getBroadcast(context, e.nomFichier.hashCode(), retour, flags)
        val titre = if (pourCreateur) "Envoyer à ${Createur.NOM}" else "Partager l'enregistrement"
        val chooser = Intent.createChooser(intentEnvoi(context, fichier, msg), titre, pi.intentSender)
        return try {
            context.startActivity(chooser)
            Resultat.Ouvert
        } catch (_: ActivityNotFoundException) {
            Resultat.FichierIntrouvable
        }
    }

    /** Bouton « CONTACTER LE CRÉATEUR » : ouvre https://wa.me/243840238592 */
    fun contacterCreateur(context: Context): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(Createur.LIEN_WHATSAPP))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    fun copierMessage(context: Context, message: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Message pour le créateur", message))
    }
}

/** Reçoit l'application choisie dans le menu de partage, pour passer le statut à « Partagé ». */
class ChoixPartageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nom = intent.getStringExtra(EXTRA_NOM) ?: return
        EnregistrementsRepository.get(context).marquerPartage(nom)
    }

    companion object {
        const val EXTRA_NOM = "nom_fichier"
    }
}
