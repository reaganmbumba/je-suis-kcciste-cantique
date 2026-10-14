package com.kcciste.cantique.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/** Un message du groupe. [enAttente] : pas encore reçu par le serveur (hors ligne). */
data class Message(
    val id: String,
    val texte: String,
    val nom: String,
    val uid: String,
    val date: Timestamp?,
    val enAttente: Boolean,
)

/**
 * Groupe de discussion unique, partagé par tous les utilisateurs de l'application.
 * Stocké dans Firestore : collection groupes/general/messages.
 * Connexion anonyme Firebase : chaque téléphone reçoit un identifiant, l'utilisateur choisit son nom.
 */
object Discussion {
    const val MAX_TEXTE = 1000
    const val MAX_NOM = 40
    private const val PREFS = "discussion"

    /** false tant que le fichier google-services.json n'a pas été ajouté (voir README). */
    fun estConfiguree(context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null

    fun nom(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("nom", "") ?: ""

    fun enregistrerNom(context: Context, nom: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("nom", nom.trim().take(MAX_NOM)).apply()
    }

    fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun connecter(ok: (String) -> Unit, erreur: (Exception) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { ok(it.uid); return }
        auth.signInAnonymously()
            .addOnSuccessListener { r -> r.user?.uid?.let(ok) ?: erreur(IllegalStateException("Connexion impossible")) }
            .addOnFailureListener(erreur)
    }

    private fun messages() =
        FirebaseFirestore.getInstance().collection("groupes").document("general").collection("messages")

    /** Écoute les 300 derniers messages, du plus ancien au plus récent. */
    fun ecouter(maj: (List<Message>) -> Unit, erreur: (Exception) -> Unit): ListenerRegistration =
        messages().orderBy("date", Query.Direction.DESCENDING).limit(300)
            .addSnapshotListener { snap, e ->
                if (e != null) { erreur(e); return@addSnapshotListener }
                val liste = snap?.documents.orEmpty().map { d ->
                    Message(
                        id = d.id,
                        texte = d.getString("texte") ?: "",
                        nom = d.getString("nom") ?: "?",
                        uid = d.getString("uid") ?: "",
                        // Message pas encore arrivé au serveur : heure estimée du téléphone.
                        date = d.getTimestamp("date", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                        enAttente = d.metadata.hasPendingWrites(),
                    )
                }
                maj(liste.reversed())
            }

    /** Envoie un message. Hors ligne, Firestore le garde et l'envoie au retour du réseau. */
    fun envoyer(texte: String, nom: String, erreur: (Exception) -> Unit) {
        val uid = uid() ?: return erreur(IllegalStateException("Non connecté"))
        messages().add(
            mapOf(
                "texte" to texte.trim().take(MAX_TEXTE),
                "nom" to nom.trim().take(MAX_NOM),
                "uid" to uid,
                "date" to FieldValue.serverTimestamp(),
            ),
        ).addOnFailureListener(erreur)
    }

    fun supprimer(id: String) {
        messages().document(id).delete()
    }
}
