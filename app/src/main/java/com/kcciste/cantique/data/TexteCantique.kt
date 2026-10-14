package com.kcciste.cantique.data

/** Texte d'un cantique tel qu'il est copié ou partagé. */
object TexteCantique {

    /** Paroles seules, strophes séparées par une ligne vide, avec « Solo : » / « Tous : » quand ils existent. */
    fun paroles(c: Cantique, traduction: Traduction? = null): String =
        c.strophes.mapIndexed { i, s ->
            val texte = traduction?.strophes?.getOrNull(i)?.takeIf { it.isNotBlank() } ?: s.texte
            val etiquette = when {
                s.estRefrain -> "Refrain"
                else -> s.voix
            }
            if (etiquette.isBlank()) texte else "$etiquette : $texte"
        }.joinToString("\n\n")

    /** Titre, numéro, paroles, crédits éventuels et signature. */
    fun pourPartage(c: Cantique, traduction: Traduction? = null): String = buildString {
        append("🎵 ").append(c.numero).append(". ").append(c.titre).append('\n')
        append("Cantique ").append(c.langue.libelle)
        if (traduction != null) {
            append(" — traduction ").append(traduction.langue.libelle)
            if (!traduction.validee) append(" (provisoire)")
        }
        append("\n\n")
        append(paroles(c, traduction))
        if (c.credits.isNotBlank()) append("\n\nCrédits : ").append(c.credits.trim())
        append("\n\n").append(Createur.SIGNATURE)
    }
}
