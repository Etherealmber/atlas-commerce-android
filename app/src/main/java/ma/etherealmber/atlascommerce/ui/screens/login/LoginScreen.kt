package ma.etherealmber.atlascommerce.ui.screens.login

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasBrandWordmark
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasUsesSystemLanguage
import ma.etherealmber.atlascommerce.ui.i18n.LocalSetAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.LocalUseSystemAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.theme.AtlasAuthTheme

@Composable
fun LoginScreen(
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit = {},
    onUnlockSavedAccount: (SavedAccountEntry, () -> Unit) -> Unit = { _, _ -> },
    viewModel: LoginViewModel = hiltViewModel(),
) {
    AtlasAuthTheme {
        LoginScreenContent(onChangeServer, onOpenQrDevice, onUnlockSavedAccount, viewModel)
    }
}

@Composable
private fun LoginScreenContent(
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit,
    onUnlockSavedAccount: (SavedAccountEntry, () -> Unit) -> Unit,
    viewModel: LoginViewModel,
) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val language = LocalAtlasLanguage.current
    val setLanguage = LocalSetAtlasLanguage.current
    val followsSystemLanguage = LocalAtlasUsesSystemLanguage.current
    val useSystemLanguage = LocalUseSystemAtlasLanguage.current
    var accountPromptHandled by remember { mutableStateOf(false) }
    var languageDialogOpen by remember { mutableStateOf(false) }
    LaunchedEffect(savedAccounts, state.step) {
        if (accountPromptHandled) return@LaunchedEffect
        if (state.step != LoginViewModel.Step.CREDENTIALS) {
            accountPromptHandled = true
            return@LaunchedEffect
        }
        if (savedAccounts.isNotEmpty()) {
            val savedAccount = savedAccounts.firstOrNull { it.canUseDeviceUnlock } ?: savedAccounts.first()
            // Keep the regular password form visible; local unlock is an explicit alternative below.
            viewModel.onUsernameChange(savedAccount.username)
            accountPromptHandled = true
        } else {
            delay(1_500)
            if (savedAccounts.isEmpty()) accountPromptHandled = true
        }
    }
    val host = server?.let { ServerUrl.display(it.baseUrl) } ?: atlasText("Serveur non sélectionné", "No server selected", "لم يتم اختيار خادم")


    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF09090B), Color(0xFF17171B), Color(0xFF26262C)),
            ),
        ),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compact = maxWidth < 600.dp || maxHeight < 840.dp
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val heroHeight = if (keyboardVisible) 82.dp else if (compact) 108.dp else 136.dp

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoginAtlasHero(
                compact = compact || keyboardVisible,
                host = host,
                onChangeServer = onChangeServer,
                onOpenQrDevice = onOpenQrDevice,
                onOpenLanguage = { languageDialogOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (motionEnabled) Modifier.animateContentSize() else Modifier)
                    .height(heroHeight),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (compact) 20.dp else 28.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (state.step == LoginViewModel.Step.CREDENTIALS) atlasText("Bon retour", "Welcome back", "مرحبًا بعودتك") else atlasText("Vérification", "Verification", "التحقق"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.step == LoginViewModel.Step.CREDENTIALS) {
                        atlasText("Connectez-vous à votre espace Atlas Commerce.", "Sign in to your Atlas Commerce workspace.", "سجّل الدخول إلى مساحة Atlas Commerce الخاصة بك.")
                    } else {
                        atlasText("Confirmez votre identité pour continuer.", "Verify your identity to continue.", "أكّد هويتك للمتابعة.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

                if (state.step == LoginViewModel.Step.CREDENTIALS) {
                    val unlockAccounts = savedAccounts.filter { it.canUseDeviceUnlock }
                    unlockAccounts.forEach { savedAccount ->
                        val accountName = savedAccount.displayName.ifBlank { savedAccount.username }
                        Surface(
                            onClick = {
                                onUnlockSavedAccount(savedAccount) {
                                    viewModel.signInWithSavedAccount(savedAccount)
                                }
                            },
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        atlasText(
                                            "Déverrouiller avec la biométrie",
                                            "Unlock with biometrics",
                                            "إلغاء القفل بالبصمة",
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        accountName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (unlockAccounts.isNotEmpty()) {
                        Text(
                            atlasText(
                                "Ou utilisez votre mot de passe",
                                "Or sign in with your password",
                                "أو سجّل الدخول بكلمة المرور",
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    CredentialsForm(
                        state = state,
                        onUsername = viewModel::onUsernameChange,
                        onPassword = viewModel::onPasswordChange,
                        onRememberChange = viewModel::onRememberAccountChange,
                        onSubmit = viewModel::submit,
                        onDismissError = viewModel::dismissError,
                        compact = compact,
                    )
                } else {
                    OtpForm(
                        state = state,
                        onOtp = viewModel::onOtpChange,
                        onSubmit = viewModel::submit,
                        onResend = viewModel::resendCode,
                        onBack = viewModel::backToCredentials,
                        onDismissError = viewModel::dismissError,
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = atlasText("Session chiffrée · ${AppDetails.VERSION_NAME}", "Encrypted session · ${AppDetails.VERSION_NAME}", "جلسة مشفّرة · ${AppDetails.VERSION_NAME}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (languageDialogOpen) {
            LoginLanguageDialog(
                language = language,
                followsSystem = followsSystemLanguage,
                onChooseSystem = { languageDialogOpen = false; useSystemLanguage() },
                onChooseLanguage = { selected -> languageDialogOpen = false; setLanguage(selected) },
                onDismiss = { languageDialogOpen = false },
            )
        }
    }
}

@Composable
private fun LoginAtlasHero(
    compact: Boolean,
    host: String,
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit,
    onOpenLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Atlas.palette
    Row(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF09090B), palette.heroStart, palette.heroEnd)))
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AtlasEntrance(delayMillis = 0) {
            AtlasBrandWordmark(
                modifier = Modifier.widthIn(max = 185.dp),
                height = if (compact) 28.dp else 36.dp,
                tint = Color.White,
            )
        }
        Spacer(Modifier.weight(1f))
        LoginOverflowMenu(
            host = host,
            onChangeServer = onChangeServer,
            onOpenQrDevice = onOpenQrDevice,
            onOpenLanguage = onOpenLanguage,
        )
    }
}

@Composable
private fun LoginOverflowMenu(
    host: String,
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit,
    onOpenLanguage: () -> Unit,
) {
    val language = LocalAtlasLanguage.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = atlasText("Plus d'options", "More options", "المزيد من الخيارات"),
                tint = Color.White,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(atlasText("Se connecter avec un QR code", "Sign in with QR code", "تسجيل الدخول برمز QR")) },
                leadingIcon = { Icon(Icons.Outlined.QrCode2, contentDescription = null) },
                onClick = { expanded = false; onOpenQrDevice() },
            )
            DropdownMenuItem(
                text = {
                    Column {
                        Text(atlasText("Choisir un serveur", "Choose server", "اختيار الخادم"))
                        Text(host, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                onClick = { expanded = false; onChangeServer() },
            )
            DropdownMenuItem(
                text = {
                    Column {
                        Text(atlasText("Langues", "Languages", "اللغات"))
                        Text(
                            language.nativeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                onClick = { expanded = false; onOpenLanguage() },
            )
        }
    }
}

@Composable
private fun LoginLanguageDialog(
    language: AppLanguage,
    followsSystem: Boolean,
    onChooseSystem: () -> Unit,
    onChooseLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedLanguage by remember(language, followsSystem) {
        mutableStateOf(if (followsSystem) null else language)
    }
    val selectionChanged = if (selectedLanguage == null) {
        !followsSystem
    } else {
        followsSystem || selectedLanguage != language
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 390.dp).padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
            shadowElevation = 20.dp,
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(
                    text = atlasText("Choisir la langue", "Choose language", "اختيار اللغة"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = atlasText("Traduisez Atlas dans votre langue", "Translate Atlas into your language", "ترجم Atlas إلى لغتك"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                LanguageChoiceRow(
                    title = atlasText("Suivre la langue du téléphone", "Follow phone language", "اتباع لغة الهاتف"),
                    subtitle = AppLanguage.fromSystemLocale().nativeLabel,
                    flag = "⚙️",
                    selected = selectedLanguage == null,
                    onClick = { selectedLanguage = null },
                )
                Spacer(Modifier.height(6.dp))
                AppLanguage.entries.forEach { option ->
                    val flag = when (option) {
                        AppLanguage.ARABIC -> "🇲🇦"
                        AppLanguage.FRENCH -> "🇫🇷"
                        AppLanguage.ENGLISH -> "🇬🇧"
                    }
                    LanguageChoiceRow(
                        title = option.nativeLabel,
                        flag = flag,
                        selected = selectedLanguage == option,
                        onClick = { selectedLanguage = option },
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(atlasText("Annuler", "Cancel", "إلغاء"))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val choice = selectedLanguage
                            if (choice == null) onChooseSystem() else onChooseLanguage(choice)
                        },
                        enabled = selectionChanged,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(atlasText("Valider", "Apply", "تأكيد"))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChoiceRow(
    title: String,
    flag: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(flag, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun CredentialsForm(
    state: LoginViewModel.UiState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRememberChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
    compact: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = atlasText("Identifiant et mot de passe", "Username and password", "اسم المستخدم وكلمة المرور"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = atlasText("Utilisez les identifiants de votre espace Atlas.", "Use the credentials for your Atlas workspace.", "استخدم بيانات اعتماد مساحة Atlas الخاصة بك."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        AtlasTextField(
            value = state.username,
            onValueChange = onUsername,
            label = atlasText("Identifiant", "Username", "اسم المستخدم"),
            leadingIcon = Icons.Outlined.Person,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            enabled = !state.loading,
        )
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        AtlasTextField(
            value = state.password,
            onValueChange = onPassword,
            label = atlasText("Mot de passe", "Password", "كلمة المرور"),
            leadingIcon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            enabled = !state.loading,
            onDone = onSubmit,
        )
        LoginInlineError(state, onDismissError)
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = atlasText("Mémoriser ce compte", "Remember this account", "تذكّر هذا الحساب"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (compact) atlasText("Chiffré ici · propre à ce serveur.", "Encrypted on this device · tied to this server.", "مشفّر على هذا الجهاز · مرتبط بهذا الخادم.") else atlasText("Chiffré localement et visible uniquement sur ce serveur.", "Encrypted locally and available only on this server.", "مشفّر محليًا ومتاح على هذا الخادم فقط."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = state.rememberAccount,
                onCheckedChange = onRememberChange,
                enabled = !state.loading,
            )
        }
        Spacer(Modifier.height(if (compact) 9.dp else 16.dp))
        AtlasButton(
            text = atlasText("Se connecter", "Sign in", "تسجيل الدخول"),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit,
            loading = state.loading,
        )
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
    }
}

@Composable
private fun OtpForm(
    state: LoginViewModel.UiState,
    onOtp: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AlternateEmail,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = atlasText("Vérification de l'appareil", "Device verification", "التحقق من الجهاز"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = atlasText("Saisissez le code à 6 chiffres envoyé par e-mail.", "Enter the six-digit code sent by email.", "أدخل الرمز المكوّن من ستة أرقام المرسل عبر البريد الإلكتروني."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        AtlasTextField(
            value = state.otp,
            onValueChange = onOtp,
            label = atlasText("Code de vérification", "Verification code", "رمز التحقق"),
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            enabled = !state.loading,
            onDone = onSubmit,
        )
        LoginInlineError(state, onDismissError)
        Spacer(Modifier.height(16.dp))
        AtlasButton(
            text = atlasText("Valider le code", "Verify code", "تحقّق من الرمز"),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit,
            loading = state.loading,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack, enabled = !state.loading) {
                Text(atlasText("Autre compte", "Use another account", "استخدام حساب آخر"))
            }
            TextButton(onClick = onResend, enabled = !state.loading) {
                Text(atlasText("Renvoyer le code", "Resend code", "إعادة إرسال الرمز"))
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoginInlineError(state: LoginViewModel.UiState, onDismiss: () -> Unit) {
    val error = state.error?.takeIf { it.isNotBlank() }
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(error) {
        if (error != null) {
            withFrameNanos { }
            requester.bringIntoView()
        }
    }
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + slideInVertically { it / 4 },
        exit = fadeOut() + slideOutVertically { it / 4 },
        modifier = Modifier.bringIntoViewRequester(requester).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        if (error != null) {
            Column {
                Spacer(Modifier.height(8.dp))
                AtlasErrorCard(
                    title = if (state.step == LoginViewModel.Step.OTP) atlasText("Code refusé", "Code rejected", "تم رفض الرمز") else atlasText("Connexion refusée", "Sign-in failed", "فشل تسجيل الدخول"),
                    message = localizedLoginMessage(error),
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun localizedLoginMessage(message: String): String = when (message) {
    "Saisissez votre identifiant et votre mot de passe." ->
        atlasText("Saisissez votre identifiant et votre mot de passe.", "Enter your username and password.", "أدخل اسم المستخدم وكلمة المرور.")
    "Saisissez les 6 chiffres du code reçu par e-mail.", "Saisissez le code reçu par e-mail.", "Saisissez le code complet reçu par e-mail." ->
        atlasText("Saisissez les 6 chiffres du code reçu par e-mail.", "Enter all six digits of the code sent by email.", "أدخل الأرقام الستة للرمز المرسل عبر البريد الإلكتروني.")
    "Identifiant ou mot de passe incorrect. Vérifiez votre saisie." ->
        atlasText("Identifiant ou mot de passe incorrect. Vérifiez votre saisie.", "Incorrect username or password. Check your entry.", "اسم المستخدم أو كلمة المرور غير صحيحة. تحقق من البيانات.")
    "Un administrateur doit réinitialiser votre mot de passe." ->
        atlasText("Un administrateur doit réinitialiser votre mot de passe.", "An administrator must reset your password.", "يجب على المسؤول إعادة تعيين كلمة المرور.")
    "Code invalide ou expiré. Vérifiez-le ou demandez-en un nouveau." ->
        atlasText("Code invalide ou expiré. Vérifiez-le ou demandez-en un nouveau.", "The code is invalid or expired. Check it or request a new one.", "الرمز غير صالح أو منتهي الصلاحية. تحقق منه أو اطلب رمزاً جديداً.")
    "Impossible de joindre le serveur. Vérifiez votre connexion et réessayez." ->
        atlasText("Impossible de joindre le serveur. Vérifiez votre connexion et réessayez.", "Couldn't reach the server. Check your connection and try again.", "تعذّر الاتصال بالخادم. تحقق من اتصالك وحاول مجدداً.")
    "Impossible de joindre ce serveur. Vérifiez votre connexion puis réessayez." ->
        atlasText("Impossible de joindre ce serveur. Vérifiez votre connexion puis réessayez.", "Couldn't reach this server. Check your connection and try again.", "تعذّر الاتصال بهذا الخادم. تحقق من اتصالك وحاول مجدداً.")
    "Une mise à jour est nécessaire pour continuer." ->
        atlasText("Une mise à jour est nécessaire pour continuer.", "An update is required to continue.", "يلزم تحديث للمتابعة.")
    "Vérification impossible. Vérifiez le code et réessayez." ->
        atlasText("Vérification impossible. Vérifiez le code et réessayez.", "Couldn't verify the code. Check it and try again.", "تعذّر التحقق من الرمز. تحقق منه وحاول مجدداً.")
    "L'origine du serveur sélectionné est invalide." ->
        atlasText("L'adresse du serveur sélectionné est invalide.", "The selected server address is invalid.", "عنوان الخادم المحدد غير صالح.")
    "Connexion impossible. Vérifiez vos informations et réessayez." ->
        atlasText("Connexion impossible. Vérifiez vos informations et réessayez.", "Couldn't sign in. Check your details and try again.", "تعذّر تسجيل الدخول. تحقق من بياناتك وحاول مجدداً.")
    else -> atlasText("Une erreur est survenue. Réessayez.", "Something went wrong. Please try again.", "حدث خطأ. يرجى المحاولة مجدداً.")
}
