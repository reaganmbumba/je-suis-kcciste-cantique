package com.kcciste.cantique.data

enum class Langue(val libelle: String) {
    LINGALA("Lingala"),
    KIKONGO("Kikongo"),
}

/** Une strophe : [voix] vaut « Solo », « Tous » ou vide. */
data class Strophe(val voix: String, val texte: String)

data class Cantique(
    val numero: Int,
    val titre: String,
    val langue: Langue,
    val strophes: List<Strophe>,
) {
    /** « 001 », « 012 »… utilisé dans les noms de fichiers. */
    val numeroFormate: String get() = numero.toString().padStart(3, '0')
}
