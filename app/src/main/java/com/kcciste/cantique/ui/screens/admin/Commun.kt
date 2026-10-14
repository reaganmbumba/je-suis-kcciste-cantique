package com.kcciste.cantique.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kcciste.cantique.data.Suggestion
import com.kcciste.cantique.enligne.Admin
import com.kcciste.cantique.enligne.ServiceFirebase
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.EtatVide
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** État partagé entre les écrans d'administration. */
object EtatAdmin {
    /** Dernière liste reçue de Firestore (écran Modération). */
    val suggestions = MutableStateFlow<List<Suggestion>>(emptyList())

    /** Suggestion en cours d'application dans l'éditeur (passée à « Publiée » après publication). */
    var suggestionAAppliquer: Suggestion? = null
}

fun dateHeure(ms: Long): String =
    if (ms <= 0) "—" else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(ms))

/**
 * Cadre commun : vérifie que Firebase est configuré et qu'un administrateur est connecté.
 * Les règles de sécurité Firestore / Storage refusent de toute façon toute écriture d'un non-administrateur.
 */
@Composable
fun PageAdmin(titre: String, retour: () -> Unit, actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}, contenu: @Composable () -> Unit) {
    val context = LocalContext.current
    val session by Admin.session.collectAsState()
    Scaffold(topBar = { Barre(titre, retour, actions) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                !ServiceFirebase.estConfigure(context) -> EtatVide("🔐", "Administration non activée", "Ajoutez app/google-services.json (voir GUIDE_FIREBASE.md) puis recompilez l'application.")
                session == null -> EtatVide("🔐", "Connexion requise", "Revenez au tableau de bord pour vous connecter.")
                else -> contenu()
            }
        }
    }
}

@Composable
fun Champ(valeur: String, changer: (String) -> Unit, libelle: String, modifier: Modifier = Modifier, lignes: Int = 1) {
    OutlinedTextField(
        value = valeur, onValueChange = changer, label = { Text(libelle) },
        singleLine = lignes == 1, minLines = lignes,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
fun MessageErreur(texte: String?) {
    if (texte != null) Text(texte, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 6.dp))
}
