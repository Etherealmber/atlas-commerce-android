package ma.etherealmber.atlascommerce.ui.screens.server

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ProbeReport
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.SavedServerEntry
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
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas

private enum class ServerSetupStep { PICK_SAVED, ADDRESS }

@Composable
fun ServerSetupScreen(
    onClose: (() -> Unit)? = null,
    viewModel: ServerSetupViewModel = hiltViewModel(),
) {
    ServerSetupScreenContent(onClose, viewModel)
}

@Composable
private fun ServerSetupScreenContent(
    onClose: (() -> Unit)?,
    viewModel: ServerSetupViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedServers by viewModel.savedServers.collectAsStateWithLifecycle()
    val serverConfigLoaded by viewModel.serverConfigLoaded.collectAsStateWithLifecycle()
    val savedServersLoaded by viewModel.savedServersLoaded.collectAsStateWithLifecycle()
    val density = Atlas.density
    val devPresets = ServerUrl.presets()
    var step by remember { mutableStateOf<ServerSetupStep?>(null) }
    var forgetTarget by remember { mutableStateOf<SavedServerEntry?>(null) }

    LaunchedEffect(serverConfigLoaded, savedServersLoaded, savedServers) {
        if (serverConfigLoaded && savedServersLoaded && step == null) {
            step = if (savedServers.isNotEmpty()) ServerSetupStep.PICK_SAVED else ServerSetupStep.ADDRESS
        }
    }

    val isPickingSaved = step == ServerSetupStep.PICK_SAVED
    val foreground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))
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
                IconButton(onClick = {
                    if (!isPickingSaved && savedServers.isNotEmpty()) step = ServerSetupStep.PICK_SAVED
                    else onClose()
                }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"), tint = foreground)
                }
            } else {
                AtlasBrandWordmark(modifier = Modifier.widthIn(max = 190.dp), height = 38.dp, tint = foreground)
            }
            Spacer(Modifier.weight(1f))
            AtlasLanguageSelector(contentColor = foreground)
        }

        AtlasEntrance(delayMillis = 55) {
            Column {
                Text(
                    text = if (isPickingSaved) atlasText("Choisissez un serveur", "Choose a server", "اختر خادماً") else atlasText("Connectez votre serveur", "Connect your server", "اربط الخادم الخاص بك"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = foreground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isPickingSaved) {
                        atlasText("Sélectionnez un espace enregistré ou ajoutez une nouvelle adresse.", "Choose a saved workspace or add a new server address.", "اختر مساحة محفوظة أو أضف عنوان خادم جديداً.")
                    } else {
                        atlasText("Entrez l'adresse de votre organisation. Vous pourrez l'enregistrer, ou continuer sans la mémoriser.", "Enter your organization's server address. Save it if you like, or continue without saving it.", "أدخل عنوان خادم مؤسستك. يمكنك حفظه أو المتابعة دون حفظه.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                when {
                    step == null -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    isPickingSaved -> {
                        Text(
                            atlasText("ÉTAPE 1 · SERVEURS ENREGISTRÉS", "STEP 1 · SAVED SERVERS", "الخطوة ١ · الخوادم المحفوظة"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        savedServers.forEach { saved ->
                            val selected = state.current.equals(saved.url, ignoreCase = true)
                            Surface(
                                onClick = { viewModel.continueWithSavedServer(saved) { onClose?.invoke() } },
                                enabled = !state.saving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Atlas.palette.cardBorder),
                            ) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { viewModel.continueWithSavedServer(saved) { onClose?.invoke() } },
                                        enabled = !state.saving,
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(saved.label.ifBlank { ServerUrl.display(saved.url) }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(ServerUrl.display(saved.url), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { forgetTarget = saved }) {
                                        Icon(Icons.Outlined.Close, contentDescription = atlasText("Supprimer ce serveur enregistré", "Remove this saved server", "إزالة هذا الخادم المحفوظ"), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        AtlasOutlinedButton(
                            text = atlasText("Ajouter une adresse de serveur", "Add a server address", "إضافة عنوان خادم"),
                            onClick = {
                                viewModel.onInputChange("")
                                viewModel.onLabelChange("")
                                viewModel.onRememberServerChange(false)
                                step = ServerSetupStep.ADDRESS
                            },
                            icon = Icons.Outlined.Dns,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        if (savedServers.isNotEmpty()) {
                            TextButton(onClick = { step = ServerSetupStep.PICK_SAVED }) {
                                Text(atlasText("Retour aux serveurs enregistrés", "Back to saved servers", "العودة إلى الخوادم المحفوظة"))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        if (savedServers.isNotEmpty()) {
                            Text(atlasText("ÉTAPE 2 · NOUVELLE ADRESSE", "STEP 2 · NEW SERVER", "الخطوة ٢ · خادم جديد"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                        }
                        if (devPresets.isNotEmpty()) {
                            Text(atlasText("ENVIRONNEMENTS LOCAUX (DEBUG)", "LOCAL ENVIRONMENTS (DEBUG)", "البيئات المحلية (تصحيح)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            devPresets.forEach { preset ->
                                val selected = state.normalizedInput == ServerUrl.normalize(preset.url)
                                Surface(
                                    onClick = { viewModel.choosePreset(preset.url, preset.label) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Atlas.palette.cardBorder),
                                ) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        Text(atlasText("ADRESSE DU SERVEUR", "SERVER ADDRESS", "عنوان الخادم"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        AtlasTextField(
                            value = state.input,
                            onValueChange = viewModel::onInputChange,
                            label = atlasText("Domaine ou URL HTTPS", "Domain or HTTPS URL", "النطاق أو رابط HTTPS"),
                            placeholder = "https://votre-domaine.exemple.com",
                            leadingIcon = Icons.Outlined.Dns,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                            supportingText = atlasText("HTTPS chiffré est requis pour les serveurs de production.", "Encrypted HTTPS is required for production servers.", "اتصال HTTPS المشفّر مطلوب لخوادم الإنتاج."),
                            onDone = viewModel::test,
                        )
                        Spacer(Modifier.height(10.dp))
                        AtlasCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(atlasText("Enregistrer ce serveur", "Save this server", "حفظ هذا الخادم"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(atlasText("Facultatif. Vous pourrez le choisir rapidement la prochaine fois.", "Optional. You can select it quickly next time.", "اختياري. يمكنك اختياره بسرعة في المرة القادمة."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = state.rememberServer, onCheckedChange = viewModel::onRememberServerChange)
                            }
                            if (state.rememberServer) {
                                Spacer(Modifier.height(10.dp))
                                AtlasTextField(
                                    value = state.label,
                                    onValueChange = viewModel::onLabelChange,
                                    label = atlasText("Nom du serveur (facultatif)", "Server name (optional)", "اسم الخادم (اختياري)"),
                                    placeholder = atlasText("Ex. Boutique principale", "e.g. Main shop", "مثال: المتجر الرئيسي"),
                                    leadingIcon = Icons.Outlined.BookmarkBorder,
                                    imeAction = ImeAction.Done,
                                    onDone = viewModel::test,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        AtlasOutlinedButton(
                            text = if (state.testing) atlasText("Test en cours…", "Testing…", "جارٍ الاختبار…") else atlasText("Tester la connexion", "Test connection", "اختبار الاتصال"),
                            onClick = viewModel::test,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.testing && state.input.isNotBlank(),
                            icon = Icons.Outlined.Dns,
                        )
                        if (state.testing) {
                            Spacer(Modifier.height(10.dp))
                            Text(atlasText("Vérification du serveur…", "Checking the server…", "جارٍ التحقق من الخادم…"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        state.report?.takeUnless { state.testing }?.let { report ->
                            Spacer(Modifier.height(10.dp))
                            ProbeCard(report)
                        }
                        state.error?.takeIf { it.isNotBlank() }?.let { error ->
                            Spacer(Modifier.height(10.dp))
                            AtlasErrorCard(message = atlasNotice(error, LocalAtlasLanguage.current), title = atlasText("Adresse refusée", "Address rejected", "تم رفض العنوان"))
                        }
                        Spacer(Modifier.height(density.sectionGap))
                        AtlasButton(
                            text = if (state.saving) atlasText("Enregistrement…", "Saving…", "جارٍ الحفظ…") else atlasText("Continuer vers la connexion", "Continue to sign in", "المتابعة إلى تسجيل الدخول"),
                            onClick = { viewModel.continueToLogin { onClose?.invoke() } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.canContinue,
                            loading = state.saving,
                        )
                    }
                }
            }
        }

        if (step != null) {
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

    forgetTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { forgetTarget = null },
            title = { Text(atlasText("Supprimer ce serveur ?", "Remove this server?", "إزالة هذا الخادم؟")) },
            text = { Text(atlasText("${ServerUrl.display(target.url)} sera retiré de la liste enregistrée sur cet appareil.", "${ServerUrl.display(target.url)} will be removed from this device's saved server list.", "ستتم إزالة ${ServerUrl.display(target.url)} من قائمة الخوادم المحفوظة على هذا الجهاز.")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.forgetServer(target)
                    forgetTarget = null
                }) { Text(atlasText("Supprimer", "Remove", "إزالة"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { forgetTarget = null }) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        )
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
