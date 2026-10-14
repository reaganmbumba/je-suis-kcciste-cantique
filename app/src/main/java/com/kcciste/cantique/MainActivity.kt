package com.kcciste.cantique

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.Langue
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.data.ReglagesRepository
import com.kcciste.cantique.ui.components.LogoKCC
import com.kcciste.cantique.ui.screens.ActionsAccueil
import com.kcciste.cantique.ui.screens.ActionsCantique
import com.kcciste.cantique.ui.screens.EcranAPropos
import com.kcciste.cantique.ui.screens.EcranAccueil
import com.kcciste.cantique.ui.screens.EcranAide
import com.kcciste.cantique.ui.screens.EcranBibliotheque
import com.kcciste.cantique.ui.screens.EcranCantique
import com.kcciste.cantique.ui.screens.EcranConfirmation
import com.kcciste.cantique.ui.screens.EcranContact
import com.kcciste.cantique.ui.screens.EcranCorrection
import com.kcciste.cantique.ui.screens.EcranDiscussion
import com.kcciste.cantique.ui.screens.EcranEnregistrement
import com.kcciste.cantique.ui.screens.EcranFavoris
import com.kcciste.cantique.ui.screens.EcranHistorique
import com.kcciste.cantique.ui.screens.EcranLecteurAudio
import com.kcciste.cantique.ui.screens.EcranMesEnregistrements
import com.kcciste.cantique.ui.screens.EcranParametres
import com.kcciste.cantique.ui.screens.EcranProfil
import com.kcciste.cantique.ui.screens.EcranRecherche
import com.kcciste.cantique.ui.screens.EcranTelechargements
import com.kcciste.cantique.ui.screens.admin.ActionsAdmin
import com.kcciste.cantique.ui.screens.admin.ActionsCantiqueAdmin
import com.kcciste.cantique.ui.screens.admin.EcranEditionCantique
import com.kcciste.cantique.ui.screens.admin.EcranFicheCantique
import com.kcciste.cantique.ui.screens.admin.EcranGestionCantiques
import com.kcciste.cantique.ui.screens.admin.EcranImport
import com.kcciste.cantique.ui.screens.admin.EcranJournal
import com.kcciste.cantique.ui.screens.admin.EcranModeration
import com.kcciste.cantique.ui.screens.admin.EcranPistesAudio
import com.kcciste.cantique.ui.screens.admin.EcranSuggestion
import com.kcciste.cantique.ui.screens.admin.EcranTableauDeBord
import com.kcciste.cantique.ui.screens.admin.EcranTraductions
import com.kcciste.cantique.ui.screens.admin.EcranVersions
import com.kcciste.cantique.ui.theme.BleuNuit
import com.kcciste.cantique.ui.theme.KCCisteTheme
import com.kcciste.cantique.ui.theme.Or
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15+ affiche toujours l'appli bord à bord ; les écrans Compose gèrent les marges.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Recueil.charger(applicationContext)
        setContent {
            val reglages by ReglagesRepository.flux.collectAsState()
            KCCisteTheme(reglages.theme) {
                Surface {
                    var lancement by rememberSaveable { mutableStateOf(savedInstanceState == null) }
                    LaunchedEffect(Unit) { delay(1200); lancement = false }
                    Box {
                        Navigation(rememberNavController())
                        AnimatedVisibility(visible = lancement, exit = fadeOut()) { EcranLancement() }
                    }
                }
            }
        }
    }
}

