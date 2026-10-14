package com.kcciste.cantique.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormatsTest {
    private val abalayama = Cantique(1, "Abalayama mosali na Yawe", Langue.LINGALA, emptyList())

    @Test
    fun nomFichierConformeALexemple() {
        assertEquals(
            "KCCiste_Cantique_001_Abalayama_Mosali_Na_Yawe_2026-10-02.m4a",
            Formats.nomFichier(abalayama, LocalDate.of(2026, 10, 2)),
        )
    }

    @Test
    fun nomFichierSansAccentsNiPonctuation() {
        val c = Cantique(12, "Adonaï yo Yawe, Alléluia !", Langue.LINGALA, emptyList())
        assertEquals(
            "KCCiste_Cantique_012_Adonai_Yo_Yawe_Alleluia_2026-10-02.m4a",
            Formats.nomFichier(c, LocalDate.of(2026, 10, 2)),
        )
    }

    @Test
    fun nomFichierDejaPrisRecoitUnSuffixe() {
        val pris = setOf("KCCiste_Cantique_001_Abalayama_Mosali_Na_Yawe_2026-10-02.m4a")
        assertEquals(
            "KCCiste_Cantique_001_Abalayama_Mosali_Na_Yawe_2026-10-02_2.m4a",
            Formats.nomFichier(abalayama, LocalDate.of(2026, 10, 2)) { it in pris },
        )
    }

    @Test
    fun duree() {
        assertEquals("00:00", Formats.duree(0))
        assertEquals("02:35", Formats.duree(155_000))
        assertEquals("03:42", Formats.duree(222_999))
        assertEquals("1:02:03", Formats.duree(3_723_000))
    }

    @Test
    fun taille() {
        assertEquals("4,6 Mo", Formats.taille(4_823_449))
        assertEquals("500 Ko", Formats.taille(512_000))
    }

    @Test
    fun messageWhatsAppPrerempli() {
        val m = Formats.messageWhatsApp("Abalayama mosali na Yawe", "Lingala", "03:42")
        assertTrue(m.startsWith("Bonjour Prédicateur Reagan MBUMBA,"))
        assertTrue(m.contains("🎵 Abalayama mosali na Yawe"))
        assertTrue(m.contains("Langue : Lingala"))
        assertTrue(m.contains("Durée : 03:42"))
        assertTrue(m.contains("KCCiste Cantique."))
        assertTrue(m.trimEnd().endsWith("Merci."))
    }

    @Test
    fun numeroCreateur() {
        assertEquals("https://wa.me/243840238592", Createur.LIEN_WHATSAPP)
        assertEquals("243840238592@s.whatsapp.net", Createur.JID_WHATSAPP)
    }
}
