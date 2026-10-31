package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.localizeReleaseNotes
import ma.etherealmber.atlascommerce.ui.i18n.localizedUpdateSize
import ma.etherealmber.atlascommerce.ui.i18n.localizedUpdateSummary
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.update.MirrorPingResult
import ma.etherealmber.atlascommerce.core.update.UpdateDownloadSource
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme

/**
 * Persistent compact update icon shown in the Top Bar when a download or update
 * banner is hidden. Downloads retain their progress ring; dismissed available or
 * install-ready notices show an update glyph. Tapping it reopens the banner.
 */
@Composable
fun UpdateMiniProgressBall(
    state: UpdateState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloading = state as? UpdateState.Downloading
    val hiddenNotice = when (state) {
        is UpdateState.Available -> state.dismissedBanner && !state.info.mandatory
        is UpdateState.ReadyToInstall -> state.dismissedBanner && !state.info.mandatory
        else -> false
    }
    val visible = (downloading?.let { it.hiddenInForeground && !it.info.mandatory } == true) || hiddenNotice

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        val current = downloading
        val targetFraction = current?.progressFraction ?: 0f
        val animatedProgress by animateFloatAsState(
            targetValue = targetFraction,
            label = "miniUpdateProgress",
        )
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (current == null) {
                    Icon(
                        imageVector = if (state is UpdateState.ReadyToInstall) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.FileDownload
                        },
                        contentDescription = if (state is UpdateState.ReadyToInstall) {
                            atlasText("Afficher l'installation de la mise à jour", "Show update installation", "عرض تثبيت التحديث")
                        } else {
                            atlasText("Afficher le téléchargement de la mise à jour", "Show update download", "عرض تنزيل التحديث")
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                } else {
                    if (current.progressFraction != null) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 2.6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 2.4.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val progressDescription = current.percent?.let { pct ->
                        atlasText(
                            "Téléchargement de la mise à jour ${current.info.displayVersion} en cours · $pct %",
                            "Downloading update ${current.info.displayVersion} · $pct%",
                            "جارٍ تنزيل التحديث ${current.info.displayVersion} · $pct٪",
                        )
                    } ?: atlasText(
                        "Téléchargement de la mise à jour ${current.info.displayVersion} en cours",
                        "Downloading update ${current.info.displayVersion}",
                        "جارٍ تنزيل التحديث ${current.info.displayVersion}",
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = progressDescription,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                        current.percent?.let { pct ->
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 9.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Global in-app banner shown across screens whenever an optional update is
 * available, actively downloading, ready to install, or failed mid-download.
 *
 * Sits cleanly below the Top Bar when [belowTopBar] is `true` so Top Bar controls
 * remain 100% usable, and collapses smoothly into [UpdateMiniProgressBall] next to
 * the Profile icon when hidden (`Masquer`).
 */
@Composable
fun AppUpdateBanner(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onHideDownload: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    belowTopBar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shouldShow = when (state) {
        is UpdateState.Available -> !state.dismissedBanner && !state.info.mandatory
        is UpdateState.Downloading -> !state.info.mandatory && !state.hiddenInForeground
        is UpdateState.ReadyToInstall -> !state.info.mandatory && !state.dismissedBanner
        is UpdateState.Error -> state.info != null && state.info.mandatory.not()
        else -> false
    }
    if (!shouldShow) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = if (belowTopBar) 62.dp else 8.dp,
                bottom = 8.dp,
            ),
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
                                text = atlasText(
                                    "Mise à jour disponible (${state.info.displayVersion})",
                                    "Update available (${state.info.displayVersion})",
                                    "يتوفر تحديث (${state.info.displayVersion})",
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = localizeReleaseNotes(state.info.effectiveNotes, LocalAtlasLanguage.current),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
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
                            Text(atlasText("Plus tard", "Later", "لاحقاً"))
                        }
                        Spacer(Modifier.width(8.dp))
                        AtlasButton(
                            text = atlasText("Télécharger", "Download", "تنزيل"),
                            onClick = onDownload,
                            icon = Icons.Outlined.FileDownload,
                        )
                    }
                }

                is UpdateState.Downloading -> {
                    DownloadProgressBlock(state = state)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onOpenSettings != null) {
                            TextButton(
                                onClick = {
                                    onHideDownload()
                                    onOpenSettings()
                                },
                            ) {
                                Text(atlasText("Voir dans Paramètres", "View in Settings", "عرض في الإعدادات"))
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        TextButton(onClick = onHideDownload) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(atlasText("Masquer", "Hide", "إخفاء"))
                        }
                    }
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
                                text = atlasText(
                                    "Mise à jour ${state.info.displayVersion} prête (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)})",
                                    "Update ${state.info.displayVersion} ready (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)})",
                                    "التحديث ${state.info.displayVersion} جاهز (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)})",
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (state.awaitingInstallPermission) {
                                    atlasText("Autorisez « Installer des applis inconnues » dans les réglages Android pour lancer l'installation.", "Allow installs from this source in Android settings to continue.", "اسمح بالتثبيت من هذا المصدر في إعدادات Android للمتابعة.")
                                } else {
                                    atlasText(
                                        "Téléchargement terminé à 100 %. Lancez l'installation maintenant.",
                                        "Download complete at 100%. Start the installation now.",
                                        "اكتمل التنزيل بنسبة 100٪. ابدأ التثبيت الآن.",
                                    )
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(atlasText("Masquer", "Hide", "إخفاء"))
                        }
                        Spacer(Modifier.width(8.dp))
                        AtlasButton(
                            text = if (state.awaitingInstallPermission) {
                                atlasText("Autoriser et installer", "Allow and install", "السماح والتثبيت")
                            } else {
                                atlasText("Installer maintenant", "Install now", "ثبّت الآن")
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
                                text = atlasText("Échec du téléchargement"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = atlasNotice(state.message, LocalAtlasLanguage.current),
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
                            text = atlasText("Réessayer", "Retry", "إعادة المحاولة"),
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
            text = atlasText("Mise à jour requise", "Update required", "التحديث مطلوب"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        AtlasChip(text = "${AppDetails.VERSION_NAME} → ${info.displayVersion}", tone = AtlasTone.WARNING)
        Spacer(Modifier.height(16.dp))

        AtlasCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = atlasText("Cette version d'Atlas Commerce doit être mise à jour avant de continuer.", "Update Atlas Commerce before continuing.", "حدّث Atlas Commerce قبل المتابعة."),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            AtlasDivider()
            Spacer(Modifier.height(10.dp))
            Text(
                text = atlasText("Nouveautés", "What's new", "الجديد"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = localizeReleaseNotes(info.effectiveNotes, LocalAtlasLanguage.current),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            when (state) {
                is UpdateState.Downloading -> {
                    DownloadProgressBlock(state = state)
                }

                is UpdateState.ReadyToInstall -> {
                    Text(
                        text = if (state.awaitingInstallPermission) {
                            atlasText(
                                "Paquet téléchargé. Autorisez l'installation depuis cette source pour appliquer la mise à jour.",
                                "Package downloaded. Allow installs from this source to apply the update.",
                                "تم تنزيل الحزمة. اسمح بالتثبيت من هذا المصدر لتطبيق التحديث.",
                            )
                        } else {
                            atlasText(
                                "Téléchargement terminé (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)}).",
                                "Download complete (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)}).",
                                "اكتمل التنزيل (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)}).",
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Atlas.palette.positive,
                    )
                    Spacer(Modifier.height(12.dp))
                    AtlasButton(
                        text = if (state.awaitingInstallPermission) {
                            atlasText("Autoriser et installer", "Allow and install", "السماح والتثبيت")
                        } else {
                            atlasText("Installer la mise à jour", "Install update", "تثبيت التحديث")
                        },
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is UpdateState.Error -> {
                    AtlasErrorCard(
                        title = atlasText("Téléchargement interrompu", "Download interrupted", "تم إيقاف التنزيل"),
                        message = atlasNotice(state.message, LocalAtlasLanguage.current),
                    )
                    Spacer(Modifier.height(12.dp))
                    AtlasButton(
                        text = atlasText("Réessayer le téléchargement", "Retry download", "إعادة محاولة التنزيل"),
                        onClick = onDownload,
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
                    AtlasButton(
                        text = atlasText("Télécharger ${info.displayVersion}", "Download ${info.displayVersion}", "تنزيل ${info.displayVersion}"),
                        onClick = onDownload,
                        icon = Icons.Outlined.FileDownload,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (state !is UpdateState.Downloading) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onChangeServer) {
                Text(atlasText("Changer de serveur", "Change server", "تغيير الخادم"))
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
    selectedSource: UpdateDownloadSource = UpdateDownloadSource.AUTO,
    mirrorPing: MirrorPingResult = MirrorPingResult(),
    onSelectSource: (UpdateDownloadSource) -> Unit = {},
    onPingMirrors: () -> Unit = {},
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current
    val notificationsEnabled = runCatching {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }.getOrDefault(true)

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
                text = atlasText("Mise à jour & Miroirs CDN", "Updates & CDN mirrors", "التحديثات ومرايا CDN"),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            AtlasChip(
                text = "v${AppDetails.VERSION_NAME}",
                tone = AtlasTone.NEUTRAL,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (!notificationsEnabled) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = atlasText("Activez les notifications Android pour voir les alertes et le téléchargement en direct."),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                    ) {
                        Text(atlasText("Activer"))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Source selector: Auto (Fastest + Failover) | CDN 1 | GitHub
        Text(
            text = atlasText("Serveur de téléchargement"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpdateDownloadSource.entries.forEach { src ->
                FilterChip(
                    selected = selectedSource == src,
                    onClick = { onSelectSource(src) },
                    label = {
                        Text(when (src) {
                            UpdateDownloadSource.AUTO -> atlasText("Auto", "Auto", "تلقائي")
                            UpdateDownloadSource.CDN1 -> "CDN 1"
                            UpdateDownloadSource.GITHUB -> "GitHub"
                        })
                    },
                )
            }
        }

        // Live Ping tester (CDN 1 vs GitHub in ms)
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 480.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MirrorLatencyChips(mirrorPing)
                    }
                    TextButton(
                        onClick = onPingMirrors,
                        enabled = !mirrorPing.testing,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Outlined.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (mirrorPing.testing) atlasText("Test en cours…") else atlasText("Tester les miroirs"),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MirrorLatencyChips(mirrorPing)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onPingMirrors, enabled = !mirrorPing.testing) {
                        Icon(Icons.Outlined.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(atlasText("Ping"))
                    }
                }
            }
        }
        val recommendation = when {
            mirrorPing.cdn1Online == true && mirrorPing.githubOnline == true && mirrorPing.cdn1Ms != null && mirrorPing.githubMs != null -> {
                if (mirrorPing.cdn1Ms <= mirrorPing.githubMs) {
                    atlasText("CDN 1 recommandé (${mirrorPing.cdn1Ms} ms)", "CDN 1 recommended (${mirrorPing.cdn1Ms} ms)", "CDN 1 موصى به (${mirrorPing.cdn1Ms} مللي ثانية)")
                } else {
                    atlasText("GitHub recommandé (${mirrorPing.githubMs} ms)", "GitHub recommended (${mirrorPing.githubMs} ms)", "GitHub موصى به (${mirrorPing.githubMs} مللي ثانية)")
                }
            }
            mirrorPing.cdn1Online == true && mirrorPing.cdn1Ms != null -> atlasText("CDN 1 actif (${mirrorPing.cdn1Ms} ms)", "CDN 1 active (${mirrorPing.cdn1Ms} ms)", "CDN 1 نشط (${mirrorPing.cdn1Ms} مللي ثانية)")
            mirrorPing.githubOnline == true && mirrorPing.githubMs != null -> atlasText("GitHub actif (${mirrorPing.githubMs} ms)", "GitHub active (${mirrorPing.githubMs} ms)", "GitHub نشط (${mirrorPing.githubMs} مللي ثانية)")
            else -> null
        }
        recommendation?.let { rec ->
            Text(text = rec, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        AtlasDivider(Modifier.padding(vertical = 8.dp))

        when (state) {
            UpdateState.Idle -> {
                Text(
                    text = atlasText(
                        "Version installée : ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE}).",
                        "Installed version: ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE}).",
                        "الإصدار المثبّت: ${AppDetails.VERSION_NAME} (البنية ${AppDetails.VERSION_CODE}).",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                AtlasOutlinedButton(
                    text = atlasText("Vérifier les mises à jour", "Check for updates", "التحقق من التحديثات"),
                    onClick = onCheck,
                    icon = Icons.Outlined.Refresh,
                )
            }

            UpdateState.Checking -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = atlasText("Recherche de mises à jour sur le serveur…", "Checking the server for updates…", "جارٍ التحقق من التحديثات على الخادم…"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is UpdateState.UpToDate -> {
                val updateSuccessColor = if (LocalAtlasDarkTheme.current) Color(0xFF4ADE80) else Color(0xFF15803D)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = updateSuccessColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = atlasText(
                            "Vous utilisez déjà la dernière version (${AppDetails.VERSION_NAME}).",
                            "You are already using the latest version (${AppDetails.VERSION_NAME}).",
                            "أنت تستخدم أحدث إصدار بالفعل (${AppDetails.VERSION_NAME}).",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = updateSuccessColor,
                    )
                }
                Spacer(Modifier.height(10.dp))
                AtlasOutlinedButton(
                    text = atlasText("Vérifier à nouveau"),
                    onClick = onCheck,
                    icon = Icons.Outlined.Refresh,
                )
            }

            is UpdateState.Available -> {
                Text(
                    text = atlasText(
                        "Nouvelle version disponible : ${state.info.displayVersion}",
                        "New version available: ${state.info.displayVersion}",
                        "يتوفر إصدار جديد: ${state.info.displayVersion}",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = atlasText("Nouveautés de cette version"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = localizeReleaseNotes(state.info.effectiveNotes, LocalAtlasLanguage.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = atlasText("Télécharger ${state.info.displayVersion}", "Download ${state.info.displayVersion}", "تنزيل ${state.info.displayVersion}"),
                    onClick = onDownload,
                    icon = Icons.Outlined.FileDownload,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UpdateState.Downloading -> {
                DownloadProgressBlock(state = state)
            }

            is UpdateState.ReadyToInstall -> {
                Text(
                    text = atlasText(
                        "Mise à jour ${state.info.displayVersion} téléchargée (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)}).",
                        "Update ${state.info.displayVersion} downloaded (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)}).",
                        "تم تنزيل التحديث ${state.info.displayVersion} (${localizedUpdateSize(state.apkFile.length(), LocalAtlasLanguage.current)}).",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Atlas.palette.positive,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = localizeReleaseNotes(state.info.effectiveNotes, LocalAtlasLanguage.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.awaitingInstallPermission) {
                        atlasText("Activez l'autorisation « Installer des applis inconnues » pour terminer l'installation.", "Allow installs from this source in Android settings to finish.", "اسمح بالتثبيت من هذا المصدر في إعدادات Android لإكمال التثبيت.")
                    } else {
                        atlasText(
                            "Appuyez ci-dessous pour ouvrir l'installateur Android.",
                            "Tap below to open the Android installer.",
                            "اضغط أدناه لفتح مُثبّت Android.",
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = if (state.awaitingInstallPermission) {
                        atlasText("Autoriser et installer", "Allow and install", "السماح والتثبيت")
                    } else {
                        atlasText("Installer maintenant", "Install now", "ثبّت الآن")
                    },
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UpdateState.Error -> {
                Text(
                    text = atlasNotice(state.message, LocalAtlasLanguage.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = Atlas.palette.danger,
                )
                Spacer(Modifier.height(10.dp))
                if (state.info != null) {
                    AtlasButton(
                        text = atlasText("Réessayer le téléchargement", "Retry download", "إعادة محاولة التنزيل"),
                        onClick = onDownload,
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AtlasOutlinedButton(
                        text = atlasText("Réessayer"),
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
                    text = atlasText("Téléchargement de la mise à jour${state.activeSourceLabel?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()} en cours…", "Downloading update${state.activeSourceLabel?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}…", "جارٍ تنزيل التحديث${state.activeSourceLabel?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}…"),
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
            text = if (state.activeConnections > 1) {
                atlasText("Téléchargement accéléré actif (${state.activeConnections} connexions parallèles). Appuyez sur Masquer pour réduire en mini-bulle.", "Accelerated download active (${state.activeConnections} parallel connections). Tap Hide to minimize.", "التنزيل السريع نشط (${state.activeConnections} اتصالات متوازية). اضغط إخفاء للتصغير.")
            } else {
                atlasText("Appuyez sur Masquer pour réduire la barre en mini-bulle à côté du profil, ou suivez dans Paramètres.", "Tap Hide to minimize next to your profile, or follow progress in Settings.", "اضغط إخفاء للتصغير بجوار الملف الشخصي، أو تابع التقدم في الإعدادات.")
            },
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
            text = localizedUpdateSummary(state, LocalAtlasLanguage.current),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MirrorLatencyChips(mirrorPing: MirrorPingResult) {
    MirrorLatencyChip("CDN 1", mirrorPing.cdn1Online, mirrorPing.cdn1Ms, mirrorPing.testing)
    MirrorLatencyChip("GitHub", mirrorPing.githubOnline, mirrorPing.githubMs, mirrorPing.testing)
}

@Composable
private fun MirrorLatencyChip(label: String, online: Boolean?, latencyMs: Long?, testing: Boolean) {
    val dark = LocalAtlasDarkTheme.current
    val (container, foreground) = when {
        testing -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        online == false ->
            (if (dark) Color(0xFF3E2023) else Color(0xFFFEE2E2)) to
                (if (dark) Color(0xFFFCA5A5) else Color(0xFFB91C1C))
        online == true && latencyMs != null && latencyMs < 250L ->
            (if (dark) Color(0xFF173728) else Color(0xFFE8F5E9)) to
                (if (dark) Color(0xFFA5D6A7) else Color(0xFF166534))
        online == true && latencyMs != null && latencyMs <= 700L ->
            (if (dark) Color(0xFF3B2B18) else Color(0xFFFFF3E0)) to
                (if (dark) Color(0xFFFFCC80) else Color(0xFF9A3412))
        online == true ->
            (if (dark) Color(0xFF3E2023) else Color(0xFFFEE2E2)) to
                (if (dark) Color(0xFFFCA5A5) else Color(0xFFB91C1C))
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val text = when {
        testing -> atlasText("$label : test…", "$label: testing…", "$label: جارٍ الاختبار…")
        online == true && latencyMs != null -> atlasText(
            "$label : $latencyMs ms",
            "$label: $latencyMs ms",
            "$label: $latencyMs مللي ثانية",
        )
        online == false -> atlasText("$label : indisponible", "$label: unavailable", "$label: غير متاح")
        else -> atlasText("$label : prêt", "$label: ready", "$label: جاهز")
    }
    Surface(shape = RoundedCornerShape(10.dp), color = container, contentColor = foreground) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
