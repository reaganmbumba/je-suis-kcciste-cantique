package com.kcciste.cantique.data

/** Langues originales des recueils. [code] sert dans les identifiants stables (« lingala-001 »). */
enum class Langue(val libelle: String, val code: String) {
    LINGALA("Lingala", "lingala"),
    KIKONGO("Kikongo", "kikongo"),
    ;

    companion object {
        fun depuisCode(code: String?): Langue? = entries.firstOrNull {
            it.code.equals(code, true) || it.name.equals(code, true) || it.libelle.equals(code, true)
        }
    }
}

/** Langues de traduction proposées en plus de la langue originale. */
enum class LangueTraduction(val libelle: String, val code: String) {
    PORTUGAIS("Portugais", "pt"),
    ANGLAIS("Anglais", "en"),
    ;

    companion object {
        fun depuisCode(code: String?): LangueTraduction? = entries.firstOrNull { it.code.equals(code, true) }
    }
}

/**
 * Une strophe : [voix] vaut « Solo », « Tous » ou vide.
 * [type] vaut « couplet » ou « refrain » quand l'administrateur l'a indiqué, sinon vide :
 * les recueils Word actuels ne distinguent que Solo / Tous, rien n'est deviné.
 */
data class Strophe(val voix: String, val texte: String, val type: String = "") {
    val estRefrain: Boolean get() = type.equals("refrain", true)
}

/**
 * Traduction d'un cantique : une strophe traduite par strophe originale, dans le même ordre.
 * Tant que [validee] est faux, l'application l'affiche comme « provisoire ».
 */
data class Traduction(
    val langue: LangueTraduction,
    val strophes: List<String>,
    val validee: Boolean,
    val auteur: String = "",
)

enum class TypePiste(val libelle: String) {
    CHANTEE("Version chantée"),
    INSTRUMENTALE("Version instrumentale"),
    REPETITION("Version de répétition"),
    ;

    companion object {
        fun depuis(nom: String?): TypePiste = entries.firstOrNull { it.name.equals(nom, true) } ?: CHANTEE
    }
}

/** Piste audio autorisée, ajoutée par l'administrateur (fichier hébergé ou URL autorisée). */
data class PisteAudio(
    val id: String,
    val type: TypePiste,
    val url: String,
    val tailleOctets: Long = 0,
    val dureeMs: Long = 0,
    val credits: String = "",
)

data class Cantique(
    val numero: Int,
    val titre: String,
    val langue: Langue,
    val strophes: List<Strophe>,
    val traductions: List<Traduction> = emptyList(),
    val audios: List<PisteAudio> = emptyList(),
    val categorie: String = "",
    /** Auteurs, compositeurs, droits d'utilisation. Vide = non renseigné. */
    val credits: String = "",
    /** Version du texte : 1 pour le recueil d'origine, +1 à chaque publication de l'administrateur. */
    val version: Int = 1,
    /** Identifiant stable : il ne change jamais, même si le titre ou les paroles changent. */
    val id: String = idPour(langue, numero),
) {
    /** « 001 », « 012 »… utilisé dans les noms de fichiers. */
    val numeroFormate: String get() = numero.toString().padStart(3, '0')

    fun traduction(l: LangueTraduction): Traduction? = traductions.firstOrNull { it.langue == l }

    companion object {
        fun idPour(langue: Langue, numero: Int) = "${langue.code}-${numero.toString().padStart(3, '0')}"
    }
}
