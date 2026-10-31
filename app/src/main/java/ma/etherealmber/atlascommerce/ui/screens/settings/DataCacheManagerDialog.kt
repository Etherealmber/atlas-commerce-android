package ma.etherealmber.atlascommerce.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.persist.SavedServerEntry
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.root.roundUpTo50MiB
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

private sealed interface DataManagerAction {
    data class ForgetAccount(val account: SavedAccountEntry) : DataManagerAction
    data class ForgetServer(val server: SavedServerEntry) : DataManagerAction
    data object ClearOfflineCatalog : DataManagerAction
    data object ClearTemporaryReports : DataManagerAction
    data object ClearAllLocalData : DataManagerAction
}

/** Local-only storage controls, available from the signed-in Profile top bar. */
@Composable
fun DataCacheManagerDialog(
    onDismiss: () -> Unit,
    onToast: (String) -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val savedServers by appViewModel.savedServers.collectAsStateWithLifecycle()
    val savedAccounts by appViewModel.allSavedAccounts.collectAsStateWithLifecycle()
    val storage by appViewModel.localStorageSummary.collectAsStateWithLifecycle()
    val cachedProductCount by appViewModel.cachedProductCount.collectAsStateWithLifecycle()
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val densityMode by appViewModel.densityMode.collectAsStateWithLifecycle()
    val appLanguage by appViewModel.appLanguage.collectAsStateWithLifecycle()
    val followsSystemLanguage by appViewModel.appLanguageFollowsSystem.collectAsStateWithLifecycle()
    val autoUpdates by appViewModel.autoCheckUpdates.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<DataManagerAction?>(null) }

    LaunchedEffect(Unit) {
        appViewModel.refreshLocalStorageSummary()
        appViewModel.refreshCachedProductCount()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            atlasText("Données et cache", "Data & cache manager", "مدير البيانات والذاكرة المؤقتة"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            atlasText("Stockage privé de cet appareil", "This device's private storage", "التخزين الخاص بهذا الجهاز"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AtlasDivider()
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        atlasText(
                            "Gérez uniquement les éléments conservés sur cet appareil. Les données de l'entreprise sur le serveur ne sont ni affichées ni modifiées. Les mots de passe et jetons ne sont jamais montrés.",
                            "Manage items stored only on this device. Company data on the server is not shown or changed. Passwords and tokens are never displayed.",
                            "أدر العناصر المحفوظة على هذا الجهاز فقط. لا يتم عرض بيانات الشركة على الخادم أو تغييرها. لا تظهر كلمات المرور أو الرموز مطلقاً.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val recommendedFreeBytes = roundUpTo50MiB(storage.recommendedFreeBytes)
                    val lowDeviceStorage = storage.deviceFreeBytes?.let { it < recommendedFreeBytes } == true
                    if (lowDeviceStorage) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        atlasText("Espace de stockage faible", "Low device storage", "مساحة تخزين الجهاز منخفضة"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                                Text(
                                    atlasText(
                                        "Atlas recommande au moins ${Formatter.formatShortFileSize(context, recommendedFreeBytes)} d'espace libre pour fonctionner normalement. Il reste ${Formatter.formatShortFileSize(context, storage.deviceFreeBytes ?: 0L)}. Libérez de l'espace sur l'appareil ou utilisez le nettoyage local ci-dessous.",
                                        "Atlas recommends at least ${Formatter.formatShortFileSize(context, recommendedFreeBytes)} free for normal operation. ${Formatter.formatShortFileSize(context, storage.deviceFreeBytes ?: 0L)} is available. Free space on your device or use the local cleanup options below.",
                                        "يوصي Atlas بتوفير ${Formatter.formatShortFileSize(context, recommendedFreeBytes)} على الأقل للعمل بشكل طبيعي. المتاح حالياً ${Formatter.formatShortFileSize(context, storage.deviceFreeBytes ?: 0L)}. أفرغ مساحة على الجهاز أو استخدم خيارات التنظيف أدناه.",
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                TextButton(onClick = {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_STORAGE)) }
                                        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
                                }) {
                                    Text(atlasText("Libérer de l'espace", "Free up space", "إخلاء مساحة"))
                                }
                            }
                        }
                    }
                    ManagerSection(title = atlasText("Stockage", "Storage", "التخزين"), icon = Icons.Outlined.Storage) {
                        ManagerValueRow(atlasText("Espace libre sur l'appareil", "Device free space", "المساحة الحرة على الجهاز"), storage.deviceFreeBytes?.let { Formatter.formatShortFileSize(context, it) } ?: atlasText("Calcul…", "Checking…", "جارٍ الحساب…"))
                        ManagerValueRow(atlasText("Recommandé pour Atlas", "Recommended for Atlas", "الموصى به لـ Atlas"), Formatter.formatShortFileSize(context, recommendedFreeBytes))
                        ManagerValueRow(atlasText("Stockage privé total", "Total app storage", "إجمالي تخزين التطبيق"), Formatter.formatShortFileSize(context, storage.totalAppBytes))
                        ManagerValueRow(atlasText("Cache", "Cache", "الذاكرة المؤقتة"), Formatter.formatShortFileSize(context, storage.cacheBytes))
                        ManagerValueRow(
                            atlasText("Catalogue hors ligne", "Offline catalog", "الكتالوج دون اتصال"),
                            "$cachedProductCount · ${Formatter.formatShortFileSize(context, storage.offlineCatalogBytes)}",
                        )
                        ManagerValueRow(
                            atlasText("Rapports PDF temporaires", "Temporary PDF reports", "تقارير PDF المؤقتة"),
                            "${storage.financeReportCount} · ${Formatter.formatShortFileSize(context, storage.financeReportBytes)}",
                        )
                        ManagerValueRow(atlasText("Journaux de diagnostic", "Diagnostic logs", "سجلات التشخيص"), Formatter.formatShortFileSize(context, storage.diagnosticsBytes))
                        ManagerValueRow(atlasText("Téléchargements de mise à jour", "Update downloads", "تنزيلات التحديث"), Formatter.formatShortFileSize(context, storage.updateDownloadBytes))
                    }
                    ManagerSection(title = atlasText("Comptes enregistrés (${savedAccounts.size})", "Saved accounts (${savedAccounts.size})", "الحسابات المحفوظة (${savedAccounts.size})"), icon = Icons.Outlined.Inventory2) {
                        if (savedAccounts.isEmpty()) ManagerEmpty(atlasText("Aucun compte enregistré.", "No saved accounts.", "لا توجد حسابات محفوظة."))
                        savedAccounts.forEach { account ->
                            ManagerEntry(
                                title = account.displayName.ifBlank { account.username },
                                detail = "@${account.username} · ${ServerUrl.display(account.serverUrl)}",
                                actionLabel = atlasText("Oublier", "Forget", "إزالة"),
                                onAction = { pendingAction = DataManagerAction.ForgetAccount(account) },
                            )
                        }
                    }
                    ManagerSection(title = atlasText("Serveurs enregistrés (${savedServers.size})", "Saved servers (${savedServers.size})", "الخوادم المحفوظة (${savedServers.size})"), icon = Icons.Outlined.Storage) {
                        if (savedServers.isEmpty()) ManagerEmpty(atlasText("Aucun serveur enregistré.", "No saved servers.", "لا توجد خوادم محفوظة."))
                        savedServers.forEach { server ->
                            ManagerEntry(
                                title = server.label.ifBlank { ServerUrl.display(server.url) },
                                detail = ServerUrl.display(server.url),
                                actionLabel = atlasText("Oublier", "Forget", "إزالة"),
                                onAction = { pendingAction = DataManagerAction.ForgetServer(server) },
                            )
                        }
                    }
                    ManagerSection(title = atlasText("Préférences", "Settings", "الإعدادات"), icon = Icons.Outlined.Storage) {
                        ManagerValueRow(atlasText("Thème", "Theme", "المظهر"), when (themeMode) {
                            ThemeMode.SYSTEM -> atlasText("Système", "System", "النظام")
                            ThemeMode.LIGHT -> atlasText("Clair", "Light", "فاتح")
                            ThemeMode.DARK -> atlasText("Sombre", "Dark", "داكن")
                        })
                        ManagerValueRow(atlasText("Langue", "Language", "اللغة"), if (followsSystemLanguage) atlasText("Système", "System", "النظام") else appLanguage.nativeLabel)
                        ManagerValueRow(atlasText("Densité", "Display density", "كثافة العرض"), when (densityMode) {
                            DensityMode.COMPACT -> atlasText("Compact", "Compact", "مضغوط")
                            DensityMode.COMFORT -> atlasText("Confort", "Comfort", "مريح")
                        })
                        ManagerValueRow(atlasText("Mises à jour automatiques", "Automatic updates", "التحديثات التلقائية"), if (autoUpdates) atlasText("Activées", "On", "مفعلة") else atlasText("Désactivées", "Off", "متوقفة"))
                    }
                    ManagerSection(title = atlasText("Nettoyage", "Cleanup", "التنظيف"), icon = Icons.Outlined.DeleteOutline) {
                        ManagerActionRow(
                            title = atlasText("Vider le catalogue hors ligne", "Clear offline catalog", "مسح الكتالوج دون اتصال"),
                            detail = atlasText("Les produits seront rechargés lors de la prochaine utilisation en ligne.", "Products will be downloaded again the next time you are online.", "سيتم تنزيل المنتجات مجدداً عند الاتصال بالإنترنت."),
                            enabled = cachedProductCount > 0,
                            onClick = { pendingAction = DataManagerAction.ClearOfflineCatalog },
                        )
                        ManagerActionRow(
                            title = atlasText("Supprimer les PDF temporaires", "Delete temporary PDFs", "حذف ملفات PDF المؤقتة"),
                            detail = atlasText("Les copies déjà exportées ne seront pas touchées.", "Copies already exported elsewhere will not be affected.", "لن تتأثر النسخ التي تم تصديرها مسبقاً."),
                            enabled = storage.financeReportCount > 0,
                            onClick = { pendingAction = DataManagerAction.ClearTemporaryReports },
                        )
                        ManagerActionRow(
                            title = atlasText("Effacer toutes les données locales…", "Clear all local data…", "مسح جميع البيانات المحلية…"),
                            detail = atlasText("Déconnecte ce compte et réinitialise les préférences de cet appareil.", "Signs out and resets this device's app data.", "يسجل الخروج ويعيد ضبط بيانات التطبيق على هذا الجهاز."),
                            enabled = true,
                            onClick = { pendingAction = DataManagerAction.ClearAllLocalData },
                            destructive = true,
                        )
                    }
                }
            }
        }
    }

    pendingAction?.let { action ->
        val title: String
        val message: String
        when (action) {
            is DataManagerAction.ForgetAccount -> {
                title = atlasText("Oublier ce compte ?", "Forget this account?", "إزالة هذا الحساب؟")
                message = atlasText("Supprimer les informations enregistrées pour @${action.account.username} de cet appareil ?", "Remove the saved sign-in entry for @${action.account.username} from this device?", "هل تريد إزالة معلومات تسجيل الدخول المحفوظة لـ @${action.account.username} من هذا الجهاز؟")
            }
            is DataManagerAction.ForgetServer -> {
                title = atlasText("Oublier ce serveur ?", "Forget this server?", "إزالة هذا الخادم؟")
                message = atlasText("Supprimer ${ServerUrl.display(action.server.url)} de la liste locale des serveurs ?", "Remove ${ServerUrl.display(action.server.url)} from this device's saved server list?", "هل تريد إزالة ${ServerUrl.display(action.server.url)} من قائمة الخوادم المحفوظة؟")
            }
            DataManagerAction.ClearOfflineCatalog -> {
                title = atlasText("Vider le catalogue hors ligne ?", "Clear offline catalog?", "مسح الكتالوج دون اتصال؟")
                message = atlasText("Les produits devront être rechargés depuis le serveur.", "Products will need to be downloaded again from the server.", "يجب تنزيل المنتجات مجدداً من الخادم.")
            }
            DataManagerAction.ClearTemporaryReports -> {
                title = atlasText("Supprimer les PDF temporaires ?", "Delete temporary PDFs?", "حذف ملفات PDF المؤقتة؟")
                message = atlasText("Seuls les rapports conservés dans le cache privé de l'application seront supprimés.", "Only reports in this app's private cache will be deleted.", "سيتم حذف التقارير الموجودة في ذاكرة التخزين المؤقت الخاصة بالتطبيق فقط.")
            }
            DataManagerAction.ClearAllLocalData -> {
                title = atlasText("Effacer les données de cet appareil ?", "Clear this device's app data?", "مسح بيانات التطبيق على هذا الجهاز؟")
                message = atlasText("La session, les comptes/serveurs enregistrés et les préférences locales seront supprimés. Aucune donnée de l'entreprise sur le serveur ne sera modifiée.", "The session, saved accounts/servers, and local preferences will be removed. No company data on the server will be changed.", "سيتم حذف الجلسة والحسابات والخوادم المحفوظة والتفضيلات المحلية. لن تتغير بيانات الشركة على الخادم.")
            }
        }
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction = null
                    when (action) {
                        is DataManagerAction.ForgetAccount -> appViewModel.forgetSavedAccount(action.account, onToast)
                        is DataManagerAction.ForgetServer -> appViewModel.forgetSavedServer(action.server, onToast)
                        DataManagerAction.ClearOfflineCatalog -> appViewModel.clearOfflineCache(onToast)
                        DataManagerAction.ClearTemporaryReports -> appViewModel.clearFinanceReportCache(onToast)
                        DataManagerAction.ClearAllLocalData -> appViewModel.clearAllLocalAppData {
                            onDismiss()
                            onToast(it)
                        }
                    }
                    appViewModel.refreshLocalStorageSummary()
                }) { Text(atlasText("Supprimer", "Delete", "حذف"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        )
    }


}

@Composable
private fun ManagerSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun ManagerValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ManagerEmpty(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ManagerEntry(title: String, detail: String, actionLabel: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun ManagerActionRow(title: String, detail: String, enabled: Boolean, onClick: () -> Unit, destructive: Boolean = false) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (destructive) Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    }
}
