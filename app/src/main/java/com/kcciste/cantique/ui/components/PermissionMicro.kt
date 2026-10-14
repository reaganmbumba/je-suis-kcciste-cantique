package com.kcciste.cantique.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Stable
class PermissionMicro internal constructor(private val context: Context) {
    internal var dialogue by mutableStateOf<Int?>(null) // 1 = explication, 2 = refus définitif
    internal var siAccorde: (() -> Unit)? = null
    internal var lancer: (() -> Unit)? = null

    val accordee: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Exécute [action] si le micro est autorisé, sinon affiche d'abord l'explication. */
    fun avecMicro(action: () -> Unit) {
        if (accordee) {
            action()
            return
        }
        siAccorde = action
        val activite = context as? Activity
        val dejaDemande = prefs().getBoolean(CLE, false)
        val bloque = dejaDemande && activite != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activite, Manifest.permission.RECORD_AUDIO)
        dialogue = if (bloque) 2 else 1
    }

    internal fun prefs() = context.getSharedPreferences("permissions", Context.MODE_PRIVATE)

    companion object {
        internal const val CLE = "micro_deja_demande"
    }
}

@Composable
fun rememberPermissionMicro(onRefus: () -> Unit): PermissionMicro {
    val context = LocalContext.current
    val etat = remember { PermissionMicro(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        etat.prefs().edit().putBoolean(PermissionMicro.CLE, true).apply()
        if (ok) etat.siAccorde?.invoke() else onRefus()
        etat.siAccorde = null
    }
    etat.lancer = { launcher.launch(Manifest.permission.RECORD_AUDIO) }

    when (etat.dialogue) {
        1 -> AlertDialog(
            onDismissRequest = { etat.dialogue = null },
            title = { Text("🎙️ Autorisation du microphone") },
            text = { Text("Cette autorisation est nécessaire pour enregistrer votre interprétation des cantiques.") },
            dismissButton = {
                TextButton(onClick = { etat.dialogue = null; etat.siAccorde = null; onRefus() }) { Text("REFUSER") }
            },
            confirmButton = {
                TextButton(onClick = { etat.dialogue = null; etat.lancer?.invoke() }) {
                    Text("AUTORISER", fontWeight = FontWeight.Bold)
                }
            },
        )
        2 -> AlertDialog(
            onDismissRequest = { etat.dialogue = null },
            title = { Text("🎙️ Microphone bloqué") },
            text = {
                Text(
                    "L'autorisation du microphone a été refusée. Pour enregistrer, autorisez le micro " +
                        "dans les paramètres Android de Je suis KCCiste Cantique.",
                )
            },
            dismissButton = { TextButton(onClick = { etat.dialogue = null }) { Text("ANNULER") } },
            confirmButton = {
                TextButton(onClick = {
                    etat.dialogue = null
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                }) { Text("OUVRIR LES PARAMÈTRES", fontWeight = FontWeight.Bold) }
            },
        )
    }
    return etat
}
