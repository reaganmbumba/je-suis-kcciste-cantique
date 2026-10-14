package com.kcciste.cantique.enligne

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.CantiqueJson
import com.kcciste.cantique.data.Recueil
import kotlinx.coroutines.tasks.await

/** Une version enregistrée d'un cantique (historique des modifications). */
data class VersionCantique(
    val version: Int,
    val cantique: Cantique,
    val dateMillis: Long,
    val admin: String,
    val note: String,
)

/**
 * Contenu officiel publié par l'administrateur, séparé des mises à jour de l'application :
 * corriger des paroles ne demande pas de nouvelle version sur Google Play.
 *
 * Firestore :
 *  cantiques/{id}                 → dernière version publiée (même champs que le JSON des cantiques)
 *  cantiques/{id}/versions/{n}    → chaque version, jamais effacée (date, admin, note)
 *  journal/{auto}                 → journal des actions administratives
 */
object ContenuEnLigne {
    private val CLES = setOf("id", "numero", "titre", "langue", "categorie", "credits", "version", "strophes", "traductions", "audios")
    private var ecoute: ListenerRegistration? = null

    private fun db() = FirebaseFirestore.getInstance()
    private fun cantiques() = db().collection("cantiques")

    /** Synchronise le recueil quand la connexion est disponible ; Firestore garde aussi une copie hors ligne. */
    fun demarrer(context: Context) {
        if (ecoute != null || !ServiceFirebase.estConfigure(context)) return
        val app = context.applicationContext
        ecoute = cantiques().addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            // Premier affichage hors ligne sans rien en cache : on garde la copie locale.
            if (snap.metadata.isFromCache && snap.isEmpty) return@addSnapshotListener
            Recueil.appliquerEnLigne(app, snap.documents.mapNotNull { lire(it) })
        }
    }

    fun lire(d: DocumentSnapshot): Cantique? = runCatching {
        CantiqueJson.lire(ServiceFirebase.versJson(d.data ?: return null, CLES))
    }.getOrNull()

    /**
     * Publie une nouvelle version : l'ancienne est conservée dans versions/, la nouvelle reçoit version + 1.
     * La première publication en ligne d'un cantique du recueil d'origine enregistre aussi la version 1.
     */
    suspend fun publier(nouveau: Cantique, admin: String, note: String): Int {
        val ref = cantiques().document(nouveau.id)
        val actuel = ref.get().await()
        val enLigne = if (actuel.exists()) lire(actuel) else null
        val precedent = enLigne ?: Recueil.versionOrigine(nouveau.id) ?: Recueil.trouver(nouveau.id)
        val numeroVersion = (precedent?.version ?: 0) + 1
        val publie = nouveau.copy(version = numeroVersion)
        val batch = db().batch()
        if (enLigne == null && precedent != null) {
            batch.set(ref.collection("versions").document(precedent.version.toString()), donneesVersion(precedent, "recueil d'origine", "Version d'origine"))
        }
        batch.set(ref, ServiceFirebase.versMap(CantiqueJson.ecrire(publie)) + mapOf("majLe" to FieldValue.serverTimestamp(), "majPar" to admin))
        batch.set(ref.collection("versions").document(numeroVersion.toString()), donneesVersion(publie, admin, note))
        batch.set(db().collection("journal").document(), journal(admin, "publication", publie.id, "Version $numeroVersion. $note".trim()))
        batch.commit().await()
        return numeroVersion
    }

    private fun donneesVersion(c: Cantique, admin: String, note: String): Map<String, Any?> =
        ServiceFirebase.versMap(CantiqueJson.ecrire(c)) + mapOf("date" to FieldValue.serverTimestamp(), "admin" to admin, "note" to note)

    private fun journal(admin: String, action: String, cantiqueId: String, details: String) = mapOf(
        "date" to FieldValue.serverTimestamp(), "admin" to admin, "action" to action, "cantiqueId" to cantiqueId, "details" to details,
    )

    suspend fun versions(id: String): List<VersionCantique> =
        cantiques().document(id).collection("versions").get().await().documents.mapNotNull { d ->
            val c = lire(d) ?: return@mapNotNull null
            VersionCantique(
                version = c.version,
                cantique = c,
                dateMillis = (d.get("date") as? Timestamp)?.toDate()?.time ?: 0,
                admin = d.getString("admin") ?: "",
                note = d.getString("note") ?: "",
            )
        }.sortedByDescending { it.version }

    /** Restaurer = republier le texte d'une ancienne version comme nouvelle version (rien n'est effacé). */
    suspend fun restaurer(v: VersionCantique, admin: String): Int =
        publier(v.cantique, admin, "Restauration de la version ${v.version}")

    /** Import en lot : uniquement des cantiques nouveaux (vérifiés par ImportCantiques), jamais de remplacement. */
    suspend fun importer(nouveaux: List<Cantique>, admin: String): Int {
        var n = 0
        nouveaux.chunked(200).forEach { lot ->
            val batch = db().batch()
            lot.forEach { c ->
                val ref = cantiques().document(c.id)
                val v = c.copy(version = 1)
                batch.set(ref, ServiceFirebase.versMap(CantiqueJson.ecrire(v)) + mapOf("majLe" to FieldValue.serverTimestamp(), "majPar" to admin))
                batch.set(ref.collection("versions").document("1"), donneesVersion(v, admin, "Import"))
            }
            batch.set(db().collection("journal").document(), journal(admin, "import", "", "${lot.size} cantique(s) importé(s)"))
            batch.commit().await()
            n += lot.size
        }
        return n
    }

    data class EntreeJournal(val dateMillis: Long, val admin: String, val action: String, val cantiqueId: String, val details: String)

    suspend fun journal(limite: Long = 100): List<EntreeJournal> =
        db().collection("journal").orderBy("date", Query.Direction.DESCENDING).limit(limite).get().await().documents.map { d ->
            EntreeJournal(
                (d.get("date") as? Timestamp)?.toDate()?.time ?: 0,
                d.getString("admin") ?: "", d.getString("action") ?: "", d.getString("cantiqueId") ?: "", d.getString("details") ?: "",
            )
        }

    suspend fun noter(admin: String, action: String, cantiqueId: String, details: String) {
        db().collection("journal").document().set(journal(admin, action, cantiqueId, details), SetOptions.merge()).await()
    }
}
