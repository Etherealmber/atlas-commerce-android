package ma.etherealmber.atlascommerce.ui.screens.server

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ProbeReport
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.toneBackground
import ma.etherealmber.atlascommerce.ui.components.toneForeground
import ma.etherealmber.atlascommerce.ui.theme.Atlas

/**
 * First-run screen: pick the Atlas server *before* logging in.
 *
 * A shop can self-host the worker, so the address is a first-class setting. We
 * offer the known deployments as presets, allow any HTTPS address, and verify
 * `/api/health` so a typo is caught immediately.
 */
@Composable
fun ServerSetupScreen(
    onClose: (() -> Unit)? = null,
    viewModel: ServerSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = Atlas.density

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = density.screenPadding, vertical = density.sectionGap),
    ) {
        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Retour")
            }
        } else {
            Spacer(Modifier.height(24.dp))
        }

        AtlasMark(size = 64.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Choisissez votre serveur",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Atlas Commerce se connecte à votre propre serveur Atlas. " +
                "Sélectionnez-le avant de vous connecter — vous pourrez en changer à tout moment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(density.sectionGap))

        Text(
            text = "SERVEURS CONNUS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        ServerUrl.presets().forEach { preset ->
            val selected = state.normalizedInput == ServerUrl.normalize(preset.url)
            Surface(
                onClick = { viewModel.choosePreset(preset.url) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Atlas.palette.cardBorder
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = { viewModel.choosePreset(preset.url) })
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (preset.recommended) {
                                Spacer(Modifier.width(6.dp))
                                AtlasChip(text = "Recommandé", tone = AtlasTone.POSITIVE)
                            }
                        }
                        Text(
                            text = ServerUrl.display(preset.url),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = preset.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "ADRESSE PERSONNALISÉE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        AtlasTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            label = "Adresse du serveur",
            placeholder = "https://mon-serveur.example.com",
            leadingIcon = Icons.Outlined.Dns,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
            onDone = viewModel::test,
            supportingText = "HTTPS recommandé. Un serveur local en HTTP n'est accepté que par les versions debug.",
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasOutlinedButton(
                text = "Tester la connexion",
                onClick = viewModel::test,
                modifier = Modifier.weight(1f),
                enabled = !state.testing && state.input.isNotBlank(),
                icon = Icons.Outlined.Dns,
            )
        }

        if (state.testing) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Vérification de /api/health…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val report = state.report
        if (report != null && !state.testing) {
            Spacer(Modifier.height(12.dp))
            ProbeCard(report)
        }

        if (!state.error.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            AtlasErrorCard(message = state.error.orEmpty(), title = "Adresse refusée")
        }

        Spacer(Modifier.height(density.sectionGap))
        AtlasButton(
            text = if (state.saving) "Enregistrement…" else "Continuer vers la connexion",
            onClick = { viewModel.continueToLogin { onClose?.invoke() } },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canContinue,
            loading = state.saving,
        )

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "La connexion est chiffrée et liée à cet appareil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(density.sectionGap))
        AtlasCard {
            Text(
                text = "Auto-hébergement",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Atlas Commerce est un projet open source : déployez le worker Cloudflare " +
                    "sur votre domaine, puis entrez son adresse ici.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = AppDetails.WEB_REPO,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(density.sectionGap))
        Text(
            text = "${AppDetails.NAME} ${AppDetails.VERSION_NAME} · Licence ${AppDetails.LICENSE_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProbeCard(report: ProbeReport) {
    val tone = if (report.ok) AtlasTone.POSITIVE else AtlasTone.DANGER
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = toneBackground(tone),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (report.ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = toneForeground(tone),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (report.ok) "Serveur Atlas vérifié" else "Vérification échouée",
                    style = MaterialTheme.typography.titleSmall,
                    color = toneForeground(tone),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = report.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (report.ok) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasChip(
                        text = if (report.database) "Base D1 OK" else "Base non vérifiée",
                        tone = if (report.database) AtlasTone.POSITIVE else AtlasTone.WARNING,
                    )
                    AtlasChip(
                        text = if (report.storage) "Stockage R2 OK" else "Stockage non vérifié",
                        tone = if (report.storage) AtlasTone.POSITIVE else AtlasTone.WARNING,
                    )
                    if (!report.environment.isNullOrBlank()) {
                        AtlasChip(text = report.environment.orEmpty(), tone = AtlasTone.INFO)
                    }
                }
            }
        }
    }
}
