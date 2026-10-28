package ma.etherealmber.atlascommerce.ui.screens.login

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasBrandWordmark
import ma.etherealmber.atlascommerce.ui.components.AtlasServerChip
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.i18n.AtlasLanguageSelector
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun LoginScreen(
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var accountPromptHandled by remember { mutableStateOf(false) }
    var accountToConfirm by remember { mutableStateOf<SavedAccountEntry?>(null) }
    LaunchedEffect(savedAccounts, state.step) {
        if (accountPromptHandled) return@LaunchedEffect
        if (state.step != LoginViewModel.Step.CREDENTIALS) {
            accountPromptHandled = true
            return@LaunchedEffect
        }
        if (savedAccounts.isNotEmpty()) {
            accountToConfirm = savedAccounts.first()
            accountPromptHandled = true
        } else {
            delay(1_500)
            if (savedAccounts.isEmpty()) accountPromptHandled = true
        }
    }
    val host = server?.let { ServerUrl.display(it.baseUrl) } ?: atlasText("Serveur non sélectionné", "No server selected", "لم يتم اختيار خادم")
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPasskeyCapability()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    accountToConfirm?.let { savedAccount ->
        AlertDialog(
            onDismissRequest = { accountToConfirm = null },
            title = { Text(atlasText("Reconnexion", "Sign in again", "تسجيل الدخول مجدداً")) },
            text = {
                Text(
                    atlasText(
                        "Voulez-vous vous reconnecter au compte ${savedAccount.displayName.ifBlank { savedAccount.username }} ?",
                        "Would you like to sign back in to ${savedAccount.displayName.ifBlank { savedAccount.username }}?",
                        "هل تريد تسجيل الدخول مجدداً إلى ${savedAccount.displayName.ifBlank { savedAccount.username }}؟",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    accountToConfirm = null
                    viewModel.onUsernameChange(savedAccount.username)
                }, enabled = !state.loading) {
                    Text(atlasText("Oui, continuer", "Yes, continue", "نعم، متابعة"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    accountToConfirm = null
                    viewModel.onUsernameChange(savedAccount.username)
                }) {
                    Text(atlasText("Utiliser un mot de passe", "Use password", "استخدام كلمة المرور"))
                }
            },
        )
    }

    state.passkeyError?.let { passkeyError ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPasskeyError,
            title = { Text(atlasText("Connexion par passkey impossible", "Passkey sign-in failed", "تعذّر تسجيل الدخول بمفتاح المرور")) },
            text = { Text(localizedLoginMessage(passkeyError)) },
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
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Atlas.palette.heroStart, Atlas.palette.heroEnd)),
        ),
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
                        .then(if (motionEnabled) Modifier.animateContentSize() else Modifier)
                        .height(heroHeight),
                )

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    AtlasServerChip(
                        host = host,
                        onClick = onChangeServer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // The credential and OTP sheet begins below the branded server selector.
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
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

                    if (state.step == LoginViewModel.Step.CREDENTIALS) {
                        CredentialsForm(
                            state = state,
                            onUsername = viewModel::onUsernameChange,
                            onPassword = viewModel::onPasswordChange,
                            onRememberChange = viewModel::onRememberAccountChange,
                            onSubmit = viewModel::submit,
                            onDismissError = viewModel::dismissError,
                            compact = compact,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (state.passkeyAvailable) {
                                TextButton(
                                    onClick = { viewModel.signInWithPasskey(context) },
                                    enabled = !state.loading,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = atlasText("Passkey", "Passkey", "مفتاح مرور"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            TextButton(
                                onClick = onOpenQrDevice,
                                enabled = !state.loading,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = atlasText("Scanner QR", "Scan QR", "مسح QR"),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
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
        }
    }
}

@Composable
private fun LoginAtlasHero(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = Atlas.palette
    var logoTaps by remember { mutableIntStateOf(0) }
    var showSurprise by remember { mutableStateOf(false) }
    if (showSurprise) {
        AlertDialog(
            onDismissRequest = { showSurprise = false },
            title = { Text(atlasText("Bienvenue, explorateur !", "Welcome, explorer!", "مرحباً أيها المستكشف!")) },
            text = { Text(atlasText("✨ Vous avez découvert une étoile Atlas.", "✨ You found an Atlas star.", "✨ لقد اكتشفت نجمة Atlas.")) },
            confirmButton = { TextButton(onClick = { showSurprise = false }) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
        )
    }
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = if (compact) 8.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasEntrance(delayMillis = 0) {
                AtlasBrandWordmark(
                    modifier = Modifier.widthIn(max = 185.dp).clickable { logoTaps++; if (logoTaps >= 5) { logoTaps = 0; showSurprise = true } },
                    height = if (compact) 28.dp else 38.dp,
                    tint = Color.White,
                )
            }
            AtlasLanguageSelector(contentColor = Color.White)
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
    val error = state.error?.takeIf { it.isNotBlank() && state.passkeyError == null }
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
    "Saisissez le code reçu par e-mail.", "Saisissez le code complet reçu par e-mail." ->
        atlasText("Saisissez le code reçu par e-mail.", "Enter the code sent by email.", "أدخل الرمز المرسل عبر البريد الإلكتروني.")
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
    "Aucune passkey n'est disponible sur cet appareil. Connectez-vous avec votre mot de passe, puis ajoutez une passkey dans les paramètres." ->
        atlasText("Aucune passkey n'est disponible sur cet appareil. Connectez-vous avec votre mot de passe, puis ajoutez une passkey dans les paramètres.", "No passkey is available on this device. Sign in with your password, then add one in Settings.", "لا يتوفر مفتاح مرور على هذا الجهاز. سجّل الدخول بكلمة المرور ثم أضف مفتاحاً من الإعدادات.")
    "Aucun fournisseur de passkeys n'est prêt sur cet appareil. Vérifiez les paramètres de mots de passe Android ou utilisez votre mot de passe." ->
        atlasText("Aucun fournisseur de passkeys n'est prêt sur cet appareil. Vérifiez les paramètres de mots de passe Android ou utilisez votre mot de passe.", "No passkey provider is ready on this device. Check Android password settings or use your password.", "لا يوجد مزوّد مفاتيح مرور جاهز على هذا الجهاز. تحقق من إعدادات كلمات مرور Android أو استخدم كلمة المرور.")
    "La connexion par passkey a été annulée." ->
        atlasText("La connexion par passkey a été annulée.", "Passkey sign-in was canceled.", "تم إلغاء تسجيل الدخول بمفتاح المرور.")
    "L'origine du serveur sélectionné est invalide." ->
        atlasText("L'adresse du serveur sélectionné est invalide.", "The selected server address is invalid.", "عنوان الخادم المحدد غير صالح.")
    "Les passkeys Android ne sont pas activées sur ce serveur.", "Les passkeys Android ne sont plus disponibles sur ce serveur." ->
        atlasText("Les passkeys Android ne sont pas activées sur ce serveur.", "Android passkeys are not enabled on this server.", "مفاتيح المرور غير مفعّلة على هذا الخادم.")
    "Connexion par passkey impossible. Réessayez ou utilisez votre mot de passe." ->
        atlasText("Connexion par passkey impossible. Réessayez ou utilisez votre mot de passe.", "Passkey sign-in failed. Try again or use your password.", "تعذّر تسجيل الدخول بمفتاح المرور. حاول مجدداً أو استخدم كلمة المرور.")
    "Connexion impossible. Vérifiez vos informations et réessayez." ->
        atlasText("Connexion impossible. Vérifiez vos informations et réessayez.", "Couldn't sign in. Check your details and try again.", "تعذّر تسجيل الدخول. تحقق من بياناتك وحاول مجدداً.")
    else -> atlasText("Une erreur est survenue. Réessayez.", "Something went wrong. Please try again.", "حدث خطأ. يرجى المحاولة مجدداً.")
}
