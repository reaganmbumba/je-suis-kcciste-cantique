package com.kcciste.cantique.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Format JSON unique des cantiques : assets, cache hors connexion, import et export.
 *
 * { "id": "lingala-001", "numero": 1, "titre": "...", "langue": "lingala",
 *   "categorie": "", "credits": "", "version": 1,
 *   "strophes": [ { "voix": "Solo", "texte": "...", "type": "couplet|refrain" } ],
 *   "traductions": [ { "langue": "pt", "strophes": ["..."], "validee": false, "auteur": "" } ],
 *   "audios": [ { "id": "...", "type": "CHANTEE", "url": "https://...", "taille": 0, "duree": 0, "credits": "" } ] }
 */
object CantiqueJson {

    /** [langueParDefaut] : langue du fichier quand l'objet n'en précise pas (assets par langue). */
    fun lire(o: JSONObject, langueParDefaut: Langue? = null): Cantique {
        val langue = Langue.depuisCode(o.optString("langue", "")) ?: langueParDefaut
            ?: throw IllegalArgumentException("langue manquante ou inconnue")
        val numero = o.getInt("numero")
        val st = o.optJSONArray("strophes") ?: JSONArray()
        return Cantique(
            numero = numero,
            titre = o.getString("titre").trim(),
            langue = langue,
            strophes = (0 until st.length()).map { j ->
                val s = st.getJSONObject(j)
                Strophe(s.optString("voix", ""), s.optString("texte", ""), s.optString("type", ""))
            },
            traductions = lireTraductions(o.optJSONArray("traductions")),
            audios = lireAudios(o.optJSONArray("audios")),
            categorie = o.optString("categorie", ""),
            credits = o.optString("credits", ""),
            version = o.optInt("version", 1),
            id = o.optString("id", "").ifBlank { Cantique.idPour(langue, numero) },
        )
    }

    private fun lireTraductions(a: JSONArray?): List<Traduction> = buildList {
        if (a == null) return@buildList
        for (i in 0 until a.length()) {
            val t = a.getJSONObject(i)
            val l = LangueTraduction.depuisCode(t.optString("langue")) ?: continue
            val s = t.optJSONArray("strophes") ?: JSONArray()
            add(Traduction(l, (0 until s.length()).map { s.getString(it) }, t.optBoolean("validee", false), t.optString("auteur", "")))
        }
    }

    private fun lireAudios(a: JSONArray?): List<PisteAudio> = buildList {
        if (a == null) return@buildList
        for (i in 0 until a.length()) {
            val p = a.getJSONObject(i)
            val url = p.optString("url", "")
            if (url.isBlank()) continue
            add(
                PisteAudio(
                    id = p.optString("id", "").ifBlank { "piste-$i" },
                    type = TypePiste.depuis(p.optString("type")),
                    url = url,
                    tailleOctets = p.optLong("taille", 0),
                    dureeMs = p.optLong("duree", 0),
                    credits = p.optString("credits", ""),
                ),
            )
        }
    }

    fun ecrire(c: Cantique): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("numero", c.numero)
        put("titre", c.titre)
        put("langue", c.langue.code)
        put("categorie", c.categorie)
        put("credits", c.credits)
        put("version", c.version)
        put("strophes", JSONArray().apply {
            c.strophes.forEach { s ->
                put(JSONObject().put("voix", s.voix).put("texte", s.texte).apply { if (s.type.isNotBlank()) put("type", s.type) })
            }
        })
        put("traductions", JSONArray().apply {
            c.traductions.forEach { t ->
                put(
                    JSONObject().put("langue", t.langue.code).put("validee", t.validee).put("auteur", t.auteur)
                        .put("strophes", JSONArray(t.strophes)),
                )
            }
        })
        put("audios", JSONArray().apply {
            c.audios.forEach { p ->
                put(
                    JSONObject().put("id", p.id).put("type", p.type.name).put("url", p.url)
                        .put("taille", p.tailleOctets).put("duree", p.dureeMs).put("credits", p.credits),
                )
            }
        })
    }

    fun lireListe(texte: String, langueParDefaut: Langue? = null): List<Cantique> {
        val arr = JSONArray(texte)
        return (0 until arr.length()).map { lire(arr.getJSONObject(it), langueParDefaut) }
    }

    fun ecrireListe(liste: List<Cantique>): String = JSONArray().apply { liste.forEach { put(ecrire(it)) } }.toString()
}
