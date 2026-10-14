package com.kcciste.cantique.data

object Historique {
    /** Place [id] en tête, sans doublon, en gardant au plus [max] éléments. */
    fun ajouter(liste: List<String>, id: String, max: Int): List<String> = (listOf(id) + liste.filter { it != id }).take(max)
}
