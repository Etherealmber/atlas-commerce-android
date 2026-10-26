package ma.etherealmber.atlascommerce.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.diagnostics.CrashReportConsent
import ma.etherealmber.atlascommerce.core.diagnostics.DiagnosticReporter
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasInfoRow
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun DeveloperDiagnosticsScreen(
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val server by appViewModel.server.collectAsStateWithLifecycle()
    val developerMode by appViewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val passkeyAvailable by appViewModel.androidPasskeysAvailable.collectAsStateWithLifecycle()
    val cachedProductCount by appViewModel.cachedProductCount.collectAsStateWithLifecycle()
    var logs by remember { mutableStateOf("Lecture des journaux…") }
    var loadingLogs by remember { mutableStateOf(false) }

    fun refreshLogs() {
        if (loadingLogs) return
        scope.launch {
            loadingLogs = true
            logs = AtlasLogBuffer.captureDiagnosticLogsAsync(context, maxChars = 16_000)
            loadingLogs = false
        }
    }

    LaunchedEffect(Unit) { refreshLogs() }

    Scaffold(
        topBar = {
            AtlasTopBar(
                title = "Diagnostics développeur",
                subtitle = "Outils locaux · secrets masqués",
                onBack = onBack,
                actions = {
                    IconButton(onClick = ::refreshLogs, enabled = !loadingLogs) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Rafraîchir les journaux")
                    }
                },
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
                        Text("Mode développeur", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Le mode est uniquement local à cet appareil.",
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
                AtlasDivider(Modifier.padding(vertical = 8.dp))
                AtlasInfoRow("Serveur sélectionné", ServerUrl.origin(server?.baseUrl) ?: "Aucun")
                AtlasInfoRow("Application", "${AppDetails.VERSION_NAME} · build ${AppDetails.VERSION_CODE}")
                AtlasInfoRow("Package / build", "${BuildConfig.APPLICATION_ID} · ${BuildConfig.BUILD_TYPE}")
                AtlasInfoRow("Android", "${Build.VERSION.RELEASE ?: "?"} · API ${Build.VERSION.SDK_INT}")
                AtlasInfoRow("Appareil", "${Build.MANUFACTURER} ${Build.MODEL}")
                AtlasInfoRow("Catalogue hors ligne", "$cachedProductCount article(s)")
                AtlasInfoRow("Passkeys Android", if (passkeyAvailable) "Annoncées par le serveur sélectionné" else "Non confirmées — consultez les événements PASSKEY")
                AtlasInfoRow(
                    "Crash reports automatiques",
                    when {
                        !CrashReportConsent.isConfigured(context) -> "Non configurés"
                        CrashReportConsent.isEnabled(context) -> "Activés avec consentement"
                        else -> "Désactivés — le diagnostic local reste disponible"
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Android n'autorise pas une application à lire les journaux privés des autres apps. Le rapport contient les événements Atlas, les réponses HTTP et les traces de ce processus; quelques avertissements graphiques/fastpipe connus sont regroupés comme bruit système.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Journaux de diagnostic", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (loadingLogs) "Capture en cours…" else "${logs.lineSequence().count()} lignes · expurgées des secrets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Journaux Atlas assainis", logs))
                            onToast("Journaux de diagnostic copiés.")
                        },
                        enabled = logs.isNotBlank() && !loadingLogs,
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copier tous les journaux")
                    }
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = ::refreshLogs,
                    enabled = !loadingLogs,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Capturer les journaux à nouveau")
                }
                AtlasDivider(Modifier.padding(vertical = 8.dp))
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(10.dp)) {
                        Text(
                            logs,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
