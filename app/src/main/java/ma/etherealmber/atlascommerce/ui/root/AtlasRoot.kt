package ma.etherealmber.atlascommerce.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.screens.legal.AboutScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.LicenseScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.SecurityScreen
import ma.etherealmber.atlascommerce.ui.screens.login.LoginScreen
import ma.etherealmber.atlascommerce.ui.screens.server.ServerSetupScreen
import ma.etherealmber.atlascommerce.ui.shell.MainShell
import ma.etherealmber.atlascommerce.ui.theme.AtlasTheme

private object Overlay {
    const val SERVER = "server"
    const val LICENSE = "license"
    const val SECURITY = "security"
    const val ABOUT = "about"
}

/**
 * Composition root of the app.
 *
 * Resolves appearance first (so themes never flash), then gates on three things
 * in order: server chosen → signed in → main shell. Overlays (server picker,
 * legal pages) sit on top of any stage, which is why the server can be changed
 * from the login screen as well as from settings.
 */
@Composable
fun AtlasRoot(viewModel: AppViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val densityMode by viewModel.densityMode.collectAsStateWithLifecycle()

    AtlasTheme(themeMode = themeMode, densityMode = densityMode) {
        AtlasGate(viewModel)
    }
}

@Composable
private fun AtlasGate(viewModel: AppViewModel) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val serverLoaded by viewModel.serverLoaded.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }

    val showToast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { message -> showToast(message) }
    }
    LaunchedEffect(session) {
        if (session is SessionState.SignedOut) overlay = null
    }

    BackHandler(enabled = overlay != null) { overlay = null }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !serverLoaded || session is SessionState.Loading -> SplashPane()

                overlay == Overlay.SERVER ->
                    ServerSetupScreen(onClose = { overlay = null })

                overlay == Overlay.LICENSE -> LicenseScreen(onBack = { overlay = null })

                overlay == Overlay.SECURITY -> SecurityScreen(onBack = { overlay = null })

                overlay == Overlay.ABOUT -> AboutScreen(onBack = { overlay = null })

                server == null -> ServerSetupScreen()

                session is SessionState.SignedOut -> LoginScreen(
                    onChangeServer = { overlay = Overlay.SERVER },
                    onOpenSecurity = { overlay = Overlay.SECURITY },
                    onOpenLicense = { overlay = Overlay.LICENSE },
                )

                session is SessionState.Active -> {
                    val user = (session as SessionState.Active).user
                    MainShell(
                        user = user,
                        snackbarHostState = snackbarHostState,
                        onChangeServer = { overlay = Overlay.SERVER },
                        onOpenLicense = { overlay = Overlay.LICENSE },
                        onOpenSecurity = { overlay = Overlay.SECURITY },
                        onOpenAbout = { overlay = Overlay.ABOUT },
                        onSignOut = viewModel::signOut,
                    )
                }

                else -> SplashPane()
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun SplashPane() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        AtlasMark(size = 72.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = AppDetails.NAME,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = AppDetails.TAGLINE,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}
