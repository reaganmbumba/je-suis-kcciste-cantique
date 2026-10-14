package com.kcciste.cantique.data

/** Identité du créateur, affichée partout où la fonction est présentée. */
object Createur {
    const val NOM = "Prédicateur Reagan MBUMBA"
    const val NOM_BOUTON = "REAGAN MBUMBA"
    const val TELEPHONE_AFFICHE = "+243 840 238 592"
    /** Format international sans « + » ni espaces, exigé par WhatsApp. */
    const val TELEPHONE_WHATSAPP = "243840238592"
    const val LIEN_WHATSAPP = "https://wa.me/$TELEPHONE_WHATSAPP"
    /** Identifiant de conversation WhatsApp (utilisé pour présélectionner le contact quand WhatsApp l'accepte). */
    const val JID_WHATSAPP = "$TELEPHONE_WHATSAPP@s.whatsapp.net"
}
