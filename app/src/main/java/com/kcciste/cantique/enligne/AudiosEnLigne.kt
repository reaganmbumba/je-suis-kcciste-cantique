package com.kcciste.cantique.enligne

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

/** Administrateur : dépôt d'un fichier audio autorisé dans Firebase Storage (audios/{cantiqueId}/{pisteId}.ext). */
object AudiosEnLigne {
    const val MAX_OCTETS = 50L * 1024 * 1024

    data class Depot(val url: String, val taille: Long)

    suspend fun deposer(context: Context, cantiqueId: String, pisteId: String, fichier: Uri): Depot {
        val mime = context.contentResolver.getType(fichier) ?: "audio/mpeg"
        require(mime.startsWith("audio/")) { "Ce fichier n'est pas un fichier audio ($mime)." }
        val taille = context.contentResolver.openAssetFileDescriptor(fichier, "r")?.use { it.length } ?: -1
        require(taille <= MAX_OCTETS) { "Le fichier audio doit faire au plus 50 Mo." }
        val ext = if (mime.contains("mpeg") || mime.contains("mp3")) "mp3" else if (mime.contains("ogg")) "ogg" else "m4a"
        val ref = FirebaseStorage.getInstance().reference.child("audios/$cantiqueId/$pisteId.$ext")
        ref.putFile(fichier, StorageMetadata.Builder().setContentType(mime).build()).await()
        return Depot(ref.downloadUrl.await().toString(), taille)
    }
}
