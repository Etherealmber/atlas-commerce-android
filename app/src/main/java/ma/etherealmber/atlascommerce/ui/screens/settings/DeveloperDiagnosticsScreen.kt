package ma.etherealmber.atlascommerce.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val developerMode by appViewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val passkeyAvailable by appViewModel.androidPasskeysAvailable.collectAsStateWithLifecycle()
    val cachedProductCount by appViewModel.cachedProductCount.collectAsStateWithLifecycle()

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
                Text(
                    atlasText("État de l'application", "App status", "حالة التطبيق"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                AtlasInfoRow(
                    atlasText("Version", "Version", "الإصدار"),
                    "${AppDetails.VERSION_NAME} · ${AppDetails.VERSION_CODE}",
                )
                AtlasInfoRow(
                    atlasText("Passkeys", "Passkeys", "مفاتيح المرور"),
                    if (passkeyAvailable) atlasText("Disponibles", "Available", "متاحة")
                    else atlasText("Non disponibles", "Unavailable", "غير متاحة"),
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
                    atlasText("Rapports Firebase", "Firebase reports", "تقارير Firebase"),
                    when {
                        !CrashReportConsent.isConfigured(context) -> atlasText("Non configurés", "Not configured", "غير مهيأة")
                        CrashReportConsent.isEnabled(context) -> atlasText("Activés avec consentement", "Enabled with consent", "مفعّلة بموافقة المستخدم")
                        else -> atlasText("Désactivés", "Disabled", "معطّلة")
                    },
                )
                Text(
                    atlasText(
                        "Les journaux techniques restent locaux et ne sont jamais affichés ici. Ils ne sont partagés qu'après l'envoi volontaire d'un rapport.",
                        "Technical logs stay local and are never displayed here. They are shared only when you choose to submit a report.",
                        "تبقى السجلات التقنية محلية ولا تُعرض هنا. لا تُشارك إلا عند اختيارك إرسال بلاغ.",
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
