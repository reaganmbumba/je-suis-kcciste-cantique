package com.kcciste.cantique

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kcciste.cantique.data.Langue
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.ui.screens.EcranAPropos
import com.kcciste.cantique.ui.screens.EcranAccueil
import com.kcciste.cantique.ui.screens.EcranAide
import com.kcciste.cantique.ui.screens.EcranCantique
import com.kcciste.cantique.ui.screens.EcranDiscussion
import com.kcciste.cantique.ui.screens.EcranEnregistrement
import com.kcciste.cantique.ui.screens.EcranMesEnregistrements
import com.kcciste.cantique.ui.screens.EcranProfil
import com.kcciste.cantique.ui.theme.KCCisteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15+ affiche toujours l'appli bord à bord ; les écrans Compose gèrent les marges.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Recueil.charger(applicationContext)
        setContent {
            KCCisteTheme {
                val nav = rememberNavController()
                val retour: () -> Unit = { nav.popBackStack() }
                val mes = { nav.navigate("mes") }
                NavHost(nav, startDestination = "accueil") {
                    composable("accueil") {
                        EcranAccueil(
                            ouvrir = { c -> nav.navigate("cantique/${c.langue.name}/${c.numero}") },
                            mesEnregistrements = mes,
                            aPropos = { nav.navigate("apropos") },
                            aide = { nav.navigate("aide") },
                            profil = { nav.navigate("profil") },
                            discussion = { nav.navigate("discussion") },
                        )
                    }
                    composable("cantique/{langue}/{numero}") { entry ->
                        val c = cantiqueDe(entry.arguments?.getString("langue"), entry.arguments?.getString("numero"))
                        if (c != null) {
                            EcranCantique(c, retour) { nav.navigate("enregistrer/${c.langue.name}/${c.numero}") }
                        }
                    }
                    composable("enregistrer/{langue}/{numero}") { entry ->
                        val c = cantiqueDe(entry.arguments?.getString("langue"), entry.arguments?.getString("numero"))
                        if (c != null) EcranEnregistrement(c, retour, mes)
                    }
                    composable("mes") { EcranMesEnregistrements(retour) }
                    composable("discussion") { EcranDiscussion(retour) }
                    composable("apropos") { EcranAPropos(retour) }
                    composable("aide") { EcranAide(retour) }
                    composable("profil") { EcranProfil(retour, mes) }
                }
            }
        }
    }

    private fun cantiqueDe(langue: String?, numero: String?) =
        runCatching { Recueil.trouver(Langue.valueOf(langue!!), numero!!.toInt()) }.getOrNull()
}
