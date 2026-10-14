package com.kcciste.cantique.enligne

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.StatutSuggestion
import com.kcciste.cantique.data.Suggestion
import com.kcciste.cantique.data.Suggestions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Suggestions de correction (texte et/ou voix).
 *  Firestore suggestions/{reference}
 *  Storage   suggestions/{uid}/{reference}.{ext}  (lisible seulement par les administrateurs)
 * Rien n'est publié automatiquement : l'administrateur décide.
 */
object SuggestionsEnLigne {
    private fun col() = FirebaseFirestore.getInstance().collection("suggestions")

    data class Brouillon(
        val cantique: Cantique,
        val indexStrophe: Int,
        val texteActuel: String,
        val correction: String,
        val explication: String,
        val nom: String,
        /** Message vocal enregistré ou importé (optionnel). */
        val audio: Uri?,
        val mimeAudio: String?,
    )

    /** Envoie la suggestion et renvoie son numéro de référence. Demande une connexion Internet. */
    suspend fun envoyer(context: Context, b: Brouillon): String {
        val uid = ServiceFirebase.connexionAnonyme()
        val reference = Suggestions.nouvelleReference(LocalDate.now())
        var chemin = ""
        if (b.audio != null) {
            val mime = b.mimeAudio ?: "audio/mp4"
            require(Suggestions.typeAudioAccepte(mime)) { "Type de fichier audio non accepté ($mime)." }
            val taille = context.contentResolver.openAssetFileDescriptor(b.audio, "r")?.use { it.length } ?: -1
            require(taille <= Suggestions.MAX_AUDIO_OCTETS) { "Le message vocal doit faire au plus 10 Mo." }
            val ext = when {
                mime.contains("mpeg") || mime.contains("mp3") -> "mp3"
                mime.contains("ogg") || mime.contains("opus") -> "ogg"
                mime.contains("wav") -> "wav"
                mime.contains("amr") -> "amr"
                mime.contains("3gpp") -> "3gp"
                else -> "m4a"
            }
            chemin = "suggestions/$uid/$reference.$ext"
            FirebaseStorage.getInstance().reference.child(chemin)
                .putFile(b.audio, StorageMetadata.Builder().setContentType(mime).build()).await()
        }
        col().document(reference).set(
            mapOf(
                "reference" to reference,
                "cantiqueId" to b.cantique.id,
                "titreCantique" to b.cantique.titre,
                "versionCantique" to b.cantique.version,
                "indexStrophe" to b.indexStrophe,
                "texteActuel" to b.texteActuel.take(Suggestions.MAX_CORRECTION),
                "correction" to b.correction.trim().take(Suggestions.MAX_CORRECTION),
                "explication" to b.explication.trim().take(Suggestions.MAX_EXPLICATION),
                "nom" to b.nom.trim().take(60),
                "uid" to uid,
                "cheminAudio" to chemin,
                "statut" to StatutSuggestion.EN_ATTENTE.name,
                "date" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return reference
    }

    fun lire(d: DocumentSnapshot) = Suggestion(
        id = d.id,
        reference = d.getString("reference") ?: d.id,
        cantiqueId = d.getString("cantiqueId") ?: "",
        titreCantique = d.getString("titreCantique") ?: "",
        versionCantique = d.getLong("versionCantique")?.toInt() ?: 1,
        indexStrophe = d.getLong("indexStrophe")?.toInt() ?: -1,
        texteActuel = d.getString("texteActuel") ?: "",
        correction = d.getString("correction") ?: "",
        explication = d.getString("explication") ?: "",
        nom = d.getString("nom") ?: "",
        uid = d.getString("uid") ?: "",
        cheminAudio = d.getString("cheminAudio") ?: "",
        statut = StatutSuggestion.depuis(d.getString("statut")),
        dateMillis = (d.get("date") as? Timestamp)?.toDate()?.time ?: 0,
        noteAdmin = d.getString("noteAdmin") ?: "",
    )

    /** Administrateur : toutes les suggestions, les plus récentes d'abord. */
    fun ecouter(maj: (List<Suggestion>) -> Unit, erreur: (Exception) -> Unit): ListenerRegistration =
        col().orderBy("date", Query.Direction.DESCENDING).limit(500).addSnapshotListener { s, e ->
            if (e != null) erreur(e) else maj(s?.documents.orEmpty().map { lire(it) })
        }

    suspend fun changerStatut(s: Suggestion, statut: StatutSuggestion, note: String, admin: String) {
        col().document(s.id).update(
            mapOf("statut" to statut.name, "noteAdmin" to note, "traitePar" to admin, "traiteLe" to FieldValue.serverTimestamp()),
        ).await()
        ContenuEnLigne.noter(admin, "suggestion ${statut.libelle}", s.cantiqueId, s.reference)
    }

    /** Lien temporaire pour écouter le message vocal (réservé aux administrateurs par les règles Storage). */
    suspend fun urlAudio(chemin: String): Uri = FirebaseStorage.getInstance().reference.child(chemin).downloadUrl.await()
}
