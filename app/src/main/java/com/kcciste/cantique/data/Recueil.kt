package com.kcciste.cantique.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Bibliothèque des cantiques.
 *  1. Recueil d'origine : assets/cantiques_lingala.json et assets/cantiques_kikongo.json
 *     (générés à partir des fichiers Word par outils/build_json.py).
 *  2. Contenu publié en ligne par l'administrateur (Firestore, voir [ContenuEnLigne]) :
 *     corrections, traductions, pistes audio, nouveaux cantiques. Il est gardé dans
 *     files/contenu_en_ligne.json pour rester consultable hors connexion.
 * À identifiant égal, la version publiée en ligne remplace celle d'origine, sans mise à jour de l'application.
 */
object Recueil {
    private const val CACHE = "contenu_en_ligne.json"

    private val _flux = MutableStateFlow<List<Cantique>>(emptyList())
    val flux: StateFlow<List<Cantique>> = _flux.asStateFlow()
    val cantiques: List<Cantique> get() = _flux.value

    @Volatile private var origine: List<Cantique> = emptyList()
    @Volatile private var enLigne: List<Cantique> = emptyList()
    @Volatile private var charge = false

    fun charger(context: Context) {
        if (charge) return
        charge = true
        origine = Langue.entries.flatMap { langue ->
            runCatching {
                val json = context.assets.open("cantiques_${langue.code}.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                CantiqueJson.lireListe(json, langue)
            }.getOrDefault(emptyList())
        }
        enLigne = runCatching { CantiqueJson.lireListe(File(context.filesDir, CACHE).readText()) }.getOrDefault(emptyList())
        _flux.value = FusionRecueil.fusionner(origine, enLigne)
    }

    /** Appelé quand le contenu en ligne a changé : mémorise pour le hors connexion et met à jour l'affichage. */
    fun appliquerEnLigne(context: Context, liste: List<Cantique>) {
        enLigne = liste
        runCatching {
            val f = File(context.filesDir, CACHE)
            val tmp = File(context.filesDir, "$CACHE.tmp")
            tmp.writeText(CantiqueJson.ecrireListe(liste))
            tmp.renameTo(f)
        }
        _flux.value = FusionRecueil.fusionner(origine, enLigne)
    }

    fun trouver(id: String): Cantique? = cantiques.firstOrNull { it.id == id }

    fun trouver(langue: Langue, numero: Int): Cantique? =
        cantiques.firstOrNull { it.langue == langue && it.numero == numero }

    /** Texte du recueil d'origine (avant toute correction en ligne). */
    fun versionOrigine(id: String): Cantique? = origine.firstOrNull { it.id == id }

    fun categories(langue: Langue?): List<String> =
        cantiques.filter { langue == null || it.langue == langue }.map { it.categorie.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

    /** Cantique précédent / suivant dans le même recueil (pour le balayage et la liste de lecture). */
    fun voisin(c: Cantique, decalage: Int): Cantique? {
        val liste = cantiques.filter { it.langue == c.langue }
        val i = liste.indexOfFirst { it.id == c.id }
        return if (i < 0) null else liste.getOrNull(i + decalage)
    }
}
