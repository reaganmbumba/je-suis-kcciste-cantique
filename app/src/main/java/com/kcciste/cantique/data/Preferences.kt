package com.kcciste.cantique.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/** Favoris, historique et derniers audios : gardés sur le téléphone, sans compte. */
class Bibliotheque private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bibliotheque", Context.MODE_PRIVATE)

    private val _favoris = MutableStateFlow(lire(FAVORIS))
    /** Identifiants des cantiques favoris, le plus récent en premier. */
    val favoris: StateFlow<List<String>> = _favoris.asStateFlow()

    private val _historique = MutableStateFlow(lire(HISTORIQUE))
    val historique: StateFlow<List<String>> = _historique.asStateFlow()

    private val _audios = MutableStateFlow(lire(AUDIOS))
    /** « idCantique|idPiste », la plus récente en premier. */
    val audiosEcoutes: StateFlow<List<String>> = _audios.asStateFlow()

    fun estFavori(id: String) = id in _favoris.value

    fun basculerFavori(id: String) {
        val l = _favoris.value
        ecrire(FAVORIS, _favoris, if (id in l) l - id else listOf(id) + l)
    }

    fun consulte(id: String) = ecrire(HISTORIQUE, _historique, Historique.ajouter(_historique.value, id, MAX_HISTORIQUE))

    fun ecoute(cantiqueId: String, pisteId: String) =
        ecrire(AUDIOS, _audios, Historique.ajouter(_audios.value, "$cantiqueId|$pisteId", MAX_AUDIOS))

    fun viderHistorique() {
        ecrire(HISTORIQUE, _historique, emptyList())
        ecrire(AUDIOS, _audios, emptyList())
    }

    private fun lire(cle: String): List<String> = runCatching {
        val a = JSONArray(prefs.getString(cle, "[]"))
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())

    private fun ecrire(cle: String, flux: MutableStateFlow<List<String>>, valeur: List<String>) {
        flux.value = valeur
        prefs.edit().putString(cle, JSONArray(valeur).toString()).apply()
    }

    companion object {
        private const val FAVORIS = "favoris"
        private const val HISTORIQUE = "historique"
        private const val AUDIOS = "audios"
        const val MAX_HISTORIQUE = 50
        const val MAX_AUDIOS = 30

        @Volatile private var instance: Bibliotheque? = null
        fun get(context: Context): Bibliotheque =
            instance ?: synchronized(this) { instance ?: Bibliotheque(context.applicationContext).also { instance = it } }
    }
}

enum class Theme(val libelle: String) { SYSTEME("Comme le téléphone"), CLAIR("Clair"), SOMBRE("Sombre") }

data class Reglages(
    val theme: Theme = Theme.SYSTEME,
    /** Taille du texte des paroles, en sp. */
    val tailleTexte: Float = 20f,
    val ecranAllume: Boolean = true,
    val afficherVoix: Boolean = true,
) {
    companion object {
        const val TAILLE_MIN = 14f
        const val TAILLE_MAX = 40f
    }
}

object ReglagesRepository {
    private val _flux = MutableStateFlow(Reglages())
    val flux: StateFlow<Reglages> = _flux.asStateFlow()
    private var prefs: SharedPreferences? = null

    fun charger(context: Context) {
        val p = context.getSharedPreferences("reglages", Context.MODE_PRIVATE)
        prefs = p
        _flux.value = Reglages(
            theme = runCatching { Theme.valueOf(p.getString("theme", Theme.SYSTEME.name)!!) }.getOrDefault(Theme.SYSTEME),
            tailleTexte = p.getFloat("taille", 20f).coerceIn(Reglages.TAILLE_MIN, Reglages.TAILLE_MAX),
            ecranAllume = p.getBoolean("ecran", true),
            afficherVoix = p.getBoolean("voix", true),
        )
    }

    fun modifier(f: (Reglages) -> Reglages) {
        val r = f(_flux.value).let { it.copy(tailleTexte = it.tailleTexte.coerceIn(Reglages.TAILLE_MIN, Reglages.TAILLE_MAX)) }
        _flux.value = r
        prefs?.edit()
            ?.putString("theme", r.theme.name)
            ?.putFloat("taille", r.tailleTexte)
            ?.putBoolean("ecran", r.ecranAllume)
            ?.putBoolean("voix", r.afficherVoix)
            ?.apply()
    }
}