/** Écran de lancement : logo KCC doré encerclé sur fond blanc. */
@Composable
private fun EcranLancement() {
    Column(
        Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LogoKCC(150.dp)
        Spacer(Modifier.height(24.dp))
        Text(Createur.MARQUE, color = BleuNuit, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
        Text(Createur.COMMUNAUTE, color = Or, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Text("Conception : ${Createur.NOM}", color = Color(0xFF5F6472), fontSize = 12.sp)
    }
}

@Composable
private fun Navigation(nav: NavHostController) {
    val retour: () -> Unit = { nav.popBackStack() }
    val ouvrirId: (String) -> Unit = { id -> nav.navigate("cantique/$id") }
    val actionsCantique = ActionsCantique(
        retour = retour,
        corriger = { c -> nav.navigate("correction/${c.id}") },
        enregistrer = { c -> nav.navigate("enregistrer/${c.id}") },
        lecteur = { nav.navigate("lecteur") },
    )
    NavHost(nav, startDestination = "accueil") {
        composable("accueil") {
            EcranAccueil(
                ActionsAccueil(
                    ouvrir = { c -> ouvrirId(c.id) },
                    bibliotheque = { l -> nav.navigate("bibliotheque/${l?.name ?: "LINGALA"}") },
                    recherche = { nav.navigate("recherche") },
                    favoris = { nav.navigate("favoris") },
                    historique = { nav.navigate("historique") },
                    telechargements = { nav.navigate("telechargements") },
                    mesEnregistrements = { nav.navigate("mes") },
                    discussion = { nav.navigate("discussion") },
                    contact = { nav.navigate("contact") },
                    parametres = { nav.navigate("parametres") },
                    aPropos = { nav.navigate("apropos") },
                    aide = { nav.navigate("aide") },
                    lecteur = { nav.navigate("lecteur") },
                ),
            )
        }
        composable("bibliotheque/{langue}") { e ->
            val l = runCatching { Langue.valueOf(e.arguments?.getString("langue")!!) }.getOrNull()
            EcranBibliotheque(l, { c -> ouvrirId(c.id) }, retour) { nav.navigate("lecteur") }
        }
        composable("recherche") { EcranRecherche({ c -> ouvrirId(c.id) }, retour) }
        composable("cantique/{id}") { e -> EcranCantique(e.arguments?.getString("id").orEmpty(), actionsCantique) }
        composable("lecteur") { EcranLecteurAudio(retour, ouvrirId) }
        composable("telechargements") { EcranTelechargements(retour, ouvrirId) }
        composable("favoris") { EcranFavoris({ c -> ouvrirId(c.id) }, retour) }
        composable("historique") { EcranHistorique({ c -> ouvrirId(c.id) }, retour) }
        composable("correction/{id}") { e ->
            EcranCorrection(e.arguments?.getString("id").orEmpty(), retour) { ref ->
                nav.navigate("confirmation/$ref") { popUpTo("correction/{id}") { inclusive = true } }
            }
        }
        composable("confirmation/{ref}") { e -> EcranConfirmation(e.arguments?.getString("ref").orEmpty(), retour) }
        composable("contact") { EcranContact(retour) }
        composable("parametres") {
            EcranParametres(
                retour,
                telechargements = { nav.navigate("telechargements") },
                admin = { nav.navigate("admin") },
                aide = { nav.navigate("aide") },
                profil = { nav.navigate("profil") },
            )
        }
        composable("apropos") { EcranAPropos(retour) }
        composable("aide") { EcranAide(retour) }
        composable("profil") { EcranProfil(retour) { nav.navigate("mes") } }

        // Fonctions existantes de KCCiste Cantique
        composable("enregistrer/{id}") { e ->
            val c = Recueil.trouver(e.arguments?.getString("id").orEmpty())
            if (c != null) EcranEnregistrement(c, retour) { nav.navigate("mes") }
        }
        composable("mes") { EcranMesEnregistrements(retour) }
        composable("discussion") { EcranDiscussion(retour) }

        // Administration
        composable("admin") {
            EcranTableauDeBord(
                ActionsAdmin(
                    retour = retour,
                    cantiques = { nav.navigate("admin/cantiques") },
                    suggestions = { nav.navigate("admin/suggestions") },
                    importer = { nav.navigate("admin/import") },
                    journal = { nav.navigate("admin/journal") },
                    nouveau = { nav.navigate("admin/nouveau") },
                ),
            )
        }
        composable("admin/cantiques") { EcranGestionCantiques({ id -> nav.navigate("admin/cantique/$id") }, retour) }
        composable("admin/cantique/{id}") { e ->
            EcranFicheCantique(
                e.arguments?.getString("id").orEmpty(),
                ActionsCantiqueAdmin(
                    retour = retour,
                    modifier = { id -> nav.navigate("admin/edition/$id") },
                    audios = { id -> nav.navigate("admin/audios/$id") },
                    traductions = { id -> nav.navigate("admin/traductions/$id") },
                    versions = { id -> nav.navigate("admin/versions/$id") },
                ),
            )
        }
        composable("admin/nouveau") { EcranEditionCantique(null, retour) }
        composable("admin/edition/{id}") { e -> EcranEditionCantique(e.arguments?.getString("id"), retour) }
        composable("admin/audios/{id}") { e -> EcranPistesAudio(e.arguments?.getString("id").orEmpty(), retour) }
        composable("admin/traductions/{id}") { e -> EcranTraductions(e.arguments?.getString("id").orEmpty(), retour) }
        composable("admin/versions/{id}") { e -> EcranVersions(e.arguments?.getString("id").orEmpty(), retour) }
        composable("admin/suggestions") { EcranModeration({ id -> nav.navigate("admin/suggestion/$id") }, retour) }
        composable("admin/suggestion/{id}") { e ->
            EcranSuggestion(e.arguments?.getString("id").orEmpty(), { cid -> nav.navigate("admin/edition/$cid") }, retour)
        }
        composable("admin/import") { EcranImport(retour) }
        composable("admin/journal") { EcranJournal(retour) }
    }
}
