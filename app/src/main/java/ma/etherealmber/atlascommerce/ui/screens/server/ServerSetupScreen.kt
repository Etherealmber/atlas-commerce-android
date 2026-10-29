package ma.etherealmber.atlascommerce.ui.screens.server

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
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
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.theme.AtlasAuthTheme

@Composable
fun ServerSetupScreen(
    onClose: (() -> Unit)? = null,
    viewModel: ServerSetupViewModel = hiltViewModel(),
) {
    AtlasAuthTheme { ServerSetupScreenContent(onClose, viewModel) }
}

@Composable
private fun ServerSetupScreenContent(
    onClose: (() -> Unit)?,
    viewModel: ServerSetupViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedServers by viewModel.savedServers.collectAsStateWithLifecycle()
    val density = Atlas.density
    val devPresets = ServerUrl.presets()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071110), Color(0xFF173B37), Color(0xFF3A7069))))
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
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"), tint = Color.White)
                }
            } else {
                AtlasBrandWordmark(
                    modifier = Modifier.widthIn(max = 190.dp),
                    height = 38.dp,
                    tint = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            AtlasLanguageSelector(contentColor = Color.White)
        }

        AtlasEntrance(delayMillis = 55) {
            Column {
                Text(
                    text = atlasText("Connectez votre serveur", "Connect your server", "اربط الخادم الخاص بك"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = atlasText("Saisissez l'adresse du site Atlas Commerce de votre organisation.", "Enter your organization's Atlas Commerce site address.", "أدخل عنوان موقع Atlas Commerce الخاص بمؤسستك."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
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
                                    Text(atlasText(preset.label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(atlasText(preset.hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                Spacer(Modifier.height(density.sectionGap))
                Text(
                    text = atlasText(
                        "${AppDetails.NAME} ${AppDetails.VERSION_NAME} · Licence ${AppDetails.LICENSE_NAME}",
                        "${AppDetails.NAME} ${AppDetails.VERSION_NAME} · License ${AppDetails.LICENSE_NAME}",
                        "${AppDetails.NAME} ${AppDetails.VERSION_NAME} · الترخيص ${AppDetails.LICENSE_NAME}",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
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
                text = atlasNotice(report.message, LocalAtlasLanguage.current),
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
