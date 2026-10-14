package com.kcciste.cantique.audio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kcciste.cantique.BuildConfig
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Enregistrement
import com.kcciste.cantique.data.EnregistrementsRepository
import com.kcciste.cantique.data.Formats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

sealed interface Etape {
    data object Pret : Etape
    data class EnCours(val enPause: Boolean) : Etape
    /** [sauvegarde] est non nul une fois le fichier rangé dans « Mes enregistrements ». */
    data class Termine(
        val fichier: File,
        val nomFichier: String,
        val dureeMs: Long,
        val tailleOctets: Long,
        val sauvegarde: Enregistrement?,
    ) : Etape
}

data class EtatEnregistrement(
    val etape: Etape = Etape.Pret,
    val dureeMs: Long = 0,
    /** Historique récent des niveaux du micro (0..1), pour la waveform. */
    val niveaux: List<Float> = emptyList(),
    val erreur: String? = null,
    val info: String? = null,
)

class EnregistrementViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EnregistrementsRepository.get(app)
    private val enregistreur = Enregistreur(app)
    private val dossierTemp = File(app.cacheDir, "en_cours")
    private var ticker: Job? = null

    lateinit var cantique: Cantique
        private set

    private val _etat = MutableStateFlow(EtatEnregistrement())
    val etat: StateFlow<EtatEnregistrement> = _etat.asStateFlow()

    val dureeMaxMs: Long = BuildConfig.MAX_RECORDING_SECONDS * 1000L

    fun init(c: Cantique) {
        if (!this::cantique.isInitialized) cantique = c
    }

    fun demarrer() {
        if (enregistreur.actif) return
        jeterTemporaire()
        val nom = Formats.nomFichier(cantique, LocalDate.now(), repo::nomExiste)
        val cible = File(dossierTemp, nom)
        try {
            enregistreur.demarrer(cible)
        } catch (_: Enregistreur.EchecEnregistrement) {
            _etat.update { it.copy(erreur = MSG_ECHEC) }
            return
        }
        _etat.value = EtatEnregistrement(etape = Etape.EnCours(enPause = false))
        ticker = viewModelScope.launch {
            while (isActive) {
                val d = enregistreur.dureeMs
                val a = enregistreur.amplitude()
                _etat.update { e ->
                    e.copy(
                        dureeMs = d,
                        niveaux = if (enregistreur.enPause) e.niveaux else (e.niveaux + a).takeLast(NB_BARRES),
                    )
                }
                if (d >= dureeMaxMs) {
                    arreter()
                    _etat.update {
                        it.copy(info = "Durée maximale atteinte (${Formats.duree(dureeMaxMs)}). L'enregistrement a été arrêté.")
                    }
                    break
                }
                delay(60)
            }
        }
    }

    fun pause() {
        runCatching { enregistreur.pause() }.onSuccess {
            _etat.update { it.copy(etape = Etape.EnCours(enPause = true)) }
        }
    }

    fun reprendre() {
        runCatching { enregistreur.reprendre() }.onSuccess {
            _etat.update { it.copy(etape = Etape.EnCours(enPause = false)) }
        }
    }

    fun arreter() {
        ticker?.cancel()
        ticker = null
        val f = enregistreur.fichier
        val duree = try {
            enregistreur.arreter()
        } catch (_: Enregistreur.EchecEnregistrement) {
            _etat.value = EtatEnregistrement(erreur = MSG_ECHEC)
            return
        }
        if (f == null) return
        _etat.update {
            it.copy(
                etape = Etape.Termine(f, f.name, duree, f.length(), sauvegarde = null),
                dureeMs = duree,
            )
        }
    }

    /** Range le fichier dans « Mes enregistrements ». Renvoie l'enregistrement sauvegardé. */
    fun sauvegarder(): Enregistrement? {
        val t = _etat.value.etape as? Etape.Termine ?: return null
        t.sauvegarde?.let { return it }
        return try {
            val e = repo.sauvegarder(t.fichier, t.nomFichier, cantique, t.dureeMs)
            val final = repo.fichier(e)
            _etat.update { it.copy(etape = t.copy(fichier = final, sauvegarde = e)) }
            e
        } catch (_: Exception) {
            _etat.update { it.copy(erreur = MSG_ECHEC) }
            null
        }
    }

    /** REFAIRE : jette la prise actuelle (si elle n'est pas sauvegardée) et revient à zéro. */
    fun refaire() {
        jeterTemporaire()
        _etat.value = EtatEnregistrement()
    }

    /** SUPPRIMER : supprime réellement le fichier, même s'il était déjà sauvegardé. */
    fun supprimer() {
        val t = _etat.value.etape as? Etape.Termine
        if (t?.sauvegarde != null) repo.supprimer(t.sauvegarde.nomFichier) else jeterTemporaire()
        _etat.value = EtatEnregistrement(info = "Enregistrement supprimé.")
    }

    fun effacerMessages() = _etat.update { it.copy(erreur = null, info = null) }

    fun signalerErreur(msg: String) = _etat.update { it.copy(erreur = msg) }

    val aUnePriseNonSauvegardee: Boolean
        get() = when (val e = _etat.value.etape) {
            is Etape.EnCours -> true
            is Etape.Termine -> e.sauvegarde == null
            Etape.Pret -> false
        }

    private fun jeterTemporaire() {
        if (enregistreur.actif) enregistreur.annuler()
        val t = _etat.value.etape as? Etape.Termine
        if (t != null && t.sauvegarde == null) t.fichier.delete()
    }

    override fun onCleared() {
        ticker?.cancel()
        jeterTemporaire()
        dossierTemp.listFiles()?.forEach { it.delete() }
    }

    companion object {
        const val NB_BARRES = 64
        const val MSG_ECHEC =
            "Impossible d'enregistrer l'audio. Vérifiez l'autorisation du microphone et l'espace disponible."
    }
}
