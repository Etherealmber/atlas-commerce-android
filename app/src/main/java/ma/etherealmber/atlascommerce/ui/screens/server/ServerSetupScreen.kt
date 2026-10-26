package ma.etherealmber.atlascommerce.ui.screens.server

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasBrandWordmark
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.toneBackground
import ma.etherealmber.atlascommerce.ui.components.toneForeground
import ma.etherealmber.atlascommerce.ui.i18n.AtlasLanguageSelector
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun ServerSetupScreen(
    onClose: (() -> Unit)? = null,
    viewModel: ServerSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedServers by viewModel.savedServers.collectAsStateWithLifecycle()
    val density = Atlas.density
    val devPresets = ServerUrl.presets()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = density.screenPadding, vertical = density.sectionGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"))
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            AtlasLanguageSelector()
        }

        AtlasEntrance(delayMillis = 0) {
            AtlasBrandWordmark(
                modifier = Modifier.fillMaxWidth(),
                height = 68.dp,
            )
        }
        Spacer(Modifier.height(18.dp))
        AtlasEntrance(delayMillis = 55) {
            Column {
                Text(
                    text = atlasText("Connectez votre serveur", "Connect your server", "اربط الخادم الخاص بك"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = atlasText("Cette application est liée à la plateforme web Atlas Commerce (à déployer en premier). Saisissez l'adresse exacte de votre site — si le site semble indisponible, vérifiez qu'il n'y a aucune faute de frappe.", "This app connects to the Atlas Commerce web platform, which must be deployed first. Enter your exact site address. If it appears unavailable, double-check the spelling.", "يرتبط هذا التطبيق بمنصة Atlas Commerce على الويب، ويجب نشرها أولًا. أدخل عنوان موقعك بدقة. إذا لم يظهر الموقع متاحًا، فتحقّق من كتابة العنوان."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(density.sectionGap))

        if (savedServers.isNotEmpty()) {
            Text(
                text = atlasText("MES SERVEURS ENREGISTRÉS", "SAVED SERVERS", "الخوادم المحفوظة"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            savedServers.forEach { saved ->
                val selected = state.normalizedInput.equals(saved.url, ignoreCase = true)
                Surface(
                    onClick = { viewModel.choosePreset(saved.url, saved.label) },
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
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.choosePreset(saved.url, saved.label) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = saved.label.ifBlank { ServerUrl.display(saved.url) },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = ServerUrl.display(saved.url),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.forgetServer(saved) }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = atlasText("Oublier ce serveur", "Forget this server", "نسيان هذا الخادم"),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        if (devPresets.isNotEmpty()) {
            Text(
                text = atlasText("ENVIRONNEMENTS LOCAUX (DEBUG)", "LOCAL ENVIRONMENTS (DEBUG)", "البيئات المحلية (تصحيح)"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            devPresets.forEach { preset ->
                val selected = state.normalizedInput == ServerUrl.normalize(preset.url)
                Surface(
                    onClick = { viewModel.choosePreset(preset.url, preset.label) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Atlas.palette.cardBorder,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = { viewModel.choosePreset(preset.url, preset.label) })
                        Column(Modifier.weight(1f)) {
                            Text(preset.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(preset.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = atlasText("ADRESSE DU SERVEUR", "SERVER ADDRESS", "عنوان الخادم"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        AtlasTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            label = atlasText("Domaine ou URL HTTPS", "Domain or HTTPS URL", "النطاق أو رابط HTTPS"),
            placeholder = "https://votre-domaine.exemple.com",
            leadingIcon = Icons.Outlined.Dns,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next,
            supportingText = atlasText("Connexion HTTPS chiffrée obligatoire en production.", "Encrypted HTTPS is required in production.", "اتصال HTTPS المشفّر مطلوب في الإصدار النهائي."),
        )

        Spacer(Modifier.height(10.dp))
        AtlasCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = atlasText("Enregistrer ce serveur", "Save this server", "حفظ هذا الخادم"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = atlasText("Garde l'adresse sur cet appareil pour basculer rapidement entre plusieurs espaces.", "Keep this address on this device to switch between workspaces quickly.", "احتفظ بالعنوان على هذا الجهاز للتبديل سريعًا بين مساحات العمل."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = state.rememberServer,
                    onCheckedChange = viewModel::onRememberServerChange,
                )
            }
            if (state.rememberServer) {
                Spacer(Modifier.height(10.dp))
                AtlasTextField(
                    value = state.label,
                    onValueChange = viewModel::onLabelChange,
                    label = atlasText("Nom du serveur (optionnel)", "Server name (optional)", "اسم الخادم (اختياري)"),
                    placeholder = atlasText("Ex. Boutique principale, Dépôt Nord…", "e.g. Main shop, North warehouse…", "مثال: المتجر الرئيسي، مستودع الشمال…"),
                    leadingIcon = Icons.Outlined.BookmarkBorder,
                    imeAction = ImeAction.Done,
                    onDone = viewModel::test,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasOutlinedButton(
                text = atlasText("Tester la connexion", "Test connection", "اختبار الاتصال"),
                onClick = viewModel::test,
                modifier = Modifier.weight(1f),
                enabled = !state.testing && state.input.isNotBlank(),
                icon = Icons.Outlined.Dns,
            )
        }

        if (state.testing) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = atlasText("Vérification de /api/health…", "Checking /api/health…", "جارٍ التحقق من /api/health…"),
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
            AtlasErrorCard(message = state.error.orEmpty(), title = atlasText("Adresse refusée", "Address rejected", "تم رفض العنوان"))
        }

        Spacer(Modifier.height(density.sectionGap))
        AtlasButton(
            text = if (state.saving) atlasText("Enregistrement…", "Saving…", "جارٍ الحفظ…") else atlasText("Continuer vers la connexion", "Continue to sign in", "المتابعة إلى تسجيل الدخول"),
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
                text = atlasText("Sessions, paniers et caches sont isolés par serveur et par utilisateur.", "Sessions, carts and caches are isolated by server and user.", "الجلسات وسلال التسوق والذاكرة المؤقتة معزولة لكل خادم ومستخدم."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(density.sectionGap))
        AtlasCard {
            Text(
                text = atlasText("Nouveau sur Atlas Commerce ? (Étape 1 requise)", "New to Atlas Commerce? (Step 1 required)", "هل أنت جديد في Atlas Commerce؟ (الخطوة الأولى مطلوبة)"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "1. Installez et déployez d'abord l'application web Atlas Commerce sur votre domaine HTTPS.\n" +
                    "2. Entrez votre URL exacte ci-dessus puis touchez « Tester la connexion ».\n" +
                    "3. Si le site apparaît indisponible, vérifiez attentivement l'orthographe de ce que vous avez tapé.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = AppDetails.WEB_REPO,
                style = MaterialTheme.typography.labelMedium,
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
                    text = if (report.ok) atlasText("Serveur Atlas vérifié", "Atlas server verified", "تم التحقق من خادم Atlas") else atlasText("Vérification échouée", "Verification failed", "فشل التحقق"),
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
                        text = if (report.database) atlasText("Base D1 OK", "D1 database OK", "قاعدة بيانات D1 سليمة") else atlasText("Base non vérifiée", "Database not verified", "لم يتم التحقق من قاعدة البيانات"),
                        tone = if (report.database) AtlasTone.POSITIVE else AtlasTone.WARNING,
                    )
                    AtlasChip(
                        text = if (report.storage) atlasText("Stockage R2 OK", "R2 storage OK", "تخزين R2 سليم") else atlasText("Stockage non vérifié", "Storage not verified", "لم يتم التحقق من التخزين"),
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
