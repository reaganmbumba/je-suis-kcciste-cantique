package com.kcciste.cantique.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/**
 * Enregistrement réel du micro en M4A/AAC.
 *
 * Qualité : AAC-LC, mono, 44,1 kHz, 128 kbit/s. C'est un bon compromis pour la voix
 * et le chant (toute la bande audible est conservée) avec environ 1 Mo par minute.
 */
class Enregistreur(private val context: Context) {

    class EchecEnregistrement(cause: Throwable? = null) : Exception(cause)

    private var recorder: MediaRecorder? = null
    private var debutSegment = 0L
    private var cumulMs = 0L
    var enPause = false
        private set
    var fichier: File? = null
        private set

    val actif: Boolean get() = recorder != null

    /** Durée enregistrée, pauses exclues. */
    val dureeMs: Long
        get() = cumulMs + if (recorder != null && !enPause) SystemClock.elapsedRealtime() - debutSegment else 0

    /** Amplitude actuelle entre 0 et 1, pour la waveform. */
    fun amplitude(): Float {
        val r = recorder ?: return 0f
        if (enPause) return 0f
        return runCatching { (r.maxAmplitude / 32767f).coerceIn(0f, 1f) }.getOrDefault(0f)
    }

    @Throws(EchecEnregistrement::class)
    fun demarrer(cible: File) {
        liberer()
        cible.parentFile?.mkdirs()
        // Au moins 20 Mo libres pour démarrer sereinement.
        if ((cible.parentFile?.usableSpace ?: 0L) < 20L * 1024 * 1024) throw EchecEnregistrement()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(128_000)
            r.setOutputFile(cible.absolutePath)
            r.prepare()
            r.start()
        } catch (t: Throwable) {
            runCatching { r.release() }
            cible.delete()
            throw EchecEnregistrement(t)
        }
        recorder = r
        fichier = cible
        cumulMs = 0
        enPause = false
        debutSegment = SystemClock.elapsedRealtime()
    }

    fun pause() {
        val r = recorder ?: return
        if (enPause) return
        r.pause()
        cumulMs += SystemClock.elapsedRealtime() - debutSegment
        enPause = true
    }

    fun reprendre() {
        val r = recorder ?: return
        if (!enPause) return
        r.resume()
        debutSegment = SystemClock.elapsedRealtime()
        enPause = false
    }

    /** Arrête et finalise le fichier. Renvoie la durée, ou lève une erreur si le fichier est inutilisable. */
    @Throws(EchecEnregistrement::class)
    fun arreter(): Long {
        val r = recorder ?: throw EchecEnregistrement()
        val duree = dureeMs
        recorder = null
        enPause = false
        try {
            r.stop()
        } catch (t: RuntimeException) {
            // stop() échoue si aucune donnée valide n'a été reçue du micro.
            fichier?.delete()
            throw EchecEnregistrement(t)
        } finally {
            runCatching { r.release() }
        }
        val f = fichier
        if (f == null || !f.exists() || f.length() == 0L) throw EchecEnregistrement()
        return duree
    }

    /** Abandonne l'enregistrement en cours sans garder de fichier. */
    fun annuler() {
        liberer()
        fichier?.delete()
        fichier = null
    }

    private fun liberer() {
        recorder?.let { r ->
            runCatching { r.stop() }
            runCatching { r.release() }
        }
        recorder = null
        enPause = false
    }
}
