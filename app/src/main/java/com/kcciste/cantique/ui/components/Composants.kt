package com.kcciste.cantique.ui.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kcciste.cantique.data.Createur
import com.kcciste.cantique.share.Partage
import com.kcciste.cantique.ui.theme.Bleu
import com.kcciste.cantique.ui.theme.BleuClair
import com.kcciste.cantique.ui.theme.Gris
import com.kcciste.cantique.ui.theme.Or
import com.kcciste.cantique.ui.theme.VertWhatsApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Barre(titre: String, retour: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Column {
        TopAppBar(
            title = { Text(titre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                if (retour != null) {
                    IconButton(onClick = retour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        // Fin filet doré sous la barre : l'accent de l'identité KCCISTE CANTIQUE.
        HorizontalDivider(thickness = 1.dp, color = Or.copy(alpha = 0.6f))
    }
}

/** Bouton large et très visible. */
@Composable
fun GrosBouton(
    texte: String,
    couleur: Color,
    modifier: Modifier = Modifier,
    couleurTexte: Color = Color.White,
    actif: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = actif,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = couleur, contentColor = couleurTexte),
    ) {
        Text(texte, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

/** Carte « 💬 CONTACTER LE CRÉATEUR » — ouvre https://wa.me/243840238592 */
@Composable
fun CarteContactCreateur(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BleuClair),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Créateur de l'application", color = Gris, fontSize = 13.sp)
            Text(Createur.NOM, color = Bleu, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("WhatsApp uniquement", color = Gris, fontSize = 13.sp)
            Text("📱 ${Createur.TELEPHONE_AFFICHE}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            GrosBouton("💬 CONTACTER LE CRÉATEUR", VertWhatsApp) {
                if (!Partage.contacterCreateur(context)) {
                    Toast.makeText(context, "Aucune application ne peut ouvrir ${Createur.LIEN_WHATSAPP}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

/** Waveform : une barre par niveau mesuré, la plus récente à droite. */
@Composable
fun Waveform(niveaux: List<Float>, nbBarres: Int, couleur: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        val pas = size.width / nbBarres
        val largeur = pas * 0.6f
        val milieu = size.height / 2
        val decalage = nbBarres - niveaux.size
        for (i in 0 until nbBarres) {
            val v = niveaux.getOrNull(i - decalage) ?: 0f
            // Échelle racine : la voix reste lisible même à faible volume.
            val h = (kotlin.math.sqrt(v) * size.height * 0.95f).coerceAtLeast(4f)
            drawRoundRect(
                color = if (i - decalage >= 0) couleur else couleur.copy(alpha = 0.2f),
                topLeft = Offset(i * pas + (pas - largeur) / 2, milieu - h / 2),
                size = Size(largeur, h),
                cornerRadius = CornerRadius(largeur / 2),
            )
        }
    }
}

@Composable
fun LigneInfo(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(libelle, color = Gris, modifier = Modifier.width(110.dp))
        Text(valeur, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Pastille(texte: String, fond: Color, couleurTexte: Color = Color.White) {
    Text(
        texte,
        color = couleurTexte,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .background(fond, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
