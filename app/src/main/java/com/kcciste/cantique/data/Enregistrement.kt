package com.kcciste.cantique.data

enum class Statut { SAUVEGARDE, PARTAGE }

data class Enregistrement(
    val nomFichier: String,
    val numero: Int,
    val titre: String,
    val langue: Langue,
    val dateMillis: Long,
    val dureeMs: Long,
    val tailleOctets: Long,
    val statut: Statut,
)
