package com.kcciste.cantique.data

import java.net.URLEncoder

/** Identité de l'application et de son concepteur, affichée partout où elle est présentée. */
object Createur {
    const val APPLICATION = "Je suis KCCiste Cantique"
    const val MARQUE = "KCCISTE CANTIQUE"
    const val COMMUNAUTE = "Je suis KCCiste"
    const val NOM = "Prédicateur Reagan MBUMBA"
    const val NOM_BOUTON = "REAGAN MBUMBA"
    const val TELEPHONE_AFFICHE = "+243 840 238 592"
    /** Format international sans « + » ni espaces, exigé par WhatsApp. */
    const val TELEPHONE_WHATSAPP = "243840238592"
    const val LIEN_WHATSAPP = "https://wa.me/$TELEPHONE_WHATSAPP"
    /** Identifiant de conversation WhatsApp (utilisé pour présélectionner le contact quand WhatsApp l'accepte). */
    const val JID_WHATSAPP = "$TELEPHONE_WHATSAPP@s.whatsapp.net"

    const val MESSAGE_CONTACT = "Bonjour Prédicateur Reagan MBUMBA, je vous contacte au sujet de KCCISTE CANTIQUE."

    /** Signature ajoutée à chaque texte partagé. Elle ne revendique aucun droit sur les paroles. */
    val SIGNATURE = """
        KCCISTE CANTIQUE
        © KCCISTE CANTIQUE — Je suis KCCiste
        Conception : Prédicateur Reagan MBUMBA
    """.trimIndent()

    val MOTIFS = listOf(
        "Correction de cantique",
        "Traduction",
        "Problème audio",
        "Problème technique",
        "Suggestion",
        "Collaboration",
    )

    /** Message prérempli, avec le motif choisi s'il y en a un. */
    fun messageContact(motif: String?): String =
        if (motif.isNullOrBlank()) MESSAGE_CONTACT else "$MESSAGE_CONTACT\nMotif : $motif"

    /** https://wa.me/243840238592?text=… */
    fun lienContact(motif: String?): String =
        "$LIEN_WHATSAPP?text=" + URLEncoder.encode(messageContact(motif), "UTF-8").replace("+", "%20")
}
