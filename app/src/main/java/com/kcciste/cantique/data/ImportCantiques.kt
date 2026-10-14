package com.kcciste.cantique.data

import org.json.JSONArray

/**
 * Vérification d'un fichier d'import (JSON ou CSV) AVANT tout enregistrement :
 * champs obligatoires, doublons dans le fichier, doublons avec le recueil existant.
 * Un cantique existant n'est jamais remplacé par un import : il est signalé et ignoré.
 */
object ImportCantiques {

    data class Rapport(
        val nouveaux: List<Cantique>,
        /** Cantiques déjà présents (même identifiant ou même langue + numéro) : ignorés. */
        val ignores: List<String>,
        val erreurs: List<String>,
    )

    fun analyser(texte: String, existants: List<Cantique>, langueParDefaut: Langue? = null): Rapport {
        val brut = texte.trimStart('﻿').trim()
        return if (brut.startsWith("[")) analyserJson(brut, existants, langueParDefaut)
        else analyserCsv(brut, existants, langueParDefaut)
    }

    private fun analyserJson(texte: String, existants: List<Cantique>, langueParDefaut: Langue?): Rapport {
        val arr = try { JSONArray(texte) } catch (e: Exception) {
            return Rapport(emptyList(), emptyList(), listOf("Fichier JSON illisible : ${e.message}"))
        }
        val lus = mutableListOf<Pair<Int, Result<Cantique>>>()
        for (i in 0 until arr.length()) {
            lus += (i + 1) to runCatching {
                val o = arr.getJSONObject(i)
                require(o.has("numero")) { "numéro manquant" }
                require(o.optString("titre").isNotBlank()) { "titre manquant" }
                CantiqueJson.lire(o, langueParDefaut)
            }
        }
        return trier(lus, existants)
    }

    /**
     * CSV avec en-tête : numero,titre,langue,paroles[,categorie,credits,id]
     * Dans « paroles », les strophes sont séparées par une ligne vide et peuvent commencer par « Solo : » ou « Tous : ».
     */
    private fun analyserCsv(texte: String, existants: List<Cantique>, langueParDefaut: Langue?): Rapport {
        val lignes = Csv.lire(texte)
        if (lignes.isEmpty()) return Rapport(emptyList(), emptyList(), listOf("Fichier vide."))
        val entete = lignes.first().map { Recherche.normaliser(it) }
        fun col(nom: String) = entete.indexOf(nom)
        val iNum = col("numero"); val iTitre = col("titre"); val iLangue = col("langue"); val iParoles = col("paroles")
        val manquantes = listOf("numero" to iNum, "titre" to iTitre, "paroles" to iParoles).filter { it.second < 0 }.map { it.first }
        if (manquantes.isNotEmpty()) return Rapport(emptyList(), emptyList(), listOf("Colonnes manquantes : ${manquantes.joinToString()}"))
        val lus = lignes.drop(1).mapIndexed { idx, l ->
            (idx + 2) to runCatching {
                fun v(i: Int) = if (i in l.indices) l[i].trim() else ""
                val numero = v(iNum).toIntOrNull() ?: error("numéro invalide « ${v(iNum)} »")
                val titre = v(iTitre).ifBlank { error("titre manquant") }
                val langue = Langue.depuisCode(v(iLangue)) ?: langueParDefaut ?: error("langue manquante ou inconnue « ${v(iLangue)} »")
                val strophes = strophesDepuisTexte(v(iParoles))
                Cantique(
                    numero = numero, titre = titre, langue = langue, strophes = strophes,
                    categorie = v(col("categorie")), credits = v(col("credits")),
                    id = v(col("id")).ifBlank { Cantique.idPour(langue, numero) },
                )
            }
        }
        return trier(lus, existants)
    }

    private fun trier(lus: List<Pair<Int, Result<Cantique>>>, existants: List<Cantique>): Rapport {
        val ids = existants.map { it.id }.toMutableSet()
        val cles = existants.map { it.langue to it.numero }.toMutableSet()
        val nouveaux = mutableListOf<Cantique>()
        val ignores = mutableListOf<String>()
        val erreurs = mutableListOf<String>()
        for ((ligne, r) in lus) {
            val c = r.getOrElse { erreurs += "Élément $ligne : ${it.message}"; null } ?: continue
            when {
                c.strophes.none { it.texte.isNotBlank() } -> erreurs += "Élément $ligne (${c.titre}) : paroles vides"
                c.id in ids || (c.langue to c.numero) in cles ->
                    ignores += "${c.langue.libelle} n° ${c.numero} « ${c.titre} » existe déjà"
                else -> { nouveaux += c; ids += c.id; cles += c.langue to c.numero }
            }
        }
        return Rapport(nouveaux, ignores, erreurs)
    }

    /** « Solo : … » / « Tous : … », strophes séparées par une ligne vide (même règle que outils/build_json.py). */
    fun strophesDepuisTexte(texte: String): List<Strophe> =
        texte.replace("\r\n", "\n").split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotEmpty() }.map { bloc ->
            val m = Regex("^(solo|tous|refrain)\\s*:\\s*", RegexOption.IGNORE_CASE).find(bloc)
            if (m == null) Strophe("", bloc)
            else {
                val mot = m.groupValues[1].lowercase()
                val reste = bloc.substring(m.range.last + 1).trim()
                if (mot == "refrain") Strophe("", reste, "refrain")
                else Strophe(mot.replaceFirstChar { it.uppercase() }, reste)
            }
        }
}

/** Lecteur CSV minimal : séparateur « , » ou « ; », guillemets doubles, retours à la ligne dans les champs. */
object Csv {
    fun lire(texte: String): List<List<String>> {
        val premiere = texte.lineSequence().firstOrNull().orEmpty()
        val sep = if (premiere.count { it == ';' } > premiere.count { it == ',' }) ';' else ','
        val lignes = mutableListOf<List<String>>()
        var champ = StringBuilder()
        var ligne = mutableListOf<String>()
        var guillemets = false
        var i = 0
        while (i < texte.length) {
            val ch = texte[i]
            when {
                guillemets && ch == '"' && i + 1 < texte.length && texte[i + 1] == '"' -> { champ.append('"'); i++ }
                ch == '"' -> guillemets = !guillemets
                !guillemets && ch == sep -> { ligne += champ.toString(); champ = StringBuilder() }
                !guillemets && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < texte.length && texte[i + 1] == '\n') i++
                    ligne += champ.toString(); champ = StringBuilder()
                    if (ligne.any { it.isNotBlank() }) lignes += ligne
                    ligne = mutableListOf()
                }
                else -> champ.append(ch)
            }
            i++
        }
        ligne += champ.toString()
        if (ligne.any { it.isNotBlank() }) lignes += ligne
        return lignes
    }
}
