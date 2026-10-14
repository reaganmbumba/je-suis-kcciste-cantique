package com.kcciste.cantique.data

import java.security.SecureRandom
import java.time.LocalDate

/** Statuts d'une suggestion de correction, dans l'ordre du traitement. */
enum class StatutSuggestion(val libelle: String) {
    EN_ATTENTE("En attente"),
    EN_EXAMEN("En examen"),
    ACCEPTEE("Acceptée"),
    REFUSEE("Refusée"),
    PUBLIEE("Publiée"),
    ;

    companion object {
        fun depuis(nom: String?) = entries.firstOrNull { it.name == nom } ?: EN_ATTENTE
    }
}

data class Suggestion(
    val id: String,
    val reference: String,
    val cantiqueId: String,
    val titreCantique: String,
    val versionCantique: Int,
    /** Index de la strophe concernée (-1 = titre ou cantique entier). */
    val indexStrophe: Int,
    val texteActuel: String,
    val correction: String,
    val explication: String,
    val nom: String,
    val uid: String,
    /** Chemin du message vocal dans Firebase Storage, vide s'il n'y en a pas. */
    val cheminAudio: String,
    val statut: StatutSuggestion,
    val dateMillis: Long,
    val noteAdmin: String = "",
)

object Suggestions {
    const val MAX_CORRECTION = 4000
    const val MAX_EXPLICATION = 2000
    const val MAX_AUDIO_OCTETS = 10L * 1024 * 1024
    const val MAX_AUDIO_SECONDES = 300
    val TYPES_AUDIO_ACCEPTES = setOf("audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac", "audio/mpeg", "audio/mp3", "audio/ogg", "audio/opus", "audio/wav", "audio/x-wav", "audio/amr", "audio/3gpp")

    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sans I, O, 0, 1 : pas de confusion à l'oral

    /** Numéro de référence affiché après l'envoi, ex. « KCC-20261014-7XQ4 ». */
    fun nouvelleReference(date: LocalDate, aleatoire: java.util.Random = SecureRandom()): String {
        val code = (1..4).map { ALPHABET[aleatoire.nextInt(ALPHABET.length)] }.joinToString("")
        return "KCC-%04d%02d%02d-%s".format(date.year, date.monthValue, date.dayOfMonth, code)
    }

    /** Messages d'erreur du formulaire ; liste vide = formulaire valide. */
    fun valider(correction: String, explication: String, aUnAudio: Boolean): List<String> = buildList {
        if (correction.isBlank() && !aUnAudio) add("Écrivez la correction proposée ou joignez un message vocal.")
        if (correction.length > MAX_CORRECTION) add("La correction dépasse $MAX_CORRECTION caractères.")
        if (explication.length > MAX_EXPLICATION) add("L'explication dépasse $MAX_EXPLICATION caractères.")
    }

    fun typeAudioAccepte(mime: String?): Boolean = mime != null && mime.lowercase() in TYPES_AUDIO_ACCEPTES
}
