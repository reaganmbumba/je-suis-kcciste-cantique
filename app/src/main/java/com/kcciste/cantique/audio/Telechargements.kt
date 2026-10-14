package com.kcciste.cantique.audio

import android.content.Context
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.PisteAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Un audio téléchargé, écoutable sans Internet. */
data class AudioTelecharge(
    val cantiqueId: String,
    val pisteId: String,
    val titre: String,
    val type: String,
    val tailleOctets: Long,
    val fichier: File,
)

data class Progression(val recu: Long, val total: Long, val erreur: String? = null) {
    val fraction: Float get() = if (total > 0) (recu.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/**
 * Téléchargement des pistes autorisées pour l'écoute hors connexion.
 * Les fichiers vont dans le stockage interne de l'application (files/audios), jamais dans le cache,
 * pour qu'Android ne les efface pas. Un fichier interrompu (.part) reprend là où il s'était arrêté
 * quand le serveur accepte les requêtes partielles (en-tête Range).
 */
class Telechargements private constructor(context: Context) {
    private val dossier = File(context.filesDir, "audios").apply { mkdirs() }
    private val index = File(dossier, "index.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val travaux = mutableMapOf<String, Job>()

    private val _enCours = MutableStateFlow<Map<String, Progression>>(emptyMap())
    /** Par identifiant de piste. */
    val enCours: StateFlow<Map<String, Progression>> = _enCours.asStateFlow()

    private val _liste = MutableStateFlow(lireIndex())
    val liste: StateFlow<List<AudioTelecharge>> = _liste.asStateFlow()

    fun fichier(p: PisteAudio): File = File(dossier, nomFichier(p.id))
    fun estTelecharge(p: PisteAudio): Boolean = _liste.value.any { it.pisteId == p.id } && fichier(p).exists()
    fun tailleTotale(): Long = _liste.value.sumOf { it.fichier.length() }
    fun espaceLibre(): Long = dossier.usableSpace

    fun telecharger(c: Cantique, p: PisteAudio) {
        if (travaux[p.id]?.isActive == true || estTelecharge(p)) return
        _enCours.update { it + (p.id to Progression(0, p.tailleOctets)) }
        travaux[p.id] = scope.launch {
            try {
                val total = telechargerFichier(p)
                synchronized(this@Telechargements) {
                    val nouvelle = _liste.value.filter { it.pisteId != p.id } +
                        AudioTelecharge(c.id, p.id, "${c.numero}. ${c.titre}", p.type.libelle, total, fichier(p))
                    _liste.value = nouvelle
                    ecrireIndex(nouvelle)
                }
                _enCours.update { it - p.id }
            } catch (e: Exception) {
                val msg = when (e) {
                    is kotlinx.coroutines.CancellationException -> null
                    is EspaceInsuffisant -> "Espace de stockage insuffisant."
                    is IOException -> "Connexion interrompue. Réessayez : le téléchargement reprendra."
                    else -> "Téléchargement impossible : ${e.message}"
                }
                _enCours.update { if (msg == null) it - p.id else it + (p.id to (it[p.id] ?: Progression(0, 0)).copy(erreur = msg)) }
            }
        }
    }

    private class EspaceInsuffisant : IOException()

    private suspend fun telechargerFichier(p: PisteAudio): Long {
        val part = File(dossier, nomFichier(p.id) + ".part")
        var deja = if (part.exists()) part.length() else 0L
        val cnx = (URL(p.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            if (deja > 0) setRequestProperty("Range", "bytes=$deja-")
        }
        try {
            val code = cnx.responseCode
            if (code == 416) { // déjà complet
                part.renameTo(fichier(p))
                return fichier(p).length()
            }
            if (code !in 200..299) throw IOException("HTTP $code")
            if (code == 200) deja = 0 // le serveur ne reprend pas : on recommence
            val restant = cnx.contentLengthLong
            val total = if (restant > 0) deja + restant else p.tailleOctets
            if (restant > 0 && restant + 1_000_000 > dossier.usableSpace) throw EspaceInsuffisant()
            var recu = deja
            cnx.inputStream.use { entree ->
                FileOutputStream(part, deja > 0).use { sortie ->
                    val tampon = ByteArray(64 * 1024)
                    var dernier = 0L
                    while (kotlin.coroutines.coroutineContext.isActive) {
                        val n = entree.read(tampon)
                        if (n < 0) break
                        sortie.write(tampon, 0, n)
                        recu += n
                        if (recu - dernier > 128 * 1024) {
                            dernier = recu
                            _enCours.update { it + (p.id to Progression(recu, total)) }
                        }
                    }
                }
            }
            if (!kotlin.coroutines.coroutineContext.isActive) throw kotlinx.coroutines.CancellationException()
            if (total > 0 && recu < total) throw IOException("incomplet")
            if (!part.renameTo(fichier(p))) throw IOException("écriture impossible")
            return recu
        } finally {
            cnx.disconnect()
        }
    }

    fun annuler(p: PisteAudio) {
        travaux.remove(p.id)?.cancel()
        _enCours.update { it - p.id }
    }

    fun oublierErreur(pisteId: String) = _enCours.update { it - pisteId }

    @Synchronized
    fun supprimer(pisteId: String) {
        File(dossier, nomFichier(pisteId)).delete()
        File(dossier, nomFichier(pisteId) + ".part").delete()
        val nouvelle = _liste.value.filter { it.pisteId != pisteId }
        _liste.value = nouvelle
        ecrireIndex(nouvelle)
    }

    @Synchronized
    fun supprimerTout() {
        travaux.values.forEach { it.cancel() }
        travaux.clear()
        _enCours.value = emptyMap()
        dossier.listFiles()?.forEach { if (it != index) it.delete() }
        _liste.value = emptyList()
        ecrireIndex(emptyList())
    }

    private fun nomFichier(pisteId: String) = pisteId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".audio"

    private fun lireIndex(): List<AudioTelecharge> = runCatching {
        val a = JSONArray(index.readText())
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val pisteId = o.getString("pisteId")
            AudioTelecharge(o.getString("cantiqueId"), pisteId, o.getString("titre"), o.optString("type"), o.optLong("taille"), File(dossier, nomFichier(pisteId)))
        }.filter { it.fichier.exists() }
    }.getOrDefault(emptyList())

    private fun ecrireIndex(l: List<AudioTelecharge>) {
        val a = JSONArray()
        l.forEach {
            a.put(JSONObject().put("cantiqueId", it.cantiqueId).put("pisteId", it.pisteId).put("titre", it.titre).put("type", it.type).put("taille", it.tailleOctets))
        }
        index.writeText(a.toString())
    }

    companion object {
        @Volatile private var instance: Telechargements? = null
        fun get(context: Context): Telechargements =
            instance ?: synchronized(this) { instance ?: Telechargements(context.applicationContext).also { instance = it } }
    }
}
