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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    onOpenAbout: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    AtlasAuthTheme {
        LoginScreenContent(onChangeServer, onOpenQrDevice, onUnlockSavedAccount, onOpenAbout, viewModel)
    }
}

@Composable
private fun LoginScreenContent(
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit,
    onUnlockSavedAccount: (SavedAccountEntry, () -> Unit) -> Unit,
    onOpenAbout: () -> Unit,
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
    var useAnotherAccount by remember { mutableStateOf(false) }
    var accountPickerOpen by remember { mutableStateOf(false) }
    var languageDialogOpen by remember { mutableStateOf(false) }
    var helpServicesOpen by remember { mutableStateOf(false) }
    var helpTopic by remember { mutableStateOf<LoginHelpTopic?>(null) }
    var recoveryDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(savedAccounts, state.step) {
        if (accountPromptHandled) return@LaunchedEffect
        if (state.step != LoginViewModel.Step.CREDENTIALS) {
            accountPromptHandled = true
            return@LaunchedEffect
        }
        if (savedAccounts.isNotEmpty()) {
            val savedAccount = savedAccounts.firstOrNull { it.canUseDeviceUnlock } ?: savedAccounts.first()
            viewModel.onUsernameChange(savedAccount.username)
            accountPromptHandled = true
        } else {
            delay(1_500)
            if (savedAccounts.isEmpty()) accountPromptHandled = true
        }
    }

    val rememberedAccount = savedAccounts.firstOrNull { it.username.equals(state.username, ignoreCase = true) }
        ?.takeUnless { useAnotherAccount }
    val selectedUnlockAccount = savedAccounts.firstOrNull {
        it.canUseDeviceUnlock && it.username.equals(state.username, ignoreCase = true)
    } ?: savedAccounts.firstOrNull { it.canUseDeviceUnlock }
    val host = server?.let { ServerUrl.display(it.baseUrl) }
        ?: atlasText("Serveur non sélectionné", "No server selected", "لم يتم اختيار خادم")

    fun useDifferentAccount() {
        accountPickerOpen = false
        accountPromptHandled = true
        useAnotherAccount = true
        viewModel.onUsernameChange("")
        viewModel.onPasswordChange("")
    }

    fun switchAccount() {
        viewModel.backToCredentials()
        if (savedAccounts.isEmpty()) useDifferentAccount() else accountPickerOpen = true
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF121316), Color(0xFF1E2025), Color(0xFF282A30)),
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
                onOpenHelpServices = { helpServicesOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (motionEnabled) Modifier.animateContentSize() else Modifier)
                    .height(heroHeight),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = if (compact) 12.dp else 18.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)), RoundedCornerShape(28.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (compact) 20.dp else 28.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val welcomeTitle = when {
                    state.step == LoginViewModel.Step.OTP -> atlasText("Vérification", "Verification", "التحقق")
                    rememberedAccount != null -> atlasText(
                        "Bienvenue, ${rememberedAccount.displayName.ifBlank { rememberedAccount.username }}",
                        "Welcome, ${rememberedAccount.displayName.ifBlank { rememberedAccount.username }}",
                        "مرحبًا، ${rememberedAccount.displayName.ifBlank { rememberedAccount.username }}",
                    )
                    else -> atlasText("Bienvenue", "Welcome", "مرحبًا")
                }
                Text(
                    text = welcomeTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (state.step == LoginViewModel.Step.CREDENTIALS) {
                        if (rememberedAccount != null) {
                            atlasText("Compte mémorisé sur cet appareil", "Remembered account on this device", "حساب محفوظ على هذا الجهاز")
                        } else {
                            atlasText("Connectez-vous à votre espace Atlas Commerce.", "Sign in to your Atlas Commerce workspace.", "سجّل الدخول إلى مساحة Atlas Commerce الخاصة بك.")
                        }
                    } else {
                        atlasText("Confirmez votre identité pour continuer.", "Verify your identity to continue.", "أكّد هويتك للمتابعة.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (state.step == LoginViewModel.Step.CREDENTIALS) {
                    TextButton(
                        onClick = { switchAccount() },
                        enabled = !state.loading,
                        modifier = Modifier.height(40.dp),
                    ) {
                        Text(atlasText("Se connecter avec un autre compte", "Sign in with another account", "تسجيل الدخول بحساب آخر"))
                    }
                    Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                    CredentialsForm(
                        state = state,
                        showUsernameField = rememberedAccount == null || useAnotherAccount,
                        onUsername = viewModel::onUsernameChange,
                        onPassword = viewModel::onPasswordChange,
                        onRememberChange = viewModel::onRememberAccountChange,
                        onSubmit = viewModel::submit,
                        onDismissError = viewModel::dismissError,
                        compact = compact,
                    )
                    TextButton(
                        onClick = { recoveryDialogOpen = true },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(atlasText("Nom d'utilisateur ou mot de passe oublié ?", "Forgot username or password?", "هل نسيت اسم المستخدم أو كلمة المرور؟"))
                    }

                    selectedUnlockAccount?.let { savedAccount ->
                        Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
                        BiometricUnlockAction(
                            account = savedAccount,
                            enabled = !state.loading,
                            onClick = {
                                useAnotherAccount = false
                                viewModel.onUsernameChange(savedAccount.username)
                                viewModel.onPasswordChange("")
                                onUnlockSavedAccount(savedAccount) {
                                    viewModel.signInWithSavedAccount(savedAccount)
                                }
                            },
                        )
                    }
                } else {
                    OtpForm(
                        state = state,
                        onOtp = viewModel::onOtpChange,
                        onSubmit = viewModel::submit,
                        onResend = viewModel::resendCode,
                        onBack = switchAccount,
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
            }
        }

        if (helpServicesOpen) {
            LoginHelpServicesSheet(
                host = host,
                languageName = language.nativeLabel,
                onDismiss = { helpServicesOpen = false },
                onHelp = { helpServicesOpen = false; helpTopic = LoginHelpTopic.HELP },
                onContacts = { helpServicesOpen = false; helpTopic = LoginHelpTopic.CONTACTS },
                onAbout = { helpServicesOpen = false; onOpenAbout() },
                onLanguages = { helpServicesOpen = false; languageDialogOpen = true },
                onServers = { helpServicesOpen = false; onChangeServer() },
                onQr = { helpServicesOpen = false; onOpenQrDevice() },
            )
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
        if (accountPickerOpen) {
            SavedAccountPickerDialog(
                accounts = savedAccounts,
                onSelect = { account ->
                    accountPickerOpen = false
                    useAnotherAccount = false
                    viewModel.onUsernameChange(account.username)
                    viewModel.onPasswordChange("")
                    accountPromptHandled = true
                },
                onDifferentAccount = ::useDifferentAccount,
                onDismiss = { accountPickerOpen = false },
            )
        }
        helpTopic?.let { topic ->
            LoginHelpInfoDialog(topic = topic, onDismiss = { helpTopic = null })
        }
        if (recoveryDialogOpen) {
            LoginHelpInfoDialog(topic = LoginHelpTopic.RECOVERY, onDismiss = { recoveryDialogOpen = false })
        }
    }
}

@Composable
private fun LoginAtlasHero(
    compact: Boolean,
    onOpenHelpServices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Atlas.palette
    Row(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF121316), palette.heroStart, palette.heroEnd)))
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
        IconButton(onClick = onOpenHelpServices) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = atlasText("Aide et services", "Help and services", "المساعدة والخدمات"),
                tint = Color.White,
            )
        }
    }
}

