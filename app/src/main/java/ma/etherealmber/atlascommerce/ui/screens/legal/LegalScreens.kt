package ma.etherealmber.atlascommerce.ui.screens.legal

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.diagnostics.CompressedReportAttachment
import ma.etherealmber.atlascommerce.core.diagnostics.DiagnosticReporter
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasBullet
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasPulseHalo
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.util.openExternal

private val DiagnosticCategories = listOf(
    "Affichage / Interface",
    "Agent IA / Tableaux",
    "Mise à jour / Réseau",
    "Scan / Caméra",
    "Autre anomalie",
    "Demande de fonctionnalité",
)

/**
 * Interactive in-app Diagnostic & Problem Reporter dialog:
 * - Captures sanitized app + own-PID Android logcat traces, anonymized Device ID,
 *   Android SDK/model/screen DPI/fontScale, and version build automatically.
 * - Lets users attach up to **3 images** (`MAX_ATTACHMENTS = 3`), automatically downscaling
 *   and JPEG-compressing any image over `1 MB` down to `<= 1 MB` (`1,000,000` bytes).
 * - Sends through the authenticated Atlas Worker report queue; GitHub delivery is server-side,
 *   so users never leave the app or need a GitHub login. Reports remain queued if GitHub delivery is unavailable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProblemReportDialog(
    screenOrigin: String,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val activeRole = (session as? SessionState.Active)?.user?.role

    var category by remember { mutableStateOf(DiagnosticCategories.first()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var logsPreview by remember { mutableStateOf("") }
    var showLogsPreview by remember { mutableStateOf(false) }
    var compressing by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var loadingLogs by remember { mutableStateOf(true) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val attachments = remember { mutableStateListOf<CompressedReportAttachment>() }

    val snapshot = remember(screenOrigin, activeRole) {
        DiagnosticReporter.collectSnapshot(
            context = context,
            shortDeviceId = appViewModel.deviceShortId,
            fullFingerprint = appViewModel.deviceFingerprint,
            role = activeRole,
            screenOrigin = screenOrigin,
        )
    }

    LaunchedEffect(Unit) {
        logsPreview = AtlasLogBuffer.captureDiagnosticLogsAsync(context, maxChars = 16_000)
        loadingLogs = false
    }

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val remainingSlots = DiagnosticReporter.MAX_ATTACHMENTS - attachments.size
        if (remainingSlots <= 0) {
            onToast("Maximum 3 captures autorisées par rapport.")
            return@rememberLauncherForActivityResult
        }
        val selected = uris.take(remainingSlots)
        if (uris.size > remainingSlots) {
            onToast("Seules les $remainingSlots premières images ont été retenues (max 3).")
        }
        scope.launch {
            compressing = true
            selected.forEachIndexed { idx, uri ->
                val prepared = DiagnosticReporter.prepareAttachment(
                    context = context,
                    uri = uri,
                    slotIndex = attachments.size + idx,
                )
                if (prepared != null) {
                    attachments.add(prepared)
                }
            }
            compressing = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = atlasText("Signaler un problème", "Report a problem", "الإبلاغ عن مشكلة"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = atlasText(
                            "Envoi via Atlas Bot · le statut GitHub est confirmé après l'envoi.",
                            "Sent via Atlas Bot · GitHub issue status is confirmed after submission.",
                            "يُرسل عبر Atlas Bot · يتم تأكيد حالة GitHub بعد الإرسال.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Catégorie",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DiagnosticCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                AtlasTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Titre court du problème",
                    placeholder = "Ex. : Tableau coupé à droite dans Agent IA",
                    imeAction = ImeAction.Next,
                )

                AtlasTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Que s'est-il passé ?",
                    placeholder = "Décrivez ce que vous voyez à l'écran…",
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )

                AtlasTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = "Étapes (optionnel)",
                    placeholder = "1. Ouvrir Agent IA  2. Envoyer une demande…",
                    singleLine = false,
                    imeAction = ImeAction.Done,
                )

                // Up to 3 attached images (auto-compressed <= 1 MB each)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Atlas.palette.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Captures jointes (${attachments.size}/${DiagnosticReporter.MAX_ATTACHMENTS})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Max 3 images · compression auto ≤ 1 Mo chacune",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (attachments.size < DiagnosticReporter.MAX_ATTACHMENTS) {
                                TextButton(
                                    onClick = { multiImagePicker.launch("image/*") },
                                    enabled = !compressing && !submitting,
                                ) {
                                    Text(if (compressing) "Compression…" else "Ajouter")
                                }
                            }
                        }

                        if (attachments.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                attachments.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (item.preview != null) {
                                            Image(
                                                bitmap = item.preview,
                                                contentDescription = item.fileName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = item.fileName,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = item.sizeSummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Atlas.palette.positive,
                                            )
                                        }
                                        IconButton(onClick = { attachments.remove(item) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Retirer l'image",
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Compact diagnostic summary with explicit copy/refresh controls.
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, Atlas.palette.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Diagnostics inclus automatiquement",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (loadingLogs) "Capture en cours…" else "${logsPreview.lineSequence().count()} lignes · ${snapshot.appVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Journaux expurgés des secrets · les captures restent privées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        loadingLogs = true
                                        logsPreview = AtlasLogBuffer.captureDiagnosticLogsAsync(context, maxChars = 16_000)
                                        loadingLogs = false
                                    }
                                },
                                enabled = !submitting && !loadingLogs,
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Rafraîchir les journaux")
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Journaux Atlas assainis", logsPreview))
                                    onToast("Journaux assainis copiés.")
                                },
                                enabled = logsPreview.isNotBlank() && !loadingLogs,
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copier les journaux")
                            }
                            TextButton(onClick = { showLogsPreview = !showLogsPreview }) {
                                Text(if (showLogsPreview) "Masquer" else "Voir")
                            }
                        }
                        if (showLogsPreview) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                            ) {
                                Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                                    Text(
                                        text = logsPreview.ifBlank { "Aucun journal récent." },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                submitError?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                if (attachments.isNotEmpty()) {
                    AtlasOutlinedButton(
                        text = "Partager captures & logs (${attachments.size} img ≤ 1 Mo)",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            val fullMd = DiagnosticReporter.buildMarkdownReport(
                                category = category,
                                title = title.ifBlank { category },
                                description = description,
                                stepsToReproduce = steps,
                                snapshot = snapshot,
                                logs = logsPreview,
                                attachments = attachments.toList(),
                            )
                            if (!DiagnosticReporter.shareBundleWithAttachments(
                                    context = context,
                                    title = title.ifBlank { category },
                                    fullMarkdown = fullMd,
                                    logs = logsPreview,
                                    attachments = attachments.toList(),
                                )
                            ) {
                                onToast("Partage indisponible sur cet appareil.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            AtlasButton(
                text = atlasText("Envoyer à Atlas Bot", "Send to Atlas Bot", "إرسال إلى Atlas Bot"),
                loading = submitting,
                onClick = {
                    scope.launch {
                        submitting = true
                        submitError = null
                        val freshLogs = AtlasLogBuffer.captureDiagnosticLogsAsync(context, maxChars = 16_000)
                        logsPreview = freshLogs
                        val reportKind = when (category) {
                            "Demande de fonctionnalité" -> "idea"
                            "Autre anomalie" -> "other"
                            else -> "bug"
                        }
                        val message = DiagnosticReporter.buildUserReportMessage(
                            kind = reportKind,
                            category = category,
                            title = title.ifBlank { category },
                            description = description,
                            stepsToReproduce = steps,
                            snapshot = snapshot,
                            logs = freshLogs,
                        )
                        when (
                            val result = appViewModel.submitDiagnosticReport(
                                kind = reportKind,
                                message = message,
                                page = screenOrigin,
                                version = "${snapshot.appVersion} (${snapshot.versionCode})",
                            )
                        ) {
                            is ApiResult.Success -> {
                                submitting = false
                                val reference = result.data.reference?.takeIf { it.isNotBlank() }
                                val issueUrl = result.data.issueUrl?.takeIf { url ->
                                    runCatching {
                                        val parsed = java.net.URI(url)
                                        parsed.scheme.equals("https", ignoreCase = true) &&
                                            parsed.host.equals("github.com", ignoreCase = true)
                                    }.getOrDefault(false)
                                }
                                val created = issueUrl != null
                                // Open the actual created issue. If the Worker only queued the report,
                                // open a prefilled GitHub issue form without placing diagnostic logs in the URL.
                                val fallbackIssueUrl = Uri.parse(AppDetails.ISSUES_URL).buildUpon()
                                    .appendQueryParameter("title", "[Atlas Android] ${title.ifBlank { category }.take(120)}")
                                    .appendQueryParameter(
                                        "body",
                                        buildString {
                                            appendLine("Catégorie : $category")
                                            appendLine("Écran : $screenOrigin")
                                            if (description.isNotBlank()) appendLine("Description : ${description.take(800)}")
                                            reference?.let { appendLine("Référence Atlas : $it") }
                                        },
                                    )
                                    .build()
                                    .toString()
                                val opened = openExternal(context, issueUrl ?: fallbackIssueUrl)
                                onToast(
                                    when {
                                        created && opened -> "Ticket GitHub créé et ouvert${reference?.let { " · $it" } ?: ""}."
                                        created -> "Ticket créé${reference?.let { " · $it" } ?: ""}, mais aucun navigateur n'est disponible."
                                        opened -> "Rapport reçu par Atlas${reference?.let { " · $it" } ?: ""}. Le formulaire GitHub prérempli est ouvert; envoyez-le pour créer l'issue."
                                        reference != null -> "Rapport reçu · $reference. Impossible d'ouvrir le suivi sur cet appareil."
                                        else -> "Rapport reçu par Atlas. Impossible d'ouvrir le suivi sur cet appareil."
                                    },
                                )
                                onDismiss()
                            }
                            is ApiResult.Failure -> {
                                submitting = false
                                submitError = "Envoi impossible (${result.code}${result.httpStatus?.let { " · HTTP $it" } ?: ""}) : ${AtlasLogBuffer.sanitize(result.message)}"
                            }
                        }
                    }
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(atlasText("Annuler", "Cancel", "إلغاء"))
            }
        },
    )
}

/**
 * Reusable GitHub maintainer identity card displaying @Etherealmber's rounded
 * circular profile picture (`CircleShape`), dynamic year MIT copyright ownership,
 * and direct one-tap link to https://github.com/Etherealmber.
 */
