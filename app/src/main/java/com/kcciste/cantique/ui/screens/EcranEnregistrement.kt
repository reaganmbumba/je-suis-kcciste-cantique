package com.kcciste.cantique.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kcciste.cantique.audio.EnregistrementViewModel
import com.kcciste.cantique.audio.Etape
import com.kcciste.cantique.data.Cantique
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.data.Formats
import com.kcciste.cantique.ui.components.Barre
import com.kcciste.cantique.ui.components.CarteContactCreateur
import com.kcciste.cantique.ui.components.DialoguesEnvoi
import com.kcciste.cantique.ui.components.GrosBouton
import com.kcciste.cantique.ui.components.LecteurAudio
import com.kcciste.cantique.ui.components.LigneInfo
import com.kcciste.cantique.ui.components.Waveform
import com.kcciste.cantique.ui.components.rememberEnvoiCreateur
import com.kcciste.cantique.ui.components.rememberPermissionMicro
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.BleuClair
import com.kcciste.cantique.ui.theme.Gris
import com.kcciste.cantique.ui.theme.Or
import com.kcciste.cantique.ui.theme.Rouge
import com.kcciste.cantique.ui.theme.RougeClair
import com.kcciste.cantique.ui.theme.VertWhatsApp

@Composable
fun EcranEnregistrement(cantique: Cantique, retour: () -> Unit, mesEnregistrements: () -> Unit) {
    val vm: EnregistrementViewModel = viewModel()
    vm.init(cantique)
    val etat by vm.etat.collectAsState()
    val envoi = rememberEnvoiCreateur()
    val micro = rememberPermissionMicro(onRefus = {
        vm.signalerErreur("Sans l'autorisation du microphone, l'enregistrement est impossible.")
    })
    var confirmerSortie by remember { mutableStateOf(false) }
    var confirmerSuppression by remember { mutableStateOf(false) }

    val quitter: () -> Unit = { if (vm.aUnePriseNonSauvegardee) { confirmerSortie = true } else { retour() } }
    BackHandler(onBack = quitter)

    // Android coupe le micro des applications en arrière-plan : on met en pause proprement.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && (vm.etat.value.etape as? Etape.EnCours)?.enPause == false) vm.pause()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(topBar = { Barre("Enregistreur audio", quitter) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("KCCISTE CANTIQUE", color = Bleu, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("🎵 ${cantique.numeroFormate}. ${cantique.titre}", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Text(cantique.langue.libelle, color = Gris)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            when (val etape = etat.etape) {
                Etape.Pret, is Etape.EnCours -> {
                    val enCours = etape is Etape.EnCours
                    val enPause = (etape as? Etape.EnCours)?.enPause == true
                    Text("🎙️ ENREGISTREMENT", fontWeight = FontWeight.Bold, color = Gris)
                    Spacer(Modifier.height(8.dp))
                    Waveform(etat.niveaux, EnregistrementViewModel.NB_BARRES, if (enPause) Gris else Bleu)
                    Text(Formats.duree(etat.dureeMs), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Bleu)
                    Text("Durée maximale : ${Formats.duree(vm.dureeMaxMs)}", color = Gris, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    when {
                        enPause -> Text("⏸ ENREGISTREMENT EN PAUSE", color = Or, fontWeight = FontWeight.Bold)
                        enCours -> Text("🔴 ENREGISTREMENT EN COURS", color = Rouge, fontWeight = FontWeight.Bold)
                    }
                    if (enCours) Text("Durée : ${Formats.duree(etat.dureeMs)}", color = Gris)
                    Spacer(Modifier.height(16.dp))
                    if (!enCours) {
                        GrosBouton("🔴 ENREGISTRER", Rouge) { micro.avecMicro { vm.demarrer() } }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (enPause) {
                                GrosBouton("▶ REPRENDRE", Bleu, Modifier.weight(1f)) { vm.reprendre() }
                            } else {
                                GrosBouton("⏸ PAUSE", Or, Modifier.weight(1f), couleurTexte = Color.Black) { vm.pause() }
                            }
                            GrosBouton("⏹ ARRÊTER", Rouge, Modifier.weight(1f)) { vm.arreter() }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = BleuClair)) {
                        Text(
                            "Chantez ce cantique. Vous pourrez l'écouter et l'envoyer au créateur après l'enregistrement.",
                            Modifier.padding(12.dp), color = Bleu, fontSize = 13.sp,
                        )
                    }
                }

                is Etape.Termine -> {
                    Text("✓ ENREGISTREMENT TERMINÉ", color = VertWhatsApp, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth()) {
                        LigneInfo("Cantique :", cantique.titre)
                        LigneInfo("Durée :", Formats.duree(etape.dureeMs))
                        LigneInfo("Taille :", Formats.taille(etape.tailleOctets))
                        LigneInfo("Fichier :", etape.nomFichier)
                        LigneInfo("Statut :", if (etape.sauvegarde != null) "🟢 Sauvegardé" else "Non sauvegardé")
                    }
                    LecteurAudio(etape.fichier, etape.dureeMs)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = vm::refaire, modifier = Modifier.fillMaxWidth()) { Text("🔄 REFAIRE") }
                    Spacer(Modifier.height(8.dp))
                    GrosBouton(
                        if (etape.sauvegarde != null) "💾 SAUVEGARDÉ ✓" else "💾 SAUVEGARDER",
                        Or, couleurTexte = Color.Black, actif = etape.sauvegarde == null,
                    ) { vm.sauvegarder() }
                    Spacer(Modifier.height(16.dp))

                    // Bloc d'envoi au créateur, très visible.
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFFE9F8EF), RoundedCornerShape(14.dp)).padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GrosBouton("💬 ENVOYER À ${Createur.NOM_BOUTON}", VertWhatsApp) {
                            // On sauvegarde d'abord pour que le fichier ait son nom définitif.
                            val e = vm.sauvegarder()
                            val f = (vm.etat.value.etape as? Etape.Termine)?.fichier
                            if (e != null && f != null) envoi.demander(e, f)
                        }
                        Text("Envoyer cet enregistrement au créateur via WhatsApp", color = Gris, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(Createur.NOM, fontWeight = FontWeight.Bold)
                        Text("📱 ${Createur.TELEPHONE_AFFICHE}")
                    }
                    Spacer(Modifier.height(12.dp))
                    GrosBouton("🗑 SUPPRIMER", RougeClair, couleurTexte = Rouge) { confirmerSuppression = true }
                    if (etape.sauvegarde != null) {
                        TextButton(onClick = mesEnregistrements) { Text("🎙️ Voir mes enregistrements") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            CarteContactCreateur()
        }
    }

    DialoguesEnvoi(envoi)

    etat.erreur?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::effacerMessages,
            title = { Text("Erreur", color = Rouge) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = vm::effacerMessages) { Text("OK") } },
        )
    }
    etat.info?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::effacerMessages,
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = vm::effacerMessages) { Text("OK") } },
        )
    }
    if (confirmerSuppression) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = { Text("Supprimer l'enregistrement ?") },
            text = { Text("Le fichier audio sera définitivement effacé de cet appareil.") },
            dismissButton = { TextButton(onClick = { confirmerSuppression = false }) { Text("ANNULER") } },
            confirmButton = {
                TextButton(onClick = { confirmerSuppression = false; vm.supprimer() }) { Text("SUPPRIMER", color = Rouge) }
            },
        )
    }
    if (confirmerSortie) {
        AlertDialog(
            onDismissRequest = { confirmerSortie = false },
            title = { Text("Quitter sans sauvegarder ?") },
            text = { Text("L'enregistrement en cours ou non sauvegardé sera perdu.") },
            dismissButton = { TextButton(onClick = { confirmerSortie = false }) { Text("RESTER") } },
            confirmButton = {
                TextButton(onClick = { confirmerSortie = false; vm.refaire(); retour() }) { Text("QUITTER", color = Rouge) }
            },
        )
    }
}
