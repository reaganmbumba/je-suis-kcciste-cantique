package com.kcciste.cantique.enligne

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Accès commun à Firebase. Tout est facultatif : sans app/google-services.json,
 * [estConfigure] renvoie false et l'application fonctionne entièrement hors ligne
 * (recueil, audio téléchargé, favoris…) ; seules les fonctions en ligne affichent « pas encore activée ».
 */
object ServiceFirebase {
    fun estConfigure(context: Context): Boolean = runCatching {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }.getOrDefault(false)

    /** Connexion anonyme (suggestions, discussion) si personne n'est connecté. Renvoie l'identifiant. */
    suspend fun connexionAnonyme(): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        return auth.signInAnonymously().await().user?.uid ?: error("Connexion impossible")
    }

    /** Firestore n'accepte que des Map / List : conversion depuis le format JSON des cantiques. */
    fun versMap(o: JSONObject): Map<String, Any?> = o.keys().asSequence().associateWith { k -> valeur(o.get(k)) }

    private fun valeur(v: Any?): Any? = when (v) {
        is JSONObject -> versMap(v)
        is JSONArray -> (0 until v.length()).map { valeur(v.get(it)) }
        JSONObject.NULL -> null
        else -> v
    }

    /** Conversion inverse, en ne gardant que les clés utiles (les dates Firestore sont ignorées). */
    fun versJson(m: Map<String, Any?>, cles: Set<String>? = null): JSONObject {
        val o = JSONObject()
        m.forEach { (k, v) -> if (cles == null || k in cles) jsonValeur(v)?.let { o.put(k, it) } }
        return o
    }

    private fun jsonValeur(v: Any?): Any? = when (v) {
        is Map<*, *> -> versJson(v.entries.associate { it.key.toString() to it.value })
        is List<*> -> JSONArray().apply { v.forEach { e -> put(jsonValeur(e) ?: JSONObject.NULL) } }
        is String, is Number, is Boolean -> v
        else -> null
    }
}
