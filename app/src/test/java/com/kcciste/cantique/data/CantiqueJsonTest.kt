package com.kcciste.cantique.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CantiqueJsonTest {
    @Test
    fun allerRetourCompletSansPerte() {
        val c = Cantique(
            4, "Ata ba pasi", Langue.LINGALA,
            listOf(Strophe("Solo", "a\nb"), Strophe("", "refrain", "refrain")),
            traductions = listOf(Traduction(LangueTraduction.ANGLAIS, listOf("x", "y"), true, "admin@kcc")),
            audios = listOf(PisteAudio("p1", TypePiste.INSTRUMENTALE, "https://exemple.org/a.mp3", 1234, 5678, "Chorale")),
            categorie = "Louange", credits = "Auteur inconnu", version = 3,
        )
        assertEquals(listOf(c), CantiqueJson.lireListe(CantiqueJson.ecrireListe(listOf(c))))
    }

    /** Les recueils fournis avec l'application se lisent tous, avec des identifiants uniques et stables. */
    @Test
    fun recueilsDeLApplicationLisibles() {
        val assets = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }
        val l = CantiqueJson.lireListe(File(assets, "cantiques_lingala.json").readText(), Langue.LINGALA)
        val k = CantiqueJson.lireListe(File(assets, "cantiques_kikongo.json").readText(), Langue.KIKONGO)
        assertEquals(113, l.size)
        assertEquals(201, k.size)
        assertEquals("lingala-001", l.first().id)
        assertEquals(314, (l + k).map { it.id }.toSet().size)
    }

    @Test
    fun fusionLaVersionEnLigneRemplaceLOrigine() {
        val o = listOf(Cantique(1, "Ancien", Langue.LINGALA, emptyList()), Cantique(2, "B", Langue.LINGALA, emptyList()))
        val enLigne = listOf(
            Cantique(1, "Corrigé", Langue.LINGALA, emptyList(), version = 2),
            Cantique(3, "Ajouté", Langue.LINGALA, emptyList()),
        )
        assertEquals(listOf("Corrigé", "B", "Ajouté"), FusionRecueil.fusionner(o, enLigne).map { it.titre })
    }
}
