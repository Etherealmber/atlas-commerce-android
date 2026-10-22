package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.update.UpdateFormatter
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.theme.Atlas

/**
 * Global in-app banner shown across screens whenever an optional update is
 * available, actively downloading, ready to install, or failed mid-download.
 *
 * Cannot be dismissed while [UpdateState.Downloading] is active.
 */
@Composable
fun AppUpdateBanner(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shouldShow = when (state) {
        is UpdateState.Available -> !state.dismissedBanner && !state.info.mandatory
        is UpdateState.Downloading -> !state.info.mandatory
        is UpdateState.ReadyToInstall -> !state.info.mandatory
        is UpdateState.Error -> state.info != null && state.info.mandatory.not()
        else -> false
    }
    if (!shouldShow) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Atlas.palette.cardBorder),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (state) {
                is UpdateState.Available -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdateAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Mise à jour disponible (${state.info.displayVersion})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = state.info.notes?.lines()?.firstOrNull()?.takeIf { it.isNotBlank() }
                                    ?: "Une nouvelle version d'Atlas Commerce est prête.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Plus tard")
                        }
                        Spacer(Modifier.width(8.dp))
                        AtlasButton(
                            text = "Télécharger",
                            onClick = onDownload,
                            icon = Icons.Outlined.SystemUpdateAlt,
                        )
                    }
                }

                is UpdateState.Downloading -> {
                    DownloadProgressBlock(state = state)
                }

                is UpdateState.ReadyToInstall -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Atlas.palette.positive,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Mise à jour ${state.info.displayVersion} prête (${UpdateFormatter.dataSize(state.apkFile.length())})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (state.awaitingInstallPermission) {
                                    "Autorisez « Installer des applis inconnues » dans les réglages Android pour lancer l'installation."
                                } else {
                                    "Téléchargement terminé à 100 %. Lancez l'installation maintenant."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AtlasButton(
                            text = if (state.awaitingInstallPermission) {
                                "Autoriser et installer"
                            } else {
                                "Installer maintenant"
                            },
                            onClick = onInstall,
                        )
                    }
                }

                is UpdateState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = Atlas.palette.danger,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Échec du téléchargement",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AtlasButton(
                            text = "Réessayer",
                            onClick = onDownload,
                            icon = Icons.Outlined.Refresh,
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

/**
 * Full-screen mandatory update gate — works both before login and after login.
 */
@Composable
fun MandatoryUpdateScreen(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onChangeServer: () -> Unit,
) {
    val density = Atlas.density
    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.ReadyToInstall -> state.info
        is UpdateState.Error -> state.info
        else -> null
    } ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = density.screenPadding, vertical = density.sectionGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AtlasMark(size = 68.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Mise à jour requise",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        AtlasChip(text = "${AppDetails.VERSION_NAME} → ${info.displayVersion}", tone = AtlasTone.WARNING)
        Spacer(Modifier.height(16.dp))

        AtlasCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Cette version d'Atlas Commerce doit être mise à jour avant de continuer.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!info.notes.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                AtlasDivider()
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Nouveautés",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = info.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))

            when (state) {
                is UpdateState.Downloading -> {
                    DownloadProgressBlock(state = state)
                }

                is UpdateState.ReadyToInstall -> {
                    Text(
                        text = if (state.awaitingInstallPermission) {
                            "Paquet téléchargé. Autorisez l'installation depuis cette source pour appliquer la mise à jour."
                        } else {
                            "Téléchargement terminé (${UpdateFormatter.dataSize(state.apkFile.length())})."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Atlas.palette.positive,
                    )
                    Spacer(Modifier.height(12.dp))
                    AtlasButton(
                        text = if (state.awaitingInstallPermission) {
                            "Autoriser et installer"
                        } else {
                            "Installer la mise à jour"
                        },
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is UpdateState.Error -> {
                    AtlasErrorCard(
                        title = "Téléchargement interrompu",
                        message = state.message,
                    )
                    Spacer(Modifier.height(12.dp))
                    AtlasButton(
                        text = "Réessayer le téléchargement",
                        onClick = onDownload,
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
                    AtlasButton(
                        text = "Télécharger ${info.displayVersion}",
                        onClick = onDownload,
                        icon = Icons.Outlined.SystemUpdateAlt,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (state !is UpdateState.Downloading) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onChangeServer) {
                Text("Changer de serveur")
            }
        }
    }
}

/**
 * Dedicated update section in Settings ("Réglages") with manual update check,
 * release notes, live download progress bar + percentage + MB, and one-tap install.
 */
@Composable
fun SettingsUpdateCard(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.SystemUpdateAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Mise à jour de l'application",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            AtlasChip(
                text = "v${AppDetails.VERSION_NAME}",
                tone = AtlasTone.NEUTRAL,
            )
        }
        Spacer(Modifier.height(8.dp))

        when (state) {
            UpdateState.Idle -> {
                Text(
                    text = "Version installée : ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                AtlasOutlinedButton(
                    text = "Vérifier les mises à jour",
                    onClick = onCheck,
                    icon = Icons.Outlined.Refresh,
                )
            }

            UpdateState.Checking -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Recherche de mises à jour sur le serveur…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is UpdateState.UpToDate -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Atlas.palette.positive,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Vous utilisez déjà la dernière version (${AppDetails.VERSION_NAME}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Atlas.palette.positive,
                    )
                }
                Spacer(Modifier.height(10.dp))
                AtlasOutlinedButton(
                    text = "Vérifier à nouveau",
                    onClick = onCheck,
                    icon = Icons.Outlined.Refresh,
                )
            }

            is UpdateState.Available -> {
                Text(
                    text = "Nouvelle version disponible : ${state.info.displayVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!state.info.notes.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.info.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = "Télécharger ${state.info.displayVersion}",
                    onClick = onDownload,
                    icon = Icons.Outlined.SystemUpdateAlt,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UpdateState.Downloading -> {
                DownloadProgressBlock(state = state)
            }

            is UpdateState.ReadyToInstall -> {
                Text(
                    text = "Mise à jour ${state.info.displayVersion} téléchargée (${UpdateFormatter.dataSize(state.apkFile.length())}).",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Atlas.palette.positive,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.awaitingInstallPermission) {
                        "Activez l'autorisation « Installer des applis inconnues » pour terminer l'installation."
                    } else {
                        "Appuyez ci-dessous pour ouvrir l'installateur Android."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = if (state.awaitingInstallPermission) {
                        "Autoriser et installer"
                    } else {
                        "Installer maintenant"
                    },
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UpdateState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Atlas.palette.danger,
                )
                Spacer(Modifier.height(10.dp))
                if (state.info != null) {
                    AtlasButton(
                        text = "Réessayer le téléchargement",
                        onClick = onDownload,
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AtlasOutlinedButton(
                        text = "Réessayer",
                        onClick = onCheck,
                        icon = Icons.Outlined.Refresh,
                    )
                }
            }
        }
    }
}

/**
 * Shared live progress view showing explicit French downloading copy, progress
 * bar, percentage and transferred megabytes.
 */
@Composable
fun DownloadProgressBlock(state: UpdateState.Downloading) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.statusTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            state.percent?.let { pct ->
                Spacer(Modifier.width(8.dp))
                AtlasChip(text = "$pct %", tone = AtlasTone.INFO)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        val fraction = state.progressFraction
        if (fraction != null) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.progressSummary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
