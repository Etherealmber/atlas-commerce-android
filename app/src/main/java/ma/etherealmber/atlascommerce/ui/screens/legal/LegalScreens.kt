package ma.etherealmber.atlascommerce.ui.screens.legal

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
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
 * - Lets users prepare up to **3 images** (`MAX_ATTACHMENTS = 3`) for a separate Android share action,
 *   automatically downscaling and JPEG-compressing any image over `1 MB` to `<= 1 MB`.
 *   Images are not uploaded with the authenticated report request.
 * - Sends the text report through the authenticated Atlas Worker queue; GitHub delivery is server-side,
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
    val language = LocalAtlasLanguage.current
    val scope = rememberCoroutineScope()
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val activeRole = (session as? SessionState.Active)?.user?.role

    var category by remember { mutableStateOf(DiagnosticCategories.first()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var compressing by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<ApiResult.Failure?>(null) }
    val attachments = remember { mutableStateListOf<CompressedReportAttachment>() }
    var deviceIdentityDiagnostics by remember(appViewModel) {
        mutableStateOf(appViewModel.deviceIdentityDiagnostics())
    }

    val snapshot = remember(screenOrigin, activeRole, deviceIdentityDiagnostics) {
        DiagnosticReporter.collectSnapshot(
            context = context,
            shortDeviceId = deviceIdentityDiagnostics?.shortId.orEmpty(),
            fullFingerprint = deviceIdentityDiagnostics?.fingerprint.orEmpty(),
            role = activeRole,
            screenOrigin = screenOrigin,
        )
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
                if (deviceIdentityDiagnostics == null) {
                    AtlasCard {
                        Text(
                            atlasText(
                                "Empreinte de l'appareil indisponible; elle sera omise du rapport. Réessayez.",
                                "Device identity is temporarily unavailable and will be omitted from this report. Retry.",
                                "بصمة الجهاز غير متاحة مؤقتاً وستُحذف من التقرير. أعد المحاولة.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = { deviceIdentityDiagnostics = appViewModel.deviceIdentityDiagnostics() }) {
                            Text(atlasText("Réessayer", "Retry", "إعادة المحاولة"))
                        }
                    }
                }
                Text(
                    text = atlasText("Catégorie"),
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
                            label = { Text(atlasText(cat), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                AtlasTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = atlasText("Titre court du problème"),
                    placeholder = atlasText("Ex. : Tableau coupé à droite dans Agent IA"),
                    imeAction = ImeAction.Next,
                )

                AtlasTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = atlasText("Que s'est-il passé ?"),
                    placeholder = atlasText("Décrivez ce que vous voyez à l'écran…"),
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )

                AtlasTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = atlasText("Étapes (optionnel)"),
                    placeholder = atlasText("1. Ouvrir Agent IA  2. Envoyer une demande…"),
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
                                    text = atlasText(
                                        "Captures à partager séparément (${attachments.size}/${DiagnosticReporter.MAX_ATTACHMENTS})",
                                        "Screenshots to share separately (${attachments.size}/${DiagnosticReporter.MAX_ATTACHMENTS})",
                                        "لقطات شاشة للمشاركة بشكل منفصل (${attachments.size}/${DiagnosticReporter.MAX_ATTACHMENTS})",
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = atlasText(
                                        "Non envoyées avec le rapport · 3 max · compression auto ≤ 1 Mo chacune",
                                        "Not sent with the report · up to 3 · auto-compressed to ≤ 1 MB each",
                                        "لا تُرسل مع البلاغ · حتى 3 · ضغط تلقائي إلى ≤ 1 ميغابايت لكل صورة",
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (attachments.size < DiagnosticReporter.MAX_ATTACHMENTS) {
                                TextButton(
                                    onClick = { multiImagePicker.launch("image/*") },
                                    enabled = !compressing && !submitting,
                                ) {
                                    Text(if (compressing) atlasText("Compression…") else atlasText("Ajouter"))
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
                                                contentDescription = atlasText("Retirer l'image"),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, Atlas.palette.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = atlasText("Diagnostics protégés", "Diagnostics stay private", "التشخيصات محمية"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = atlasText(
                                "Aucun journal brut n'est affiché. Un résumé expurgé sera joint uniquement si vous envoyez ce rapport.",
                                "Raw logs are not shown. A redacted summary is attached only if you send this report.",
                                "لا تُعرض السجلات الخام. لن يُرفق ملخص منقح إلا عند إرسال هذا البلاغ.",
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                submitError?.let { failure ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (failure.message.isBlank()) {
                                atlasText(
                                    "Envoi impossible. Vérifiez votre connexion et réessayez.",
                                    "Could not send the report. Check your connection and try again.",
                                    "تعذّر إرسال البلاغ. تحقق من اتصالك وحاول مجدداً.",
                                )
                            } else {
                                atlasNotice(failure.message, language)
                            },
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                if (attachments.isNotEmpty()) {
                    AtlasOutlinedButton(
                        text = atlasText(
                            "Partager les captures séparément (${attachments.size})",
                            "Share screenshots separately (${attachments.size})",
                            "مشاركة لقطات الشاشة بشكل منفصل (${attachments.size})",
                        ),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            val shareText = buildString {
                                appendLine("[Atlas Commerce] ${title.ifBlank { category }.take(120)}")
                                appendLine("Category: $category")
                                if (description.isNotBlank()) appendLine(description.trim().take(800))
                            }
                            if (!DiagnosticReporter.shareBundleWithAttachments(
                                    context = context,
                                    title = title.ifBlank { category },
                                    fullMarkdown = shareText,
                                    logs = "",
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
                text = atlasText("Envoyer le rapport à Atlas", "Send report to Atlas", "إرسال البلاغ إلى Atlas"),
                loading = submitting,
                onClick = {
                    scope.launch {
                        submitting = true
                        submitError = null
                        val freshLogs = AtlasLogBuffer.captureDiagnosticLogsAsync(context, maxChars = 32_000)
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
                                AtlasLogBuffer.log(
                                    "REPORT",
                                    "User report submission failed: code=${result.code}; http=${result.httpStatus ?: "none"}; message=${AtlasLogBuffer.sanitize(result.message).take(180)}",
                                    "ERROR",
                                )
                                submitError = result
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
                    AtlasChip(text = "MIT OR Apache-2.0", tone = AtlasTone.POSITIVE)
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
                contentDescription = atlasText("Ouvrir le profil GitHub Etherealmber"),
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
        AtlasTopBar(title = atlasText(title), subtitle = subtitle?.let { atlasText(it) }, onBack = onBack)
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
    val apacheLicenseText = remember(context) {
        context.assets.open("licenses/LICENSE-APACHE.txt").bufferedReader().use { it.readText() }
    }
    LegalPage(
        title = atlasText("Licence"),
        subtitle = atlasText("MIT ou Apache 2.0 · open source", "MIT or Apache 2.0 · open source", "MIT أو Apache 2.0 · مفتوح المصدر"),
        onBack = onBack,
    ) {
        AtlasEntrance(delayMillis = 0) {
            EtherealmberAuthorCard()
        }

        AtlasEntrance(delayMillis = 40) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = atlasText("Licence MIT ou Apache 2.0", "MIT OR Apache License 2.0", "ترخيص MIT أو Apache 2.0"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    AtlasChip(text = atlasText("Open source"), tone = AtlasTone.POSITIVE)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = atlasText(
                        "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}. Le code original de l'application Android Atlas Commerce est proposé sous licence MIT ou Apache 2.0, au choix. Le projet web conserve sa licence distincte.",
                        "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}. The original Atlas Commerce Android source is available under either the MIT License or Apache License 2.0, at your option. The web project has a separate license.",
                        "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}. يتوفر المصدر الأصلي لتطبيق Atlas Commerce على Android بموجب ترخيص MIT أو Apache 2.0، حسب اختيارك. للمشروع الإلكتروني ترخيص منفصل.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = atlasText("Profil GitHub @${AppDetails.GITHUB_HANDLE}", "GitHub profile @${AppDetails.GITHUB_HANDLE}", "ملف GitHub @${AppDetails.GITHUB_HANDLE}"),
                    onClick = { openExternal(context, AppDetails.GITHUB_PROFILE_URL) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = atlasText("Voir le dépôt web"),
                    onClick = { openExternal(context, AppDetails.WEB_REPO) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AtlasEntrance(delayMillis = 80) {
            AtlasCard {
                Text(
                    text = atlasText("Texte de la licence MIT", "MIT License text", "نص ترخيص MIT"),
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
                    text = atlasText("Licence Apache 2.0", "Apache License 2.0", "ترخيص Apache 2.0"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = apacheLicenseText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AtlasEntrance(delayMillis = 160) {
            AtlasCard {
                Text(
                    text = atlasText("Composants tiers"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = atlasText("Chaque composant reste la propriété de ses auteurs et est utilisé selon sa licence."),
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
                ThirdParty("Atlas Commerce Android", "MIT OR Apache License 2.0")
                ThirdParty("Atlas Commerce Web backend", "MIT (see upstream repository)")
            }
        }

        AtlasEntrance(delayMillis = 200) {
            AtlasCard {
                Text(
                    text = atlasText("Marques"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = atlasText(
                        "« Atlas Commerce » et le logo Atlas appartiennent à ${AppDetails.COPYRIGHT_HOLDER}. Android et Google Play sont des marques de Google LLC. Cloudflare est une marque de Cloudflare, Inc.",
                        "Atlas Commerce and the Atlas logo are owned by ${AppDetails.COPYRIGHT_HOLDER}. Android and Google Play are trademarks of Google LLC. Cloudflare is a trademark of Cloudflare, Inc.",
                        "تعود ملكية Atlas Commerce وشعار Atlas إلى ${AppDetails.COPYRIGHT_HOLDER}. Android وGoogle Play علامتان تجاريتان لشركة Google LLC. وCloudflare علامة تجارية لشركة Cloudflare, Inc.",
                    ),
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
        title = atlasText("Conditions & confidentialité", "Terms & privacy", "الشروط والخصوصية"),
        subtitle = atlasText(
            "Données échangées par l'application et rôle de votre serveur",
            "What the app sends and the role of your server",
            "البيانات التي يرسلها التطبيق ودور خادمك",
        ),
        onBack = onBack,
    ) {
        AtlasEntrance(delayMillis = 0) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasMark(size = 36.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(atlasText("Confidentialité de l'application", "App privacy", "خصوصية التطبيق"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = atlasText(
                                "L'application Android est un client : les demandes de compte et de gestion sont envoyées au serveur Atlas que vous avez choisi. Nous n'exploitons pas votre serveur auto-hébergé et ne recevons pas ses données dans le fonctionnement normal de l'app.",
                                "The Android app is a client: account and business requests go to the Atlas server you selected. We do not operate your self-hosted server or receive its data during normal app use.",
                                "تطبيق Android هو عميل: تُرسل طلبات الحساب وإدارة الأعمال إلى خادم Atlas الذي اخترته. نحن لا ندير خادمك المستضاف ذاتياً ولا نتلقى بياناته أثناء الاستخدام المعتاد للتطبيق.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                AtlasBullet(atlasText(
                    "À la connexion, votre nom d'utilisateur, votre mot de passe et, si demandé, le code OTP sont transmis au serveur choisi via HTTPS. Après connexion, le jeton de session accompagne les requêtes authentifiées ; le mot de passe n'est pas conservé par l'app.",
                    "At sign-in, your username, password and, if requested, OTP code are sent to the selected server over HTTPS. After sign-in, the session token accompanies authenticated requests; the app does not store your password.",
                    "عند تسجيل الدخول، يُرسل اسم المستخدم وكلمة المرور ورمز OTP عند طلبه إلى الخادم المحدد عبر HTTPS. بعد الدخول، يُرفق رمز الجلسة بالطلبات الموثّقة؛ ولا يحتفظ التطبيق بكلمة مرورك.",
                ))
                AtlasBullet(atlasText(
                    "Le serveur choisi reçoit l'adresse IP source de la connexion et les données des requêtes. L'app utilise aussi une empreinte d'appareil pseudonyme et une description Android pour gérer la session et les appareils de confiance.",
                    "The selected server can see the connection's source IP and request data. The app also uses a pseudonymous device fingerprint and Android device description for session and trusted-device management.",
                    "يمكن للخادم المحدد رؤية عنوان IP المصدر وبيانات الطلبات. كما يستخدم التطبيق بصمة جهاز مستعارة ووصف جهاز Android لإدارة الجلسة والأجهزة الموثوقة.",
                ))
                AtlasBullet(atlasText(
                    "Les données professionnelles que vous consultez ou modifiez — produits, stock, ventes et finances, par exemple — sont envoyées à votre serveur, qui les conserve et les gère.",
                    "Business data you view or change—such as products, inventory, sales and financial data—is sent to your selected server, which stores and manages those records.",
                    "تُرسل بيانات الأعمال التي تعرضها أو تعدّلها—مثل المنتجات والمخزون والمبيعات والبيانات المالية—إلى خادمك المحدد، وهو الذي يخزّن هذه السجلات ويديرها.",
                ))
                AtlasBullet(atlasText(
                    "Les messages d'Atlas Agent sont envoyés à votre serveur, qui peut les transmettre au fournisseur d'IA configuré par son exploitant.",
                    "Atlas Agent messages go to your selected server; that server may forward them to the AI provider configured by its operator.",
                    "تُرسل رسائل Atlas Agent إلى خادمك المحدد؛ وقد يحيلها الخادم إلى مزوّد الذكاء الاصطناعي الذي أعدّه مشغّله.",
                ))
                AtlasBullet(atlasText(
                    "Si Firebase est configuré, Google fournit un jeton FCM que l'app enregistre auprès de votre serveur pour les notifications. Crashlytics n'envoie des rapports d'erreur que si vous l'activez dans les paramètres ; ils peuvent contenir une trace d'exception et des informations sur l'appareil, Android, l'heure et l'installation Firebase, mais ni mot de passe, ni code OTP, ni jeton de session, ni données financières.",
                    "If Firebase is configured, Google provides an FCM token that the app registers with your server for notifications. Crashlytics sends crash reports only if you enable it in Settings; reports may include an exception stack trace, app/device/Android details, time and Firebase installation identifier, but not passwords, OTP codes, session tokens or financial records.",
                    "إذا كان Firebase مُعدّاً، توفّر Google رمز FCM الذي يسجّله التطبيق لدى خادمك للإشعارات. ولا يرسل Crashlytics تقارير الأعطال إلا إذا فعّلته من الإعدادات؛ وقد تتضمن التقارير أثر الاستثناء ومعلومات التطبيق والجهاز وAndroid والوقت ومعرّف تثبيت Firebase، لكنها لا تتضمن كلمات المرور أو رموز OTP أو رموز الجلسات أو السجلات المالية.",
                ))
                AtlasBullet(atlasText(
                    "Lors du scan des codes-barres, ML Kit de Google peut transmettre des métriques d'utilisation et de performance (version de l'application et identifiants d'installation/appareil) ; ni les images de caméra ni les codes scannés ne sont envoyés. L'application peut aussi contacter GitHub ou un réseau de distribution pour vérifier et télécharger les mises à jour.",
                    "When you use barcode scanning, Google's ML Kit may send usage and performance metrics (app version and installation/device identifiers); camera images and scanned codes are not sent. The app may contact GitHub or a file-delivery network to check for and download updates.",
                    "عند استخدام ماسح الباركود، قد يرسل ML Kit من Google مقاييس الاستخدام والأداء (إصدار التطبيق ومعرّفات التثبيت والجهاز)؛ ولا تُرسل صور الكاميرا أو الرموز الممسوحة. وقد يتصل التطبيق بـ GitHub أو شبكة توزيع الملفات للتحقق من التحديثات وتنزيلها.",
                ))
                AtlasBullet(atlasText(
                    "Le rapport d'assistance et son résumé technique expurgé sont envoyés au serveur choisi uniquement si vous appuyez sur Envoyer. Selon sa configuration, ce serveur peut transmettre le rapport à l'équipe d'assistance ou créer une issue GitHub. Les captures d'écran ne sont pas jointes ; elles ne sont partagées qu'avec l'action Android séparée.",
                    "A support report and its redacted technical summary are sent to the selected server only when you tap Submit. Depending on its setup, that server may forward the report to support or create a GitHub issue. Screenshots are not attached; they are shared only through the separate Android sharing action.",
                    "لا يُرسل تقرير الدعم وملخصه التقني المنقّح إلى الخادم المحدد إلا عند الضغط على إرسال. وقد يحيل الخادم التقرير إلى فريق الدعم أو ينشئ بلاغاً في GitHub حسب إعداداته. ولا تُرفق لقطات الشاشة؛ بل تُشارك فقط عبر إجراء المشاركة المنفصل في Android.",
                ))
                AtlasBullet("Aucune image de caméra n'est envoyée : les codes-barres sont décodés sur l'appareil.")
                AtlasBullet("Aucun contact, aucune localisation, aucun accès au stockage n'est demandé.")
            }
        }

        AtlasEntrance(delayMillis = 35) {
            AtlasCard {
                Text(atlasText("Auto-hébergement et confidentialité du serveur", "Self-hosted server and its privacy policy", "الاستضافة الذاتية وخصوصية الخادم"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    atlasText(
                        "Si vous utilisez votre propre site ou serveur Atlas, son exploitant en est responsable : accès aux données, journaux, sauvegardes, durée de conservation, sécurité, prestataires et politique de confidentialité du site. Le développeur de l'application ne contrôle pas ces choix. Pour savoir ce que ce site collecte, conserve ou transmet, consultez sa politique de confidentialité ou demandez directement à son administrateur. Cette page décrit l'application Android, pas la politique de votre serveur.",
                        "If you use your own Atlas website or server, its operator is responsible for data access, logs, backups, retention, security, service providers and the website's privacy policy. The app developer does not control those choices. To learn what that site collects, stores or shares, read its privacy notice or ask its administrator directly. This page describes the Android app, not your server's policy.",
                        "إذا استخدمت موقع Atlas أو خادمه الخاص بك، فمشغّله مسؤول عن الوصول إلى البيانات والسجلات والنسخ الاحتياطية ومدة الاحتفاظ والأمان ومزوّدي الخدمة وسياسة خصوصية الموقع. لا يتحكم مطوّر التطبيق بهذه الخيارات. لمعرفة ما يجمعه ذلك الموقع أو يخزّنه أو يشاركه، اقرأ إشعار الخصوصية لديه أو اسأل مسؤول الخادم مباشرةً. تصف هذه الصفحة تطبيق Android، لا سياسة خادمك.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AtlasEntrance(delayMillis = 55) {
            AtlasCard {
                Text(atlasText("Conditions d'utilisation", "Terms of use", "شروط الاستخدام"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    atlasText(
                        "Utilisez l'application uniquement avec un compte et un serveur auxquels vous êtes autorisé à accéder. Protégez votre mot de passe et votre appareil, utilisez l'adresse HTTPS exacte du serveur choisi et respectez les règles de son exploitant ainsi que les lois applicables. Vous êtes responsable des données que vous saisissez et des actions effectuées depuis votre compte.",
                        "Use the app only with an account and server you are authorized to access. Protect your password and device, use the exact HTTPS address of the server you chose, and follow its operator's rules and applicable law. You are responsible for the data you enter and actions performed from your account.",
                        "استخدم التطبيق فقط مع حساب وخادم مخوّل لك الوصول إليهما. احمِ كلمة مرورك وجهازك، واستخدم عنوان HTTPS الدقيق للخادم الذي اخترته، واتبع قواعد مشغّله والقوانين المعمول بها. أنت مسؤول عن البيانات التي تدخلها والإجراءات المنفذة من حسابك.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    atlasText(
                        "Pour un serveur auto-hébergé, son propriétaire est responsable du site web, de son fonctionnement, de sa sécurité, de ses sauvegardes, des données et des avis de confidentialité qu'il publie. Consultez ses conditions et sa politique de confidentialité pour toute question sur le traitement des données côté serveur.",
                        "For a self-hosted server, its owner is responsible for the website, its operation, security, backups, data and privacy notices it publishes. Consult that operator's terms and privacy policy for questions about server-side data handling.",
                        "في الخادم المستضاف ذاتياً، يكون مالكه مسؤولاً عن الموقع وتشغيله وأمانه ونسخه الاحتياطية وبياناته وإشعارات الخصوصية التي ينشرها. راجع شروط المشغّل وسياسة الخصوصية لديه لأي سؤال عن معالجة البيانات على الخادم.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AtlasEntrance(delayMillis = 80) {
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
                    text = atlasText(
                        "Atlas Commerce fonctionne avec votre propre serveur. L'administrateur du serveur voit les ventes, le stock et les comptes. Choisissez un serveur de confiance et un mot de passe fort, et activez la double authentification côté web.",
                        "Atlas Commerce uses your own server. Its administrator can see sales, inventory and accounts. Choose a trusted server, use a strong password and enable two-factor authentication on the web app.",
                        "يعمل Atlas Commerce عبر خادمك الخاص. يمكن لمسؤول الخادم الاطلاع على المبيعات والمخزون والحسابات. اختر خادماً موثوقاً واستخدم كلمة مرور قوية وفعّل المصادقة الثنائية في تطبيق الويب.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AtlasEntrance(delayMillis = 180) {
            AtlasCard {
                SectionTitle("Signaler une vulnérabilité", "Réponse sous 48 h")
                Text(
                    text = atlasText(
                        "Merci de ne pas ouvrir d'issue publique pour une faille. Écrivez à ${AppDetails.SECURITY_EMAIL} ou à @${AppDetails.GITHUB_HANDLE} avec les étapes de reproduction.",
                        "Please do not open a public issue for a security vulnerability. Email ${AppDetails.SECURITY_EMAIL} or contact @${AppDetails.GITHUB_HANDLE} with reproduction steps.",
                        "يرجى عدم فتح بلاغ عام عن ثغرة أمنية. راسل ${AppDetails.SECURITY_EMAIL} أو @${AppDetails.GITHUB_HANDLE} مع خطوات إعادة إنتاجها.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                AtlasButton(
                    text = atlasText("Contacter la sécurité"),
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
            Text(text = atlasText(title), style = MaterialTheme.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = atlasText(subtitle),
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
        title = atlasText("À propos"),
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
                    text = atlasText(AppDetails.TAGLINE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasChip(text = "v${AppDetails.VERSION_NAME}", tone = AtlasTone.INFO)
                    AtlasChip(text = "build ${AppDetails.VERSION_CODE}")
                    AtlasChip(text = "MIT OR Apache-2.0", tone = AtlasTone.POSITIVE)
                }
            }
        }

        AtlasEntrance(delayMillis = 45) {
            EtherealmberAuthorCard()
        }

        AtlasEntrance(delayMillis = 85) {
            AtlasCard {
                Text(
                    text = atlasText(AppDetails.DESCRIPTION),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = atlasText(
                        "Cette application est le compagnon Android du projet web Atlas Commerce : elle se connecte à votre serveur et ne stocke aucune comptabilité localement.",
                        "This Android app accompanies the Atlas Commerce web project: it connects to your server and stores no accounting data locally.",
                        "هذا التطبيق هو رفيق Android لمشروع Atlas Commerce على الويب؛ يتصل بخادمك ولا يخزّن بيانات المحاسبة محلياً.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AtlasEntrance(delayMillis = 120) {
            AtlasCard {
                Text(atlasText("Fonctionnalités"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                AtlasBullet("Tableau de bord : trésorerie, valeur du stock, créances, tendance.")
                AtlasBullet("Catalogue produits avec recherche, stock faible et mode hors ligne.")
                AtlasBullet("Scan de codes-barres pour ajouter un article au panier.")
                AtlasBullet("Vente rapide : ticket VR sans TVA, prêt à encaisser sur le web.")
                AtlasBullet("Mises à jour accélérées multi-connexions (CDN : 6 sur mobile, 16 en Wi-Fi ; GitHub limité à 4) et diagnostic @atlas-bot.")
            }
        }

        AtlasEntrance(delayMillis = 155) {
            AtlasCard {
                Text(atlasText("Liens & Support"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = "GitHub @${AppDetails.GITHUB_HANDLE}",
                    onClick = { openExternal(context, AppDetails.GITHUB_PROFILE_URL) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = atlasText("Projet Atlas Commerce (web)"),
                    onClick = { openExternal(context, AppDetails.WEB_REPO) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = atlasText("Code source Android"),
                    onClick = { openExternal(context, AppDetails.PROJECT_REPO) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasOutlinedButton(
                    text = atlasText("Signaler un problème (avec journaux & captures)"),
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
                            text = atlasText("Distribué sous licence MIT ou Apache 2.0. Forkez, modifiez, citez-nous."),
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
