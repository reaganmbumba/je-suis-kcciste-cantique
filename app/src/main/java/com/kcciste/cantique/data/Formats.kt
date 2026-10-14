package com.kcciste.cantique.data

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

object Formats {

    /** 222 000 ms -> « 03:42 » ; au-delà d'une heure -> « 1:02:03 ». */
    fun duree(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0) / 1000)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ROOT, "%02d:%02d", m, s)
    }

    /** 4 823 449 octets -> « 4,6 Mo ». */
    fun taille(octets: Long): String {
        val ko = octets / 1024.0
        return when {
            ko < 1 -> "$octets o"
            ko < 1024 -> String.format(Locale.FRANCE, "%.0f Ko", ko)
            else -> String.format(Locale.FRANCE, "%.1f Mo", ko / 1024.0)
        }
    }

    /**
     * Nom automatique : KCCiste_Cantique_[Numero]_[Titre]_[Date].m4a
     * ex. KCCiste_Cantique_001_Abalayama_Mosali_Na_Yawe_2026-10-02.m4a
     * Les accents et la ponctuation sont retirés pour un nom sûr sur tous les systèmes.
     * [existe] permet d'ajouter un suffixe _2, _3… si le nom est déjà pris.
     */
    fun nomFichier(
        cantique: Cantique,
        date: LocalDate,
        existe: (String) -> Boolean = { false },
    ): String {
        val base = "KCCiste_Cantique_${cantique.numeroFormate}_${titrePourFichier(cantique.titre)}_$date"
        var nom = "$base.m4a"
        var n = 2
        while (existe(nom)) {
            nom = "${base}_$n.m4a"
            n++
        }
        return nom
    }

    fun titrePourFichier(titre: String): String {
        val sansAccents = Normalizer.normalize(titre, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return sansAccents
            .split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotBlank() }
            .joinToString("_") { mot -> mot.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) } }
            .take(60)
            .trimEnd('_')
            .ifEmpty { "Sans_Titre" }
    }

    /** Message WhatsApp prérempli, mot pour mot selon le cahier des charges. */
    fun messageWhatsApp(titre: String, langue: String, duree: String): String =
        """
        Bonjour ${Createur.NOM},

        Je vous envoie mon enregistrement du cantique :

        🎵 $titre

        Langue : $langue
        Durée : $duree

        Enregistrement réalisé depuis l'application
        KCCiste Cantique.

        Merci.
        """.trimIndent()
}
