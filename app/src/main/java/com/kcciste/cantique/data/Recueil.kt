package com.kcciste.cantique.data

/**
 * Recueils complets, lus depuis assets/cantiques_lingala.json et assets/cantiques_kikongo.json
 * (générés à partir des fichiers Word par outils/build_json.py).
 */
object Recueil {
    @Volatile
    var cantiques: List<Cantique> = emptyList()
        private set

    fun charger(context: android.content.Context) {
        if (cantiques.isNotEmpty()) return
        cantiques = Langue.entries.flatMap { langue ->
            val json = context.assets.open("cantiques_${langue.name.lowercase()}.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val st = o.getJSONArray("strophes")
                Cantique(
                    numero = o.getInt("numero"),
                    titre = o.getString("titre"),
                    langue = langue,
                    strophes = (0 until st.length()).map { j ->
                        val s = st.getJSONObject(j)
                        Strophe(s.getString("voix"), s.getString("texte"))
                    },
                )
            }
        }
    }

    fun trouver(langue: Langue, numero: Int): Cantique? =
        cantiques.firstOrNull { it.langue == langue && it.numero == numero }
}
