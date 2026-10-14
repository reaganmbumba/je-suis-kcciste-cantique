package com.kcciste.cantique.data

import java.text.Normalizer
import java.util.Locale

/** Recherche par numéro, titre et mots des paroles, sans tenir compte des accents ni des majuscules. */
object Recherche {

    fun normaliser(texte: String): String =
        Normalizer.normalize(texte, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    /** Où la requête a été trouvée, du plus pertinent au moins pertinent. */
    enum class Trouve { NUMERO, DEBUT_TITRE, TITRE, PAROLES }

    data class Resultat(val cantique: Cantique, val trouve: Trouve, val extrait: String?)

    /**
     * [langue] nul = toutes les langues. Une requête vide renvoie toute la liste triée par numéro.
     * Plusieurs mots : chaque mot doit être présent (dans le titre ou les paroles).
     */
    fun rechercher(liste: List<Cantique>, requete: String, langue: Langue? = null): List<Resultat> {
        val base = liste.filter { langue == null || it.langue == langue }
        val q = normaliser(requete)
        if (q.isEmpty()) return base.sortedWith(compareBy({ it.langue.ordinal }, { it.numero })).map { Resultat(it, Trouve.TITRE, null) }
        val mots = q.split(' ')
        val numero = q.toIntOrNull()
        return base.mapNotNull { c ->
            val titre = normaliser(c.titre)
            when {
                numero != null && c.numero == numero -> Resultat(c, Trouve.NUMERO, null)
                titre.startsWith(q) -> Resultat(c, Trouve.DEBUT_TITRE, null)
                mots.all { it in titre } -> Resultat(c, Trouve.TITRE, null)
                else -> {
                    val paroles = c.strophes.joinToString("\n") { it.texte }
                    val parolesN = normaliser(paroles)
                    if (mots.all { it in parolesN || it in titre }) Resultat(c, Trouve.PAROLES, extrait(paroles, mots)) else null
                }
            }
        }.sortedWith(compareBy({ it.trouve.ordinal }, { it.cantique.langue.ordinal }, { it.cantique.numero }))
    }

    /** Première ligne des paroles qui contient un des mots cherchés. */
    fun extrait(paroles: String, mots: List<String>): String? =
        paroles.lines().firstOrNull { l -> val n = normaliser(l); mots.any { it in n } }?.trim()
}
