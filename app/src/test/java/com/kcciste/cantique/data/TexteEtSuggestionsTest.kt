package com.kcciste.cantique.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Random

class TexteEtSuggestionsTest {
    private val c = Cantique(
        12, "Yesu azali", Langue.LINGALA,
        listOf(Strophe("Solo", "Yesu azali"), Strophe("Tous", "Azali awa")),
        traductions = listOf(Traduction(LangueTraduction.PORTUGAIS, listOf("Jesus está", ""), validee = false)),
    )

    @Test
    fun texteDePartageContientTitreNumeroParolesEtSignature() {
        val t = TexteCantique.pourPartage(c)
        assertTrue(t.startsWith("🎵 12. Yesu azali"))
        assertTrue(t.contains("Solo : Yesu azali\n\nTous : Azali awa"))
        assertTrue(t.endsWith("KCCISTE CANTIQUE\n© KCCISTE CANTIQUE — Je suis KCCiste\nConception : Prédicateur Reagan MBUMBA"))
    }

    @Test
    fun traductionProvisoireSignaleeEtStropheManquanteGardeLOriginal() {
        val t = TexteCantique.pourPartage(c, c.traduction(LangueTraduction.PORTUGAIS))
        assertTrue(t.contains("traduction Portugais (provisoire)"))
        assertTrue(t.contains("Solo : Jesus está\n\nTous : Azali awa"))
    }

    @Test
    fun referenceAuBonFormat() {
        val ref = Suggestions.nouvelleReference(LocalDate.of(2026, 10, 14), Random(1))
        assertTrue(ref, Regex("KCC-20261014-[A-HJ-NP-Z2-9]{4}").matches(ref))
    }

    @Test
    fun validationDuFormulaire() {
        assertEquals(1, Suggestions.valider("", "", aUnAudio = false).size)
        assertTrue(Suggestions.valider("", "", aUnAudio = true).isEmpty())
        assertTrue(Suggestions.valider("texte", "", aUnAudio = false).isEmpty())
        assertEquals(1, Suggestions.valider("x".repeat(Suggestions.MAX_CORRECTION + 1), "", false).size)
    }

    @Test
    fun typesAudio() {
        assertTrue(Suggestions.typeAudioAccepte("audio/mp4"))
        assertTrue(!Suggestions.typeAudioAccepte("video/mp4"))
        assertTrue(!Suggestions.typeAudioAccepte(null))
    }

    @Test
    fun lienContactWhatsApp() {
        assertEquals(
            "https://wa.me/243840238592?text=Bonjour%20Pr%C3%A9dicateur%20Reagan%20MBUMBA%2C%20je%20vous%20contacte%20au%20sujet%20de%20KCCISTE%20CANTIQUE.",
            Createur.lienContact(null),
        )
        assertTrue(Createur.lienContact("Traduction").endsWith("%0AMotif%20%3A%20Traduction"))
    }

    @Test
    fun comparaisonLigneParLigne() {
        val d = Comparaison.lignes("a\nb\nc", "a\nB\nc\nd")
        assertEquals(
            listOf("= a", "- b", "+ B", "= c", "+ d"),
            d.map { (when (it.etat) { Comparaison.Etat.IDENTIQUE -> "= "; Comparaison.Etat.SUPPRIMEE -> "- "; Comparaison.Etat.AJOUTEE -> "+ " }) + it.texte },
        )
    }

    @Test
    fun historiqueSansDoublonEtLimite() {
        assertEquals(listOf("b", "a", "c"), Historique.ajouter(listOf("a", "b", "c"), "b", 10))
        assertEquals(listOf("z", "a"), Historique.ajouter(listOf("a", "b", "c"), "z", 2))
    }
}
