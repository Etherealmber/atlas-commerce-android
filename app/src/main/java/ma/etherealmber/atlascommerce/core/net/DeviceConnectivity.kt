package ma.etherealmber.atlascommerce.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Reports whether Android's active default network is configured for internet access.
 * This intentionally does not require NET_CAPABILITY_VALIDATED: Android's validation probe
 * can lag behind real connectivity (or be unavailable on VPNs/custom networks).
 */
fun hasDeviceInternetCapability(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return@runCatching false
    val network = manager.activeNetwork ?: return@runCatching false
    manager.getNetworkCapabilities(network)?.let { capabilities ->
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
    } == true
}.getOrDefault(false)
