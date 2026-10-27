package ma.etherealmber.atlascommerce

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import ma.etherealmber.atlascommerce.core.net.VerifyLink
import ma.etherealmber.atlascommerce.core.net.VerifyLinkParser
import ma.etherealmber.atlascommerce.ui.root.AtlasRoot
import ma.etherealmber.atlascommerce.ui.screens.devices.QrTokenParser

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLinkQrToken by mutableStateOf<String?>(null)
    private var deepLinkVerify by mutableStateOf<VerifyLink?>(null)
    private var deepLinkServerOrigin by mutableStateOf<String?>(null)
    private var openNotificationsIntent by mutableStateOf(false)
    private var openSettingsIntent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkQrToken = extractQrFromIntent(intent)
        deepLinkVerify = extractVerifyFromIntent(intent)
        deepLinkServerOrigin = extractVerifyOriginFromIntent(intent)
        openNotificationsIntent = intent?.getBooleanExtra(AtlasApp.EXTRA_OPEN_NOTIFICATIONS, false) == true ||
            intent?.action == AtlasApp.ACTION_ASSIST_NOTIFICATION
        openSettingsIntent = intent?.getBooleanExtra(AtlasApp.EXTRA_OPEN_SETTINGS, false) == true
        enableEdgeToEdge()
        // Let the app surface/content draw behind Android's navigation area. The
        // floating Atlas navigation pill itself remains opaque in MainShell.
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        setContent {
            AtlasRoot(
                deepLinkQrToken = deepLinkQrToken,
                onDeepLinkConsumed = { deepLinkQrToken = null },
                deepLinkVerify = deepLinkVerify,
                onVerifyLinkConsumed = {
                    deepLinkVerify = null
                    deepLinkServerOrigin = null
                },
                deepLinkServerOrigin = deepLinkServerOrigin,
                openNotificationsRequested = openNotificationsIntent,
                onOpenNotificationsConsumed = { openNotificationsIntent = false },
                openSettingsRequested = openSettingsIntent,
                onOpenSettingsConsumed = { openSettingsIntent = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractQrFromIntent(intent)?.let { token ->
            deepLinkQrToken = token
        }
        extractVerifyFromIntent(intent)?.let { link ->
            deepLinkServerOrigin = extractVerifyOriginFromIntent(intent)
            deepLinkVerify = link
        }
        if (intent.getBooleanExtra(AtlasApp.EXTRA_OPEN_NOTIFICATIONS, false) ||
            intent.action == AtlasApp.ACTION_ASSIST_NOTIFICATION
        ) {
            openNotificationsIntent = true
        }
        if (intent.getBooleanExtra(AtlasApp.EXTRA_OPEN_SETTINGS, false)) {
            openSettingsIntent = true
        }
    }

    private fun extractQrFromIntent(intent: Intent?): String? {
        val dataString = intent?.dataString ?: return null
        return QrTokenParser.extractQrToken(dataString)
    }

    private fun extractVerifyFromIntent(intent: Intent?): VerifyLink? =
        VerifyLinkParser.parse(intent?.dataString)

    private fun extractVerifyOriginFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return VerifyLinkParser.extractServerOrigin(intent.dataString)
            ?: VerifyLinkParser.extractServerOrigin(intent.getStringExtra("browser_fallback_url"))
    }
}