@Composable
fun EtherealmberAuthorCard(
    onToast: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val openProfile = {
        if (!openExternal(context, AppDetails.GITHUB_PROFILE_URL)) {
            onToast?.invoke("Aucun navigateur disponible.")
        }
    }

    AtlasCard(modifier = modifier, onClick = openProfile) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasAvatar(
                avatarUrl = AppDetails.GITHUB_AVATAR_URL,
                label = AppDetails.GITHUB_HANDLE,
                size = 50.dp,
                onClick = openProfile,
                modifier = Modifier.clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = AppDetails.COPYRIGHT_HOLDER,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AtlasChip(text = "MIT", tone = AtlasTone.POSITIVE)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "github.com/${AppDetails.GITHUB_HANDLE} · © ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = "Ouvrir le profil GitHub Etherealmber",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun LegalPage(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AtlasTopBar(title = title, subtitle = subtitle, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Atlas.density.screenPadding, vertical = Atlas.density.itemGap),
            verticalArrangement = Arrangement.spacedBy(Atlas.density.itemGap),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// License (Dynamic Year + Etherealmber MIT Ownership)
// ---------------------------------------------------------------------------

private fun mitLicenseText(): String = """MIT License

Copyright (c) ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE."""

@Composable
fun LicenseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LegalPage(
        title = "Licence",
        subtitle = "MIT · open source",
        onBack = onBack,
    ) {
        AtlasEntrance(delayMillis = 0) {
            EtherealmberAuthorCard()
        }

        AtlasEntrance(delayMillis = 40) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Licence MIT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    AtlasChip(text = "Open source", tone = AtlasTone.POSITIVE)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}. L'application Android " +
                        "Atlas Commerce est distribuée sous licence MIT, comme le projet web dont elle dépend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = "Profil GitHub @${AppDetails.GITHUB_HANDLE}",
                    onClick = { openExternal(context, AppDetails.GITHUB_PROFILE_URL) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = "Voir le dépôt web",
                    onClick = { openExternal(context, AppDetails.WEB_REPO) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AtlasEntrance(delayMillis = 80) {
            AtlasCard {
                Text(
                    text = "Texte de la licence",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mitLicenseText(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AtlasEntrance(delayMillis = 120) {
            AtlasCard {
                Text(
                    text = "Composants tiers",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Chaque composant reste la propriété de ses auteurs et est utilisé selon sa licence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                ThirdParty("Kotlin, Kotlin Coroutines", "Apache License 2.0")
                ThirdParty("Jetpack Compose, AndroidX, DataStore", "Apache License 2.0")
                ThirdParty("Hilt / Dagger", "Apache License 2.0")
                ThirdParty("OkHttp, Retrofit, Moshi", "Apache License 2.0")
                ThirdParty("CameraX", "Apache License 2.0")
                ThirdParty("Google ML Kit — Barcode Scanning", "Google APIs Terms of Service (modèle embarqué)")
                ThirdParty(
                    "Atlas Commerce worker & Android",
                    "MIT — © ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}",
                )
            }
        }

        AtlasEntrance(delayMillis = 150) {
            AtlasCard {
                Text(
                    text = "Marques",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "« Atlas Commerce » et le logo Atlas appartiennent à ${AppDetails.COPYRIGHT_HOLDER}. " +
                        "Android et Google Play sont des marques de Google LLC. " +
                        "Cloudflare est une marque de Cloudflare, Inc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ThirdParty(name: String, license: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = license,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(8.dp))
}

// ---------------------------------------------------------------------------
// Security & privacy
// ---------------------------------------------------------------------------

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LegalPage(
        title = "Sécurité & confidentialité",
        subtitle = "Ce que l'app fait de vos données",
        onBack = onBack,
    ) {
        AtlasEntrance(delayMillis = 0) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasMark(size = 36.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Aucun traqueur publicitaire", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Pas d'analytics, pas de publicité. Seul ML Kit (scan) transmet ses " +
                                "statistiques d'usage à Google.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                AtlasBullet("L'application ne contacte que le serveur Atlas que vous avez choisi, à une exception : ML Kit.")
                AtlasBullet("Aucune image de caméra n'est envoyée : les codes-barres sont décodés sur l'appareil.")
                AtlasBullet(
                    "ML Kit (Google, lecture des codes-barres) transmet ses métriques d'usage et de " +
                        "performance à Google : version de l'app, identifiant d'installation, appareil. " +
                        "Aucune image, aucun code scanné.",
                )
                AtlasBullet("Aucun contact, aucune localisation, aucun accès au stockage n'est demandé.")
            }
        }

        AtlasEntrance(delayMillis = 50) {
            AtlasCard {
                SectionTitle("Authentification", "Mot de passe + code e-mail")
                AtlasBullet("Le mot de passe part en HTTPS et n'est jamais stocké sur l'appareil.")
                AtlasBullet("Un nouvel appareil déclenche un code OTP à 6 chiffres, valable 10 minutes.")
                AtlasBullet("La session est liée à l'adresse IP et à l'empreinte de l'appareil.")
                AtlasBullet("Le jeton est chiffré en AES-256-GCM avec une clé du Keystore Android.")
                AtlasBullet("La déconnexion efface le jeton, le panier et le cache du catalogue.")
            }
        }

        AtlasEntrance(delayMillis = 90) {
            AtlasCard {
                SectionTitle("Transport réseau", "HTTPS obligatoire")
                AtlasBullet("Les versions de production refusent le trafic HTTP en clair.")
                AtlasBullet("Le certificat TLS est vérifié par la pile réseau d'Android.")
                AtlasBullet("Seules les versions debug autorisent un serveur local (émulateur).")
            }
        }

        AtlasEntrance(delayMillis = 120) {
            AtlasCard {
                SectionTitle("Données conservées", "Minimum vital")
                AtlasBullet("Jeton de session chiffré + identifiant, rôle et nom affiché.")
                AtlasBullet("Catalogue produits en cache pour le mode hors ligne (aucune donnée financière).")
                AtlasBullet("Préférences d'apparence et adresse du serveur.")
                AtlasBullet("Effacement complet : déconnexion, puis désinstallation de l'app.")
            }
        }

        AtlasEntrance(delayMillis = 150) {
            AtlasCard {
                SectionTitle("Vos responsabilités", "Auto-hébergement")
                Text(
                    text = "Atlas Commerce fonctionne avec votre propre serveur. L'administrateur du serveur " +
                        "voit les ventes, le stock et les comptes. Choisissez un serveur de confiance et un " +
                        "mot de passe fort, et activez la double authentification côté web.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AtlasEntrance(delayMillis = 180) {
            AtlasCard {
                SectionTitle("Signaler une vulnérabilité", "Réponse sous 48 h")
                Text(
                    text = "Merci de ne pas ouvrir d'issue publique pour une faille. Écrivez à " +
                        "${AppDetails.SECURITY_EMAIL} ou à @${AppDetails.GITHUB_HANDLE} avec les étapes de reproduction.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = "Contacter la sécurité",
                    onClick = { openExternal(context, "mailto:${AppDetails.SECURITY_EMAIL}") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

// ---------------------------------------------------------------------------
// About
// ---------------------------------------------------------------------------

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        ProblemReportDialog(
            screenOrigin = "À propos",
            onDismiss = { showReportDialog = false },
            onToast = {},
        )
    }

    LegalPage(
        title = "À propos",
        subtitle = "${AppDetails.NAME} ${AppDetails.VERSION_NAME}",
        onBack = onBack,
    ) {
        AtlasEntrance(delayMillis = 0) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AtlasPulseHalo(size = 104.dp) {
                    AtlasMark(size = 68.dp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = AppDetails.NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = AppDetails.TAGLINE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasChip(text = "v${AppDetails.VERSION_NAME}", tone = AtlasTone.INFO)
                    AtlasChip(text = "build ${AppDetails.VERSION_CODE}")
                    AtlasChip(text = "MIT", tone = AtlasTone.POSITIVE)
                }
            }
        }

        AtlasEntrance(delayMillis = 45) {
            EtherealmberAuthorCard()
        }

        AtlasEntrance(delayMillis = 85) {
            AtlasCard {
                Text(
                    text = AppDetails.DESCRIPTION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Cette application est le compagnon Android du projet web Atlas Commerce : " +
                        "elle se connecte à votre serveur et ne stocke aucune comptabilité localement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AtlasEntrance(delayMillis = 120) {
            AtlasCard {
                Text("Fonctionnalités", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                AtlasBullet("Tableau de bord : trésorerie, valeur du stock, créances, tendance.")
                AtlasBullet("Catalogue produits avec recherche, stock faible et mode hors ligne.")
                AtlasBullet("Scan de codes-barres pour ajouter un article au panier.")
                AtlasBullet("Vente rapide : ticket VR sans TVA, prêt à encaisser sur le web.")
                AtlasBullet("Mises à jour accélérées multi-connexions (4 flux HTTP Range) et diagnostic @atlas-bot.")
            }
        }

        AtlasEntrance(delayMillis = 155) {
            AtlasCard {
                Text("Liens & Support", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = "GitHub @${AppDetails.GITHUB_HANDLE}",
                    onClick = { openExternal(context, AppDetails.GITHUB_PROFILE_URL) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = "Projet Atlas Commerce (web)",
                    onClick = { openExternal(context, AppDetails.WEB_REPO) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = "Code source Android",
                    onClick = { openExternal(context, AppDetails.PROJECT_REPO) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasOutlinedButton(
                    text = "Signaler un problème (avec journaux & captures)",
                    icon = Icons.Outlined.BugReport,
                    onClick = { showReportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AtlasEntrance(delayMillis = 185) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Atlas.palette.positive,
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Distribué sous licence MIT. Forkez, modifiez, citez-nous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
