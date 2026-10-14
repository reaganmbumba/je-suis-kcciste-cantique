package com.kcciste.cantique.data

/** Comparaison ligne par ligne entre l'ancien texte et la correction proposée. */
object Comparaison {
    enum class Etat { IDENTIQUE, SUPPRIMEE, AJOUTEE }

    data class Ligne(val etat: Etat, val texte: String)

    /** Différence par plus longue sous-séquence commune (textes courts : quelques dizaines de lignes). */
    fun lignes(ancien: String, nouveau: String): List<Ligne> {
        val a = ancien.lines()
        val b = nouveau.lines()
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val res = mutableListOf<Ligne>()
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            when {
                a[i] == b[j] -> { res += Ligne(Etat.IDENTIQUE, a[i]); i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> { res += Ligne(Etat.SUPPRIMEE, a[i]); i++ }
                else -> { res += Ligne(Etat.AJOUTEE, b[j]); j++ }
            }
        }
        while (i < a.size) res += Ligne(Etat.SUPPRIMEE, a[i++])
        while (j < b.size) res += Ligne(Etat.AJOUTEE, b[j++])
        return res
    }
}
