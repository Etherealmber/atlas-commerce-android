package ma.etherealmber.atlascommerce.ui.screens.login

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasBrandWordmark
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasServerChip
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.i18n.AtlasLanguageSelector
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun LoginScreen(
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit = {},
    onOpenSecurity: () -> Unit,
    onOpenLicense: () -> Unit,
    onCheckUpdates: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val host = server?.let { ServerUrl.display(it.baseUrl) } ?: atlasText("Serveur non sélectionné", "No server selected", "لم يتم اختيار خادم")
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPasskeyCapability()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    state.passkeyError?.let { passkeyError ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPasskeyError,
            title = { Text(atlasText("Connexion par passkey impossible", "Passkey sign-in failed", "تعذّر تسجيل الدخول بمفتاح المرور")) },
            text = { Text(passkeyError) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPasskeyError) { Text(atlasText("Fermer", "Close", "إغلاق")) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissPasskeyError()
                        val credentialSettings = Intent("android.settings.PASSWORDS_AND_ACCOUNTS_SETTINGS")
                        runCatching { context.startActivity(credentialSettings) }
                            .onFailure {
                                runCatching { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                            }
                    },
                ) { Text(atlasText("Paramètres Android", "Android settings", "إعدادات Android")) }
            },
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compact = maxWidth < 600.dp || maxHeight < 840.dp
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        // Keep the brand header compact; the form already carries the welcome and
        // verification context, so a tall duplicate hero wastes space on phones.
        val heroHeight = if (keyboardVisible) 72.dp else 104.dp

        Box(Modifier.fillMaxSize()) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight),
                )

                // The rounded sheet follows the reference layout while retaining
                // the real Atlas credential, OTP, passkey, and QR flows.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(MaterialTheme.colorScheme.surface)
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
                    )
                    Spacer(Modifier.height(16.dp))
                    AtlasServerChip(
                        host = host,
                        onClick = onChangeServer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

                    if (state.step == LoginViewModel.Step.CREDENTIALS) {
                        if (savedAccounts.isNotEmpty()) {
                            if (compact) {
                                SavedAccountsCompact(
                                    accounts = savedAccounts,
                                    loading = state.loading,
                                    onSelect = viewModel::signInWithSavedAccount,
                                    onForget = viewModel::forgetSavedAccount,
                                )
                            } else {
                                SavedAccountsCard(
                                    accounts = savedAccounts,
                                    serverBaseUrl = server?.baseUrl,
                                    loading = state.loading,
                                    onSelect = viewModel::signInWithSavedAccount,
                                    onForget = viewModel::forgetSavedAccount,
                                )
                            }
                            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                        }

                        CredentialsForm(
                            state = state,
                            onUsername = viewModel::onUsernameChange,
                            onPassword = viewModel::onPasswordChange,
                            onRememberChange = viewModel::onRememberAccountChange,
                            onSubmit = viewModel::submit,
                            compact = compact,
                        )
                        if (state.passkeyAvailable) {
                            Spacer(Modifier.height(4.dp))
                            if (compact) {
                                TextButton(
                                    onClick = { viewModel.signInWithPasskey(context) },
                                    enabled = !state.loading,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(atlasText("Connexion avec une passkey", "Sign in with a passkey", "تسجيل الدخول بمفتاح مرور"))
                                }
                            } else {
                                AtlasOutlinedButton(
                                    text = atlasText("Connexion avec une passkey", "Sign in with a passkey", "تسجيل الدخول بمفتاح مرور"),
                                    onClick = { viewModel.signInWithPasskey(context) },
                                    icon = Icons.Outlined.Fingerprint,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.loading,
                                )
                            }
                        }
                        if (compact) {
                            TextButton(
                                onClick = onOpenQrDevice,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(atlasText("Scanner un QR de connexion PC", "Scan a PC sign-in QR code", "امسح رمز QR لتسجيل الدخول على الكمبيوتر"))
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            AtlasOutlinedButton(
                                text = atlasText("Scanner un QR de connexion PC", "Scan a PC sign-in QR code", "امسح رمز QR لتسجيل الدخول على الكمبيوتر"),
                                onClick = onOpenQrDevice,
                                icon = Icons.Outlined.QrCode2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            text = atlasText("Besoin d'un accès ? Contactez l'administrateur de votre espace Atlas.", "Need access? Contact your Atlas workspace administrator.", "هل تحتاج إلى وصول؟ تواصل مع مسؤول مساحة Atlas."),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        OtpForm(
                            state = state,
                            onOtp = viewModel::onOtpChange,
                            onSubmit = viewModel::submit,
                            onResend = viewModel::resendCode,
                            onBack = viewModel::backToCredentials,
                        )
                    }

                    AnimatedVisibility(
                        visible = !state.error.isNullOrBlank(),
                        enter = fadeIn() + slideInVertically { it / 4 },
                        exit = fadeOut() + slideOutVertically { it / 4 },
                    ) {
                        if (!state.error.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            AtlasErrorCard(
                                title = if (state.step == LoginViewModel.Step.OTP) atlasText("Code refusé", "Code rejected", "تم رفض الرمز") else atlasText("Connexion refusée", "Sign-in failed", "فشل تسجيل الدخول"),
                                message = state.error.orEmpty(),
                            )
                        }
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
                            text = if (compact) "Session chiffrée · ${AppDetails.VERSION_NAME}" else "Sessions chiffrées et isolées par utilisateur · ${AppDetails.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 4.dp)) {
                        TextButton(onClick = onOpenSecurity) { Text(atlasText("Sécurité", "Security", "الأمان")) }
                        TextButton(onClick = onOpenLicense) { Text(atlasText("Licence", "License", "الترخيص")) }
                        TextButton(onClick = onCheckUpdates) { Text(atlasText("Mises à jour", "Updates", "التحديثات")) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LoginAtlasHero(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = Atlas.palette
    val horizontalDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)),
        ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 92.dp * horizontalDirection, y = (-100).dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 134.dp * horizontalDirection, y = 54.dp)
                .size(250.dp)
                .clip(RoundedCornerShape(120.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.50f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-150).dp * horizontalDirection, y = 118.dp)
                .size(330.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-38).dp * horizontalDirection, y = 20.dp)
                .size(if (compact) 42.dp else 56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = if (compact) 8.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasEntrance(delayMillis = 0) {
                AtlasBrandWordmark(
                    modifier = Modifier.widthIn(max = 185.dp),
                    height = if (compact) 28.dp else 38.dp,
                    tint = Color.White,
                )
            }
            AtlasLanguageSelector(contentColor = Color.White)
        }
    }
}

@Composable
private fun SavedAccountsCard(
    accounts: List<SavedAccountEntry>,
    serverBaseUrl: String?,
    loading: Boolean,
    onSelect: (SavedAccountEntry) -> Unit,
    onForget: (SavedAccountEntry) -> Unit,
) {
    AtlasCard {
        Text(
            text = atlasText("Comptes mémorisés sur ce serveur", "Saved accounts on this server", "الحسابات المحفوظة على هذا الخادم"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = atlasText("Touchez un compte pour ouvrir sa session isolée instantanément.", "Choose an account to open its isolated session.", "اختر حسابًا لفتح جلسته المعزولة."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { acc ->
                AtlasCard(
                    onClick = if (loading) null else ({ onSelect(acc) }),
                    padding = 10.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasAvatar(
                            avatarUrl = acc.avatarUrl,
                            label = acc.displayName.ifBlank { acc.username },
                            serverBaseUrl = serverBaseUrl,
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = acc.displayName.ifBlank { acc.username },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "@${acc.username} · ${Roles.label(acc.role)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AtlasChip(text = Roles.label(acc.role), tone = AtlasTone.NEUTRAL)
                        IconButton(
                            onClick = { onForget(acc) },
                            enabled = !loading,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = atlasText("Oublier ce compte", "Forget this account", "نسيان هذا الحساب"),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAccountsCompact(
    accounts: List<SavedAccountEntry>,
    loading: Boolean,
    onSelect: (SavedAccountEntry) -> Unit,
    onForget: (SavedAccountEntry) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(atlasText("Comptes mémorisés", "Saved accounts", "الحسابات المحفوظة"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            accounts.forEach { account ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasOutlinedButton(
                        text = account.displayName.ifBlank { account.username },
                        onClick = { onSelect(account) },
                        enabled = !loading,
                    )
                    IconButton(onClick = { onForget(account) }, enabled = !loading, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Oublier ${account.username}", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialsForm(
    state: LoginViewModel.UiState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRememberChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
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
            text = state.info ?: atlasText("Saisissez le code à 6 chiffres envoyé par e-mail.", "Enter the six-digit code sent by email.", "أدخل الرمز المكوّن من ستة أرقام المرسل عبر البريد الإلكتروني."),
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
