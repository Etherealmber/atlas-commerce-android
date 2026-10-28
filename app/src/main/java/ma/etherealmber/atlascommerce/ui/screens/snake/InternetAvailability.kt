package ma.etherealmber.atlascommerce.ui.screens.snake

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Device connectivity only: this does not claim the Atlas server is down. */
@Composable
fun rememberInternetAvailable(): Boolean? {
    val context = LocalContext.current
    var available by remember { mutableStateOf<Boolean?>(null) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val handler = Handler(Looper.getMainLooper())
        fun update() {
            available = manager?.let { cm ->
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { handler.post { update() } }
            override fun onLost(network: Network) { handler.post { update() } }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { handler.post { update() } }
        }
        update()
        val registered = manager != null && runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
        onDispose { if (registered) runCatching { manager?.unregisterNetworkCallback(callback) } }
    }
    return available
}
