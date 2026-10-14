package com.kcciste.cantique.data

/** Règle de fusion, séparée pour être testée sans Android. */
object FusionRecueil {
    /** À identifiant égal, la version la plus récente l'emporte ; les cantiques ajoutés en ligne s'ajoutent. */
    fun fusionner(origine: List<Cantique>, enLigne: List<Cantique>): List<Cantique> {
        val parId = LinkedHashMap<String, Cantique>()
        origine.forEach { parId[it.id] = it }
        enLigne.forEach { c -> val o = parId[c.id]; if (o == null || c.version >= o.version) parId[c.id] = c }
        return parId.values.sortedWith(compareBy({ it.langue.ordinal }, { it.numero }))
    }
}
