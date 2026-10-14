package com.kcciste.cantique.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kcciste.cantique.data.Bibliotheque
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.PisteAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Un élément de la liste de lecture. */
data class ElementLecture(val cantique: Cantique, val piste: PisteAudio) {
    val mediaId: String get() = "${cantique.id}|${piste.id}"
}

data class EtatLecteur(
    val element: ElementLecture? = null,
    val enLecture: Boolean = false,
    val chargement: Boolean = false,
    val positionMs: Long = 0,
    val dureeMs: Long = 0,
    val index: Int = 0,
    val nbElements: Int = 0,
    val vitesse: Float = 1f,
    val volume: Float = 1f,
    val repeter: Boolean = false,
    val horsLigne: Boolean = false,
    val erreur: String? = null,
)

/**
 * Pilote le [LecteurService] depuis l'interface (MediaController).
 * Lit le fichier téléchargé quand il existe, sinon la piste en ligne (streaming).
 */
object LecteurCantiques {
    private val _etat = MutableStateFlow(EtatLecteur())
    val etat: StateFlow<EtatLecteur> = _etat.asStateFlow()

    private var controleur: MediaController? = null
    private var enConnexion = false
    private val enAttente = mutableListOf<(MediaController) -> Unit>()
    private var elements: List<ElementLecture> = emptyList()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var suivi: Job? = null
    private lateinit var appli: Context

    /** Dernière position connue par piste, pour reprendre après une interruption. */
    private val positions = mutableMapOf<String, Long>()

    private fun avecControleur(context: Context, action: (MediaController) -> Unit) {
        controleur?.let { action(it); return }
        appli = context.applicationContext
        enAttente += action
        if (enConnexion) return
        enConnexion = true
        val jeton = SessionToken(appli, ComponentName(appli, LecteurService::class.java))
        val futur = MediaController.Builder(appli, jeton).buildAsync()
        futur.addListener({
            enConnexion = false
            val c = runCatching { futur.get() }.getOrNull()
            if (c == null) {
                _etat.update { it.copy(erreur = "Le lecteur audio n'a pas pu démarrer.") }
                enAttente.clear()
                return@addListener
            }
            controleur = c
            c.addListener(ecouteur)
            enAttente.toList().forEach { it(c) }
            enAttente.clear()
        }, ContextCompat.getMainExecutor(appli))
    }

    private val ecouteur = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _etat.update { it.copy(enLecture = isPlaying) }
            if (isPlaying) suivre() else memoriserPosition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _etat.update { it.copy(chargement = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_ENDED) controleur?.currentMediaItem?.mediaId?.let { positions.remove(it) }
            rafraichir()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            rafraichir()
            val e = _etat.value.element ?: return
            Bibliotheque.get(appli).ecoute(e.cantique.id, e.piste.id)
        }

        override fun onPlayerError(error: PlaybackException) {
            val msg = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                -> "Pas de connexion Internet. Téléchargez l'audio pour l'écouter hors ligne."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                -> "Fichier audio introuvable."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                -> "Format audio non pris en charge."
                else -> "Lecture impossible (${error.errorCodeName})."
            }
            _etat.update { it.copy(erreur = msg, enLecture = false, chargement = false) }
        }
    }

    /** Lance la liste [liste] à partir de [depart]. */
    fun jouer(context: Context, liste: List<ElementLecture>, depart: Int) {
        if (liste.isEmpty()) return
        val tel = Telechargements.get(context)
        elements = liste
        avecControleur(context) { c ->
            val items = liste.map { e ->
                val local = tel.fichier(e.piste)
                val uri = if (tel.estTelecharge(e.piste)) Uri.fromFile(local) else Uri.parse(e.piste.url)
                MediaItem.Builder()
                    .setMediaId(e.mediaId)
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("${e.cantique.numero}. ${e.cantique.titre}")
                            .setArtist("${e.piste.type.libelle} · ${e.cantique.langue.libelle}")
                            .setAlbumTitle(Createur.MARQUE)
                            .build(),
                    )
                    .build()
            }
            val i = depart.coerceIn(0, items.lastIndex)
            val reprise = positions[liste[i].mediaId] ?: 0L
            c.setMediaItems(items, i, reprise)
            c.repeatMode = if (_etat.value.repeter) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            c.prepare()
            c.play()
            _etat.update { it.copy(erreur = null) }
            rafraichir()
        }
    }

    fun lecturePause() {
        val c = controleur ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun deplacer(ms: Long) {
        val c = controleur ?: return
        val d = c.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        c.seekTo((c.currentPosition + ms).coerceIn(0, d))
        rafraichir()
    }

    fun allerA(ms: Long) {
        controleur?.seekTo(ms.coerceAtLeast(0))
        rafraichir()
    }

    fun suivant() { controleur?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem() }
    fun precedent() {
        val c = controleur ?: return
        if (c.currentPosition > 3_000 || !c.hasPreviousMediaItem()) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun vitesse(v: Float) {
        controleur?.setPlaybackSpeed(v)
        _etat.update { it.copy(vitesse = v) }
    }

    /** Volume du lecteur (0 à 1), en plus du volume du téléphone. */
    fun volume(v: Float) {
        controleur?.volume = v.coerceIn(0f, 1f)
        _etat.update { it.copy(volume = v.coerceIn(0f, 1f)) }
    }

    fun repeter(oui: Boolean) {
        controleur?.repeatMode = if (oui) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _etat.update { it.copy(repeter = oui) }
    }

    fun arreter() {
        memoriserPosition()
        controleur?.stop()
        controleur?.clearMediaItems()
        elements = emptyList()
        _etat.update { EtatLecteur(vitesse = it.vitesse, volume = it.volume, repeter = it.repeter) }
    }

    fun effacerErreur() = _etat.update { it.copy(erreur = null) }

    private fun memoriserPosition() {
        val c = controleur ?: return
        val id = c.currentMediaItem?.mediaId ?: return
        val pos = c.currentPosition
        if (pos > 5_000 && (c.duration <= 0 || pos < c.duration - 5_000)) positions[id] = pos else positions.remove(id)
    }

    private fun rafraichir() {
        val c = controleur ?: return
        val i = c.currentMediaItemIndex
        val element = elements.getOrNull(i)
        _etat.update {
            it.copy(
                element = element,
                enLecture = c.isPlaying,
                positionMs = c.currentPosition.coerceAtLeast(0),
                dureeMs = c.duration.takeIf { d -> d > 0 } ?: (element?.piste?.dureeMs ?: 0),
                index = i,
                nbElements = elements.size,
                horsLigne = element?.let { e -> Telechargements.get(appli).estTelecharge(e.piste) } == true,
            )
        }
    }

    private fun suivre() {
        suivi?.cancel()
        suivi = scope.launch {
            while (isActive && controleur?.isPlaying == true) {
                rafraichir()
                delay(250)
            }
            rafraichir()
        }
    }
}
