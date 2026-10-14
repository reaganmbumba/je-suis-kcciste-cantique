package com.kcciste.cantique.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RechercheTest {
    private val liste = listOf(
        Cantique(1, "Abalayama mosali na Yawe", Langue.LINGALA, listOf(Strophe("Solo", "Apambuamaki wuta na Njambe"))),
        Cantique(2, "Adonaï yo Yawe", Langue.LINGALA, listOf(Strophe("Tous", "Njambe na nguya pe na ngolu"))),
        Cantique(12, "Yesu azali", Langue.LINGALA, listOf(Strophe("", "Alleluya"))),
        Cantique(2, "Nzambi ya Nguya", Langue.KIKONGO, listOf(Strophe("", "Beto ke sanisa"))),
    )

    @Test
    fun normaliserRetireAccentsEtMajuscules() {
        assertEquals("adonai yo yawe", Recherche.normaliser("  Adonaï, YO Yawé ! "))
    }

    @Test
    fun rechercheParNumeroExact() {
        val r = Recherche.rechercher(liste, "2", Langue.LINGALA)
        assertEquals("Adonaï yo Yawe", r.first().cantique.titre)
        assertEquals(Recherche.Trouve.NUMERO, r.first().trouve)
    }

    @Test
    fun rechercheSansAccent() {
        assertEquals(listOf("lingala-002"), Recherche.rechercher(liste, "adonai", null).map { it.cantique.id })
    }

    @Test
    fun rechercheDansLesParolesAvecExtrait() {
        val r = Recherche.rechercher(liste, "nguya", null)
        // Titre kikongo d'abord (plus pertinent), puis les paroles lingala.
        assertEquals("kikongo-002", r[0].cantique.id)
        assertEquals(Recherche.Trouve.PAROLES, r[1].trouve)
        assertEquals("Njambe na nguya pe na ngolu", r[1].extrait)
    }

    @Test
    fun plusieursMotsDoiventTousEtreTrouves() {
        assertTrue(Recherche.rechercher(liste, "yawe mosali", null).map { it.cantique.numero } == listOf(1))
        assertTrue(Recherche.rechercher(liste, "yawe inexistant", null).isEmpty())
    }

    @Test
    fun requeteVideRenvoieToutTrieParNumero() {
        assertEquals(listOf(1, 2, 12), Recherche.rechercher(liste, "", Langue.LINGALA).map { it.cantique.numero })
    }
}
