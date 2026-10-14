package com.kcciste.cantique.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.data.Discussion
import com.kcciste.cantique.data.Message
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.BleuClair
import com.kcciste.cantique.ui.theme.Gris
import com.kcciste.cantique.ui.theme.Or
import com.kcciste.cantique.ui.theme.Rouge
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EcranDiscussion(retour: () -> Unit) {
    val context = LocalContext.current
    val configuree = remember { Discussion.estConfiguree(context) }
    Scaffold(topBar = { Barre("💬 Groupe KCCiste", retour) }) { pad ->
        Box(Modifier.padding(pad).consumeWindowInsets(pad).fillMaxSize()) {
            if (configuree) Groupe() else Info("La discussion de groupe n'est pas encore activée dans cette version de l'application.")
        }
    }
}

@Composable
private fun Info(texte: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(texte, color = Gris, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Groupe() {
    val context = LocalContext.current
    var nom by rememberSaveable { mutableStateOf(Discussion.nom(context)) }
    var uid by remember { mutableStateOf(Discussion.uid()) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<Message>?>(null) }
    var texte by rememberSaveable { mutableStateOf("") }
    var aSupprimer by remember { mutableStateOf<Message?>(null) }
    var changerNom by remember { mutableStateOf(false) }
    val liste = rememberLazyListState()

    LaunchedEffect(Unit) {
        Discussion.connecter(ok = { uid = it }, erreur = { erreur = "Connexion impossible. Vérifiez internet puis rouvrez cette page." })
    }
    if (uid != null) {
        DisposableEffect(uid) {
            val ecoute = Discussion.ecouter(
                maj = { messages = it; erreur = null },
                erreur = { erreur = "Impossible de charger les messages." },
            )
            onDispose { ecoute.remove() }
        }
    }
    LaunchedEffect(messages?.size) {
        val n = messages?.size ?: 0
        if (n > 0) liste.animateScrollToItem(n - 1)
    }

    if (nom.isBlank() || changerNom) {
        DialogueNom(nom, annulable = nom.isNotBlank(), fermer = { changerNom = false }) {
            Discussion.enregistrerNom(context, it)
            nom = Discussion.nom(context)
            changerNom = false
        }
    }
    aSupprimer?.let { m ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Supprimer ce message ?") },
            text = { Text(m.texte) },
            confirmButton = { TextButton({ Discussion.supprimer(m.id); aSupprimer = null }) { Text("Supprimer", color = Rouge) } },
            dismissButton = { TextButton({ aSupprimer = null }) { Text("Annuler") } },
        )
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().background(BleuClair).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Vous écrivez en tant que $nom", color = Bleu, fontSize = 13.sp, modifier = Modifier.weight(1f))
            TextButton({ changerNom = true }) { Text("Changer", fontSize = 13.sp) }
        }
        erreur?.let { Text(it, color = Rouge, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val m = messages
            when {
                m == null && erreur == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                m.isNullOrEmpty() -> Info("Aucun message pour l'instant. Écrivez le premier !")
                else -> LazyColumn(
                    state = liste,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(m, key = { it.id }) { msg ->
                        val moi = msg.uid == uid
                        Bulle(
                            msg, moi,
                            Modifier.combinedClickable(onClick = {}, onLongClick = { if (moi) aSupprimer = msg }),
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = texte,
                onValueChange = { texte = it.take(Discussion.MAX_TEXTE) },
                placeholder = { Text("Écrire un message…") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.padding(4.dp))
            Button(
                onClick = {
                    Discussion.envoyer(texte, nom) { erreur = "Message non envoyé. Réessayez." }
                    texte = ""
                },
                enabled = texte.isNotBlank() && uid != null,
                shape = RoundedCornerShape(24.dp),
            ) { Text("Envoyer") }
        }
    }
}

@Composable
private fun Bulle(m: Message, moi: Boolean, modifier: Modifier) {
    val heure = m.date?.toDate()?.let { SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(it) } ?: ""
    Box(Modifier.fillMaxWidth(), contentAlignment = if (moi) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            modifier
                .widthIn(max = 300.dp)
                .background(if (moi) Bleu else BleuClair, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!moi) Text(m.nom, color = Bleu, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(m.texte, color = if (moi) Color.White else Color(0xFF1B1B1B), fontSize = 16.sp)
            Text(
                if (m.enAttente) "$heure · ⏳ en attente" else heure,
                color = if (moi) Or else Gris, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun DialogueNom(actuel: String, annulable: Boolean, fermer: () -> Unit, valider: (String) -> Unit) {
    var saisie by remember { mutableStateOf(actuel) }
    AlertDialog(
        onDismissRequest = { if (annulable) fermer() },
        title = { Text("Votre nom dans le groupe") },
        text = {
            Column {
                Text("Les autres membres verront ce nom à côté de vos messages.", color = Gris, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(saisie, { saisie = it.take(Discussion.MAX_NOM) }, singleLine = true, placeholder = { Text("Ex. : Frère Jean") })
            }
        },
        confirmButton = { TextButton({ valider(saisie) }, enabled = saisie.isNotBlank()) { Text("Valider") } },
        dismissButton = { if (annulable) TextButton(fermer) { Text("Annuler") } },
    )
}
