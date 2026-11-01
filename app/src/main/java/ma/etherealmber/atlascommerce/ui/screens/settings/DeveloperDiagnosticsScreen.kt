package ma.etherealmber.atlascommerce.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.diagnostics.DiagnosticReporter
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.diagnostics.CrashReportConsent
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasInfoRow
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun DeveloperDiagnosticsScreen(
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val crashReportsConfigured = CrashReportConsent.isConfigured(context)
    val firebaseAppConfigured = CrashReportConsent.hasDefaultFirebaseApp(context)
    val crashReportsEnabled = CrashReportConsent.isEnabled(context)
    val developerMode by appViewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val cachedProductCount by appViewModel.cachedProductCount.collectAsStateWithLifecycle()
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val activeRole = (session as? SessionState.Active)?.user?.role
    val diagnosticsRefreshed = atlasText("État actualisé.", "Diagnostics refreshed.", "تم تحديث التشخيصات.")
    val diagnosticsCopied = atlasText("Diagnostics expurgés copiés.", "Redacted diagnostics copied.", "تم نسخ التشخيصات المنقحة.")
    val diagnosticsCopyFailed = atlasText("Impossible de copier les diagnostics.", "Could not copy diagnostics.", "تعذّر نسخ التشخيصات.")
    val diagnosticsPrepareFailed = atlasText("Impossible de préparer les diagnostics.", "Could not prepare diagnostics.", "تعذّر إعداد التشخيصات.")
    val scope = rememberCoroutineScope()
    var copyingDiagnostics by remember { mutableStateOf(false) }
    var diagnosticSnapshot by remember(activeRole) {
        mutableStateOf(
            DiagnosticReporter.collectSnapshot(
                context = context,
                shortDeviceId = appViewModel.deviceShortId,
                fullFingerprint = appViewModel.deviceFingerprint,
                role = activeRole,
                screenOrigin = "Developer diagnostics",
            ),
        )
    }

    Scaffold(
        topBar = {
            AtlasTopBar(
                title = atlasText("Diagnostics développeur", "Developer status", "حالة المطوّر"),
                subtitle = atlasText("État local · détails sensibles masqués", "Local status · sensitive details hidden", "حالة محلية · إخفاء التفاصيل الحساسة"),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Atlas.density.screenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            atlasText("Mode développeur", "Developer mode", "وضع المطوّر"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            atlasText("Ce réglage reste sur cet appareil.", "This setting stays on this device.", "يبقى هذا الإعداد على هذا الجهاز."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = developerMode,
                        onCheckedChange = { enabled ->
                            appViewModel.setDeveloperModeEnabled(enabled)
                            if (!enabled) onBack()
                        },
                    )
                }
            }

            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        atlasText("État de l'application", "App status", "حالة التطبيق"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = {
                            diagnosticSnapshot = DiagnosticReporter.collectSnapshot(
                                context = context,
                                shortDeviceId = appViewModel.deviceShortId,
                                fullFingerprint = appViewModel.deviceFingerprint,
                                role = activeRole,
                                screenOrigin = "Developer diagnostics",
                            )
                            onToast(diagnosticsRefreshed)
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = atlasText("Actualiser les diagnostics", "Refresh diagnostics", "تحديث التشخيصات"),
                        )
                    }
                    IconButton(
                        enabled = !copyingDiagnostics,
                        onClick = {
                            scope.launch {
                                copyingDiagnostics = true
                                try {
                                    val freshSnapshot = DiagnosticReporter.collectSnapshot(
                                        context = context,
                                        shortDeviceId = appViewModel.deviceShortId,
                                        fullFingerprint = appViewModel.deviceFingerprint,
                                        role = activeRole,
                                        screenOrigin = "Developer diagnostics",
                                    )
                                    val safeLogs = AtlasLogBuffer.sanitize(
                                        AtlasLogBuffer.captureDiagnosticLogsAsync(context, maxChars = 32_000),
                                    )
                                    val report = DiagnosticReporter.buildMarkdownReport(
                                        category = "Developer diagnostics",
                                        title = "Atlas app diagnostics",
                                        description = "User-requested local diagnostic copy.",
                                        stepsToReproduce = "",
                                        snapshot = freshSnapshot,
                                        logs = safeLogs,
                                        attachments = emptyList(),
                                    )
                                    val copied = DiagnosticReporter.copyFullReportToClipboard(context, report)
                                    diagnosticSnapshot = freshSnapshot
                                    onToast(if (copied) diagnosticsCopied else diagnosticsCopyFailed)
                                } catch (_: Exception) {
                                    onToast(diagnosticsPrepareFailed)
                                } finally {
                                    copyingDiagnostics = false
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = atlasText("Copier les diagnostics expurgés", "Copy redacted diagnostics", "نسخ التشخيصات المنقحة"),
                        )
                    }
                }
                Text(
                    atlasText(
                        "Actualiser pour mettre à jour le résumé. Copier capture les journaux expurgés uniquement sur demande.",
                        "Refresh to update the summary. Copy captures redacted logs only when explicitly requested.",
                        "حدّث لتجديد الملخص. لا يتم نسخ السجلات المنقحة إلا عند الطلب صراحةً.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AtlasInfoRow(
                    atlasText("Version", "Version", "الإصدار"),
                    "${AppDetails.VERSION_NAME} · ${AppDetails.VERSION_CODE}",
                )
                AtlasInfoRow(
                    atlasText("Appareil", "Device", "الجهاز"),
                    diagnosticSnapshot.manufacturerModel,
                )
                AtlasInfoRow(
                    atlasText("Android", "Android", "Android"),
                    diagnosticSnapshot.androidVersion,
                )
                AtlasInfoRow(
                    atlasText("Réseau", "Network", "الشبكة"),
                    diagnosticSnapshot.networkStatus,
                )
                AtlasInfoRow(
                    atlasText("Dernière actualisation", "Last refreshed", "آخر تحديث"),
                    diagnosticSnapshot.timestampUtc,
                )
                AtlasInfoRow(
                    atlasText("Catalogue hors ligne", "Offline catalog", "الكتالوج غير المتصل"),
                    atlasText(
                        "$cachedProductCount article(s)",
                        "$cachedProductCount item(s)",
                        "$cachedProductCount عنصر",
                    ),
                )
                AtlasInfoRow(
                    atlasText("Rapports Crashlytics", "Crashlytics reports", "تقارير Crashlytics"),
                    when {
                        crashReportsConfigured && crashReportsEnabled -> atlasText("Activés avec consentement", "Enabled with consent", "مفعّلة بموافقة المستخدم")
                        crashReportsConfigured -> atlasText("Désactivés · consentement requis", "Disabled · consent required", "معطّلة · تتطلب الموافقة")
                        firebaseAppConfigured -> atlasText(
                            "Firebase est initialisé, mais Crashlytics est indisponible dans cette version",
                            "Firebase is initialized, but Crashlytics is unavailable in this build",
                            "تم تهيئة Firebase لكن Crashlytics غير متاح في هذا الإصدار",
                        )
                        else -> atlasText(
                            "Configuration Firebase absente de cette version",
                            "Firebase configuration is not bundled in this build",
                            "إعداد Firebase غير مضمّن في هذا الإصدار",
                        )
                    },
                )
                Text(
                    atlasText(
                        "Les notifications Firebase Cloud Messaging et les rapports Crashlytics sont deux services distincts. Cette ligne indique uniquement si Crashlytics est disponible dans l'application installée. Les journaux techniques restent locaux et ne sont partagés qu'après votre accord.",
                        "Firebase Cloud Messaging notifications and Crashlytics reports are separate services. This row only reports whether Crashlytics is available in the installed app. Technical logs stay local and are shared only with your consent.",
                        "إشعارات Firebase Cloud Messaging وتقارير Crashlytics خدمتان منفصلتان. يوضح هذا السطر توفر Crashlytics فقط في التطبيق المثبت. تبقى السجلات التقنية محلية ولا تُشارك إلا بموافقتك.",
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
