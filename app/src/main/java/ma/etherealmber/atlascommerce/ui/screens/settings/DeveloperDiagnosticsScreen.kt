package ma.etherealmber.atlascommerce.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ma.etherealmber.atlascommerce.core.net.ProbeReport
import ma.etherealmber.atlascommerce.core.net.hasDeviceInternetCapability
import ma.etherealmber.atlascommerce.core.diagnostics.DeviceDiagnosticSnapshot
import ma.etherealmber.atlascommerce.core.diagnostics.DiagnosticReporter
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.diagnostics.CrashReportConsent
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasInfoRow
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.root.LocalStorageSummary
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import java.util.Locale

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
    val localStorage by appViewModel.localStorageSummary.collectAsStateWithLifecycle()
    val serverSelection by appViewModel.server.collectAsStateWithLifecycle()
    val probeReport by appViewModel.developerProbeReport.collectAsStateWithLifecycle()
    val probeLoading by appViewModel.developerProbeLoading.collectAsStateWithLifecycle()
    val session by appViewModel.session.collectAsStateWithLifecycle()
    var deviceReportsInternetCapability by remember { mutableStateOf(hasDeviceInternetCapability(context)) }
    var pendingCacheAction by remember { mutableStateOf<DeveloperCacheAction?>(null) }
    var cacheActionInProgress by remember { mutableStateOf(false) }
    var exportingDiagnostics by remember { mutableStateOf(false) }
    val activeRole = (session as? SessionState.Active)?.user?.role
    val language = LocalAtlasLanguage.current
    val diagnosticsRefreshed = atlasText("État actualisé.", "Diagnostics refreshed.", "تم تحديث التشخيصات.")
    val diagnosticsCopied = atlasText("Diagnostics expurgés copiés.", "Redacted diagnostics copied.", "تم نسخ التشخيصات المنقحة.")
    val diagnosticsCopyFailed = atlasText("Impossible de copier les diagnostics.", "Could not copy diagnostics.", "تعذّر نسخ التشخيصات.")
    val diagnosticsShareFailed = atlasText("Impossible de préparer le partage des diagnostics.", "Could not prepare diagnostics for sharing.", "تعذّر إعداد التشخيصات للمشاركة.")
    val diagnosticsPrepareFailed = atlasText("Impossible de préparer les diagnostics.", "Could not prepare diagnostics.", "تعذّر إعداد التشخيصات.")
    val diagnosticsShareReady = atlasText("Choisissez une application pour partager le rapport expurgé.", "Choose an app to share the redacted report.", "اختر تطبيقاً لمشاركة التقرير المنقح.")
    val updatePackageRemoved = atlasText("Fichier de mise à jour supprimé.", "Downloaded update removed.", "تم حذف ملف التحديث المُنزّل.")
    val updatePackageRemoveFailed = atlasText("Impossible de supprimer le fichier de mise à jour maintenant.", "Could not remove the downloaded update right now.", "تعذّر حذف التحديث المُنزّل الآن.")
    val scope = rememberCoroutineScope()
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

    LaunchedEffect(Unit) {
        appViewModel.refreshCachedProductCount()
        appViewModel.refreshLocalStorageSummary()
    }
    LaunchedEffect(serverSelection?.baseUrl) {
        appViewModel.clearDeveloperApiProbe()
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

            DeveloperApiDiagnosticsCard(
                deviceReportsInternetCapability = deviceReportsInternetCapability,
                hasConfiguredServer = serverSelection != null,
                configuredServer = serverSelection?.display
                    ?: atlasText("Aucun serveur configuré", "No server configured", "لا يوجد خادم محدد"),
                report = probeReport,
                loading = probeLoading,
                onRunProbe = {
                    deviceReportsInternetCapability = hasDeviceInternetCapability(context)
                    appViewModel.runDeveloperApiProbe()
                },
            )

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
                            deviceReportsInternetCapability = hasDeviceInternetCapability(context)
                            appViewModel.refreshCachedProductCount()
                            appViewModel.refreshLocalStorageSummary()
                            appViewModel.runDeveloperApiProbe()
                            onToast(diagnosticsRefreshed)
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = atlasText("Actualiser les diagnostics", "Refresh diagnostics", "تحديث التشخيصات"),
                        )
                    }
                    IconButton(
                        enabled = !exportingDiagnostics,
                        onClick = {
                            scope.launch {
                                exportingDiagnostics = true
                                try {
                                    val export = collectDeveloperDiagnosticExport(context, appViewModel, activeRole, probeReport)
                                    val copied = DiagnosticReporter.copyFullReportToClipboard(context, export.markdown)
                                    diagnosticSnapshot = export.snapshot
                                    onToast(if (copied) diagnosticsCopied else diagnosticsCopyFailed)
                                } catch (_: Exception) {
                                    onToast(diagnosticsPrepareFailed)
                                } finally {
                                    exportingDiagnostics = false
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = atlasText("Copier les diagnostics expurgés", "Copy redacted diagnostics", "نسخ التشخيصات المنقحة"),
                        )
                    }
                    IconButton(
                        enabled = !exportingDiagnostics,
                        onClick = {
                            scope.launch {
                                exportingDiagnostics = true
                                try {
                                    val export = collectDeveloperDiagnosticExport(context, appViewModel, activeRole, probeReport)
                                    val shared = DiagnosticReporter.shareBundleWithAttachments(
                                        context = context,
                                        title = "Atlas app diagnostics",
                                        fullMarkdown = export.markdown,
                                        logs = export.logs,
                                        attachments = emptyList(),
                                    )
                                    diagnosticSnapshot = export.snapshot
                                    appViewModel.refreshLocalStorageSummary()
                                    onToast(
                                        if (shared) diagnosticsShareReady else diagnosticsShareFailed,
                                    )
                                } catch (_: Exception) {
                                    onToast(diagnosticsPrepareFailed)
                                } finally {
                                    exportingDiagnostics = false
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = atlasText("Exporter les journaux expurgés", "Export redacted logs", "تصدير السجلات المنقحة"),
                        )
                    }
                }
                Text(
                    atlasText(
                        "Actualiser pour mettre à jour le résumé. Les journaux sont expurgés; vérifiez le rapport avant de le partager.",
                        "Refresh to update the summary. Logs are redacted; review the report before sharing it.",
                        "حدّث لتجديد الملخص. السجلات منقحة؛ راجع التقرير قبل مشاركته.",
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

            DeveloperCacheToolsCard(
                summary = localStorage,
                cachedProductCount = cachedProductCount,
                hasActiveSession = session is SessionState.Active,
                actionInProgress = cacheActionInProgress,
                onRequestClear = { pendingCacheAction = it },
            )
        }
    }

    pendingCacheAction?.let { action ->
        AlertDialog(
            onDismissRequest = { if (!cacheActionInProgress) pendingCacheAction = null },
            title = {
                Text(
                    when (action) {
                        DeveloperCacheAction.OFFLINE_DATA -> atlasText("Vider les instantanés hors ligne ?", "Clear offline snapshots?", "مسح اللقطات المحفوظة دون اتصال؟")
                        DeveloperCacheAction.FINANCE_REPORTS -> atlasText("Supprimer les PDF temporaires ?", "Delete temporary PDFs?", "حذف ملفات PDF المؤقتة؟")
                        DeveloperCacheAction.UPDATE_PACKAGE -> atlasText("Supprimer la mise à jour téléchargée ?", "Remove downloaded update?", "إزالة التحديث المُنزّل؟")
                    },
                )
            },
            text = {
                Text(
                    when (action) {
                        DeveloperCacheAction.OFFLINE_DATA -> atlasText(
                            "Supprime uniquement les produits et données financières/paie hors ligne de ce compte et de ce serveur. La session, les réglages et les données du serveur restent intacts.",
                            "Removes only this account/server's offline product and finance/payroll snapshots. Your session, settings, and server records stay untouched.",
                            "يحذف فقط المنتجات وبيانات المالية/الرواتب المحفوظة دون اتصال لهذا الحساب والخادم. تبقى الجلسة والإعدادات وبيانات الخادم دون تغيير.",
                        )
                        DeveloperCacheAction.FINANCE_REPORTS -> atlasText(
                            "Supprime les rapports PDF temporaires stockés sur cet appareil. Les enregistrements du serveur ne sont pas modifiés.",
                            "Removes temporary finance PDF files stored on this device. Server records are not changed.",
                            "يحذف ملفات تقارير PDF المؤقتة على هذا الجهاز. لا تتغير سجلات الخادم.",
                        )
                        DeveloperCacheAction.UPDATE_PACKAGE -> atlasText(
                            "Supprime uniquement le fichier d'installation téléchargé. L'application installée reste en place; le fichier pourra être téléchargé à nouveau.",
                            "Removes only the downloaded installer. The installed app stays in place; you can download the update again.",
                            "يحذف ملف التثبيت المُنزّل فقط. يبقى التطبيق المثبت كما هو ويمكن تنزيل التحديث مجدداً.",
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !cacheActionInProgress,
                    onClick = {
                        cacheActionInProgress = true
                        when (action) {
                            DeveloperCacheAction.OFFLINE_DATA -> appViewModel.clearOfflineCache { message ->
                                cacheActionInProgress = false
                                pendingCacheAction = null
                                onToast(atlasNotice(message, language))
                            }
                            DeveloperCacheAction.FINANCE_REPORTS -> appViewModel.clearFinanceReportCache { message ->
                                cacheActionInProgress = false
                                pendingCacheAction = null
                                onToast(atlasNotice(message, language))
                            }
                            DeveloperCacheAction.UPDATE_PACKAGE -> appViewModel.clearCachedUpdateFiles { cleared ->
                                cacheActionInProgress = false
                                pendingCacheAction = null
                                onToast(if (cleared) updatePackageRemoved else updatePackageRemoveFailed)
                            }
                        }
                    },
                ) {
                    if (cacheActionInProgress) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(atlasText("Supprimer", "Clear", "حذف"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(enabled = !cacheActionInProgress, onClick = { pendingCacheAction = null }) {
                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                }
            },
        )
    }
}

private enum class DeveloperCacheAction {
    OFFLINE_DATA,
    FINANCE_REPORTS,
    UPDATE_PACKAGE,
}

@Composable
private fun DeveloperApiDiagnosticsCard(
    deviceReportsInternetCapability: Boolean,
    hasConfiguredServer: Boolean,
    configuredServer: String,
    report: ProbeReport?,
    loading: Boolean,
    onRunProbe: () -> Unit,
) {
    AtlasCard {
        Text(
            atlasText("Réseau et API", "Network and API", "الشبكة وواجهة API"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            atlasText(
                "Android signale une capacité réseau; le test vérifie réellement le point public /api/health, sans session ni jeton.",
                "Android reports a network capability; the test actually checks the public /api/health endpoint without a session or token.",
                "يعرض Android قدرة الشبكة؛ ويتحقق الاختبار فعلياً من نقطة /api/health العامة دون جلسة أو رمز.",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AtlasInfoRow(
            atlasText("Capacité réseau Android", "Android network capability", "قدرة شبكة Android"),
            if (deviceReportsInternetCapability) {
                atlasText("Le réseau actif annonce Internet", "Active network reports internet capability", "الشبكة النشطة تعلن توفر الإنترنت")
            } else {
                atlasText("Aucune capacité Internet active", "No active internet capability", "لا توجد قدرة إنترنت نشطة")
            },
        )
        AtlasInfoRow(
            atlasText("Serveur configuré", "Configured server", "الخادم المحدد"),
            configuredServer,
        )
        TextButton(onClick = onRunProbe, enabled = !loading && hasConfiguredServer) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            Text(atlasText("Tester l'API maintenant", "Test API now", "اختبر واجهة API الآن"))
        }
        report?.let { result ->
            AtlasInfoRow(
                atlasText("Point de santé", "Health endpoint", "نقطة الصحة"),
                if (result.ok) atlasText("Réponse valide", "Valid response", "استجابة صالحة")
                else atlasText("Échec de vérification", "Check failed", "فشل التحقق"),
            )
            if (result.latencyMs > 0L) {
                AtlasInfoRow(
                    atlasText("Latence", "Latency", "زمن الاستجابة"),
                    "${result.latencyMs} ms",
                )
            }
            if (result.ok) {
                result.environment?.takeIf { it.isNotBlank() }?.let { environment ->
                    AtlasInfoRow(atlasText("Environnement", "Environment", "البيئة"), environment)
                }
                AtlasInfoRow(
                    atlasText("Base de données", "Database", "قاعدة البيانات"),
                    if (result.database) atlasText("Disponible", "Available", "متاحة")
                    else atlasText("Indisponible", "Unavailable", "غير متاحة"),
                )
                AtlasInfoRow(
                    atlasText("Stockage objet", "Object storage", "تخزين الكائنات"),
                    if (result.storage) atlasText("Disponible", "Available", "متاح")
                    else atlasText("Indisponible", "Unavailable", "غير متاح"),
                )
            }
            Text(
                if (result.ok) atlasText(
                    "Le serveur Atlas a répondu correctement.",
                    "The Atlas server responded successfully.",
                    "استجاب خادم Atlas بنجاح.",
                ) else atlasNotice(result.message, LocalAtlasLanguage.current),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (result.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DeveloperCacheToolsCard(
    summary: LocalStorageSummary,
    cachedProductCount: Int,
    hasActiveSession: Boolean,
    actionInProgress: Boolean,
    onRequestClear: (DeveloperCacheAction) -> Unit,
) {
    AtlasCard {
        Text(
            atlasText("Cache et stockage local", "Cache and local storage", "الذاكرة المؤقتة والتخزين المحلي"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            atlasText(
                "Les actions ci-dessous sont séparées et demandent confirmation. Elles ne suppriment aucune donnée du serveur.",
                "The actions below are separate and require confirmation. They do not delete server data.",
                "الإجراءات أدناه منفصلة وتتطلب تأكيداً. لا تحذف بيانات الخادم.",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AtlasInfoRow(atlasText("Cache de l'application", "App cache", "ذاكرة التطبيق المؤقتة"), formatDeveloperBytes(summary.cacheBytes))
        AtlasInfoRow(
            atlasText("Fichiers hors ligne (tous les comptes)", "Offline files (all accounts)", "ملفات دون اتصال (كل الحسابات)"),
            formatDeveloperBytes(summary.offlineCatalogBytes),
        )
        AtlasInfoRow(
            atlasText("Produits hors ligne actifs", "Active offline products", "المنتجات النشطة دون اتصال"),
            cachedProductCount.toString(),
        )
        AtlasInfoRow(
            atlasText("Rapports PDF temporaires", "Temporary finance PDFs", "تقارير PDF المالية المؤقتة"),
            "${summary.financeReportCount} · ${formatDeveloperBytes(summary.financeReportBytes)}",
        )
        AtlasInfoRow(
            atlasText("Installateur téléchargé", "Downloaded installer", "ملف التثبيت المُنزّل"),
            formatDeveloperBytes(summary.updateDownloadBytes),
        )
        AtlasInfoRow(
            atlasText("Journaux de diagnostic", "Diagnostic logs", "سجلات التشخيص"),
            formatDeveloperBytes(summary.diagnosticsBytes),
        )
        summary.deviceFreeBytes?.let { freeBytes ->
            AtlasInfoRow(atlasText("Espace libre sur l'appareil", "Device free space", "المساحة المتاحة على الجهاز"), formatDeveloperBytes(freeBytes))
        }
        TextButton(
            enabled = hasActiveSession && !actionInProgress,
            onClick = { onRequestClear(DeveloperCacheAction.OFFLINE_DATA) },
        ) {
            Text(atlasText("Vider les données hors ligne", "Clear offline data", "مسح البيانات دون اتصال"), color = MaterialTheme.colorScheme.error)
        }
        TextButton(
            enabled = !actionInProgress && (summary.financeReportCount > 0 || summary.financeReportBytes > 0L),
            onClick = { onRequestClear(DeveloperCacheAction.FINANCE_REPORTS) },
        ) {
            Text(atlasText("Supprimer les rapports PDF", "Clear finance PDFs", "مسح تقارير PDF"), color = MaterialTheme.colorScheme.error)
        }
        TextButton(
            enabled = !actionInProgress && summary.updateDownloadBytes > 0L,
            onClick = { onRequestClear(DeveloperCacheAction.UPDATE_PACKAGE) },
        ) {
            Text(atlasText("Supprimer l'installateur téléchargé", "Remove downloaded installer", "حذف ملف التثبيت المُنزّل"), color = MaterialTheme.colorScheme.error)
        }
    }
}

private data class DeveloperDiagnosticExport(
    val snapshot: DeviceDiagnosticSnapshot,
    val logs: String,
    val markdown: String,
)

private suspend fun collectDeveloperDiagnosticExport(
    context: android.content.Context,
    appViewModel: AppViewModel,
    role: String?,
    apiReport: ProbeReport?,
): DeveloperDiagnosticExport {
    val baseSnapshot = DiagnosticReporter.collectSnapshot(
        context = context,
        shortDeviceId = appViewModel.deviceShortId,
        fullFingerprint = appViewModel.deviceFingerprint,
        role = role,
        screenOrigin = "Developer diagnostics",
    )
    val apiSummary = apiReport?.let { report ->
        "Atlas API ${if (report.ok) "reachable" else "unavailable"}" +
            report.latencyMs.takeIf { it > 0L }?.let { " · ${it}ms" }.orEmpty() +
            if (report.ok) " · database=${report.database} · storage=${report.storage}" else ""
    }
    val snapshot = if (apiSummary.isNullOrBlank()) baseSnapshot else {
        baseSnapshot.copy(networkStatus = "${baseSnapshot.networkStatus} · $apiSummary")
    }
    val safeLogs = AtlasLogBuffer.sanitize(
        AtlasLogBuffer.captureDiagnosticLogsAsync(context, maxChars = 32_000),
    )
    val markdown = DiagnosticReporter.buildMarkdownReport(
        category = "Developer diagnostics",
        title = "Atlas app diagnostics",
        description = "User-requested local diagnostic export.",
        stepsToReproduce = "",
        snapshot = snapshot,
        logs = safeLogs,
        attachments = emptyList(),
    )
    return DeveloperDiagnosticExport(snapshot, safeLogs, markdown)
}

private fun formatDeveloperBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KiB", "MiB", "GiB", "TiB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return if (unitIndex == 0) "$bytes ${units[unitIndex]}"
    else String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}
