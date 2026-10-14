package com.kcciste.cantique.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stocke les enregistrements sauvegardés dans le stockage privé de l'application
 * (files/enregistrements/). Ils restent privés : rien ne quitte l'appareil
 * tant que l'utilisateur ne lance pas lui-même un partage.
 */
class EnregistrementsRepository private constructor(context: Context) {

    val dossier: File = File(context.filesDir, "enregistrements").apply { mkdirs() }
    private val index = File(dossier, "index.json")

    private val _liste = MutableStateFlow(charger())
    val liste: StateFlow<List<Enregistrement>> = _liste.asStateFlow()

    fun fichier(e: Enregistrement): File = File(dossier, e.nomFichier)

    fun nomExiste(nom: String): Boolean =
        File(dossier, nom).exists() || _liste.value.any { it.nomFichier == nom }

    /** Déplace le fichier temporaire dans « Mes enregistrements ». */
    @Synchronized
    fun sauvegarder(temp: File, nomFichier: String, cantique: Cantique, dureeMs: Long): Enregistrement {
        val cible = File(dossier, nomFichier)
        if (!temp.renameTo(cible)) {
            temp.copyTo(cible, overwrite = true)
            temp.delete()
        }
        val e = Enregistrement(
            nomFichier = nomFichier,
            numero = cantique.numero,
            titre = cantique.titre,
            langue = cantique.langue,
            dateMillis = System.currentTimeMillis(),
            dureeMs = dureeMs,
            tailleOctets = cible.length(),
            statut = Statut.SAUVEGARDE,
        )
        ecrire(listOf(e) + _liste.value.filterNot { it.nomFichier == nomFichier })
        return e
    }

    @Synchronized
    fun marquerPartage(nomFichier: String) {
        ecrire(_liste.value.map { if (it.nomFichier == nomFichier) it.copy(statut = Statut.PARTAGE) else it })
    }

    @Synchronized
    fun supprimer(nomFichier: String) {
        File(dossier, nomFichier).delete()
        ecrire(_liste.value.filterNot { it.nomFichier == nomFichier })
    }

    private fun ecrire(nouvelle: List<Enregistrement>) {
        val arr = JSONArray()
        nouvelle.forEach { e ->
            arr.put(
                JSONObject()
                    .put("nomFichier", e.nomFichier)
                    .put("numero", e.numero)
                    .put("titre", e.titre)
                    .put("langue", e.langue.name)
                    .put("date", e.dateMillis)
                    .put("dureeMs", e.dureeMs)
                    .put("taille", e.tailleOctets)
                    .put("statut", e.statut.name),
            )
        }
        val tmp = File(dossier, "index.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(index)
        _liste.value = nouvelle
    }

    private fun charger(): List<Enregistrement> {
        if (!index.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(index.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Enregistrement(
                    nomFichier = o.getString("nomFichier"),
                    numero = o.getInt("numero"),
                    titre = o.getString("titre"),
                    langue = Langue.valueOf(o.getString("langue")),
                    dateMillis = o.getLong("date"),
                    dureeMs = o.getLong("dureeMs"),
                    tailleOctets = o.getLong("taille"),
                    statut = Statut.valueOf(o.getString("statut")),
                )
            }.filter { File(dossier, it.nomFichier).exists() }
        }.getOrDefault(emptyList())
    }

    companion object {
        @Volatile private var instance: EnregistrementsRepository? = null
        fun get(context: Context): EnregistrementsRepository =
            instance ?: synchronized(this) {
                instance ?: EnregistrementsRepository(context.applicationContext).also { instance = it }
            }
    }
}
