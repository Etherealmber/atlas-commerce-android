package ma.etherealmber.atlascommerce.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasInfoRow
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.util.openExternal

@Composable
fun SettingsScreen(
    user: SessionUser,
    onChangeServer: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit,
    onToast: (String) -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = Atlas.density
    val palette = Atlas.palette
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val densityMode by appViewModel.densityMode.collectAsStateWithLifecycle()
    val serverSelection by appViewModel.server.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = density.screenPadding,
            end = density.screenPadding,
            top = density.cardMargin,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(density.itemGap),
    ) {
        item {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(palette.markFace, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.markInk,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        AtlasChip(text = Roles.label(user.role), tone = AtlasTone.INFO)
                    }
                }
                if (user.email.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AtlasInfoRow(label = "E-mail", value = user.email)
                }
                Spacer(Modifier.height(6.dp))
                AtlasOutlinedButton(
                    text = "Se déconnecter",
                    onClick = { confirmSignOut = true },
                    icon = Icons.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Serveur", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ServerUrl.display(serverSelection?.baseUrl ?: AppDetails.DEFAULT_API_BASE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Vos données restent sur votre serveur Atlas. Changer de serveur vous déconnecte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                AtlasOutlinedButton(
                    text = "Changer de serveur",
                    onClick = onChangeServer,
                    icon = Icons.Outlined.SwapHoriz,
                )
            }
        }

        item {
            AtlasCard {
                Text("Apparence", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Thème",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { appViewModel.setThemeMode(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Densité",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DensityMode.entries.forEach { mode ->
                        FilterChip(
                            selected = densityMode == mode,
                            onClick = { appViewModel.setDensityMode(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }

        item {
            AtlasCard(padding = 4.dp) {
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Sécurité & confidentialité",
                    subtitle = "Sessions, appareil, données",
                    onClick = onOpenSecurity,
                )
                AtlasDivider(Modifier.padding(horizontal = 12.dp))
                SettingsRow(
                    icon = Icons.Outlined.Description,
                    title = "Licence MIT",
                    subtitle = "Sources et composants tiers",
                    onClick = onOpenLicense,
                )
                AtlasDivider(Modifier.padding(horizontal = 12.dp))
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "À propos",
                    subtitle = "${AppDetails.NAME} ${AppDetails.VERSION_NAME}",
                    onClick = onOpenAbout,
                )
            }
        }

        item {
            AtlasCard {
                Text("Liens", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                SettingsRow(
                    icon = Icons.Outlined.Language,
                    title = "Projet web Atlas Commerce",
                    subtitle = "Etherealmber/atlas-commerce",
                    onClick = {
                        if (!openExternal(context, AppDetails.WEB_REPO)) {
                            onToast("Aucun navigateur disponible.")
                        }
                    },
                )
                SettingsRow(
                    icon = Icons.Outlined.OpenInNew,
                    title = "Code source Android",
                    subtitle = "Etherealmber/atlas-commerce-android",
                    onClick = {
                        if (!openExternal(context, AppDetails.PROJECT_REPO)) {
                            onToast("Aucun navigateur disponible.")
                        }
                    },
                )
                SettingsRow(
                    icon = Icons.Outlined.OpenInNew,
                    title = "Signaler un problème",
                    subtitle = "Issues GitHub",
                    onClick = {
                        if (!openExternal(context, AppDetails.ISSUES_URL)) {
                            onToast("Aucun navigateur disponible.")
                        }
                    },
                )
            }
        }

        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "${AppDetails.NAME} ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER} · Licence ${AppDetails.LICENSE_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Se déconnecter ?") },
            text = {
                Text(
                    "Le jeton de session sera effacé de cet appareil et le panier vidé. " +
                        "Aucune donnée de vente n'est supprimée sur le serveur.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        onSignOut()
                    },
                ) { Text("Se déconnecter") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
