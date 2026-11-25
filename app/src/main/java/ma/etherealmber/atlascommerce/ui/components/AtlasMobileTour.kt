package ma.etherealmber.atlascommerce.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme

private data class TourStep(val id: String, val title: String, val body: String, val targetOnLowerHalf: Boolean)

private val mobileTourSteps = listOf(
    TourStep("home", "Votre accueil", "Trésorerie ou stock, alertes et derniers documents : l’essentiel de la journée, mis à jour depuis le serveur. Touchez ↻ pour actualiser.", true),
    TourStep("dock", "La barre du bas", "Accueil, Produits, Scan, puis Balance et Employés selon votre rôle. Elle reste visible sur les pages principales.", true),
    TourStep("scan", "Scanner un code-barres", "Le bouton central ouvre la caméra : la fiche produit, le stock et l’historique s’affichent. Un ajustement part en demande vers le bureau.", true),
    TourStep("quick", "Le menu rapide", "Notifications et actions en attente, nouvelle vente et clients, en un toucher depuis n’importe quelle page principale.", true),
    TourStep("agent", "Atlas Agent", "« Rapport du jour », « Quels articles sont en rupture ? »… L’agent prépare la réponse ou l’action, vous la confirmez.", false),
    TourStep("profile", "Profil et réglages", "Photo, thème, taille du texte et page d’ouverture. Vous pourrez revoir cette visite depuis Réglages → Visite guidée.", false),
)

@Composable
fun AtlasMobileTourOverlay(
    currentStep: Int,
    onStep: (Int) -> Unit,
    onFinish: () -> Unit,
    loading: Boolean = false,
    safetyCapMs: Long = 4_000,
) {
    val dark = LocalAtlasDarkTheme.current
    val card = if (dark) Color(0xFF1C1C1F) else Color.White
    val foreground = if (dark) Color(0xFFF4F4F5) else Color(0xFF18181B)
    val muted = if (dark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val border = if (dark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
    val primary = if (dark) Color(0xFFF4F4F5) else Color(0xFF18181B)
    val onPrimary = if (dark) Color(0xFF111113) else Color.White
    val step = mobileTourSteps[currentStep.coerceIn(0, mobileTourSteps.lastIndex)]
    var stableTicks by remember(step.id) { mutableIntStateOf(0) }
    var settled by remember(step.id) { mutableIntStateOf(0) }
    LaunchedEffect(step.id, loading) {
        stableTicks = 0
        settled = 0
        var elapsedMs = 0L
        while (stableTicks < 3 && elapsedMs < safetyCapMs) {
            delay(120L)
            elapsedMs += 120L
            if (!loading) stableTicks++ else stableTicks = 0
        }
        settled = 1
    }
    BackHandler(enabled = true) {
        if (currentStep > 0) onStep(currentStep - 1)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .pointerInput(Unit) { detectTapGestures(onTap = {}) }
            .semantics { dialog() },
    ) {
        if (settled == 1) {
            Surface(
                modifier = Modifier
                    .align(if (step.targetOnLowerHalf) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(22.dp),
                color = card,
                border = BorderStroke(1.dp, border),
                shadowElevation = 20.dp,
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AtlasIcons.Outlined.Compass, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
                        Text("Visite guidée · ${currentStep + 1} / ${mobileTourSteps.size}", modifier = Modifier.padding(start = 7.dp).weight(1f), color = muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(
                            modifier = Modifier.clip(CircleShape).clickable(onClick = onFinish),
                            shape = CircleShape,
                            color = Color.Transparent,
                        ) {
                            Icon(AtlasIcons.Outlined.Close, contentDescription = "Quitter la visite", tint = muted, modifier = Modifier.padding(6.dp).size(18.dp))
                        }
                    }
                    Text(step.title, color = foreground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(step.body, color = muted, style = MaterialTheme.typography.bodyMedium)
                    Text("Démonstration uniquement : rien n’est ajouté à votre base de données. Vous pouvez passer la visite à tout moment.", color = muted, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            mobileTourSteps.indices.forEach { index ->
                                Box(Modifier.size(width = if (index == currentStep) 17.dp else 6.dp, height = 6.dp).clip(CircleShape).background(if (index <= currentStep) foreground else border))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(modifier = Modifier.clip(CircleShape).clickable(onClick = onFinish), shape = CircleShape, color = Color.Transparent) {
                            Text("Passer", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = muted, fontWeight = FontWeight.SemiBold)
                        }
                        if (currentStep > 0) {
                            Surface(modifier = Modifier.padding(start = 4.dp).size(40.dp).clip(CircleShape).clickable { onStep(currentStep - 1) }, shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, border)) {
                                Box(contentAlignment = Alignment.Center) { Icon(AtlasIcons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Étape précédente", tint = foreground, modifier = Modifier.size(18.dp)) }
                            }
                        }
                        Surface(
                            modifier = Modifier.padding(start = 7.dp).height(42.dp).clip(RoundedCornerShape(14.dp)).clickable {
                                if (currentStep == mobileTourSteps.lastIndex) onFinish() else onStep(currentStep + 1)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = primary,
                        ) {
                            Row(Modifier.padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (currentStep == mobileTourSteps.lastIndex) "Terminer" else "Suivant", color = onPrimary, fontWeight = FontWeight.Bold)
                                Icon(if (currentStep == mobileTourSteps.lastIndex) AtlasIcons.Outlined.Check else AtlasIcons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = onPrimary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
