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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Device connectivity only: this does not claim the Atlas server is down. */
@Composable
fun rememberInternetAvailable(): Boolean? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var available by remember { mutableStateOf<Boolean?>(null) }
    DisposableEffect(context, lifecycleOwner) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val handler = Handler(Looper.getMainLooper())
        fun update() {
            available = manager?.let { cm ->
                val network = cm.activeNetwork ?: return@let false
                val capabilities = cm.getNetworkCapabilities(network) ?: return@let false
                // VALIDATED is Android's best-effort probe, not a guarantee that this app's
                // server is unreachable. VPNs and networks during validation can still work.
                // A captive portal is not usable internet until its sign-in flow completes.
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
            }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { handler.post { update() } }
            override fun onLost(network: Network) { handler.post { update() } }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { handler.post { update() } }
        }
        update()
        val registered = manager != null && runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) update()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (registered) runCatching { manager?.unregisterNetworkCallback(callback) }
        }
    }
    return available
}
