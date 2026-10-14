package com.kcciste.cantique.audio

import android.media.MediaPlayer
import java.io.File

/** Lecture réelle d'un fichier enregistré. */
class Lecteur {
    private var player: MediaPlayer? = null
    var fichierCourant: File? = null
        private set

    val enLecture: Boolean get() = runCatching { player?.isPlaying == true }.getOrDefault(false)
    val positionMs: Int get() = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
    val dureeMs: Int get() = runCatching { player?.duration ?: 0 }.getOrDefault(0)

    /** Lance (ou relance) la lecture. Renvoie false si le fichier est illisible. */
    fun lire(fichier: File, aLaFin: () -> Unit): Boolean {
        if (fichierCourant == fichier && player != null) {
            player?.start()
            return true
        }
        arreter()
        return try {
            val p = MediaPlayer()
            p.setDataSource(fichier.absolutePath)
            p.setOnCompletionListener { aLaFin() }
            p.prepare()
            p.start()
            player = p
            fichierCourant = fichier
            true
        } catch (t: Exception) {
            arreter()
            false
        }
    }

    fun pause() {
        runCatching { player?.pause() }
    }

    fun allerA(ms: Int) {
        runCatching { player?.seekTo(ms) }
    }

    fun arreter() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        fichierCourant = null
    }
}
