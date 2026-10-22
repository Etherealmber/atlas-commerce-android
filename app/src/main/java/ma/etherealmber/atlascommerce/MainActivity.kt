package ma.etherealmber.atlascommerce

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import ma.etherealmber.atlascommerce.ui.root.AtlasRoot
import ma.etherealmber.atlascommerce.ui.screens.devices.QrTokenParser

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLinkQrToken by mutableStateOf<String?>(null)
    private var openNotificationsIntent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkQrToken = extractQrFromIntent(intent)
        openNotificationsIntent = intent?.getBooleanExtra(AtlasApp.EXTRA_OPEN_NOTIFICATIONS, false) == true
        enableEdgeToEdge()
        setContent {
            AtlasRoot(
                deepLinkQrToken = deepLinkQrToken,
                onDeepLinkConsumed = { deepLinkQrToken = null },
                openNotificationsRequested = openNotificationsIntent,
                onOpenNotificationsConsumed = { openNotificationsIntent = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractQrFromIntent(intent)?.let { token ->
            deepLinkQrToken = token
        }
        if (intent.getBooleanExtra(AtlasApp.EXTRA_OPEN_NOTIFICATIONS, false)) {
            openNotificationsIntent = true
        }
    }

    private fun extractQrFromIntent(intent: Intent?): String? {
        val dataString = intent?.dataString ?: return null
        return QrTokenParser.extractQrToken(dataString)
    }
}