private enum class LoginHelpTopic { HELP, CONTACTS, RECOVERY }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LoginHelpServicesSheet(
    host: String,
    languageName: String,
    onDismiss: () -> Unit,
    onHelp: () -> Unit,
    onContacts: () -> Unit,
    onAbout: () -> Unit,
    onLanguages: () -> Unit,
    onServers: () -> Unit,
    onQr: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp).size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.outline),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        atlasText("Aide et services", "Help & services", "المساعدة والخدمات"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        atlasText("Accès rapide à l'assistance et aux réglages Atlas.", "Quick access to Atlas help and settings.", "وصول سريع إلى المساعدة وإعدادات Atlas."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = atlasText("Fermer", "Close", "إغلاق"))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LoginServiceItem(Icons.Outlined.HelpOutline, atlasText("Aide / FAQ", "Help / FAQ", "المساعدة / الأسئلة الشائعة"), onHelp, Modifier.weight(1f))
                LoginServiceItem(Icons.Outlined.AlternateEmail, atlasText("Contacts", "Contacts", "جهات الاتصال"), onContacts, Modifier.weight(1f))
                LoginServiceItem(Icons.Outlined.Info, atlasText("À propos", "About", "حول التطبيق"), onAbout, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LoginServiceItem(Icons.Outlined.Language, atlasText("Langues", "Languages", "اللغات"), onLanguages, Modifier.weight(1f), subtitle = languageName)
                LoginServiceItem(Icons.Outlined.Dns, atlasText("Serveurs", "Servers", "الخوادم"), onServers, Modifier.weight(1f), subtitle = host)
                LoginServiceItem(Icons.Outlined.QrCode2, atlasText("Connexion QR", "QR sign-in", "تسجيل الدخول عبر QR"), onQr, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LoginServiceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SavedAccountPickerDialog(
    accounts: List<SavedAccountEntry>,
    onSelect: (SavedAccountEntry) -> Unit,
    onDifferentAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(atlasText("Changer de compte", "Switch account", "تغيير الحساب")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    Surface(
                        onClick = { onSelect(account) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(account.displayName.ifBlank { account.username }, fontWeight = FontWeight.SemiBold)
                            if (account.displayName.isNotBlank()) {
                                Text(account.username, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDifferentAccount) {
                Text(atlasText("Utiliser un autre compte", "Use a different account", "استخدام حساب آخر"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LoginHelpInfoDialog(topic: LoginHelpTopic, onDismiss: () -> Unit) {
    val title: String
    val message: String
    when (topic) {
        LoginHelpTopic.HELP -> {
            title = atlasText("Aide et FAQ", "Help & FAQ", "المساعدة والأسئلة الشائعة")
            message = atlasText(
                "Vérifiez le serveur sélectionné et votre connexion. Pour récupérer un accès, contactez l'administrateur de votre espace Atlas Commerce.",
                "Check the selected server and your connection. To recover access, contact your Atlas Commerce workspace administrator.",
                "تحقق من الخادم المحدد واتصالك. لاستعادة الوصول، تواصل مع مسؤول مساحة Atlas Commerce.",
            )
        }
        LoginHelpTopic.CONTACTS -> {
            title = atlasText("Contacts", "Contacts", "جهات الاتصال")
            message = atlasText(
                "Pour confirmer votre nom d'utilisateur ou réinitialiser votre mot de passe, contactez l'administrateur de votre organisation.",
                "To confirm your username or reset your password, contact your organization administrator.",
                "لتأكيد اسم المستخدم أو إعادة تعيين كلمة المرور، تواصل مع مسؤول مؤسستك.",
            )
        }
        LoginHelpTopic.RECOVERY -> {
            title = atlasText("Récupérer l'accès", "Recover account access", "استعادة الوصول إلى الحساب")
            message = atlasText(
                "Demandez à l'administrateur de votre espace de confirmer votre nom d'utilisateur ou de réinitialiser votre mot de passe. Atlas ne peut pas réinitialiser le mot de passe depuis cet écran. Après une connexion valide, un code de vérification peut être envoyé par e-mail.",
                "Ask your workspace administrator to confirm your username or reset your password. Atlas cannot reset it from this screen. After a valid sign-in, a verification code may be sent by email.",
                "اطلب من مسؤول مساحة العمل تأكيد اسم المستخدم أو إعادة تعيين كلمة المرور. لا يمكن لـ Atlas إعادة تعيينها من هذه الشاشة. بعد تسجيل دخول صحيح، قد يُرسل رمز تحقق عبر البريد الإلكتروني.",
            )
        }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(atlasText("Compris", "Got it", "حسنًا")) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BiometricUnlockAction(
    account: SavedAccountEntry,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(62.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Fingerprint,
                    contentDescription = atlasText("Déverrouiller par biométrie", "Unlock with biometrics", "إلغاء القفل بالبصمة"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        TextButton(onClick = onClick, enabled = enabled) {
            Text(atlasText("Utiliser la biométrie ou le code de l'appareil", "Use biometrics or device PIN", "استخدم البصمة أو رمز الجهاز"))
        }
        Text(
            account.displayName.ifBlank { account.username },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
    showUsernameField: Boolean,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRememberChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
    compact: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = if (showUsernameField) {
                atlasText("Identifiant et mot de passe", "Username and password", "اسم المستخدم وكلمة المرور")
            } else {
                atlasText("Mot de passe", "Password", "كلمة المرور")
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (showUsernameField) {
                atlasText("Utilisez les identifiants de votre espace Atlas.", "Use the credentials for your Atlas workspace.", "استخدم بيانات اعتماد مساحة Atlas الخاصة بك.")
            } else {
                atlasText("Saisissez le mot de passe de ce compte.", "Enter this account's password.", "أدخل كلمة مرور هذا الحساب.")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        if (showUsernameField) {
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
        }
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
