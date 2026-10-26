package ma.etherealmber.atlascommerce.ui.screens.login

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
    val host = server?.let { ServerUrl.display(it.baseUrl) } ?: "Serveur non sélectionné"
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPasskeyCapability()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compact = maxWidth < 600.dp || maxHeight < 840.dp
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val heroHeight = when {
            keyboardVisible -> 104.dp
            maxHeight < 680.dp -> maxHeight * 0.30f
            else -> maxHeight * 0.36f
        }

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
                        text = if (state.step == LoginViewModel.Step.CREDENTIALS) "Bon retour" else "Vérification",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (state.step == LoginViewModel.Step.CREDENTIALS) {
                            "Connectez-vous à votre espace Atlas Commerce."
                        } else {
                            "Confirmez votre identité pour continuer."
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
                                    Text("Connexion avec une passkey")
                                }
                            } else {
                                AtlasOutlinedButton(
                                    text = "Connexion avec une passkey",
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
                                Text("Scanner un QR de connexion PC")
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            AtlasOutlinedButton(
                                text = "Scanner un QR de connexion PC",
                                onClick = onOpenQrDevice,
                                icon = Icons.Outlined.QrCode2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            text = "Besoin d'un accès ? Contactez l'administrateur de votre espace Atlas.",
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
                                title = if (state.step == LoginViewModel.Step.OTP) "Code refusé" else "Connexion refusée",
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
                        TextButton(onClick = onOpenSecurity) { Text("Sécurité") }
                        TextButton(onClick = onOpenLicense) { Text("Licence") }
                        TextButton(onClick = onCheckUpdates) { Text("Mises à jour") }
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
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)),
        ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 92.dp, y = (-100).dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 134.dp, y = 54.dp)
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
                .offset(x = (-150).dp, y = 118.dp)
                .size(330.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-38).dp, y = 20.dp)
                .size(if (compact) 42.dp else 56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = if (compact) 12.dp else 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AtlasEntrance(delayMillis = 0) {
                AtlasBrandWordmark(
                    modifier = Modifier.widthIn(max = 185.dp),
                    height = if (compact) 28.dp else 38.dp,
                    tint = Color.White,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 6.dp)) {
                Text(
                    text = "Bienvenue chez Atlas",
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                )
                Text(
                    text = "Retrouvez votre espace professionnel en toute sécurité.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = if (compact) 1 else 2,
                )
            }
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
            text = "Comptes mémorisés sur ce serveur",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Touchez un compte pour ouvrir sa session isolée instantanément.",
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
                                contentDescription = "Oublier ce compte",
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
        Text("Comptes mémorisés", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            text = "Identifiant et mot de passe",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Utilisez les identifiants de votre espace Atlas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        AtlasTextField(
            value = state.username,
            onValueChange = onUsername,
            label = "Identifiant",
            leadingIcon = Icons.Outlined.Person,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            enabled = !state.loading,
        )
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        AtlasTextField(
            value = state.password,
            onValueChange = onPassword,
            label = "Mot de passe",
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
                    text = "Mémoriser ce compte",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (compact) "Chiffré ici · propre à ce serveur." else "Chiffré localement et visible uniquement sur ce serveur.",
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
            text = "Se connecter",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit,
            loading = state.loading,
        )
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
        Text(
            text = if (compact) "Nouvel appareil ? Code envoyé par e-mail." else "Nouvel appareil ? Un code de vérification vous sera envoyé par e-mail.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                text = "Vérification de l'appareil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.info ?: "Saisissez le code à 6 chiffres envoyé par e-mail.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        AtlasTextField(
            value = state.otp,
            onValueChange = onOtp,
            label = "Code de vérification",
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            enabled = !state.loading,
            onDone = onSubmit,
        )
        Spacer(Modifier.height(16.dp))
        AtlasButton(
            text = "Valider le code",
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
                Text("Autre compte")
            }
            TextButton(onClick = onResend, enabled = !state.loading) {
                Text("Renvoyer le code")
            }
        }
    }
}
