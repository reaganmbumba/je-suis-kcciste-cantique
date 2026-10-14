package com.kcciste.cantique.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.kcciste.cantique.data.Createur

/** Partage des paroles : WhatsApp, Facebook, menu Android, copier. */
object PartageTexte {
    const val LIEN_APPLICATION = "https://play.google.com/store/apps/details?id=com.kcciste.cantique"

    fun copier(context: Context, texte: String, libelle: String = "Paroles") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(libelle, texte))
        Toast.makeText(context, "Paroles copiées", Toast.LENGTH_SHORT).show()
    }

    private fun intentTexte(texte: String) = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texte)
        putExtra(Intent.EXTRA_SUBJECT, Createur.MARQUE)
    }

    fun menuAndroid(context: Context, texte: String) {
        context.startActivity(Intent.createChooser(intentTexte(texte), "Partager le cantique"))
    }

    fun whatsapp(context: Context, texte: String) {
        val pkg = Partage.paquetWhatsApp(context)
        if (pkg == null) {
            Toast.makeText(context, "WhatsApp n'est pas installé : choisissez une autre application.", Toast.LENGTH_LONG).show()
            menuAndroid(context, texte)
            return
        }
        try {
            context.startActivity(intentTexte(texte).setPackage(pkg))
        } catch (_: ActivityNotFoundException) {
            menuAndroid(context, texte)
        }
    }

    /**
     * Facebook n'accepte pas de texte prérempli venant d'une autre application (règle de Facebook).
     * Les paroles sont donc copiées : l'utilisateur les colle dans sa publication.
     * Sans l'application Facebook, on ouvre la page de partage du lien de l'application.
     */
    fun facebook(context: Context, texte: String) {
        copier(context, texte)
        val ouvert = try {
            context.startActivity(intentTexte(texte).setPackage("com.facebook.katana"))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
        if (ouvert) {
            Toast.makeText(context, "Paroles copiées : appuyez longuement dans la publication puis « Coller ».", Toast.LENGTH_LONG).show()
        } else {
            val url = "https://www.facebook.com/sharer/sharer.php?u=" + Uri.encode(LIEN_APPLICATION)
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }

    /** Contact WhatsApp avec message prérempli et motif. */
    fun contacter(context: Context, motif: String?): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Createur.lienContact(motif))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
