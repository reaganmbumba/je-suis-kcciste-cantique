package com.kcciste.cantique.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportCantiquesTest {
    private val existants = listOf(Cantique(1, "Abalayama mosali na Yawe", Langue.LINGALA, listOf(Strophe("Solo", "x"))))

    @Test
    fun jsonValideDoublonsEtErreurs() {
        val json = """
            [
              {"numero": 1, "titre": "Déjà là", "langue": "lingala", "strophes": [{"voix": "Tous", "texte": "a"}]},
              {"numero": 5, "titre": "Nouveau", "langue": "lingala", "strophes": [{"voix": "Tous", "texte": "b"}]},
              {"numero": 5, "titre": "Doublon dans le fichier", "langue": "lingala", "strophes": [{"voix": "", "texte": "c"}]},
              {"titre": "Sans numéro", "langue": "lingala", "strophes": [{"voix": "", "texte": "d"}]},
              {"numero": 7, "titre": "Sans paroles", "langue": "kikongo", "strophes": []}
            ]
        """.trimIndent()
        val r = ImportCantiques.analyser(json, existants)
        assertEquals(listOf("lingala-005"), r.nouveaux.map { it.id })
        assertEquals(2, r.ignores.size)
        assertEquals(2, r.erreurs.size)
    }

    @Test
    fun csvAvecGuillemetsEtStrophes() {
        val csv = "numero;titre;langue;paroles;credits\n" +
            "3;Yesu azali;lingala;\"Solo : Yesu azali\n\nTous : Azali awa\nna biso\n\nRefrain : Alleluya\";Chorale KCC\n"
        val r = ImportCantiques.analyser(csv, existants)
        assertTrue(r.erreurs.toString(), r.erreurs.isEmpty())
        val c = r.nouveaux.single()
        assertEquals("Chorale KCC", c.credits)
        assertEquals(listOf("Solo", "Tous", ""), c.strophes.map { it.voix })
        assertEquals("Azali awa\nna biso", c.strophes[1].texte)
        assertTrue(c.strophes[2].estRefrain)
    }

    @Test
    fun csvColonnesManquantes() {
        val r = ImportCantiques.analyser("numero,titre\n1,A\n", existants)
        assertTrue(r.nouveaux.isEmpty())
        assertTrue(r.erreurs.single().contains("paroles"))
    }

    @Test
    fun langueParDefaut() {
        val r = ImportCantiques.analyser("""[{"numero": 9, "titre": "T", "strophes": [{"voix": "", "texte": "z"}]}]""", existants, Langue.KIKONGO)
        assertEquals("kikongo-009", r.nouveaux.single().id)
    }
}
